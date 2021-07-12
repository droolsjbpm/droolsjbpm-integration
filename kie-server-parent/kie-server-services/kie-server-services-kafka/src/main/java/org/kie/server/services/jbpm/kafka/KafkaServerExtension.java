/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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
package org.kie.server.services.jbpm.kafka;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.IsolationLevel;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jbpm.services.api.DeploymentEvent;
import org.jbpm.services.api.DeploymentEventListener;
import org.jbpm.services.api.DeploymentService;
import org.jbpm.services.api.ListenerSupport;
import org.jbpm.services.api.ProcessService;
import org.kie.internal.runtime.manager.deploy.DeploymentDescriptorManager;
import org.kie.server.api.KieServerConstants;
import org.kie.server.services.api.KieContainerInstance;
import org.kie.server.services.api.KieServerExtension;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.api.SupportedTransports;
import org.kie.server.services.impl.KieServerImpl;
import org.kie.server.services.jbpm.JbpmKieServerExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.server.services.jbpm.kafka.KafkaServerUtils.KAFKA_EXTENSION_PREFIX;
import static org.kie.server.services.jbpm.kafka.KafkaServerUtils.buildEventProcessorFactory;

public class KafkaServerExtension implements KieServerExtension, DeploymentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(KafkaServerExtension.class);

    public static final String EXTENSION_NAME = "Kafka";
    private static final String DEFAULT_HOST = "localhost:9092";

    private KafkaServerConsumer kafkaServerConsumer;
    private KafkaEventProcessorFactory factory;

    private AtomicBoolean initialized = new AtomicBoolean();
    // JBPM services
    private ListenerSupport deploymentService;

    private Map<String, Object> consumerProperties = new HashMap<>();
    private Map<String, Object> producerProperties = new HashMap<>();

    @Override
    public boolean isInitialized() {
        return initialized.get();
    }

    @Override
    public boolean isActive() {
        return !Boolean.parseBoolean(System.getProperty(KieServerConstants.KIE_KAFKA_SERVER_EXT_DISABLED, "true"));
    }

    @Override
    public void init(KieServerImpl kieServer, KieServerRegistry registry) {
        if (initialized.get()) {
            logger.warn("Kafka extension already initialized");
            return;
        }

        initProperties();
        factory = buildEventProcessorFactory();
        KafkaServerProducer.init(factory, this::getKafkaProducer);

        DeploymentDescriptorManager.addDescriptorLocation(
                "classpath:/META-INF/kafka-deployment-descriptor-defaults.xml");

        KieServerExtension jbpmExt = registry.getServerExtension(JbpmKieServerExtension.EXTENSION_NAME);
        if (jbpmExt == null) {
            logger.warn("Extension " + JbpmKieServerExtension.EXTENSION_NAME + " is required");
            return;
        }
        ProcessService processService = null;
        for (Object service : jbpmExt.getServices()) {
            if (deploymentService == null && DeploymentService.class.isAssignableFrom(service.getClass())) {
                deploymentService = (ListenerSupport) service;
            } else if (processService == null && ProcessService.class.isAssignableFrom(service.getClass())) {
                processService = (ProcessService) service;
            }
            if (deploymentService != null && processService != null) {
                break;
            }
        }
        if (deploymentService == null) {
            throw new IllegalStateException("Cannot find deployment service");
        }
        if (processService == null) {
            throw new IllegalStateException("Cannot find process service");
        }
        kafkaServerConsumer = new KafkaServerConsumer(factory, this::getKafkaConsumer, processService);
        deploymentService.addListener(this);
        initialized.set(true);
    }

    @Override
    public void destroy(KieServerImpl kieServer, KieServerRegistry registry) {
        if (deploymentService != null) {
            deploymentService.removeListener(this);
        }
        Duration duration = Duration.ofSeconds(Long.getLong(KAFKA_EXTENSION_PREFIX + "close.timeout", 30L));
        kafkaServerConsumer.close(duration);
        kafkaServerConsumer = null;
        KafkaServerProducer.cleanup(duration);
        factory.close();
        factory = null;
        deploymentService = null;
        initialized.set(false);
        consumerProperties.clear();
        producerProperties.clear();
    }

    protected Consumer<String, byte[]> getKafkaConsumer() {
        consumerProperties.putIfAbsent(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, DEFAULT_HOST);
        // read only committed events
        consumerProperties.putIfAbsent(ConsumerConfig.ISOLATION_LEVEL_CONFIG, IsolationLevel.READ_COMMITTED.toString()
                .toLowerCase());
        consumerProperties.putIfAbsent(ConsumerConfig.GROUP_ID_CONFIG, "jbpm-consumer");
        return new KafkaConsumer<>(consumerProperties, new StringDeserializer(), new ByteArrayDeserializer());
    }

    protected Producer<String, byte[]> getKafkaProducer() {
        producerProperties.putIfAbsent(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, DEFAULT_HOST);
        producerProperties.putIfAbsent(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000L);
        return new KafkaProducer<>(producerProperties, new StringSerializer(), new ByteArraySerializer());
    }

    @Override
    public void onDeploy(DeploymentEvent event) {
        kafkaServerConsumer.addRegistration(event);
    }

    @Override
    public void onUnDeploy(DeploymentEvent event) {
        kafkaServerConsumer.removeRegistration(event);
    }

    @Override
    public void onActivate(DeploymentEvent event) {
        kafkaServerConsumer.addRegistration(event);
    }

    @Override
    public void onDeactivate(DeploymentEvent event) {
        kafkaServerConsumer.removeRegistration(event);
    }

    @Override
    public boolean isUpdateContainerAllowed(String id,
                                            KieContainerInstance kieContainerInstance,
                                            Map<String, Object> parameters) {
        return true;
    }

    @Override
    public String getImplementedCapability() {
        return KieServerConstants.CAPABILITY_BPM_KAFKA;
    }

    @Override
    public List<Object> getServices() {
        return Collections.emptyList();
    }

    @Override
    public String getExtensionName() {
        return EXTENSION_NAME;
    }

    @Override
    public Integer getStartOrder() {
        return 20;
    }

    @Override
    public String toString() {
        return EXTENSION_NAME + " KIE Server extension";
    }

    @Override
    public void serverStarted() {
        // will use lazy initialization for consumer
    }

    @Override
    public void createContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
        // will be done by listener
    }

    @Override
    public void prepareContainerUpdate(String id,
                                       KieContainerInstance kieContainerInstance,
                                       Map<String, Object> parameters) {
        // will be done by listener
    }

    @Override
    public void updateContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
        // will be done by listener
    }

    @Override
    public void disposeContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
        // will be done by listener
    }

    @Override
    public List<Object> getAppComponents(SupportedTransports type) {
        return Collections.emptyList();
    }

    @Override
    public <T> T getAppComponents(Class<T> serviceType) {
        return null;
    }

    protected final Map<String, Object> getConsumerProperties() {
        return Collections.unmodifiableMap(consumerProperties);
    }

    protected final Map<String, Object> getProducerProperties() {
        return Collections.unmodifiableMap(producerProperties);
    }

    protected final void initProperties() {
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = entry.getKey().toString();
            if (key.startsWith(KAFKA_EXTENSION_PREFIX)) {
                String propName = key.substring(KAFKA_EXTENSION_PREFIX.length());
                if (ConsumerConfig.configNames().contains(propName)) {
                    consumerProperties.put(propName, entry.getValue());
                }
                if (ProducerConfig.configNames().contains(propName)) {
                    producerProperties.put(propName, entry.getValue());
                }
            }
        }
    }
}
