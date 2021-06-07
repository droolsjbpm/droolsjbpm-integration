/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.server.controller.websocket;

import jakarta.websocket.Session;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.controller.api.model.KieServerSetup;
import org.kie.server.controller.api.model.events.ServerInstanceConnected;
import org.kie.server.controller.api.model.events.ServerInstanceUpdated;
import org.kie.server.controller.api.model.runtime.ServerInstance;
import org.kie.server.controller.api.model.runtime.ServerInstanceKey;
import org.kie.server.controller.api.model.spec.ServerTemplate;
import org.kie.server.controller.impl.KieServerControllerImpl;
import org.kie.server.controller.websocket.common.WebSocketUtils;
import org.kie.server.controller.websocket.common.handlers.InternalMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConnectedKieServerHandler implements InternalMessageHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectedKieServerHandler.class);

    private WebSocketSessionManager manager;
    private Session session;
    private KieServerControllerImpl controller;
    private String serverId;    
    
    private KieServerInfo serverInfo;
    
    public ConnectedKieServerHandler(WebSocketSessionManager manager, Session session, KieServerControllerImpl controller, String serverId) {
        super();
        this.manager = manager;
        this.session = session;
        this.controller = controller;
        this.serverId = serverId;
    }

    @Override
    public String onMessage(String message) {
        serverInfo = WebSocketUtils.unmarshal(message, KieServerInfo.class);

        logger.debug("Server info {}", serverInfo);
        KieServerSetup serverSetup = controller.connect(serverInfo);

        if (serverSetup.hasNoErrors()) {
            manager.addSession(serverInfo, session);
            logger.info("Server with id '{}' connected", serverId);
        } else {
            logger.warn("Server with id '{}' failed to connect", serverId);
        }

        return WebSocketUtils.marshal(serverSetup);
    }

    @Override
    public void afterResponseSent() {
        
        ServerTemplate serverTemplate = controller.getTemplateStorage().load(serverInfo.getServerId());
        
        ServerInstanceKey serverInstanceKey = serverTemplate.getServerInstanceKeys().stream()
                .filter(server -> server.getUrl().equals(serverInfo.getLocation()))
                .findFirst()
                .get();
        ServerInstance serverInstance = new ServerInstance();
        serverInstance.setServerName(serverInstanceKey.getServerName());
        serverInstance.setServerTemplateId(serverInstanceKey.getServerTemplateId());
        serverInstance.setServerInstanceId(serverInstanceKey.getServerInstanceId());
        serverInstance.setUrl(serverInstanceKey.getUrl());
        
        controller.getNotificationService().notify(new ServerInstanceUpdated(serverInstance));
        controller.getNotificationService().notify(new ServerInstanceConnected(serverInstance));
    }

}
