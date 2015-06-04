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

import eu.matejkormuth.fbrepostbot.facebook.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeedPublisher {

    private static final Logger log = LoggerFactory.getLogger(FeedPublisher.class);

    private final FacebookPage targetPage;
    private final FacebookAPI api;
    private final AccessToken pageAccessToken;

    public FeedPublisher(FacebookAPI api, FacebookPage targetPage, AccessToken pageAccessToken) {
        this.api = api;
        this.targetPage = targetPage;
        this.pageAccessToken = pageAccessToken;
    }

    public void publish(PathHelper pathHelper, FacebookPost facebookPost) throws FacebookException {
        // Different ways of publishing for different post types.

        if (facebookPost.getType() == PostType.PHOTO) {
            // TODO: Add support for uploading photos directly to facebook.
            publishUploaded(pathHelper, facebookPost);
        } else {
            log.warn("Post {} has unsupported post type.", facebookPost.getId());
        }
    }

    private void publishUploaded(PathHelper pathHelper, FacebookPost facebookPost) throws FacebookException {
        // Create status with photo.
        String publicPhotoUrl = pathHelper.getPublicUrl(
                PathHelper.NamingConventions.getPhotoName(facebookPost.getObjectId()));

        log.info("Public path to photo: {}", publicPhotoUrl);

        // TODO: Make a way to override post message.
        String message = facebookPost.getMessage();

        JSONObject result = api
                .createPostRequest(pageAccessToken)
                .url(targetPage.getId() + "/photos")
                .data("url", publicPhotoUrl)
                .data("message", message)
                .send();

        // Make something with result...
    }
}
