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

import java.util.Collections;
import java.util.ServiceLoader;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.controller.api.model.runtime.ServerInstance;
import org.kie.server.controller.api.service.NotificationService;
import org.kie.server.controller.api.service.NotificationServiceFactory;
import org.kie.server.controller.api.service.PersistingServerTemplateStorageService;
import org.kie.server.controller.api.storage.KieServerTemplateStorage;
import org.kie.server.controller.impl.KieServerControllerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerEndpoint("/websocket/controller/{server-id}")
public class WebSocketKieServerControllerImpl extends KieServerControllerImpl {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketKieServerControllerImpl.class);
    
    private WebSocketSessionManager manager = WebSocketSessionManager.getInstance();
    
    @Inject @Any
    private Instance<KieServerTemplateStorage> templateStorage;
    
    @Inject @Any
    private Instance<NotificationService> notificationService;
    
    public WebSocketKieServerControllerImpl() {
        ServiceLoader<PersistingServerTemplateStorageService> storageServices = ServiceLoader.load(PersistingServerTemplateStorageService.class);
        if (storageServices != null && storageServices.iterator().hasNext()) {
            PersistingServerTemplateStorageService storageService = storageServices.iterator().next();
            setTemplateStorage(storageService.getTemplateStorage());
            logger.debug("Server template storage for standalone kie server controller is {}", storageService.getTemplateStorage().toString());
        } else {
            logger.debug("No server template storage defined. Default storage: InMemoryKieServerTemplateStorage will be used");
        }

        ServiceLoader<NotificationServiceFactory> notificationServiceLoader = ServiceLoader.load(NotificationServiceFactory.class);
        if (notificationServiceLoader != null && notificationServiceLoader.iterator().hasNext()) {
            final NotificationService notificationService = notificationServiceLoader.iterator().next().getNotificationService();
            this.setNotificationService(notificationService);

            logger.debug("Notification service for standalone kie server controller is {}",
                         notificationService.toString());
        } else {
            logger.warn("Notification service not defined. Default notification: LoggingNotificationService will be used");
        }
    }
    
    @PostConstruct
    public void configure() {
        if(templateStorage.isUnsatisfied()){
            logger.warn("Unable to find template storage implementation, using in memory");
        } else {
            setTemplateStorage(templateStorage.get());
        }
        if(notificationService.isUnsatisfied()) {
            logger.warn("Unable to find notification service implementation, using logging only");
        } else {
            setNotificationService(notificationService.get());
        }
    }

    
    @OnOpen
    public void onKieServerConnect(@PathParam("server-id") String serverId, Session session) {
        synchronized (manager) {
            manager.addSession(session);
            manager.getHandler(session.getId()).addHandler(new ConnectedKieServerHandler(manager, session, this, serverId));
        }
    }
    
    @OnClose
    public void onKieServerDisconnect(@PathParam("server-id") String serverId, Session session, CloseReason closeReason) {
        synchronized (manager) {
            String url = manager.removeSession(session);
            if (url != null) {
                KieServerInfo serverInfo = new KieServerInfo(serverId, "", "", Collections.<String>emptyList(), url);
                disconnect(serverInfo);
                logger.info("Server with id '{}' disconnected", serverId);
            }
        }
    }
    
    @OnError
    public void onKieServerError(Session session, Throwable e) {
//        manager.removeSession(session);
        logger.error("Unexpected error", e);
    }

    @Override
    protected void notifyOnConnect(ServerInstance serverInstance) {
        // mute the events until server is notified with configuration
    }
    
    
}
