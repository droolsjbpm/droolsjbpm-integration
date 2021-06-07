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

package org.kie.server.controller.websocket.common.handlers;

import java.util.function.Consumer;

import jakarta.websocket.MessageHandler;
import org.kie.server.controller.api.model.notification.KieServerControllerNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieServerControllerNotificationMessageHandler implements MessageHandler.Whole<KieServerControllerNotification> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KieServerControllerNotificationMessageHandler.class);

    private Consumer<KieServerControllerNotification> notificationConsumer;

    public KieServerControllerNotificationMessageHandler(final Consumer<KieServerControllerNotification> notificationConsumer) {
        this.notificationConsumer = notificationConsumer;
    }

    @Override
    public void onMessage(final KieServerControllerNotification message) {
        LOGGER.debug("Kie Server Controller Notification Message received: {}",
                     message);
        if (notificationConsumer != null) {
            notificationConsumer.accept(message);
        }
    }
}
