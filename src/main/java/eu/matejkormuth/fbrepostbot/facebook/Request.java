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

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Request {
    private final String method;
    private final String accessToken;
    private String url = "";
    private String data = "";

    Request(String method, String accessToken) {
        this.method = method;
        this.accessToken = accessToken;
    }

    public Request url(String url) {
        this.url = FacebookAPI.API_URL + url;
        return this;
    }

    public Request data(String key, String value) {
        if(data.isEmpty()) {
            data = key + "=" + value;
        } else {
            data += "&" + key + "=" + value;
        }
        return this;
    }

    private void appendAccessToken() {
        if(url.contains("?")) {
            url += "&access_token=" + accessToken;
        } else {
            url += "?access_token=" + accessToken;
        }
    }

    public JSONObject send() throws FacebookException {
        try {
            // Append access token to URL.
            this.appendAccessToken();

            URL url = new URL(this.url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(this.method);
            connection.connect();

            // Write POST data.
            if(!data.isEmpty()) {
                connection.getOutputStream().write(data.getBytes(Charsets.UTF_8));
            }

            // Read response.
            String response = CharStreams.toString(new InputStreamReader(connection.getInputStream(), Charsets.UTF_8));
            JSONObject obj = new JSONObject(response);

            // Check for exceptions.
            if (obj.has("error")) {
                JSONObject error = obj.getJSONObject("error");

                throw new FacebookException("API Exception: " + error.getString("type") + ": " + error.getString("message"));
            }

            // Return result object.
            return obj;

        } catch (Exception e) {
            throw new FacebookException("Nested exception: ", e);
        }
    }

}
