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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.Session;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.controller.websocket.common.handlers.KieServerMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionManager.class);
    
    private ConcurrentMap<String, Session> availableSessionsById = new ConcurrentHashMap<>();
    private ConcurrentMap<String, List<Session>> availableSessionsByUrl = new ConcurrentHashMap<>();
    private ConcurrentMap<String, KieServerInfo> sessionToUrl = new ConcurrentHashMap<>();
    private ConcurrentMap<String, KieServerMessageHandler> handlersPerSession = new ConcurrentHashMap<>();
        
    private static WebSocketSessionManager INSTANCE = new WebSocketSessionManager();

    public static WebSocketSessionManager getInstance() {
        return INSTANCE;
    }
    
    public void addSession(Session session) {
        this.availableSessionsById.put(session.getId(),
                                       session);
        final KieServerMessageHandler messageHandler = new KieServerMessageHandler(session);
        this.handlersPerSession.put(session.getId(),
                                    messageHandler);
        session.addMessageHandler(messageHandler);
        logger.debug("Session '{}' added to Web Socket manager", session.getId());
    }
    
    public void addSession(KieServerInfo serverInfo, Session session) {
        List<Session> newSessions =  new ArrayList<>();
        List<Session> sessions = this.availableSessionsByUrl.putIfAbsent(serverInfo.getLocation(), newSessions);
        if (sessions == null) {
            sessions = newSessions;
        }
        sessions.add(session);
        this.sessionToUrl.put(session.getId(), serverInfo);        
        logger.debug("Session '{}' associated with url: {}", session.getId(), serverInfo.getLocation());
    }
    
    public String removeSession(Session session) {
        this.availableSessionsById.remove(session.getId());
        KieServerInfo serverInfo = sessionToUrl.remove(session.getId());
        
        List<Session> sessions = availableSessionsByUrl.get(serverInfo.getLocation());
        Iterator<Session> it = sessions.iterator();
        
        while (it.hasNext()) {
            Session s = it.next();
            if (s.getId().equals(session.getId())) {
                it.remove();
                break;
            }
        }
        
        this.handlersPerSession.remove(session.getId());
        logger.debug("Session '{}' removed to Web Socket manager", session.getId());
        
        if (availableSessionsByUrl.get(serverInfo.getLocation()).isEmpty()) {
            return serverInfo.getLocation();
        }
        
        return null;
    }
    
    public List<Session> getByUrl(String url) {
        List<Session> sessions = availableSessionsByUrl.get(url);
        
        if (sessions == null) {
            return Collections.emptyList();
        }
        
        return sessions.stream().filter(s -> s.isOpen()).collect(Collectors.toList());
    }
    
    public KieServerInfo getServerInfoByUrl(String url) {
                
        String sessionId = getByUrl(url).get(0).getId();
        return this.sessionToUrl.get(sessionId);
    }
    
    public KieServerMessageHandler getHandler(String sessionId) {
        return this.handlersPerSession.get(sessionId);
    }
    
    public void close() {
        availableSessionsById.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.close(new CloseReason(CloseCodes.GOING_AWAY, "Server is going down"));
                } catch (Exception e) {
                   logger.warn("Unexpected error while shutting down Web Socket session", e);
                }
            }
        });
    }
}
