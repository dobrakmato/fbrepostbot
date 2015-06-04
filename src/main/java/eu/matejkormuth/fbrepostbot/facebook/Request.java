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
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Request {
    private static final Logger log = LoggerFactory.getLogger(Request.class);
    private final String method;
    private final String accessToken;
    private String url = "";
    private List<NameValuePair> postData;

    Request(String method, String accessToken) {
        this.method = method;
        this.accessToken = accessToken;
    }

    public Request url(String url) {
        this.url = FacebookAPI.API_URL + url;
        return this;
    }

    public Request data(String key, String value) {
        if (this.postData == null) {
            this.postData = new ArrayList<>();
        }
        this.postData.add(new BasicNameValuePair(key, value));
        return this;
    }

    private void appendAccessToken() {
        if (url.contains("?")) {
            url += "&access_token=" + accessToken;
        } else {
            url += "?access_token=" + accessToken;
        }
    }

    public JSONObject send() throws FacebookException {
        try {
            // Append access token to URL.
            this.appendAccessToken();

            // Create HttpClient.
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpRequestBase request;
            CloseableHttpResponse response = null;
            // Create request.
            if (this.method == "GET") {
                request = new HttpGet(this.url);
            } else if (this.method == "POST") {
                request = new HttpPost(this.url);
                ((HttpPost) request).setEntity(new UrlEncodedFormEntity(this.postData, Charsets.UTF_8));
            } else {
                throw new IllegalStateException("Unsupported method " + this.method);
            }

            // Read response.
            try {
                response = httpclient.execute(request);
                HttpEntity entity = response.getEntity();

                String responseString = CharStreams.toString(new InputStreamReader(entity.getContent(),
                        Charsets.UTF_8));
                JSONObject obj = new JSONObject(responseString);

                // Check for errors.
                if (obj.has("error")) {
                    JSONObject error = obj.getJSONObject("error");
                    throw new FacebookException("API Exception: " + error.getString("type") + ": "
                            + error.getString("message"));
                }

                // Ensure the entity is fully consumed.
                EntityUtils.consume(entity);

                return obj;
            } finally {
                httpclient.close();
                if (response != null) {
                    response.close();
                }
            }
        } catch (Exception e) {
            throw new FacebookException("Nested exception: ", e);
        }
    }

}
