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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.websocket.EncodeException;
import jakarta.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketNotificationSessionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketNotificationSessionManager.class);
    private static final int RETRIES = 20;
    private static final WebSocketNotificationSessionManager INSTANCE = new WebSocketNotificationSessionManager();

    private final ConcurrentMap<Session, ExecutorService> executorsBySession = new ConcurrentHashMap<>();

    public static WebSocketNotificationSessionManager getInstance() {
        return INSTANCE;
    }

    protected Map<Session, ExecutorService> getExecutorsBySession() {
        return executorsBySession;
    }

    public void addSession(final Session session) {
        executorsBySession.put(session,
                               Executors.newSingleThreadExecutor());
        LOGGER.debug("Session '" + session.getId() + "' added to Web Socket Notification manager");
    }

    public void removeSession(final Session session) {
        final ExecutorService service = executorsBySession.remove(session);
        service.shutdownNow();
        LOGGER.debug("Session '" + session.getId() + "' removed from Web Socket Notification manager");
    }

    public void broadcastObject(final Object object) {
        executorsBySession.forEach((session, executor) -> executor.submit(() -> {
            for (int i = 0; i < RETRIES; i++) {
                LOGGER.debug("Sending notification to session with id: {}, open: {}",
                             session.getId(),
                             session.isOpen());
                try {
                    if (session.isOpen()) {
                        session.getBasicRemote().sendObject(object);
                        LOGGER.debug("Notification sent to session with id: {}",
                                     session.getId());
                    } else {
                        LOGGER.debug("Notification not sent, session is closed.");
                    }
                    break;
                } catch (EncodeException ex) {
                    LOGGER.warn("Failed to send notification, due to encoding error: {}, skipping message",
                                ex.getMessage(),
                                ex);
                    break;
                } catch (IOException ex) {
                    LOGGER.warn("Failed to send notification, error: {}",
                                ex.getMessage(),
                                ex);
                    Thread.sleep(i * 500);
                }
            }
            return null;
        }));
    }
}
