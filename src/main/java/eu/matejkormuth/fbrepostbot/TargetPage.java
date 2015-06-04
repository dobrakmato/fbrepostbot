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
import com.google.common.eventbus.Subscribe;
import eu.matejkormuth.fbrepostbot.events.IncomingPostEvent;
import eu.matejkormuth.fbrepostbot.events.OutgoingPostEvent;
import eu.matejkormuth.fbrepostbot.facebook.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TargetPage {

    private static final Logger log = LoggerFactory.getLogger(TargetPage.class);

    private final EventBus eventBus;
    private final FacebookPage page;
    private final FeedPublisher feedPublisher;
    private final PageCache cache;
    private final PostFilter filter;
    private final FacebookPostFactory facebookPostFactory;
    private final PathHelper pathHelper;

    public TargetPage(FacebookAPI api, EventBus eventBus, FacebookPage page, PageCache cache, PostFilter filter,
                      PathHelper pathHelper, AccessToken pageAccessToken) {
        this.eventBus = eventBus;
        this.page = page;
        this.cache = cache;
        this.filter = filter;
        this.pathHelper = pathHelper;
        this.feedPublisher = new FeedPublisher(api, page, pageAccessToken);
        // TODO: Add more configuration to PostFactory.
        this.facebookPostFactory = new FacebookPostFactory();

        // Register listener.
        log.info("Registering events for target page {}...", this.getId());
        this.eventBus.register(this);
    }

    @Subscribe
    public void incomingPostListener(final IncomingPostEvent event) {
        // Filter out posts from source pages we do not subscribe.
        if (filter.isRelevant(event.getPost())) {
            log.info("Publishing post {} to page {}...", event.getPost().getOriginalPost().getId(), this.page.getUsername());
            FacebookPost facebookPost = facebookPostFactory.create(event.getPost());
            try {
                feedPublisher.publish(pathHelper, facebookPost);
                log.info("Published post {} to page {}",
                        event.getPost().getOriginalPost().getId(),
                        this.page.getUsername());

                // Create new cached post in local cache.
                try {
                    CachedPost cachedPost = this.cache.add(facebookPost, this);
                    cachedPost.setTargetPage(this);

                    // Dispatch new OutgoingPostEvent
                    eventBus.post(new OutgoingPostEvent(cachedPost));
                } catch (IOException e) {
                    log.error("Can't save (cache) post " + event.getPost().getOriginalPost().getId(), e);
                }
            } catch (FacebookException e) {
                log.info("Can't publish post " + event.getPost().getOriginalPost().getId() + " from page " +
                        event.getPost().getSourcePageId() + " to page " + this.page.getUsername(), e);
            }
        }
    }

    public long getId() {
        return this.page.getId();
    }
}
