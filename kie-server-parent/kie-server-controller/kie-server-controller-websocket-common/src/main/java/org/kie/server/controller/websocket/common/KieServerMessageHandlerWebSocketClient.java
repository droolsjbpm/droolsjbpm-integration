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

package org.kie.server.controller.websocket.common;

import java.io.IOException;
import java.util.function.Consumer;

import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import org.kie.server.controller.websocket.common.handlers.InternalMessageHandler;
import org.kie.server.controller.websocket.common.handlers.KieServerMessageHandler;

public class KieServerMessageHandlerWebSocketClient extends WebSocketClientImpl<KieServerMessageHandler> {

    public KieServerMessageHandlerWebSocketClient() {
        super();
    }

    public KieServerMessageHandlerWebSocketClient(final Consumer<WebSocketClient> onReconnect) {
        super(onReconnect);
    }

    public void sendTextWithInternalHandler(final String content,
                                    final InternalMessageHandler handler) throws IOException {
        super.sendTextWithHandler(content,
                                  messageHandler -> messageHandler.addHandler(handler));
    }

    @Override
    public void onOpen(final Session session,
                       final EndpointConfig config) {
        this.messageHandler = new KieServerMessageHandler(session);
        super.onOpen(session,
                     config);
    }
}
