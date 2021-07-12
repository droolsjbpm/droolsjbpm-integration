/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.server.services.jbpm.kafka;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.drools.core.event.MessageEventImpl;
import org.drools.core.event.SignalEventImpl;
import org.jbpm.runtime.manager.impl.SimpleRegisterableItemsFactory;
import org.jbpm.services.api.DeploymentService;
import org.jbpm.services.api.ListenerSupport;
import org.jbpm.services.api.ProcessService;
import org.jbpm.workflow.core.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.KieRuntime;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.runtime.manager.InternalRegisterableItemsFactory;
import org.kie.server.services.api.KieServerExtension;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.impl.KieServerImpl;
import org.kie.server.services.jbpm.kafka.KafkaServerUtils.Mapping;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.kie.server.services.jbpm.kafka.KafkaServerUtils.MESSAGE_MAPPING_PROPERTY;
import static org.kie.server.services.jbpm.kafka.KafkaServerUtils.SIGNAL_MAPPING_PROPERTY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class KafkaServerExtensionProducerTest {

    private static class MockKafkaServerExtension extends KafkaServerExtension {

        private MockProducer<String, byte[]> producer;

        public MockKafkaServerExtension(MockProducer<String, byte[]> producer) {
            this.producer = producer;
        }

        @Override
        protected Producer<String, byte[]> getKafkaProducer() {
            return producer;
        }
    }

    private ProcessService processService;
    private MockKafkaServerExtension extension;
    private KieServerImpl server;
    private KieServerRegistry registry;
    private MockProducer<String, byte[]> mockProducer;
    private ProcessInstance pInstance;
    private KieRuntime runtime;
    private NodeInstance nInstance;
    private Node node;
    private InternalRegisterableItemsFactory itemsFactory;
    private RuntimeEngine runtimeEngine = mock(RuntimeEngine.class);
    private KafkaEventReader eventReader = new CloudEventReader(Thread.currentThread().getContextClassLoader());

    @Before
    public void setup() {
        System.setProperty(SIGNAL_MAPPING_PROPERTY, Mapping.AUTO.toString());
        itemsFactory = new SimpleRegisterableItemsFactory();
        itemsFactory.addProcessListener(new KafkaServerProcessListener());
        mockProducer = new MockProducer<>();
        extension = new MockKafkaServerExtension(mockProducer);
        server = mock(KieServerImpl.class);
        registry = mock(KieServerRegistry.class);
        KieServerExtension serverExtension = mock(KieServerExtension.class);
        when(registry.getServerExtension(Mockito.anyString())).thenReturn(serverExtension);
        ListenerSupport deployService = mock(ListenerSupport.class, withSettings().extraInterfaces(
                DeploymentService.class));
        processService = mock(ProcessService.class);
        when(serverExtension.getServices()).thenReturn(Arrays.asList(deployService, processService));
        extension.init(server, registry);
        extension.serverStarted();
        pInstance = mock(ProcessInstance.class);
        runtime = mock(KieRuntime.class);
        nInstance = mock(NodeInstance.class);
        node = mock(Node.class);
        when(nInstance.getNode()).thenReturn(node);
        when(node.getMetaData()).thenReturn(Collections.emptyMap());
    }

    @After
    public void close() {
        extension.destroy(server, registry);
        System.clearProperty(SIGNAL_MAPPING_PROPERTY);
        System.clearProperty(MESSAGE_MAPPING_PROPERTY);
    }

    

    @Test 
    public void testMessageSent() throws IOException, ClassNotFoundException {
        itemsFactory.getProcessEventListeners(runtimeEngine).forEach(l -> l.onMessage(new MessageEventImpl(
                pInstance, runtime, nInstance,
                "MyMessage", "Javierito")));
        List<ProducerRecord<String, byte[]>> events = mockProducer.history();
        assertFalse(events.isEmpty());
        ProducerRecord<String, byte[]> event = events.get(0);
        assertEquals("MyMessage", event.topic());
        assertEquals("Javierito", eventReader.readEvent(event.value(), String.class));
    }

    @Test
    public void testSignalSent() throws IOException, ParseException, ClassNotFoundException {
        itemsFactory.getProcessEventListeners(runtimeEngine).forEach(l -> l.onSignal(new SignalEventImpl(
                pInstance, runtime, nInstance, "MySignal", "Javierito")));
        List<ProducerRecord<String, byte[]>> events = mockProducer.history();
        assertFalse(events.isEmpty());
        ProducerRecord<String, byte[]> event = events.get(0);
        assertEquals("MySignal", event.topic());
        assertEquals("Javierito", eventReader.readEvent(event.value(), String.class));
    }

    @Test
    public void testSignalSentImplementation() throws IOException, ClassNotFoundException {
        when(node.getMetaData()).thenReturn(Collections.singletonMap("implementation", "##kafka"));
        System.clearProperty(SIGNAL_MAPPING_PROPERTY);
        itemsFactory.getProcessEventListeners(runtimeEngine).forEach(l -> l.onSignal(new SignalEventImpl(
                pInstance, runtime, nInstance, "MySignal", "Javierito")));
        List<ProducerRecord<String, byte[]>> events = mockProducer.history();
        assertFalse(events.isEmpty());
        ProducerRecord<String, byte[]> event = events.get(0);
        assertEquals("MySignal", event.topic());
        assertEquals("Javierito", eventReader.readEvent(event.value(), String.class));
    }

    @Test
    public void testSignalDisable() throws IOException {
        System.clearProperty(SIGNAL_MAPPING_PROPERTY);
        itemsFactory.getProcessEventListeners(runtimeEngine).forEach(l -> l.onSignal(new SignalEventImpl(
                pInstance, runtime, nInstance, "MySignal", "Javierito")));
        assertTrue(mockProducer.history().isEmpty());
    }

    @Test
    public void testMessageDisable() throws IOException, ParseException {
        System.setProperty(MESSAGE_MAPPING_PROPERTY, Mapping.NONE.toString());
        itemsFactory.getProcessEventListeners(runtimeEngine).forEach(l -> l.onMessage(new MessageEventImpl(
                pInstance, runtime, nInstance, "MyMessage", "Javierito")));
        assertTrue(mockProducer.history().isEmpty());
    }
}
