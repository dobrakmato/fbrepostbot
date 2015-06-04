/**
 * Facebook Re-post Bot - Bot that checks some pages and copies posts to other pages.
 * Copyright (c) 2015, Matej Kormuth <http://www.github.com/dobrakmato>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package eu.matejkormuth.fbrepostbot;

import com.google.common.base.Charsets;
import com.google.common.eventbus.EventBus;
import eu.matejkormuth.fbrepostbot.facebook.AccessToken;
import eu.matejkormuth.fbrepostbot.facebook.FacebookAPI;
import eu.matejkormuth.fbrepostbot.facebook.FacebookException;
import eu.matejkormuth.fbrepostbot.facebook.FacebookPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Bootstrap {

    private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args) {
        log.info("Application started at {}!", new SimpleDateFormat().format(new Date()));
        new Bootstrap().boot();
    }

    private Scheduler scheduler;
    private FacebookAPI facebookAPI;
    private EventBus eventBus;
    private PathHelper pathHelper;
    private PageRegistry pageRegistry;

    private static final String MAIN_CONF = "main.conf";
    private static final String SEPARATOR = "->";
    private static final long DEFAULT_CHECK_INTERVAL = 5 * 60;

    private void shutdown() {
        // TODO: Add way of safe shutdown.
        log.info("Safe shutdown not yet supported!");
        log.info("Terminating...");
        System.exit(0);
    }

    private void boot() {
        log.info("Booting up...");

        // Create objects.
        scheduler = new Scheduler();
        eventBus = new EventBus();
        pageRegistry = new PageRegistry();

        // Load access key and connect to facebook.
        try {
            // Load main conf and initialize PathHelper.
            Properties mainConf = initPathHelperAndMainConf();

            // Init facebook api from access config.
            initFacebookAPI(mainConf);

            // Resolve repost mappings.
            List<String> lines = Files.readAllLines(pathHelper.getRepostConfPath());
            Set<Long> targetPageIds = new HashSet<>();
            Map<Long, Set<Long>> filterMappings = new HashMap<>();

            // First pass: gather unique pages, build mappings.
            initRepostFirstPass(lines, targetPageIds, filterMappings);

            // Finish FacebookAPI initialization by acquiring all page access tokens.
            fetchPageAccessTokens(targetPageIds);

            // Second pass: build objects.
            initRepostSecondPass(lines, filterMappings);

            // Set up scheduling.
            for (SourcePage page : pageRegistry.getSourcePages()) {
                // When an exception is thrown in scheduled task it will not be propagated unless we
                // explicitly call .get() method on ScheduledFuture. The whole execution will stop without
                // any message. So we call get() to make ScheduledExecutorService propagate exceptions from
                // task to main thread and stop application.
                scheduler.periodic(page::check, page.getCheckInterval(), TimeUnit.SECONDS).get();
            }
        } catch (Exception e) {
            log.info("Exception occurred during initialization: ", e);
            System.exit(1);
        }
    }

    private void initRepostSecondPass(List<String> lines, Map<Long, Set<Long>> filterMappings)
            throws IOException, FacebookException {
        for (String line : lines) {
            long sourcePageId = Long.valueOf(line.split(SEPARATOR)[0].trim());
            long targetPageId = Long.valueOf(line.split(SEPARATOR)[1].trim());

            // Load source page if not loaded.
            initSourcePage(sourcePageId);

            // Load target page if not loaded.
            initTargetPage(filterMappings, targetPageId);
        }
    }

    private void initTargetPage(Map<Long, Set<Long>> filterMappings, long targetPageId)
            throws IOException, FacebookException {
        if (!pageRegistry.containsTargetPage(targetPageId)) {
            FacebookPage facebookPage;
            // Try to load, if not found, create with default check interval.
            facebookPage = loadTargetFacebookPage(targetPageId);

            // Create new cache for this page.
            PageCache pageCache = new PageCache(facebookPage, pathHelper);
            // Create filter.
            PostFilter pageFilter = new PostFilter(filterMappings.get(targetPageId));
            AccessToken pageAccessToken = facebookAPI.getPageAccessToken(targetPageId);

            TargetPage page = new TargetPage(facebookAPI, eventBus, facebookPage, pageCache, pageFilter,
                    pathHelper, pageAccessToken);
            pageRegistry.add(page);
        }
    }

    private FacebookPage loadTargetFacebookPage(long targetPageId) throws IOException, FacebookException {
        FacebookPage facebookPage;
        if (Files.exists(pathHelper.getTargetPageJsonPath(targetPageId))) {
            log.info("Loading page {}...", targetPageId);
            String pageJson = new String(Files.readAllBytes(pathHelper.getTargetPageJsonPath(targetPageId)),
                    Charsets.UTF_8);

            // Build facebookPage object from cache.
            facebookPage = new FacebookPage();
            facebookPage.deserialize(pageJson);

        } else {
            // Create new page.
            log.info("Creating new page with id {} and default check interval.", targetPageId);

            facebookPage = new FacebookPage(targetPageId, null, DEFAULT_CHECK_INTERVAL);
            facebookPage.fetchDetails(facebookAPI);
            Files.createDirectories(pathHelper.getTargetPageJsonPath(targetPageId).getParent());
            Files.write(pathHelper.getTargetPageJsonPath(targetPageId),
                    facebookPage.serialize().getBytes(Charsets.UTF_8));
        }
        return facebookPage;
    }

    private void initSourcePage(long sourcePageId) throws IOException, FacebookException {
        if (!pageRegistry.containsSourcePage(sourcePageId)) {
            FacebookPage facebookPage;
            // Try to load, if not found, create with default check interval.
            facebookPage = loadSourceFacebookPage(sourcePageId);

            // Create new cache for this page.
            PageCache pageCache = new PageCache(facebookPage, pathHelper);

            // We use main access token for source pages.
            // There are no special permissions needed to be able to read their stream.
            AccessToken pageAccessToken = facebookAPI.getMainAccessToken();

            SourcePage page = new SourcePage(facebookAPI, eventBus, facebookPage, pageCache,
                    pathHelper, pageAccessToken);
            pageRegistry.add(page);
        }
    }

    private FacebookPage loadSourceFacebookPage(long sourcePageId) throws IOException, FacebookException {
        FacebookPage facebookPage;
        if (Files.exists(pathHelper.getSourcePageJsonPath(sourcePageId))) {
            log.info("Loading page {}...", sourcePageId);
            String pageJson = new String(Files.readAllBytes(pathHelper.getSourcePageJsonPath(sourcePageId)),
                    Charsets.UTF_8);

            // Build facebookPage object from cache.
            facebookPage = new FacebookPage();
            facebookPage.deserialize(pageJson);
        } else {
            // Create new page.
            log.info("Creating new page with id {} and default check interval.", sourcePageId);

            facebookPage = new FacebookPage(sourcePageId, null, DEFAULT_CHECK_INTERVAL);
            facebookPage.fetchDetails(facebookAPI);
            // Save created page file.
            Files.createDirectories(pathHelper.getSourcePageJsonPath(sourcePageId).getParent());
            Files.write(pathHelper.getSourcePageJsonPath(sourcePageId),
                    facebookPage.serialize().getBytes(Charsets.UTF_8));
        }
        return facebookPage;
    }

    private void fetchPageAccessTokens(Set<Long> targetPageIds) {
        try {
            facebookAPI.fetchPageAccessTokens(targetPageIds);
        } catch (FacebookException e) {
            log.error("Failed to fetch page access tokens!", e);
            System.exit(3);
        }
    }

    private void initRepostFirstPass(List<String> lines, Set<Long> targetPageIds,
                                     Map<Long, Set<Long>> filterMappings) {
        try {
            for (String line : lines) {
                // {source-id} -> {target-page}
                long sourcePageId = Long.valueOf(line.split(SEPARATOR)[0].trim());
                long targetPageId = Long.valueOf(line.split(SEPARATOR)[1].trim());

                // Check for cyclic mapping.
                if (sourcePageId == targetPageId) {
                    throw new IllegalStateException("Source page can't be the same page as target page!");
                }

                // Add target page to targets set.
                targetPageIds.add(targetPageId);
                // Add to mapping.
                if (filterMappings.containsKey(targetPageId)) {
                    filterMappings.get(targetPageId).add(sourcePageId);
                } else {
                    Set<Long> set = new HashSet<>();
                    set.add(sourcePageId);
                    filterMappings.put(targetPageId, set);
                }
            }
        } catch (Exception e) {
            log.error("Can't parse repost config.", e);
        }
    }

    private void initFacebookAPI(Properties mainConf) throws IOException {
        Properties accessConf = new Properties();
        accessConf.load(new FileInputStream(pathHelper.getAccessConfPath().toFile()));

        if (accessConf.getProperty("accessToken", "invalid").equals("invalid")) {
            log.error("accessToken must be set in access config!");
            System.exit(1);
        }

        String accessToken = accessConf.getProperty("accessToken");

        try {
            facebookAPI = new FacebookAPI(accessToken);
        } catch (FacebookException e) {
            log.info("Can't create FacebookAPI (log in to facebook)!", e);
            System.exit(2);
        }
    }

    private Properties initPathHelperAndMainConf() throws IOException {
        Properties mainConf = new Properties();
        mainConf.load(new FileInputStream(new File("./" + MAIN_CONF)));

        if (mainConf.getProperty("dataPath", "invalid").equals("invalid")) {
            log.error("dataPath must be set in " + MAIN_CONF + "!");
            System.exit(1);
        }

        if (mainConf.getProperty("publicPath", "invalid").equals("invalid")) {
            log.error("publicPath must be set in " + MAIN_CONF + "!");
            System.exit(1);
        }

        if (mainConf.getProperty("publicPathUrl", "invalid").equals("invalid")) {
            log.error("publicPathUrl must be set in " + MAIN_CONF + "!");
            System.exit(1);
        }

        Path dataPath = Paths.get(mainConf.getProperty("dataPath"));
        Path publicPath = Paths.get(mainConf.getProperty("publicPath"));
        String publicPathUrl = mainConf.getProperty("publicPathUrl");

        pathHelper = new PathHelper(dataPath, publicPath, publicPathUrl);
        return mainConf;
    }
}
