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
package eu.matejkormuth.fbrepostbot.facebook;

import eu.matejkormuth.fbrepostbot.PathHelper;
import eu.matejkormuth.fbrepostbot.PostType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FacebookPost {

    private static final Logger log = LoggerFactory.getLogger(FacebookPost.class);

    private String id = "";
    private PostType type;
    private String message;
    private long objectId;
    private boolean requestedDetails = false;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PostType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public long getObjectId() {
        return objectId;
    }

    public boolean isRequestedDetails() {
        return requestedDetails;
    }

    public void setType(PostType type) {
        this.type = type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }

    public void setRequestedDetails(boolean requestedDetails) {
        this.requestedDetails = requestedDetails;
    }

    public void fetchDetails(FacebookAPI api, AccessToken pageAccessToken) throws FacebookException {
        if (this.id.isEmpty()) {
            throw new IllegalStateException("To fetch post details, the post must have its id.");
        }

        JSONObject postDetails = api
                .createGetRequest(pageAccessToken)
                .url(this.id + "?fields=type,message,status_type,object_id")
                .send();

        if (postDetails.has("message")) {
            this.message = postDetails.getString("message");
        }

        if (postDetails.has("object_id")) {
            this.objectId = postDetails.getLong("object_id");
        }
        this.type = PostType.byFacebookType(postDetails.getString("type"));
        this.requestedDetails = true;
    }

    public boolean hasDetails() {
        return requestedDetails;
    }

    public boolean hasAttachment() {
        return objectId != 0;
    }

    public void fetchAttachment(PathHelper pathHelper, FacebookAPI api, AccessToken pageAccessToken)
            throws FacebookException {
        if (this.id.isEmpty()) {
            throw new IllegalStateException("To fetch post details, the post must have its id.");
        }

        if (this.objectId == 0) {
            throw new IllegalStateException("To fetch attachment, the post must have attachment.");
        }

        // Fetch details about attachement.
        JSONObject postAttachment = api
                .createGetRequest(pageAccessToken)
                .url(this.objectId + "?fields=source")
                .send();

        // Process attachment by it's type.
        switch (this.type) {
            case PHOTO:
                downloadPhoto(pathHelper, postAttachment);
                break;
        }
    }

    private void downloadPhoto(PathHelper pathHelper, JSONObject postAttachment) throws FacebookException {
        String photoUrl = postAttachment.getString("source");
        Path targetFile = pathHelper.getPublicPath(PathHelper.NamingConventions.getPhotoName(this.objectId));

        // Download photo.
        try {
            log.info("Downloading post attachment to {}.jpg...", this.objectId);
            URL website = new URL(photoUrl);
            Files.createDirectories(targetFile.getParent());
            Files.copy(website.openStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new FacebookException("Error during photo download: ", e);
        }
    }
}
