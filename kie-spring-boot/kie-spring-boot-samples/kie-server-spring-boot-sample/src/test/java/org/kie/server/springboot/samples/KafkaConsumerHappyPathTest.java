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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.query.QueryFilter;
import org.kie.server.api.marshalling.MarshallingException;
import org.kie.server.springboot.samples.listeners.CountDownLatchEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.KafkaContainer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE;
import static org.kie.internal.runtime.conf.RuntimeStrategy.SINGLETON;
import static org.kie.server.springboot.samples.KafkaFixture.ALT_PROJECT;
import static org.kie.server.springboot.samples.KafkaFixture.BOUNDARY_MESSAGE;
import static org.kie.server.springboot.samples.KafkaFixture.BOUNDARY_MESSAGE_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.BOUNDARY_SIGNAL;
import static org.kie.server.springboot.samples.KafkaFixture.BOUNDARY_SIGNAL_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.INTERMEDIATE_CATCH_EVENT_MESSAGE_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.INTERMEDIATE_CATCH_EVENT_SIGNAL_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.INTERMEDIATE_MESSAGE;
import static org.kie.server.springboot.samples.KafkaFixture.INTERMEDIATE_SIGNAL;
import static org.kie.server.springboot.samples.KafkaFixture.JOHN;
import static org.kie.server.springboot.samples.KafkaFixture.KAFKA_EXTENSION_PREFIX;
import static org.kie.server.springboot.samples.KafkaFixture.SIGNALLING_PROJECT;
import static org.kie.server.springboot.samples.KafkaFixture.START_MESSAGE;
import static org.kie.server.springboot.samples.KafkaFixture.START_MESSAGE_COMPLEX_POJO;
import static org.kie.server.springboot.samples.KafkaFixture.START_MESSAGE_COMPLEX_POJO_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.START_MESSAGE_POJO;
import static org.kie.server.springboot.samples.KafkaFixture.START_MESSAGE_POJO_CLASS_NOT_FOUND;
import static org.kie.server.springboot.samples.KafkaFixture.START_MESSAGE_POJO_CLASS_NOT_FOUND_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.START_MESSAGE_POJO_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.START_MESSAGE_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.START_SIGNAL;
import static org.kie.server.springboot.samples.KafkaFixture.START_SIGNAL_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.SUBPROCESS_MESSAGE;
import static org.kie.server.springboot.samples.KafkaFixture.SUBPROCESS_MESSAGE_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.SUBPROCESS_SCRIPT_NODE;
import static org.kie.server.springboot.samples.KafkaFixture.SUBPROCESS_SIGNAL;
import static org.kie.server.springboot.samples.KafkaFixture.SUBPROCESS_SIGNAL_PROCESS_ID;
import static org.kie.server.springboot.samples.KafkaFixture.generalSetup;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {KieServerApplication.class, TestAutoConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations="classpath:application-kafka.properties")
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class KafkaConsumerHappyPathTest {
    
    private static final String USELESS_DATA_EVENT = "useless-data-event.json";
    private static final String MALFORMED_EVENT = "malformed-event.json";
    private static final String MONEY_DATA_EVENT = "money-data-event.json";
    private static final String MONEY_DATA_NULL_AMOUNT_EVENT = "money-data-null-amount-event.json";
    private static final String MONEY_DATA_NULL_CURRENCY_EVENT = "money-data-null-currency-event.json";
    private static final String MONEY_DATA_NULL_BOTH_EVENT = "money-data-null-both-event.json";
    private static final String MONEY_DATA_WRONG_TYPE_EVENT = "money-data-wrong-type-event.json";
    private static final String MONEY_DATA_WRONG_DATE_EVENT = "money-data-wrong-date-event.json";
    private static final String REIMBURSEMENT_DATA_EVENT = "reimbursement-data-event.json";

    private static final String CURRENCY_EUR = "EUR";

    private static final String AMOUNT_294 = "294";

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerHappyPathTest.class);

    protected static KafkaContainer kafka = new KafkaContainer();
    
    protected static KafkaFixture kafkaFixture = new KafkaFixture();
    
    @Rule
    public TestRule watcher = new TestWatcher() {
       protected void starting(Description description) {
          logger.info(">>> Starting test: " + description.getMethodName());
       }
    };
    
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
    
    @BeforeClass
    public static void beforeClass() {
        generalSetup();
    }
    
    
    @Before
    public void setup() {
        unit = kafkaFixture.setup(deploymentService, SIGNALLING_PROJECT, SINGLETON.name());
        deploymentId = unit.getIdentifier();
        kafkaFixture.waitForConsumerGroupToBeReady();
        listAppender = kafkaFixture.addLogAppender();
    }

    @After
    public void cleanup() {
        NotificationListenerManager.get().reset();
        kafkaFixture.abortAllProcesses(runtimeDataService, processService);
        kafkaFixture.cleanup(deploymentService, unit);
    }
    
    @AfterClass
    public static void teardown() {
        kafka.stop();
        System.clearProperty(KAFKA_EXTENSION_PREFIX+BOOTSTRAP_SERVERS_CONFIG);
    }
    
    @Test(timeout = 60000)
    public void testStartSignal() throws InterruptedException {
        countDownListener.configure(START_SIGNAL_PROCESS_ID, 1);
        
        sendEvent(START_SIGNAL, USELESS_DATA_EVENT);
        countDownListener.getCountDown().await();
        
        assertEquals(1, countDownListener.getIds().size());
        Long pid = countDownListener.getIds().get(0);
        assertThat(processService.getProcessInstance(deploymentId, pid).getState(), is(STATE_ACTIVE));
        
        processService.abortProcessInstance(pid);
    }
    
    @Test(timeout = 60000)
    public void testStartMessage() throws InterruptedException {
        countDownListener.configure(START_MESSAGE_PROCESS_ID, 1);
        
        sendEvent(START_MESSAGE, USELESS_DATA_EVENT);
        
        countDownListener.getCountDown().await();
        
        assertEquals(1, countDownListener.getIds().size());
        Long pid = countDownListener.getIds().get(0);
        assertThat(processService.getProcessInstance(deploymentId, pid).getState(), is(STATE_ACTIVE));
        
        processService.abortProcessInstance(pid);
    }
    
    @Test(timeout = 60000)
    public void testBoundarySignal() throws InterruptedException {
        Long processInstanceId = startAndAssertProcess(BOUNDARY_SIGNAL_PROCESS_ID);
        
        countDownListener.configure(BOUNDARY_SIGNAL_PROCESS_ID, 1);
        
        sendEvent(BOUNDARY_SIGNAL, USELESS_DATA_EVENT);
        
        countDownListener.getCountDown().await();
        
        ProcessInstance pi = processService.getProcessInstance(processInstanceId);
        assertNull(pi);
    }
    
    @Test(timeout = 60000)
    public void testBoundaryMessage() throws InterruptedException {
        Long processInstanceId = startAndAssertProcess(BOUNDARY_MESSAGE_PROCESS_ID);
        
        countDownListener.configure(BOUNDARY_MESSAGE_PROCESS_ID, 1);
        
        sendEvent(BOUNDARY_MESSAGE, USELESS_DATA_EVENT);
        
        countDownListener.getCountDown().await();
        
        ProcessInstance pi = processService.getProcessInstance(processInstanceId);
        assertNull(pi);
    }

    @Test(timeout = 60000)
    public void testIntermediateCatchEventSignal() throws Exception {
        Long processInstanceId = startAndAssertProcess(INTERMEDIATE_CATCH_EVENT_SIGNAL_PROCESS_ID);
        
        countDownListener.configure(INTERMEDIATE_CATCH_EVENT_SIGNAL_PROCESS_ID, 1);
        
        autocompleteSingleTask();
        
        sendEvent(INTERMEDIATE_SIGNAL, USELESS_DATA_EVENT);
        countDownListener.getCountDown().await();
        
        ProcessInstance pi = processService.getProcessInstance(processInstanceId);
        assertNull(pi);
    }
    
    @Test(timeout = 60000)
    public void testIntermediateCatchEventMessage() throws Exception {
        Long processInstanceId = startAndAssertProcess(INTERMEDIATE_CATCH_EVENT_MESSAGE_PROCESS_ID);
        
        countDownListener.configure(INTERMEDIATE_CATCH_EVENT_MESSAGE_PROCESS_ID, 1);
        
        autocompleteSingleTask();
        
        sendEvent(INTERMEDIATE_MESSAGE, USELESS_DATA_EVENT);
        countDownListener.getCountDown().await();
        
        ProcessInstance pi = processService.getProcessInstance(processInstanceId);
        assertNull(pi);
    }
    
    @Test(timeout = 60000)
    public void testSubprocessSignal() throws Exception {
        int numOfSignals = 4;
        Long processInstanceId = startAndAssertProcess(SUBPROCESS_SIGNAL_PROCESS_ID);
        
        countDownListener.configureNode(SUBPROCESS_SIGNAL_PROCESS_ID, SUBPROCESS_SCRIPT_NODE, numOfSignals);
        
        for(int i = 0; i < numOfSignals; i++)
            sendEvent(SUBPROCESS_SIGNAL, USELESS_DATA_EVENT);
        
        countDownListener.getCountDown().await();
        
        autocompleteSingleTask();
        
        ProcessInstance pi = processService.getProcessInstance(processInstanceId);
        assertNull(pi);
    }
    
    @Test(timeout = 60000)
    public void testSubprocessMessage() throws Exception {
        int numOfMessages = 4;
        Long processInstanceId = startAndAssertProcess(SUBPROCESS_MESSAGE_PROCESS_ID);
        
        countDownListener.configureNode(SUBPROCESS_MESSAGE_PROCESS_ID, SUBPROCESS_SCRIPT_NODE, numOfMessages);
        
        for(int i = 0; i < numOfMessages; i++)
            sendEvent(SUBPROCESS_MESSAGE, USELESS_DATA_EVENT);
        
        countDownListener.getCountDown().await();
        
        autocompleteSingleTask();
        
        ProcessInstance pi = processService.getProcessInstance(processInstanceId);
        assertNull(pi);
    }
    
    @Test(timeout = 60000)
    public void testBoundarySignalMultipleProcesses() throws InterruptedException {
        int numOfProcesses = 5;
        
        Long[] pids = startAndAssertProcesses(BOUNDARY_SIGNAL_PROCESS_ID, numOfProcesses);
        
        countDownListener.configure(BOUNDARY_SIGNAL_PROCESS_ID, numOfProcesses);
        
        //Just a single signal for the all processes
        sendEvent(BOUNDARY_SIGNAL, USELESS_DATA_EVENT);
        
        countDownListener.getCountDown().await();
        
        assertNullProcesses(numOfProcesses, pids);
    }
    
    @Test(timeout = 60000)
    public void testBoundaryMessageMultipleProcesses() throws InterruptedException {
        int numOfProcesses = 5;
        
        Long[] pids = startAndAssertProcesses(BOUNDARY_MESSAGE_PROCESS_ID, numOfProcesses);
        
        countDownListener.configure(BOUNDARY_MESSAGE_PROCESS_ID, numOfProcesses);
        
        //Just a single message for the all processes
        //Notice that message is for peer-to-peer communication, but here it works like a signal (broadcast)
        sendEvent(BOUNDARY_MESSAGE, USELESS_DATA_EVENT);
        
        countDownListener.getCountDown().await();
        
        assertNullProcesses(numOfProcesses, pids);
    }
    
    @Test(timeout = 60000)
    public void testStartMessagePojo() throws InterruptedException {
        startMessagePojoParam(MONEY_DATA_EVENT, AMOUNT_294, CURRENCY_EUR);
    }
    
    @Test(timeout = 60000)
    public void testStartMessagePojoFirstParamNull() throws InterruptedException {
        startMessagePojoParam(MONEY_DATA_NULL_AMOUNT_EVENT, null, CURRENCY_EUR);
    }
    
    @Test(timeout = 60000)
    public void testStartMessagePojoSecondParamNull() throws InterruptedException {
        startMessagePojoParam(MONEY_DATA_NULL_CURRENCY_EVENT, AMOUNT_294, null);
    }
    
    @Test(timeout = 60000)
    public void testStartMessagePojoBothParamsNull() throws InterruptedException {
        startMessagePojoParam(MONEY_DATA_NULL_BOTH_EVENT, null, null);
    }
    
    @Test(timeout = 60000)
    public void testStartMessageComplexPojo() throws InterruptedException {
        countDownListener.configure(START_MESSAGE_COMPLEX_POJO_PROCESS_ID, 1);
        
        sendEvent(START_MESSAGE_COMPLEX_POJO, REIMBURSEMENT_DATA_EVENT);
        
        countDownListener.getCountDown().await();
        
        Long pid = countDownListener.getIds().get(0);
        Map<String, Object> map = getVariableMap(pid, "assignedReimbursement");
        
        assertEquals(2, ((List<?>)map.get("expenses")).size());
        
        processService.abortProcessInstance(pid);
    }
    
    @Test(timeout = 60000)
    public void testStartSignalsTransactional() throws InterruptedException {
        countDownListener.configure(START_SIGNAL_PROCESS_ID, 4);
        
        Map<String, List<String>> map =createTopicEventsMap(START_SIGNAL, 
                Arrays.asList(USELESS_DATA_EVENT, USELESS_DATA_EVENT, USELESS_DATA_EVENT, USELESS_DATA_EVENT));
        
        kafkaFixture.sendTransactionalRecords(map);
        
        countDownListener.getCountDown().await();
        
        List<Long> pids = countDownListener.getIds();
        assertEquals(4, pids.size());
        
        assertActiveProcessesAndAbort(pids);
    }
    
    @Test(timeout = 60000)
    public void testStartMessagesTransactionalMalformedEvents() throws InterruptedException {
        countDownListener.configure(START_MESSAGE_PROCESS_ID, 2);
        
        Map<String, List<String>> map =createTopicEventsMap(START_MESSAGE, 
                Arrays.asList(MALFORMED_EVENT, USELESS_DATA_EVENT, MALFORMED_EVENT,
                USELESS_DATA_EVENT, MALFORMED_EVENT));
        
        kafkaFixture.sendTransactionalRecords(map);
        
        countDownListener.getCountDown().await();
        
        List<Long> pids = countDownListener.getIds();
        assertEquals(2, pids.size());
        
        assertActiveProcessesAndAbort(pids);
    }
    
    @Test(timeout = 60000)
    public void testStartSignalsAndMessagesTransactional() throws InterruptedException {
        int numOfEvents = 12;
        countDownListener.configure(START_SIGNAL_PROCESS_ID, numOfEvents*2);
        
        List<String> events = new ArrayList<>();
        
        for (int i=0; i<numOfEvents; i++) {
            events.add(USELESS_DATA_EVENT);
        }
        
        Map<String, List<String>> map = createTopicEventsMap(START_SIGNAL, events);
        
        map.putAll(createTopicEventsMap(START_MESSAGE, events));
        
        kafkaFixture.sendTransactionalRecords(map);
        
        countDownListener.getCountDown().await();
        
        List<Long> pids = countDownListener.getIds();
        assertEquals(numOfEvents*2, pids.size());
        
        assertActiveProcessesAndAbort(pids);
    }
    
    @Test(timeout = 60000)
    public void testStartMessagePojoMismatchedInput() throws InterruptedException {
        countDownListener.configure(START_MESSAGE_POJO_PROCESS_ID, 1);
        
        sendEvent(START_MESSAGE_POJO, USELESS_DATA_EVENT);
        
        assertExceptionInLogs(listAppender, MarshallingException.class);
    }
    
    @Test(timeout = 60000)
    public void testStartMessagePojoMalformedData() throws InterruptedException {
        countDownListener.configure(START_MESSAGE_POJO_PROCESS_ID, 1);
        
        sendEvent(START_MESSAGE_POJO, MALFORMED_EVENT);
        
        assertExceptionInLogs(listAppender, JsonEOFException.class);
    }
    
    @Test(timeout = 60000)
    public void testStartMessagePojoWrongType() throws InterruptedException {
        countDownListener.configure(START_MESSAGE_POJO_PROCESS_ID, 1);
        
        sendEvent(START_MESSAGE_POJO, MONEY_DATA_WRONG_TYPE_EVENT);
        
        assertExceptionInLogs(listAppender, MarshallingException.class);
    }
    
    @Test(timeout = 60000)
    public void testStartMessagePojoClassNotFound() throws InterruptedException {
        countDownListener.configure(START_MESSAGE_POJO_CLASS_NOT_FOUND_PROCESS_ID, 1);
        
        sendEvent(START_MESSAGE_POJO_CLASS_NOT_FOUND, MONEY_DATA_EVENT);
        
        assertExceptionInLogs(listAppender, ClassNotFoundException.class);
    }
    
    @Test(timeout = 60000)
    @Ignore("Wrong dates are ignored since only data field is used in cloud event for now")
    public void testStartMessageParseException() throws InterruptedException {
        countDownListener.configure(START_MESSAGE_POJO_PROCESS_ID, 1);
        
        sendEvent(START_MESSAGE_POJO, MONEY_DATA_WRONG_DATE_EVENT);
        
        assertExceptionInLogs(listAppender, ParseException.class);
    }
    
    @Test(timeout = 60000)
    public void testStartMessageInDifferentContainers() throws InterruptedException {
        startInDifferentContainers(START_MESSAGE_PROCESS_ID, START_MESSAGE);
    }
    
    @Test(timeout = 60000)
    public void testStartSignalInDifferentContainers() throws InterruptedException {
        startInDifferentContainers(START_SIGNAL_PROCESS_ID, START_SIGNAL);
    }

    protected void startInDifferentContainers(String processId, String eventName) throws InterruptedException {
        countDownListener.configure(processId, 2);
        
        KModuleDeploymentUnit altDeploymentUnit = kafkaFixture.setup(deploymentService, ALT_PROJECT, SINGLETON.name());
        
        try {
            sendEvent(eventName, USELESS_DATA_EVENT);
            countDownListener.getCountDown().await();
            
            assertEquals(2, countDownListener.getIds().size());
            
            for (Long pid: countDownListener.getIds()) {
                try {
                    processService.abortProcessInstance(altDeploymentUnit.getIdentifier(), pid);
                } catch(Exception e) {
                    //if pid corresponds to the other deployment unit
                    processService.abortProcessInstance(unit.getIdentifier(), pid);
                }
            }
        } finally {
            if (deploymentService!=null) {
                deploymentService.undeploy(altDeploymentUnit);
            }
        }
    }
    
    protected void startMessagePojoParam(String dataFile, String amount, String currency) throws InterruptedException {
        countDownListener.configure(START_MESSAGE_POJO_PROCESS_ID, 1);
        
        sendEvent(START_MESSAGE_POJO, dataFile);
        
        countDownListener.getCountDown().await();
        
        assertEquals(1, countDownListener.getIds().size());
        Long pid = countDownListener.getIds().get(0);
        assertAssignedMoney(pid, amount, currency);
        
        processService.abortProcessInstance(pid);
    }
    
    protected void assertExceptionInLogs(ListAppender<ILoggingEvent> listAppender, Class<?> clazz) throws InterruptedException {
        countDownListener.getCountDown().await(2, TimeUnit.SECONDS);
        
        Optional<ILoggingEvent> logEvent = kafkaFixture.getErrorLog(listAppender);
        assertEquals(clazz.getCanonicalName(), logEvent.get().getThrowableProxy().getClassName());
        
        assertEquals(0, countDownListener.getIds().size());
    }

    protected Long startAndAssertProcess(String processId) {
        Long pid1 = processService.startProcess(deploymentId, processId);
        assertNotNull(pid1);
        assertTrue(pid1 > 0);
        return pid1;
    }
    
    protected void autocompleteSingleTask() {
        List<TaskSummary> tasks =  runtimeDataService.getTasksAssignedAsPotentialOwner(JOHN, new QueryFilter());
        userTaskService.completeAutoProgress(tasks.get(0).getId(), JOHN, emptyMap());
    }

    protected Long[] startAndAssertProcesses(String processId, int numOfProcesses) {
        Long[] pids = new Long[numOfProcesses];
        
        for (int i = 0; i < numOfProcesses; i++)
            pids[i] = startAndAssertProcess(processId);
        return pids;
    }
    
    protected void sendEvent(String topic, String filename) {
        kafkaFixture.sendRecord(topic, readData(filename));
    }
    
    protected Map<String, List<String>> createTopicEventsMap(String topic, List<String> filenames) {
        Map<String, List<String>> map = new HashMap<>();
        map.put(topic, filenames.stream().map(this::readData).collect(Collectors.toList()));
        return map;
    }

    protected String readData(String filename) {
        String result = null;
        try {
            result = IOUtils.toString(this.getClass().getResourceAsStream("/producer/data/"+filename), UTF_8);
        } catch (IOException e) {
            fail("Not expected exception: " + e.getMessage());
        }
        return result;
    }
    
    protected void assertAssignedMoney(Long pid, String amount, String currency) {
        Map<String, Object> map = getVariableMap(pid, "assignedMoney");
        
        int expectedAmount = amount!=null ? Integer.parseInt(amount) : 0;
        assertEquals(expectedAmount, map.get("amount"));
        
        assertEquals(currency, map.get("currency"));
    }
    
    protected Map<String, Object> getVariableMap(Long pid, String variableName) {
        Object instanceVar = processService.getProcessInstanceVariable(deploymentId, pid, variableName);
        assertNotNull(instanceVar);

        // Convert Pojo to Map
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = mapper.convertValue(instanceVar, new TypeReference<Map<String, Object>>() {});
        return map;
    }

    protected void assertNullProcesses(int numOfProcesses, Long[] pids) {
        for (int i = 0; i < numOfProcesses; i++) {
            ProcessInstance pi = processService.getProcessInstance(pids[i]);
            assertNull(pi);
        }
    }
    
    protected void assertActiveProcessesAndAbort(List<Long> pids) {
        for (Long pid: pids) {
            assertThat(processService.getProcessInstance(deploymentId, pid).getState(), is(STATE_ACTIVE));
            processService.abortProcessInstance(pid);
        }
    }
}
