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

import com.google.common.eventbus.EventBus;
import eu.matejkormuth.fbrepostbot.events.IncomingPostEvent;
import eu.matejkormuth.fbrepostbot.facebook.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SourcePage {

    private static final Logger log = LoggerFactory.getLogger(SourcePage.class);

    private final FacebookAPI api;
    private final EventBus eventBus;
    private final FacebookPage page;
    private final FeedFetcher feedFetcher;
    private final PageCache cache;
    private final PathHelper pathHelper;
    private final AccessToken pageAccessToken;

    public SourcePage(FacebookAPI api, EventBus eventBus, FacebookPage page, PageCache pageCache, PathHelper pathHelper, AccessToken pageAccessToken) {
        this.eventBus = eventBus;
        this.api = api;
        this.page = page;
        this.cache = pageCache;
        this.pathHelper = pathHelper;
        this.feedFetcher = new FeedFetcher(api, page, pageAccessToken);
        this.pageAccessToken = pageAccessToken;
    }

    public void check() {
        int limit = 10;

        log.info("Fetching last {} posts from page {}.", limit, page.getUsername());
        // Fetch all posts and offer them to cache.
        try {
            this.feedFetcher
                    .fetch(limit)
                    .stream() // TODO: Check whether this could be replaced with parallel stream.
                    .forEach(this::offerToCache);
        } catch (FacebookException e) {
            log.error("Can't fetch last " + limit + " posts from page " + page.getUsername(), e);
        }
    }

    private void offerToCache(FacebookPost post) {
        if (!this.cache.contains(post)) {
            // Request additional details about post if it's new.
            if (!post.hasDetails()) {
                try {
                    post.fetchDetails(api, pageAccessToken);
                } catch (FacebookException e) {
                    log.error("Can't fetch details about post " + post.getId() + " of page " + page.getUsername() + "!", e);
                    return;
                }


                // Also fetch attachment, if post has one.
                if (post.hasAttachment()) {
                    try {
                        // Fetch and save (cache) attachment.
                        post.fetchAttachment(pathHelper, api, pageAccessToken);
                    } catch (FacebookException e) {
                        log.error("Can't fetch attachment of post " + post.getId() + " of page " + page.getUsername() + "!", e);
                        return;
                    }
                }
            }

            // Add FacebookPost to cache.
            try {
                CachedPost cachedPost = this.cache.add(post);
                cachedPost.setSourcePage(this);
                log.info("Post {} from page {} cached successfully!", post.getId(), page.getUsername());

                // Dispatch event about this post.
                eventBus.post(new IncomingPostEvent(cachedPost));
            } catch (IOException e) {
                log.error("Can't save (cache) post " + post.getId() + " from page " + page.getUsername(), e);
            }
        }
    }

    public String getUsername() {
        return this.page.getUsername();
    }

    public long getId() {
        return this.page.getId();
    }

    public long getCheckInterval() {
        return page.getCheckInterval();
    }
}
