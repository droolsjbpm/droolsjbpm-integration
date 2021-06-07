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
import java.util.concurrent.ExecutorService;

import jakarta.websocket.EncodeException;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.controller.api.model.notification.KieServerControllerNotification;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WebSocketNotificationSessionManagerTest {

    private static long TIMEOUT = 5 * 1000;

    @Mock
    Session session;

    WebSocketNotificationSessionManager manager = WebSocketNotificationSessionManager.getInstance();

    @Test
    public void testSessionCleanUp() {
        manager.addSession(session);

        assertTrue(manager.getExecutorsBySession().containsKey(session));

        manager.removeSession(session);

        assertFalse(manager.getExecutorsBySession().containsKey(session));
    }

    @Test
    public void testBroadcast() throws Exception {
        final RemoteEndpoint.Basic endpoint = mock(RemoteEndpoint.Basic.class);
        when(session.getBasicRemote()).thenReturn(endpoint);
        when(session.isOpen()).thenReturn(true);
        manager.addSession(session);

        final KieServerControllerNotification notification = new KieServerControllerNotification();
        manager.broadcastObject(notification);

        verify(endpoint,
               timeout(TIMEOUT)).sendObject(notification);
    }

    @Test
    public void testRemoveSessionWithMessageInProgress() throws Exception {
        final RemoteEndpoint.Basic endpoint = mock(RemoteEndpoint.Basic.class);
        doThrow(new IOException()).when(endpoint).sendObject(any());
        when(session.getBasicRemote()).thenReturn(endpoint);
        when(session.isOpen()).thenReturn(true);
        manager.addSession(session);

        final KieServerControllerNotification notification = new KieServerControllerNotification();
        manager.broadcastObject(notification);

        final ExecutorService executor = manager.getExecutorsBySession().get(session);

        manager.removeSession(session);

        //Wait for complete shutdown
        Thread.sleep(500);
        assertTrue(executor.isShutdown());
        assertTrue(executor.isTerminated());
    }

    @Test
    public void testBroadcastWithEncodeException() throws Exception {
        final RemoteEndpoint.Basic endpoint = mock(RemoteEndpoint.Basic.class);
        doThrow(new EncodeException(null,
                                    "encode error")).when(endpoint).sendObject(any());
        when(session.getBasicRemote()).thenReturn(endpoint);
        when(session.isOpen()).thenReturn(true);
        manager.addSession(session);

        final KieServerControllerNotification notification = new KieServerControllerNotification();
        manager.broadcastObject(notification);

        verify(endpoint,
               timeout(TIMEOUT)).sendObject(notification);
    }
}
