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

import com.google.common.base.Preconditions;
import eu.matejkormuth.fbrepostbot.facebook.FacebookPost;
import org.json.JSONObject;

public class CachedPost implements JsonSerializable {
    private FacebookPost originalPost;
    private long sourcePageId;
    private long targetPageId;
    private boolean published = false;

    public CachedPost() {
    }

    public CachedPost(FacebookPost originalPost, SourcePage sourcePage, TargetPage targetPage, boolean published) {
        Preconditions.checkNotNull(originalPost);

        this.originalPost = originalPost;
        if(sourcePage != null) {
            this.sourcePageId = sourcePage.getId();
        }
        if(targetPage != null) {
            this.targetPageId = targetPage.getId();
        }
        this.published = published;
    }

    public void setSourcePage(SourcePage sourcePage) {
        this.sourcePageId = sourcePage.getId();
    }

    public void setTargetPage(TargetPage targetPage) {
        this.targetPageId = targetPage.getId();
    }

    public FacebookPost getOriginalPost() {
        return originalPost;
    }

    public long getSourcePageId() {
        return sourcePageId;
    }

    public long getTargetPageId() {
        return targetPageId;
    }

    public boolean isPublished() {
        return published;
    }

    @Override
    public String serialize() {
        JSONObject obj = new JSONObject();

        obj.put("id", originalPost.getId());
        obj.put("message", originalPost.getMessage());
        obj.put("type", originalPost.getType().name());
        obj.put("objectId", originalPost.getObjectId());
        obj.put("requestedDetails", originalPost.isRequestedDetails());

        obj.put("sourcePageId", sourcePageId);
        obj.put("targetPageId", targetPageId);
        obj.put("published", published);

        return obj.toString(2);
    }

    @Override
    public void deserialize(String contents) {
        JSONObject obj = new JSONObject(contents);

        FacebookPost post = new FacebookPost();
        post.setId(obj.getString("id"));
        post.setMessage(obj.getString("message"));
        post.setType(PostType.valueOf(obj.getString("type")));
        post.setObjectId(obj.getLong("objectId"));
        post.setRequestedDetails(obj.getBoolean("requestedDetails"));

        this.originalPost = post;
        this.published = obj.getBoolean("published");
        this.sourcePageId = obj.getLong("sourcePageId");
        this.targetPageId = obj.getLong("targetPageId");
    }
}
