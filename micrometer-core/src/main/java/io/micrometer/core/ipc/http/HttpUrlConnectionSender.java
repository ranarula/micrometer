/**
 * Copyright 2018 Pivotal Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.core.ipc.http;

import io.micrometer.core.instrument.util.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.Map;

/**
 * {@link HttpURLConnection}-based {@link HttpSender}.
 *
 * @author Jon Schneider
 * @author Johnny Lim
 */
public class HttpUrlConnectionSender implements HttpSender {
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public HttpUrlConnectionSender(Duration connectTimeout, Duration readTimeout) {
        this.connectTimeoutMs = (int) connectTimeout.toMillis();
        this.readTimeoutMs = (int) readTimeout.toMillis();
    }

    public HttpUrlConnectionSender() {
        this.connectTimeoutMs = 1000;
        this.readTimeoutMs = 10000;
    }

    @Override
    public Response send(Request request) throws IOException {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) request.getUrl().openConnection();
            con.setConnectTimeout(connectTimeoutMs);
            con.setReadTimeout(readTimeoutMs);
            Method method = request.getMethod();
            con.setRequestMethod(method.name());

            for (Map.Entry<String, String> header : request.getRequestHeaders().entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }

            if (method != Method.GET) {
                con.setDoOutput(true);
                try (OutputStream os = con.getOutputStream()) {
                    os.write(request.getEntity());
                    os.flush();
                }
            }

            int status = con.getResponseCode();

            String body = null;
            try {
                if (con.getErrorStream() != null) {
                    body = IOUtils.toString(con.getErrorStream());
                } else if (con.getInputStream() != null) {
                    body = IOUtils.toString(con.getInputStream());
                }
            } catch (IOException ignored) {
            }

            return new Response(status, body);
        } finally {
            try {
                if (con != null) {
                    con.disconnect();
                }
            } catch (Exception ignore) {
            }
        }
    }
}
