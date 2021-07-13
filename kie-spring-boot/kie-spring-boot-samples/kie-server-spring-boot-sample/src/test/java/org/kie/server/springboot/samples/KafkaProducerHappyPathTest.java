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

import static java.util.Collections.emptyMap;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.kie.server.springboot.samples.KafkaFixture.END_MESSAGE;
import static org.kie.server.springboot.samples.KafkaFixture.END_MESSAGE_OUTPUT_POJO;
import static org.kie.server.springboot.samples.KafkaFixture.END_MESSAGE_OUTPUT_POJO_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.END_MESSAGE_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.END_SIGNAL;
import static org.kie.server.springboot.samples.KafkaFixture.END_SIGNAL_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.GROUP_ID;
import static org.kie.server.springboot.samples.KafkaFixture.INTERMEDIATE_MESSAGE;
import static org.kie.server.springboot.samples.KafkaFixture.INTERMEDIATE_SIGNAL;
import static org.kie.server.springboot.samples.KafkaFixture.INTERMEDIATE_THROW_EVENT_MESSAGE_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.INTERMEDIATE_THROW_EVENT_SIGNAL_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.KAFKA_EXTENSION_PREFIX;
import static org.kie.server.springboot.samples.KafkaFixture.MESSAGE_MAPPING_PROPERTY;
import static org.kie.server.springboot.samples.KafkaFixture.NONE;
import static org.kie.server.springboot.samples.KafkaFixture.PARALLEL_INTERMEDIATE_THROW_EVENT_MESSAGE_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.PARALLEL_INTERMEDIATE_THROW_EVENT_SIGNAL_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.SEND_PROJECT;
import static org.kie.server.springboot.samples.KafkaFixture.SIGNAL_MAPPING_PROPERTY;
import static org.kie.server.springboot.samples.KafkaFixture.VERSION;
import static org.kie.server.springboot.samples.KafkaFixture.kafka;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.services.api.DeploymentService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.jbpm.services.task.deadlines.notifications.impl.NotificationListenerManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.query.QueryFilter;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.springboot.samples.listeners.CountDownLatchEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.TestPropertySource;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@RunWith(Parameterized.class)
@SpringBootTest(classes = {KieServerApplication.class, TestAutoConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations="classpath:application-kafka.properties")
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_CLASS)
public class KafkaProducerHappyPathTest extends KafkaBaseTest {
    
    private static final String MY_VALUE2 = "my-value2";

    private static final String MY_VALUE1 = "my-value1";

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerHappyPathTest.class);
    
    @LocalServerPort
    private int port;

    private String user = "john";
    private String password = "john@pwd1";

    @Rule
    public TestRule watcher = new TestWatcher() {
       protected void starting(Description description) {
          logger.info(">>> Starting test: " + description.getMethodName());
       }
    };
    
    @ClassRule
    public static final SpringClassRule scr = new SpringClassRule();
 
    @Rule
    public final SpringMethodRule smr = new SpringMethodRule();
    
    @Autowired
    protected DeploymentService deploymentService;

    @Autowired
    protected ProcessService processService;
    
    @Autowired
    protected UserTaskService userTaskService;
    
    @Autowired
    protected RuntimeDataService runtimeDataService;
    
    @Autowired
    protected CountDownLatchEventListener countDownListener;
    
    protected String deploymentId;
    protected KModuleDeploymentUnit unit;
    
    protected ListAppender<ILoggingEvent> listAppender;
    
    private KieServicesClient kieServicesClient;
    
    protected static KafkaFixture kafkaFixture = new KafkaFixture();
    
    @BeforeClass
    public static void beforeClass() {
        System.setProperty(KAFKA_EXTENSION_PREFIX+"topics._2_Message", INTERMEDIATE_MESSAGE);
        System.setProperty(KAFKA_EXTENSION_PREFIX+"topics._2_Signal", INTERMEDIATE_SIGNAL);
        
        KafkaFixture.generalSetup();
    }
    
    
    @Before
    public void setup() throws Exception {
        unit = kafkaFixture.setup(deploymentService, SEND_PROJECT, strategy);
        deploymentId = unit.getIdentifier();
        listAppender = kafkaFixture.addLogAppender();
    }
    
    public void setupRestClient() {
        ReleaseId releaseId = new ReleaseId(GROUP_ID, SEND_PROJECT, VERSION);
        String serverUrl = "http://localhost:" + port + "/rest/server";
        KieServicesConfiguration configuration = KieServicesFactory.newRestConfiguration(serverUrl, user, password);
        configuration.setTimeout(60000);
        configuration.setMarshallingFormat(MarshallingFormat.JSON);
        this.kieServicesClient =  KieServicesFactory.newKieServicesClient(configuration);
        
        KieContainerResource resource = new KieContainerResource(SEND_PROJECT, releaseId);
        resource.setContainerAlias(SEND_PROJECT);
        kieServicesClient.createContainer(SEND_PROJECT, resource);
    }

    @After
    public void cleanup() {
        NotificationListenerManager.get().reset();
        kafkaFixture.abortAllProcesses(runtimeDataService, processService);
        kafkaFixture.cleanup(deploymentService, unit);
        if (kieServicesClient != null) {
            kieServicesClient.disposeContainer(SEND_PROJECT);
        }
    }
    
    @AfterClass
    public static void teardown() {
        kafka.stop();
        System.clearProperty(KAFKA_EXTENSION_PREFIX+BOOTSTRAP_SERVERS_CONFIG);
        System.clearProperty(KAFKA_EXTENSION_PREFIX+"topics._2_Message");
        System.clearProperty(KAFKA_EXTENSION_PREFIX+"topics._2_Signal");
    }
    
    @Test(timeout = 60000)
    public void testEndSignal() throws Exception {
        countDownListener.configure(END_SIGNAL_PROCESS_ID, 2);
        
        Long pid1 = processService.startProcess(deploymentId, END_SIGNAL_PROCESS_ID);
        assertTrue(pid1 > 0);
        
        countDownListener.getCountDown().await();
        
        kafkaFixture.consumeAndAssertRecords(END_SIGNAL, 1);
    }
    
    @Test(timeout = 60000)
    public void testEndMessage() throws Exception {
        countDownListener.configure(END_MESSAGE_PROCESS_ID, 2);
        
        Long pid1 = processService.startProcess(deploymentId, END_MESSAGE_PROCESS_ID);
        assertTrue(pid1 > 0);
        
        countDownListener.getCountDown().await();
        
        kafkaFixture.consumeAndAssertRecords(END_MESSAGE, 1);
    }
    
    @Test(timeout = 60000)
    public void testEndMessageOutputPojo() throws Exception {
        setupRestClient();
        
        countDownListener.configure(END_MESSAGE_OUTPUT_POJO_PROCESS_ID, 2);
        
        ProcessServicesClient processClient = kieServicesClient.getServicesClient(ProcessServicesClient.class);
        
        Long pid1 = processClient.startProcess(SEND_PROJECT, END_MESSAGE_OUTPUT_POJO_PROCESS_ID, buildDog("German Shepherd"));
        assertTrue(pid1 > 0);
        
        countDownListener.getCountDown().await();
        
        ConsumerRecords<String, byte[]>  records = kafkaFixture.consumeMessages(END_MESSAGE_OUTPUT_POJO);
        assertEquals(1, records.count());
        
        Map<String, Object> event = kafkaFixture.getJsonObject(records.iterator().next());
        assertEquals("org.jbpm.data.Dog", event.get("type"));
    }

    
    @Test(timeout = 60000)
    public void testEndMessageRecordTooLargeException() throws Exception {
        setupRestClient();
        
        countDownListener.configure(END_MESSAGE_OUTPUT_POJO_PROCESS_ID, 2);
        
        ProcessServicesClient processClient = kieServicesClient.getServicesClient(ProcessServicesClient.class);
        
        Long pid = processClient.startProcess(SEND_PROJECT, END_MESSAGE_OUTPUT_POJO_PROCESS_ID, buildDog(RandomStringUtils.random(300000)));
        assertTrue(pid > 0);
        
        countDownListener.getCountDown().await(1, TimeUnit.SECONDS);
        
        Optional<ILoggingEvent> logEvent = kafkaFixture.getErrorLog(listAppender);
        assertEquals(RecordTooLargeException.class.getCanonicalName(), logEvent.get().getThrowableProxy().getClassName());
    }
    
    @Test(timeout = 60000)
    public void testCustomEventListenerMerged() throws Exception {
        Long pid1 = processService.startProcess(deploymentId, INTERMEDIATE_THROW_EVENT_MESSAGE_PROCESS_ID,
                Collections.singletonMap("x", MY_VALUE1));
        
        kafkaFixture.consumeAndAssertRecords(INTERMEDIATE_MESSAGE, 1);

        Boolean listenerVar = (Boolean) processService.getProcessInstanceVariable(deploymentId, pid1, "testListenerStarted");
        assertNotNull(listenerVar);
        assertTrue(listenerVar);
        autocompleteSingleTask(1);
    }
    
    @Test(timeout = 60000)
    public void testIntermediateThrowEventMessage() throws Exception {
        processService.startProcess(deploymentId, INTERMEDIATE_THROW_EVENT_MESSAGE_PROCESS_ID,
                Collections.singletonMap("x", MY_VALUE1));
        
        processService.startProcess(deploymentId, INTERMEDIATE_THROW_EVENT_MESSAGE_PROCESS_ID,
                Collections.singletonMap("x", MY_VALUE2));
        
        kafkaFixture.consumeAndAssertRecords(INTERMEDIATE_MESSAGE, 2);

        autocompleteSingleTask(2);
    }
    
    @Test(timeout = 60000)
    public void testIntermediateThrowEventSignal() throws Exception {
        processService.startProcess(deploymentId, INTERMEDIATE_THROW_EVENT_SIGNAL_PROCESS_ID,
                Collections.singletonMap("x", MY_VALUE1));
        
        processService.startProcess(deploymentId, INTERMEDIATE_THROW_EVENT_SIGNAL_PROCESS_ID,
                Collections.singletonMap("x", MY_VALUE2));
        
        kafkaFixture.consumeAndAssertRecords(INTERMEDIATE_SIGNAL, 2);

        autocompleteSingleTask(2);
    }
    
    @Test(timeout = 60000)
    public void testWhenMappingNoneIntermediateThrowEventMessage() throws Exception {
        System.setProperty(MESSAGE_MAPPING_PROPERTY, NONE);
        
        processService.startProcess(deploymentId, INTERMEDIATE_THROW_EVENT_MESSAGE_PROCESS_ID,
                Collections.singletonMap("x", MY_VALUE1));
        
        kafkaFixture.consumeAndAssertRecords(INTERMEDIATE_MESSAGE, 0);

        autocompleteSingleTask(1);
    }
    
    @Test(timeout = 60000)
    public void testWhenMappingNoneIntermediateThrowEventSignal() throws Exception {
        System.setProperty(SIGNAL_MAPPING_PROPERTY, NONE);
        
        processService.startProcess(deploymentId, INTERMEDIATE_THROW_EVENT_SIGNAL_PROCESS_ID,
                Collections.singletonMap("x", MY_VALUE2));
        
        kafkaFixture.consumeAndAssertRecords(INTERMEDIATE_SIGNAL, 0);

        autocompleteSingleTask(1);
    }
    
    @Test(timeout = 60000)
    public void testParallelIntermediateThrowEventMessages() throws Exception {
        Map<String,Object> params = new HashMap<>();
        params.put("y", MY_VALUE2);
        
        processService.startProcess(deploymentId, PARALLEL_INTERMEDIATE_THROW_EVENT_MESSAGE_PROCESS_ID, params);
        
        kafkaFixture.consumeAndAssertRecords(INTERMEDIATE_MESSAGE, 2);

        autocompleteSingleTask(1);
    }
    
    @Test(timeout = 60000)
    public void testWhenMappingNoneAndKafkaMetadataParallelIntermediateThrowEventSignals() throws Exception {
        System.setProperty(SIGNAL_MAPPING_PROPERTY, NONE);
        
        Map<String,Object> params = new HashMap<>();
        params.put("y", MY_VALUE2);
        
        processService.startProcess(deploymentId, PARALLEL_INTERMEDIATE_THROW_EVENT_SIGNAL_PROCESS_ID, params);
        
        kafkaFixture.consumeAndAssertRecords(INTERMEDIATE_SIGNAL, 1);

        autocompleteSingleTask(1);
    }
    
    protected void autocompleteSingleTask(int numberOfTasks) {
        List<TaskSummary> tasks =  runtimeDataService.getTasksAssignedAsPotentialOwner("john", new QueryFilter());
        assertEquals(numberOfTasks, tasks.size());
        for (int i=0; i<numberOfTasks; i++)
          userTaskService.completeAutoProgress(tasks.get(i).getId(), "john", emptyMap());
    }
    
    protected Map<String, Object> buildDog(String breed) {
        Map<String,Object> dogMap = new HashMap<>();
        dogMap.put("weight", 34.58);
        dogMap.put("breed",breed);
        Map<String,Object> dogClass = Collections.singletonMap("org.jbpm.data.Dog",dogMap);
        Map<String,Object> params = Collections.singletonMap("input",dogClass);
        return params;
    }

}
