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

import java.util.ArrayList;

import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.controller.api.model.events.ContainerSpecUpdated;
import org.kie.server.controller.api.model.events.ServerInstanceConnected;
import org.kie.server.controller.api.model.events.ServerInstanceDeleted;
import org.kie.server.controller.api.model.events.ServerInstanceDisconnected;
import org.kie.server.controller.api.model.events.ServerInstanceUpdated;
import org.kie.server.controller.api.model.events.ServerTemplateDeleted;
import org.kie.server.controller.api.model.events.ServerTemplateUpdated;
import org.kie.server.controller.api.model.notification.KieServerControllerNotification;
import org.kie.server.controller.api.model.runtime.Container;
import org.kie.server.controller.api.model.runtime.ServerInstance;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.controller.api.model.spec.ServerTemplate;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WebSocketNotificationServiceTest {

    private static long TIMEOUT = 5 * 1000;

    @Mock
    Session session;

    @Mock
    RemoteEndpoint.Basic endpoint;

    @InjectMocks
    WebSocketNotificationService notificationService;

    @Before
    public void setUp() {
        when(session.getId()).thenReturn("id");
        when(session.getBasicRemote()).thenReturn(endpoint);
        when(session.isOpen()).thenReturn(true);
        WebSocketNotificationSessionManager.getInstance().addSession(session);
    }

    @Test
    public void testServerInstanceConnected() throws Exception {
        final ServerInstanceConnected event = new ServerInstanceConnected(new ServerInstance());
        notificationService.notify(event);

        verify(endpoint,
               timeout(TIMEOUT)).sendObject(new KieServerControllerNotification(event));
    }

    @Test
    public void testServerInstanceDisconnected() throws Exception {
        final ServerInstanceDisconnected event = new ServerInstanceDisconnected("serverId");
        notificationService.notify(event);

        verify(endpoint,
               timeout(TIMEOUT)).sendObject(new KieServerControllerNotification(event));
    }

    @Test
    public void testServerInstanceDeleted() throws Exception {
        final ServerInstanceDeleted event = new ServerInstanceDeleted("serverId");
        notificationService.notify(event);

        verify(endpoint,
               timeout(TIMEOUT)).sendObject(new KieServerControllerNotification(event));
    }

    @Test
    public void testServerInstanceUpdated() throws Exception {
        final ServerInstanceUpdated event = new ServerInstanceUpdated(new ServerInstance());
        notificationService.notify(event);

        verify(endpoint,
               timeout(TIMEOUT)).sendObject(new KieServerControllerNotification(event));
    }

    @Test
    public void testServerTemplateUpdated() throws Exception {
        final ServerTemplateUpdated event = new ServerTemplateUpdated(new ServerTemplate());
        notificationService.notify(event);

        verify(endpoint,
               timeout(TIMEOUT)).sendObject(new KieServerControllerNotification(event));
    }

    @Test
    public void testServerTemplateDeleted() throws Exception {
        final ServerTemplateDeleted event = new ServerTemplateDeleted("serverTemplateId");
        notificationService.notify(event);

        verify(endpoint,
               timeout(TIMEOUT)).sendObject(new KieServerControllerNotification(event));
    }

    @Test
    public void testContainerSpecUpdated() throws Exception {
        final ServerTemplate serverTemplate = new ServerTemplate();
        final ContainerSpec containerSpec = new ContainerSpec();
        final ArrayList<Container> containers = new ArrayList<>();

        final ContainerSpecUpdated event = new ContainerSpecUpdated(serverTemplate,
                                                                    containerSpec,
                                                                    containers);
        notificationService.notify(serverTemplate,
                                   containerSpec,
                                   containers);

        verify(endpoint,
               timeout(TIMEOUT)).sendObject(new KieServerControllerNotification(event));
    }
}
