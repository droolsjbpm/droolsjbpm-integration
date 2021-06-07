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

package org.kie.server.controller.websocket.common.config;

import java.util.Arrays;
import java.util.List;

import jakarta.websocket.Decoder;
import jakarta.websocket.Encoder;

public interface WebSocketClientConfiguration {

    static Builder builder() {
        return new Builder();
    }

    String getControllerUrl();

    String getUserName();

    String getPassword();

    String getToken();

    Long getMaxSessionIdleTimeout();

    Long getAsyncSendTimeout();

    List<Class<? extends Encoder>> getEncoders();

    List<Class<? extends Decoder>> getDecoders();

    class Builder {

        private WebSocketClientConfigurationImpl config = new WebSocketClientConfigurationImpl();

        public Builder controllerUrl(final String controllerUrl) {
            config.setControllerUrl(controllerUrl);
            return this;
        }

        public Builder userName(final String userName) {
            config.setUserName(userName);
            return this;
        }

        public Builder token(final String token) {
            config.setToken(token);
            return this;
        }

        public Builder password(final String password) {
            config.setPassword(password);
            return this;
        }

        public Builder setMaxSessionIdleTimeout(final Long maxSessionIdleTimeout) {
            config.setMaxSessionIdleTimeout(maxSessionIdleTimeout);
            return this;
        }

        public Builder setAsyncSendTimeout(final Long asyncSendTimeout) {
            config.setAsyncSendTimeout(asyncSendTimeout);
            return this;
        }

        public Builder encoders(final Class<? extends Encoder>... encoders) {
            config.setEncoders(Arrays.asList(encoders));
            return this;
        }

        public Builder decoders(final Class<? extends Decoder>... decoders) {
            config.setDecoders(Arrays.asList(decoders));
            return this;
        }

        public WebSocketClientConfiguration build() {
            return config;
        }
    }
}
