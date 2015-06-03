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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathHelper {

    private static final Logger log = LoggerFactory.getLogger(PathHelper.class);

    private Path basePath;
    private Path publicPath;
    private String publicPathUrl;

    public PathHelper(Path basePath, Path publicPath, String publicPathUrl) {

        log.info("Initializing PathHelper...");
        log.info("Data path: {}", basePath);
        log.info("Public folder path: {}", publicPath);
        log.info("Public folder URL: {}", publicPathUrl);

        this.basePath = basePath;
        this.publicPath = publicPath;

        if (publicPathUrl.endsWith("/")) {
            // Trim last slash -> substring from index zero to index (length - 1) - 1
            this.publicPathUrl = publicPathUrl.substring(0, publicPathUrl.length() - 2);
        } else {
            this.publicPathUrl = publicPathUrl;
        }

        checkPublicPathUrlValidity();
    }

    private void checkPublicPathUrlValidity() {
        try {
            new URL(this.publicPathUrl);
        } catch (Exception e) {
            log.error("Specified public path url is not valid URL: ", e);
        }
    }

    public Path getRepostConfPath() {
        return getPath("repost.conf");
    }

    public Path getAccessConfPath() {
        return getPath("access.conf");
    }

    public Path getPageCachePath(long pageId) {
        return getPath("pages", String.valueOf(pageId));
    }

    public Path getPath(String first, String... more) {
        return basePath.resolve(_getPath(first, more));
    }

    private Path _getPath(String first, String[] more) {
        return Paths.get(first, more);
    }

    public Path getPublicPath(String first) {
        return publicPath.resolve(first);
    }

    public String getPublicUrl(String s) {
        return publicPathUrl + "/" + s;
    }

    public Path getSourcePageJsonPath(long sourcePageId) {
        return getPath("pages", String.valueOf(sourcePageId), "page.json");
    }

    public Path getTargetPageJsonPath(long targetPageId) {
        return getPath("pages", String.valueOf(targetPageId), "page.json");
    }

    public static class NamingConventions {
        public static String getPhotoName(long objectId) {
            return objectId + ".jpg";
        }
    }
}
