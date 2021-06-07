/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.server.controller.websocket.common.auth;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import jakarta.websocket.ClientEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketAuthConfigurator extends ClientEndpointConfig.Configurator {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketAuthConfigurator.class);
    private static final String AUTHORIZATION = "Authorization";

    private final String userName;
    private final String password;
    private final String token;

    public WebSocketAuthConfigurator(final String userName,
                                     final String password,
                                     final String token) {
        this.userName = userName;
        this.password = password;
        this.token = token;
    }

    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
        super.beforeRequest(headers);

        if (token != null && !token.isEmpty()) {
            headers.put(AUTHORIZATION,
                        Arrays.asList("Bearer " + token));
        } else {
            try {
                headers.put(AUTHORIZATION,
                            Arrays.asList("Basic " + Base64.getEncoder().encodeToString((userName + ':' + password).getBytes("UTF-8"))));
            } catch (UnsupportedEncodingException e) {
                LOGGER.warn(e.getMessage());
            }
        }
    }
}
