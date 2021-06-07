/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.controller.websocket.notification;

import javax.annotation.PostConstruct;

import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.kie.server.controller.websocket.common.encoder.KieServerControllerNotificationEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerEndpoint(value = "/websocket/controller/notification",
        encoders = KieServerControllerNotificationEncoder.class)
public class WebSocketKieServerControllerNotification {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketKieServerControllerNotification.class);

    private WebSocketNotificationSessionManager manager = WebSocketNotificationSessionManager.getInstance();

    @PostConstruct
    public void configure() {
        LOGGER.info("Kie Server Controller WebSocket Notification service initialized");
    }

    @OnOpen
    public void onNotificationClientConnect(final Session session) {
        LOGGER.debug("New Web Socket Notification Client session: {}",
                     session.getId());
        manager.addSession(session);
    }

    @OnClose
    public void onNotificationClientDisconnect(final Session session,
                                             final CloseReason closeReason) {
        LOGGER.debug("Web Socket Notification Client session: {}, disconnected, reason: {}",
                     session.getId(),
                     closeReason);
        manager.removeSession(session);
    }

    @OnError
    public void onNotificationClientError(final Session session,
                                        final Throwable e) {
        LOGGER.error("Web Socket Notification Client session: {}, unexpected error",
                     session.getId(),
                     e);
    }
}