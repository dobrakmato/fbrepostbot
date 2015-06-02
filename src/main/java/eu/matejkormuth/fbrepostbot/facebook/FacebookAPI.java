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

import eu.matejkormuth.fbrepostbot.PageRegistry;
import eu.matejkormuth.fbrepostbot.TargetPage;

import java.util.Collection;

public class FacebookAPI {
    public static final String API_URL = "https://graph.facebook.com/";

    private String accessToken;

    public FacebookAPI(PageRegistry pageRegistry, String accessToken) {
        this.accessToken = accessToken;

        checkAccessToken();
        checkAccessTokenPermissions(pageRegistry.getTargetPages());
        scheduleAccessTokenRenewal();
    }

    private void checkAccessToken() {
        if (accessToken == null) {
            throw new IllegalArgumentException("Access token can't be null!");
        }

        if (accessToken.isEmpty()) {
            throw new IllegalArgumentException("Access token can't be empty!");
        }

        try {
            this.createGetRequest()
                    .url("debug_token")
                    .send();
        } catch (FacebookException e) {
            // Session has expired on Tuesday, 02-Jun-15 11:00:00 PDT. The current time is Tuesday, 02-Jun-15 14:13:39
            if (e.getMessage().contains("has expired")) {
                throw new IllegalArgumentException("Provided access token is invalid: " + e.getMessage());
            }
        }
    }

    private void scheduleAccessTokenRenewal() {

    }

    private void checkAccessTokenPermissions(Collection<TargetPage> targetPages) {
        // Check publish permission for each target page.
        targetPages.forEach(this::checkPublishSteamPermission);
    }

    private void checkPublishSteamPermission(TargetPage page) {

    }

    public Request createGetRequest() {
        return new Request("GET", this.accessToken);
    }

    public Request createPostRequest() {
        return new Request("POST", this.accessToken);
    }
}
