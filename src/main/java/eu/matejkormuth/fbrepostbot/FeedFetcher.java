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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FeedFetcher {

    private final FacebookPage page;
    private final FacebookAPI api;
    private final AccessToken pageAccessToken;

    public FeedFetcher(FacebookAPI api, FacebookPage page, AccessToken pageAccessToken) {
        this.api = api;
        this.page = page;
        this.pageAccessToken = pageAccessToken;
    }

    public List<FacebookPost> fetch(int limit) throws FacebookException {
        ArrayList<FacebookPost> posts = new ArrayList<>();

        // Download feed.
        JSONObject pageFeed = api
                .createGetRequest(pageAccessToken)
                .url(page.getId() + "/feed?fields=admin_creator&limit=" + limit)
                .send();

        JSONArray data = pageFeed.getJSONArray("data");
        for (int i = 0; i < data.length(); i++) {
            JSONObject postJsonObj = data.getJSONObject(i);

            FacebookPost post = new FacebookPost();
            post.setId(postJsonObj.getLong("id"));

            posts.add(post);
        }

        return posts;
    }

}
