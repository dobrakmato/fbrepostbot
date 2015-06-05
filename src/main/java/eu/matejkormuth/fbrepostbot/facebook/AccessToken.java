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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AccessToken {

    private static final Logger log = LoggerFactory.getLogger(AccessToken.class);

    private String token;
    private Type type;
    private List<String> scopes;
    private long expires;
    private long appId;
    private long userId;
    private long profileId;

    public AccessToken(String accessToken) {
        this.token = accessToken;

        if (accessToken == null) {
            throw new IllegalArgumentException("Access token can't be null!");
        }

        if (accessToken.isEmpty()) {
            throw new IllegalArgumentException("Access token can't be empty!");
        }
    }

    public void fetchDetails(AccessToken token) throws FacebookException {
        log.info("Fetching details about access token {}", token.getToken());
        JSONObject obj = null;
        try {
            obj = new Request("GET", token.getToken())
                    .url("debug_token?input_token=" + this.token)
                    .send();
        } catch (FacebookException e) {
            // Session has expired on Tuesday, 02-Jun-15 11:00:00 PDT. The current time is Tuesday, 02-Jun-15 14:13:39
            if (e.getMessage().contains("has expired")) {
                throw new FacebookException("Provided access token is invalid: " + e.getMessage());
            }

            log.error("An error occurred: ", e);
        }

        if (obj == null) {
            throw new RuntimeException("Response is null!");
        }

        JSONObject data = obj.getJSONObject("data");

        if (data.has("profile_id")) {
            this.type = Type.PAGE_TOKEN;
            this.profileId = data.getLong("profile_id");
            this.userId = data.getLong("user_id");
        } else if (data.has("user_id")) {
            this.type = Type.USER_TOKEN;
            this.userId = data.getLong("user_id");
        } else {
            this.type = Type.APPLICATION_TOKEN;
        }

        this.appId = data.getLong("app_id");
        this.expires = data.getLong("expires_at");

        log.info("This access token expires at: {}", new SimpleDateFormat().format(new Date(this.expires * 1000)));

        this.scopes = new ArrayList<>();

        JSONArray scopes = data.getJSONArray("scopes");
        for (int i = 0; i < scopes.length(); i++) {
            this.scopes.add(scopes.getString(i));
        }
    }

    public String getToken() {
        return token;
    }

    public Type getType() {
        return type;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public long getExpires() {
        return expires;
    }

    public long getAppId() {
        return appId;
    }

    public long getUserId() {
        return userId;
    }

    public long getProfileId() {
        return profileId;
    }

    public boolean hasScope(String scope) {
        return this.scopes.contains(scope);
    }

    public enum Type {
        USER_TOKEN,
        APPLICATION_TOKEN,
        PAGE_TOKEN;
    }
}
