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

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FacebookAPI {
    private static final Logger log = LoggerFactory.getLogger(FacebookAPI.class);
    public static final String API_URL = "https://graph.facebook.com/";

    private AccessToken mainAccessToken;
    private Map<Long, AccessToken> pageAccessTokens;

    public FacebookAPI(String accessToken) throws FacebookException {
        this.pageAccessTokens = new HashMap<>();
        this.mainAccessToken = new AccessToken(accessToken);

        // Fetch access token details using itself.
        this.mainAccessToken.fetchDetails(mainAccessToken);

        if (this.mainAccessToken.getType() == AccessToken.Type.APPLICATION_TOKEN) {
            throw new FacebookException("Can't get access to pages with application token." +
                    "Please provide user access token with manage_pages permission!");
        }
    }

    public void fetchPageAccessTokens(Set<Long> targetPageIds) throws FacebookException {
        log.info("Fetching page access tokens valid for this user token...");
        JSONObject obj = createGetRequest(this.mainAccessToken)
                .url("me/accounts")
                .send();

        JSONArray data = obj.getJSONArray("data");
        for (int i = 0; i < data.length(); i++) {
            JSONObject account = data.getJSONObject(i);

            String pageAccessToken = account.getString("access_token");
            long pageId = account.getLong("id");
            String pageName = account.getString("name");

            log.info("Access token for page {} was found!", pageName);

            // Request details and save only tokens of pages we need to publish to.
            if (targetPageIds.contains(pageId)) {
                AccessToken accessToken = new AccessToken(pageAccessToken);
                // Fetch details about this access token.
                accessToken.fetchDetails(this.mainAccessToken);
                this.pageAccessTokens.put(pageId, accessToken);
            }
        }
    }

    public boolean hasPostPermissions(long pageId) {
        AccessToken token = pageAccessTokens.get(pageId);

        if (token == null) {
            return false;
        }

        return token.hasScope("publish_pages");
    }

    public AccessToken getPageAccessToken(long pageId) {
        return this.pageAccessTokens.get(pageId);
    }

    public Request createGetRequest(AccessToken token) {
        return new Request("GET", token.getToken());
    }

    public Request createPostRequest(AccessToken token) {
        return new Request("POST", token.getToken());
    }


    public AccessToken getMainAccessToken() {
        return mainAccessToken;
    }
}
