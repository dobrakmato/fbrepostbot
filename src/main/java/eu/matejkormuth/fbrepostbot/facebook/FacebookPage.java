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

import eu.matejkormuth.fbrepostbot.JsonSerializable;
import org.json.JSONObject;

public class FacebookPage implements JsonSerializable {
    private long id;
    private String username;
    // Check interval in seconds.
    private long checkInterval;

    public FacebookPage() {

    }

    public FacebookPage(long id, String username, long checkInterval) {
        this.id = id;
        this.username = username;
        this.checkInterval = checkInterval;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public long getCheckInterval() {
        return checkInterval;
    }

    @Override
    public String serialize() {
        JSONObject obj = new JSONObject();

        obj.put("id", this.id);
        obj.put("username", this.username);
        obj.put("checkInterval", this.checkInterval);

        return obj.toString(2);
    }

    @Override
    public void deserialize(String contents) {
        JSONObject obj = new JSONObject(contents);

        this.id = obj.getLong("id");
        this.username = obj.getString("username");
        this.checkInterval = obj.getLong("checkInterval");
    }

    public void fetchDetails(FacebookAPI api) throws FacebookException {
        JSONObject details = api.createGetRequest(api.getMainAccessToken())
                .url(this.id + "?fields=name")
                .send();

        this.username = details.getString("name");
    }
}
