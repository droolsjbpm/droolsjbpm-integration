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

package org.kie.server.springboot.samples;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.runtime.manager.impl.jpa.EntityManagerFactoryManager;
import org.jbpm.services.api.DeploymentService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.kie.server.services.jbpm.kafka.KafkaServerExtension;
import org.kie.server.springboot.samples.utils.KieJarBuildHelper;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.KafkaContainer;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.TRANSACTIONAL_ID_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE;

public class KafkaFixture {

    protected static final String GROUP_ID = "org.kie.server.testing";
    protected static final String VERSION = "1.0.0.Final";
    
    protected static final String KAFKA_EXTENSION_PREFIX = "org.kie.server.jbpm-kafka.ext.";
    protected static final String SIGNAL_MAPPING_PROPERTY = KAFKA_EXTENSION_PREFIX + "signals.mapping";
    protected static final String MESSAGE_MAPPING_PROPERTY = KAFKA_EXTENSION_PREFIX + "message.mapping";
    protected static final String AUTO = "AUTO";
    protected static final String NONE = "NONE";
    
    //Kafka Consumer (catch) scenarios
    protected static final String SIGNALLING_PROJECT = "signalling-project";
    protected static final String START_SIGNAL_PROCESS_ID = "StartSignalProcess";
    protected static final String START_MESSAGE_PROCESS_ID = "StartMessageProcess";
    protected static final String START_MESSAGE_POJO_PROCESS_ID = "StartMessagePojoProcess";
    protected static final String START_MESSAGE_COMPLEX_POJO_PROCESS_ID = "StartMessageComplexPojoProcess";
    protected static final String START_MESSAGE_POJO_CLASS_NOT_FOUND_PROCESS_ID = "StartMessagePojoClassNotFoundProcess";
    protected static final String BOUNDARY_SIGNAL_PROCESS_ID = "BoundarySignalProcess";
    protected static final String BOUNDARY_MESSAGE_PROCESS_ID = "BoundaryMessageProcess";
    protected static final String INTERMEDIATE_CATCH_EVENT_SIGNAL_PROCESS_ID = "IntermediateCatchEventSignal";
    protected static final String INTERMEDIATE_CATCH_EVENT_MESSAGE_PROCESS_ID = "IntermediateCatchEventMessage";
    protected static final String SUBPROCESS_SIGNAL_PROCESS_ID = "SubprocessSignalProcess";
    protected static final String SUBPROCESS_MESSAGE_PROCESS_ID = "SubprocessMessageProcess";
    
    //Kafka Producer (throw) scenarios
    protected static final String SEND_PROJECT = "send-project";
    protected static final String INTERMEDIATE_THROW_EVENT_MESSAGE_PROCESS_ID = "IntermediateThrowEventMessage";
    protected static final String INTERMEDIATE_THROW_EVENT_SIGNAL_PROCESS_ID = "IntermediateThrowEventSignal";
    protected static final String END_SIGNAL_PROCESS_ID = "EndSignalProcess";
    protected static final String END_MESSAGE_PROCESS_ID = "EndMessageProcess";
    protected static final String END_MESSAGE_OUTPUT_POJO_PROCESS_ID = "EndMessageOutputPojo";
    protected static final String PARALLEL_INTERMEDIATE_THROW_EVENT_MESSAGE_PROCESS_ID = "ParallelIntermediateThrowEventMessages";
    protected static final String PARALLEL_INTERMEDIATE_THROW_EVENT_SIGNAL_PROCESS_ID = "ParallelIntermediateThrowEventSignals";
    
    protected static final String ALT_PROJECT = "alt-project";
    
    //signal/messages names
    protected static final String BOUNDARY_SIGNAL = "boundarySignal";
    protected static final String BOUNDARY_MESSAGE = "boundaryMessage";
    protected static final String START_SIGNAL = "startSignal";
    protected static final String START_MESSAGE = "startMessage";
    protected static final String START_MESSAGE_POJO = "startMessagePojo";
    protected static final String START_MESSAGE_COMPLEX_POJO = "startMessageComplexPojo";
    protected static final String START_MESSAGE_POJO_CLASS_NOT_FOUND = "startMessagePojoClassNotFound";
    
    protected static final String INTERMEDIATE_SIGNAL = "intermediateSignal";
    protected static final String INTERMEDIATE_MESSAGE = "intermediateMessage";
    protected static final String SUBPROCESS_SIGNAL = "subprocessSignal";
    protected static final String SUBPROCESS_MESSAGE = "subprocessMessage";
    
    protected static final String END_SIGNAL = "endSignal";
    protected static final String END_MESSAGE = "endMessage";
    protected static final String END_MESSAGE_OUTPUT_POJO = "endMessageOutputPojo";

    protected static final String SUBPROCESS_SCRIPT_NODE = "sub-script";

    protected static final String PATH = "src/test/resources/kjars/";
    
    protected static final String TEMPLATE_FILE = "src/test/resources/template/kie-deployment-descriptor.template";
    protected static final String DEPLOYMENT_DESCRIPTOR_FILE_NAME = "/src/main/resources/META-INF/kie-deployment-descriptor.xml";
    protected static final String STRATEGY_TEMPLATE = "STRATEGY_TEMPLATE";
    
    protected static final String VARIABLES = "variables";
    protected static final String JOHN = "john";
    
    protected static KafkaContainer kafka = new KafkaContainer();
    
    protected static String bootstrapServers;
    protected static Properties props = new Properties();
    
    private String templateFile = TEMPLATE_FILE;
    
    public static void generalSetup() {
        // Currently, Docker is needed for testcontainers
        assumeTrue(isDockerAvailable());
        
        //for the transactional tests
        kafka.addEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1");
        kafka.addEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1");
        kafka.start();
        bootstrapServers = kafka.getBootstrapServers();
        
        System.setProperty(KAFKA_EXTENSION_PREFIX+BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        EntityManagerFactoryManager.get().clear();
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        createTopics();
    }

    public static void createTopics() {
        try (AdminClient adminClient = AdminClient.create(props)) {
            adminClient.createTopics(Arrays.asList(new NewTopic(BOUNDARY_SIGNAL, 1, (short) 1),
                                                   new NewTopic(BOUNDARY_MESSAGE, 1, (short) 1),
                                                   new NewTopic(START_SIGNAL, 1, (short) 1),
                                                   new NewTopic(START_MESSAGE, 1, (short) 1),
                                                   new NewTopic(START_MESSAGE_POJO, 1, (short) 1),
                                                   new NewTopic(START_MESSAGE_COMPLEX_POJO, 1, (short) 1),
                                                   new NewTopic(START_MESSAGE_POJO_CLASS_NOT_FOUND, 1, (short) 1),
                                                   new NewTopic(INTERMEDIATE_SIGNAL, 1, (short) 1),
                                                   new NewTopic(INTERMEDIATE_MESSAGE, 1, (short) 1),
                                                   new NewTopic(SUBPROCESS_SIGNAL, 1, (short) 1),
                                                   new NewTopic(SUBPROCESS_MESSAGE, 1, (short) 1),
                                                   new NewTopic(END_SIGNAL, 1, (short) 1),
                                                   new NewTopic(END_MESSAGE, 1, (short) 1),
                                                   new NewTopic(END_MESSAGE_OUTPUT_POJO, 1, (short) 1)))
                       .all().get(1, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            fail("Exception when creating topics: " + e.getMessage());
        }
    }
    
    public KModuleDeploymentUnit setup(DeploymentService ds, String artifactId, String strategy) {
        System.setProperty(SIGNAL_MAPPING_PROPERTY, AUTO);
        
        Map<String, String> map = new HashMap<>();
        map.put(STRATEGY_TEMPLATE, strategy);
        
        KieJarBuildHelper.replaceInFile(templateFile, PATH+artifactId+DEPLOYMENT_DESCRIPTOR_FILE_NAME, map);
        
        KieJarBuildHelper.createKieJar(PATH + artifactId);
        KModuleDeploymentUnit unit = new KModuleDeploymentUnit(GROUP_ID, artifactId, VERSION);
        
        ds.deploy(unit);
        return unit;
    }

    protected void cleanup(DeploymentService ds, KModuleDeploymentUnit unit) {
        if (ds!=null && unit!=null) {
            ds.undeploy(unit);
        }
        System.clearProperty(MESSAGE_MAPPING_PROPERTY);
        System.clearProperty(SIGNAL_MAPPING_PROPERTY);
    }

    protected void abortAllProcesses(RuntimeDataService runtimeDataService, ProcessService processService) {
        if (runtimeDataService == null || processService == null) {
            return;
        }
        Collection<ProcessInstanceDesc> activeInstances = runtimeDataService.getProcessInstances(singletonList(STATE_ACTIVE), null, null);
        if (activeInstances != null) {
            for (ProcessInstanceDesc instance : activeInstances) {
                processService.abortProcessInstance(instance.getDeploymentId(), instance.getId());
            }
        }
    }

    protected void waitForConsumerGroupToBeReady() {
        await().atMost(3, SECONDS).pollDelay(1, SECONDS).until(() -> !listConsumerGroups().isEmpty());
    }
    
    protected List<String> listConsumerGroups() {
        List<String> consumerGroups = new ArrayList<>();
        try (AdminClient adminClient = AdminClient.create(props)) {
            consumerGroups = adminClient.listConsumerGroups().all().get().
                    stream().map(ConsumerGroupListing::groupId).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception when listConsumerGroups: " + e.getMessage());
        }
        return consumerGroups;
    }
    
    protected Properties producerProps(String bootstrapServer, boolean transactional) {
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        producerProperties.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProperties.put(VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        if (transactional) {
            producerProperties.put(TRANSACTIONAL_ID_CONFIG, "my-transactional-id");
        }
            
        return producerProperties;
    }
    
    protected ListAppender<ILoggingEvent> addLogAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(KafkaServerExtension.class.getPackage().getName());
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }
    
    protected Optional<ILoggingEvent> getErrorLog(ListAppender<ILoggingEvent> listAppender) {
        Optional<ILoggingEvent> logEvent = listAppender.list.stream().filter(log -> log.getLevel() == Level.ERROR)
                .findAny();
        assertTrue("no trace printed when failed", logEvent.isPresent());
        return logEvent;
    }

    public void sendRecord(String topic, String event) {
        try (KafkaProducer<String, byte[]> producer = new KafkaProducer<>(producerProps(bootstrapServers, false))) {
            send(producer, topic, event);
        } 
    }

    public void send(KafkaProducer<String, byte[]> producer, String topic, String event) {
        try {
          ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, event.getBytes());
          producer.send(record).get();
        } catch (InterruptedException | ExecutionException e) {
            fail("Not expected exception: " + e.getMessage());
        }
    }
    
    public void sendTransactionalRecords(Map<String, List<String>> map) {
        try (KafkaProducer<String, byte[]> producer = new KafkaProducer<>(producerProps(bootstrapServers, true))) {
            producer.initTransactions();
            producer.beginTransaction();
            
            map.entrySet().forEach(e -> {
                e.getValue().forEach(i -> send(producer, e.getKey(), i));
            });
            
            producer.commitTransaction();
        }
    }
    
    protected void consumeAndAssertRecords(String topic, int expectedProcesses) {
        ConsumerRecords<String, byte[]>  records = consumeMessages(topic);
        assertEquals(expectedProcesses, records.count());
        Iterator<ConsumerRecord<String, byte[]>> iterator = records.iterator();
        while (iterator.hasNext()) {
            assertRecord(iterator.next(), topic);
        }
    }
    
    protected void assertRecord(ConsumerRecord<String, byte[]> record, String topic) {
        Map<String, Object> event = getJsonObject(record);
        
        assertNull(record.key());
        assertEquals(topic, record.topic());
        assertEquals("1.0", event.get("specversion"));
        
        assertTrue(event.get("data").toString().contains("my-value"));
    }
    
    protected Map<String, Object> getJsonObject(ConsumerRecord<String, byte[]> record) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        ObjectMapper mapper = new ObjectMapper()
                .setDateFormat(dateFormat)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        Map<String, Object> jsonEvent = null;
        
        try {
            jsonEvent = mapper.readValue(record.value(), Map.class);
        } catch(IOException e) {
            fail("Exception when reading value: " + e.getMessage());
        }
        return jsonEvent;
    }

    protected <T> ConsumerRecords<String, T> consumeMessages(String topic) {
        try (KafkaConsumer<String, T> consumer = createConsumer(topic)) {
            ConsumerRecords<String, T> records = consumer.poll(Duration.ofSeconds(5));
            consumer.commitSync();
            return records;
        }
    }

    protected <T> KafkaConsumer<String, T> createConsumer(String topic) {
        KafkaConsumer<String, T> consumer = new KafkaConsumer<>(consumerProperties());
        consumer.subscribe(singletonList(topic));
        return consumer;
    }

    protected Properties consumerProperties() {
        Properties props = new Properties();
        props.setProperty(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.setProperty(GROUP_ID_CONFIG, "jbpm_test_consumer");
        props.setProperty(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.setProperty(AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }
    
    private static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
}