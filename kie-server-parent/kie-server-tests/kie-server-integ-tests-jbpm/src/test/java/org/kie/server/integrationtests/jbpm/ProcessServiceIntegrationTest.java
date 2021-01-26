/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.integrationtests.jbpm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.api.KieServices;
import org.kie.api.task.model.Status;
import org.kie.internal.KieInternalServices;
import org.kie.internal.executor.api.STATUS;
import org.kie.internal.process.CorrelationKey;
import org.kie.internal.process.CorrelationKeyFactory;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.exception.KieServicesException;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieServerConfigItem;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.instance.NodeInstance;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.RequestInfoInstance;
import org.kie.server.api.model.instance.TaskInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.api.model.instance.VariableInstance;
import org.kie.server.api.model.instance.WorkItemInstance;
import org.kie.server.integrationtests.category.Smoke;
import org.kie.server.integrationtests.category.UnstableOnJenkinsPrBuilder;
import org.kie.server.integrationtests.config.TestConfig;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.kie.server.integrationtests.shared.KieServerReflections;
import org.kie.server.integrationtests.shared.KieServerSynchronization;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED;
import static org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE;
import static org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED;

public class ProcessServiceIntegrationTest extends JbpmKieServerBaseIntegrationTest {

    private static final KieServerConfigItem PPI_RUNTIME_STRATEGY = new KieServerConfigItem(KieServerConstants.PCFG_RUNTIME_STRATEGY, RuntimeStrategy.PER_PROCESS_INSTANCE.name(), String.class.getName());
    
    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");

    protected static final String SORT_BY_PROCESS_ID = "ProcessId";

    private static final String CONTAINER_ID_ALIAS = "Javierito";


    @BeforeClass
    public static void buildAndDeployArtifacts() {

        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProjectFromResource("/kjars-sources/definition-project");
        KieServerDeployer.buildAndDeployMavenProjectFromResource("/kjars-sources/exception-handling");

        kieContainer = KieServices.Factory.get().newKieContainer(releaseId);

        createContainer(CONTAINER_ID, releaseId, CONTAINER_ID_ALIAS, PPI_RUNTIME_STRATEGY);

        createContainer(CONTAINER_ID_RESTART, new ReleaseId("org.kie.server.testing", "exception-handling",
                                                       "1.0.0.Final"), PPI_RUNTIME_STRATEGY);
    }

    @Override
    protected void addExtraCustomClasses(Map<String, Class<?>> extraClasses) throws Exception {
        extraClasses.put(PERSON_CLASS_NAME, Class.forName(PERSON_CLASS_NAME, true, kieContainer.getClassLoader()));
    }

    @Test
    public void testStartCheckVariablesAndAbortProcess() throws Exception {
        Class<?> personClass = Class.forName(PERSON_CLASS_NAME, true, kieContainer.getClassLoader());

        Object person = createPersonInstance(USER_JOHN);

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("test", USER_MARY);
        parameters.put("number", new Integer(12345));

        List<Object> list = new ArrayList<Object>();
        list.add("item");

        parameters.put("list", list);
        parameters.put("person", person);
        Long processInstanceId = null;
        try {
            processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);

            assertNotNull(processInstanceId);
            assertTrue(processInstanceId.longValue() > 0);

            Object personVariable = processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "person");
            assertNotNull(personVariable);
            assertTrue(personClass.isAssignableFrom(personVariable.getClass()));

            personVariable = processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "person");
            assertNotNull(personVariable);
            assertTrue(personClass.isAssignableFrom(personVariable.getClass()));

            Map<String, Object> variables = processClient.getProcessInstanceVariables(CONTAINER_ID, processInstanceId);
            assertNotNull(variables);
            assertEquals(5, variables.size());
            assertTrue(variables.containsKey("test"));
            assertTrue(variables.containsKey("number"));
            assertTrue(variables.containsKey("list"));
            assertTrue(variables.containsKey("person"));
            assertTrue(variables.containsKey("initiator"));

            assertNotNull(variables.get("test"));
            assertNotNull(variables.get("number"));
            assertNotNull(variables.get("list"));
            assertNotNull(variables.get("person"));
            assertNotNull(variables.get("initiator"));

            assertTrue(String.class.isAssignableFrom(variables.get("test").getClass()));
            assertTrue(Integer.class.isAssignableFrom(variables.get("number").getClass()));
            assertTrue(List.class.isAssignableFrom(variables.get("list").getClass()));
            assertTrue(personClass.isAssignableFrom(variables.get("person").getClass()));
            assertTrue(String.class.isAssignableFrom(variables.get("initiator").getClass()));

            assertEquals(USER_MARY, variables.get("test"));
            assertEquals(12345, variables.get("number"));
            assertEquals(1, ((List<?>) variables.get("list")).size());
            assertEquals("item", ((List<?>) variables.get("list")).get(0));
            assertEquals(USER_JOHN, KieServerReflections.valueOf(variables.get("person"), "name"));
            assertEquals(TestConfig.getUsername(), variables.get("initiator"));
        } finally {
            if (processInstanceId != null) {
                processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            }
        }



    }

    @Test(expected = KieServicesException.class)
    public void testStartNotExistingProcess() {
        processClient.startProcess(CONTAINER_ID, "not-existing", (Map<String, Object>) null);
    }

    @Test()
    public void testAbortExistingProcess() {
        Map<String, Object> parameters = new HashMap<String, Object>();

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        try {
            assertNotNull(processInstanceId);
            assertTrue(processInstanceId.longValue() > 0);

            // Process instance is running and is active.
            ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(processInstance);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, processInstance.getState().intValue());

            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);

            // Process instance is now aborted.
            processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(processInstance);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED, processInstance.getState().intValue());
        } catch (Exception e) {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            fail(e.getMessage());
        }
    }
    
    @Test
    public void testAbortExistingProcessWrongContainer() {
        Map<String, Object> parameters = new HashMap<>();

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        try {
            assertNotNull(processInstanceId);
            assertTrue(processInstanceId > 0);

            // Process instance is running and is active.
            ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(processInstance);
            assertEquals(STATE_ACTIVE, processInstance.getState().intValue());
            assertThrows(KieServicesException.class,
                    () -> processClient.abortProcessInstance(CONTAINER_ID_RESTART, processInstanceId));
        }
        finally {
            processClient.abortProcessInstance(CONTAINER_ID , processInstanceId);
        }

    }

    @Test(expected = KieServicesException.class)
    public void testAbortNonExistingProcess() {
        processClient.abortProcessInstance(CONTAINER_ID, 9999l);
    }

    @Test
    public void testAbortProcessInNonExistingContainer() {
        assertClientException(() -> processClient.abortProcessInstance(BAD_CONTAINER_ID, 9999L), 404, BAD_CONTAINER_ID);
    }

    @Test(expected = KieServicesException.class)
    public void testStartCheckNonExistingVariables() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        try {
            processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "person");
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }

    }

    @Test
    public void testAbortMultipleProcessInstances() throws Exception {

        Long processInstanceId1 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION);
        Long processInstanceId2 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION);
        Long processInstanceId3 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION);
        Long processInstanceId4 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION);

        List<Long> processInstances = new ArrayList<Long>();
        processInstances.add(processInstanceId1);
        processInstances.add(processInstanceId2);
        processInstances.add(processInstanceId3);
        processInstances.add(processInstanceId4);

        processClient.abortProcessInstances(CONTAINER_ID, processInstances);
    }

    @Test
    public void testSignalProcessInstance() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        try {
            checkAvailableSignals(CONTAINER_ID, processInstanceId);

            Object person = createPersonInstance(USER_JOHN);
            processClient.signalProcessInstance(CONTAINER_ID, processInstanceId, "Signal1", person);

            processClient.signalProcessInstance(CONTAINER_ID, processInstanceId, "Signal2", "My custom string event");
        } catch (Exception e) {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            fail(e.getMessage());
        }
    }
    
    @Test
    public void testSignalProcessInstanceByCorrelationKey() {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId > 0);
        
        ProcessInstance pi = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
        CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();
        CorrelationKey correlationKey = correlationKeyFactory.newCorrelationKey(pi.getCorrelationKey());
        try {
            checkAvailableSignals(CONTAINER_ID, processInstanceId);

            Object person = createPersonInstance(USER_JOHN);
            processClient.signalProcessInstanceByCorrelationKey(CONTAINER_ID, correlationKey, "Signal1", person);

            processClient.signalProcessInstanceByCorrelationKey(CONTAINER_ID, correlationKey, "Signal2", "My custom string event");
            
            // only can be completed if they receive both signals
            assertThat(processClient.getProcessInstance(CONTAINER_ID, processInstanceId).getState(), is(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED));
        } catch (Exception e) {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            fail(e.getMessage());
        }
    }

    @Test
    public void testSignalProcessInstancesByCorrelationKeys() {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId > 0);
        ProcessInstance pi1 = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);

        Long processInstanceId2 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);
        assertNotNull(processInstanceId2);
        assertTrue(processInstanceId2.longValue() > 0);
        ProcessInstance pi2 = processClient.getProcessInstance(CONTAINER_ID, processInstanceId2);
        

        CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();
        CorrelationKey correlationKey1 = correlationKeyFactory.newCorrelationKey(pi1.getCorrelationKey());
        CorrelationKey correlationKey2 = correlationKeyFactory.newCorrelationKey(pi2.getCorrelationKey());

        List<CorrelationKey> correlationKeys = new ArrayList<>();
        correlationKeys.add(correlationKey1);
        correlationKeys.add(correlationKey2);

        try {
            checkAvailableSignals(CONTAINER_ID, processInstanceId);
            checkAvailableSignals(CONTAINER_ID, processInstanceId2);

            Object person = createPersonInstance(USER_JOHN);
            processClient.signalProcessInstancesByCorrelationKeys(CONTAINER_ID, correlationKeys, "Signal1", person);

            processClient.signalProcessInstancesByCorrelationKeys(CONTAINER_ID, correlationKeys, "Signal2", "My custom string event");
            
            // only can be completed if they receive both signals
            assertThat(processClient.getProcessInstance(CONTAINER_ID, processInstanceId).getState(), is(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED));
            assertThat(processClient.getProcessInstance(CONTAINER_ID, processInstanceId2).getState(), is(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED));
        } catch (Exception e) {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId2);
            fail(e.getMessage());
        }

    }

    @Test
    public void testSignalProcessInstanceWrongContainer() {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId > 0);

        try {
            checkAvailableSignals(CONTAINER_ID, processInstanceId);

            Object person = createPersonInstance(USER_JOHN);
            assertThrows(KieServicesException.class, () -> processClient.signalProcessInstance(CONTAINER_ID_RESTART,
                    processInstanceId, "Signal1", person));
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testSignalProcessInstanceNullEvent() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        try {
            checkAvailableSignals(CONTAINER_ID, processInstanceId);

            processClient.signalProcessInstance(CONTAINER_ID, processInstanceId, "Signal1", null);

            processClient.signalProcessInstance(CONTAINER_ID, processInstanceId, "Signal2", null);
        } catch (Exception e) {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            fail(e.getMessage());
        }

    }

    @Test
    public void testBoundarySignalProcessInstance() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_BOUNDARY_SIGNAL_PROCESS);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        try {
            checkAvailableBoundarySignals(CONTAINER_ID, processInstanceId);
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testBoundarySignalWithExpressionProcessInstance() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_BOUNDARY_SIGNAL_EXPRESSION_PROCESS);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        try {
            checkAvailableBoundarySignals(CONTAINER_ID, processInstanceId);
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }



    @Test
    public void testSignalProcessInstances() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        Long processInstanceId2 = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);
        assertNotNull(processInstanceId2);
        assertTrue(processInstanceId2.longValue() > 0);

        List<Long> processInstanceIds = new ArrayList<Long>();
        processInstanceIds.add(processInstanceId);
        processInstanceIds.add(processInstanceId2);

        try {
            checkAvailableSignals(CONTAINER_ID, processInstanceId);
            checkAvailableSignals(CONTAINER_ID, processInstanceId2);

            Object person = createPersonInstance(USER_JOHN);
            processClient.signalProcessInstances(CONTAINER_ID, processInstanceIds, "Signal1", person);

            processClient.signalProcessInstances(CONTAINER_ID, processInstanceIds, "Signal2", "My custom string event");
        } catch (Exception e) {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId2);
            fail(e.getMessage());
        }

    }

    @Test
    public void testManipulateProcessVariable() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        try {
            Object personVar = null;
            try {
                personVar = processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "personData");
                fail("Should fail as there is no process variable personData set yet");
            } catch (KieServicesException e) {
                // expected
            }
            assertNull(personVar);

            personVar = createPersonInstance(USER_JOHN);
            processClient.setProcessVariable(CONTAINER_ID, processInstanceId, "personData", personVar);

            personVar = processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "personData");
            assertNotNull(personVar);
            assertEquals(USER_JOHN, KieServerReflections.valueOf(personVar, "name"));


            processClient.setProcessVariable(CONTAINER_ID, processInstanceId, "stringData", "custom value");

            String stringVar = (String) processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "stringData");
            assertNotNull(personVar);
            assertEquals("custom value", stringVar);

        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }

    }
    
    @Test
    public void testManipulateProcessVariableWrongContainer() {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId > 0);

        try {
            Object personVar = createPersonInstance(USER_JOHN);
            assertNotNull(personVar);
            assertThrows(KieServicesException.class, () -> processClient.setProcessVariable(CONTAINER_ID_RESTART,
                    processInstanceId, "personData", personVar));
            
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }

    }

    @Test
    public void testManipulateProcessVariables() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        try {
            Object personVar = null;
            String stringVar = null;
            try {
                personVar = processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "personData");
                fail("Should fail as there is no process variable personData set yet");
            } catch (KieServicesException e) {
                // expected
            }
            assertNull(personVar);

            try {
                stringVar = (String) processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "stringData");
                fail("Should fail as there is no process variable personData set yet");
            } catch (KieServicesException e) {
                // expected
            }
            assertNull(personVar);
            assertNull(stringVar);

            personVar = createPersonInstance(USER_JOHN);
            stringVar = "string variable test";

            Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("personData", personVar);
            variables.put("stringData", stringVar);

            processClient.setProcessVariables(CONTAINER_ID, processInstanceId, variables);

            personVar = processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "personData");
            assertNotNull(personVar);
            assertEquals(USER_JOHN, KieServerReflections.valueOf(personVar, "name"));

            stringVar = (String) processClient.getProcessInstanceVariable(CONTAINER_ID, processInstanceId, "stringData");
            assertNotNull(personVar);
            assertEquals("string variable test", stringVar);

        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }

    }

    @Test
    @Category(Smoke.class)
    public void testGetProcessInstance() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        try {
            ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            ProcessInstance expectedInstance = createSignalProcessInstance(processInstanceId);
            assertProcessInstance(expectedInstance, processInstance);

            Map<String, Object> variables = processInstance.getVariables();
            assertNull(variables);
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }

    }
    
        
    @Test
    public void testGetProcessInstanceWrongContainer() {
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId > 0);
        
        try {
            assertThrows(KieServicesException.class, () -> processClient.getProcessInstance(CONTAINER_ID_RESTART,
                    processInstanceId, true));
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testGetProcessInstanceWithVariables() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        try {
            ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId, true);
            ProcessInstance expectedInstance = createSignalProcessInstance(processInstanceId);
            assertProcessInstance(expectedInstance, processInstance);

            Map<String, Object> variables = processInstance.getVariables();
            assertNotNull(variables);
            assertEquals(4, variables.size());

            assertTrue(variables.containsKey("stringData"));
            assertTrue(variables.containsKey("personData"));
            assertTrue(variables.containsKey("initiator"));
            assertTrue(variables.containsKey("nullAccepted"));

            String stringVar = (String) variables.get("stringData");
            Object personVar = variables.get("personData");
            String initiator = (String) variables.get("initiator");

            assertNotNull(personVar);
            assertEquals(USER_JOHN, KieServerReflections.valueOf(personVar, "name"));

            assertNotNull(personVar);
            assertEquals("waiting for signal", stringVar);

            assertNotNull(initiator);
            assertEquals(TestConfig.getUsername(), initiator);

        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }

    }
    



    @Test(expected = KieServicesException.class)
    public void testGetNonExistingProcessInstance() {
        processClient.getProcessInstance(CONTAINER_ID, -9999l);
    }

    @Test
    public void testWorkItemOperations() throws Exception {

        Map<String, Object> parameters = new HashMap<String, Object>();

        parameters.put("person", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        try {
            assertNotNull(processInstanceId);
            assertTrue(processInstanceId.longValue() > 0);

            // Completing human task so we can move in process flow to work item.
            // User task shouldn't be handled as work item because in such case it doesn't behave consistently:
            // i.e. leaving open tasks after finishing process instance.
            List<String> status = Arrays.asList(Status.Ready.toString());
            List<TaskSummary> taskList = taskClient.findTasksByStatusByProcessInstanceId(processInstanceId, status, 0, 10);

            assertEquals(1, taskList.size());
            TaskSummary taskSummary = taskList.get(0);
            taskClient.startTask(CONTAINER_ID, taskSummary.getId(), USER_YODA);
            taskClient.completeTask(CONTAINER_ID, taskSummary.getId(), USER_YODA, null);

            TaskInstance userTask = taskClient.findTaskById(taskSummary.getId());
            assertNotNull(userTask);
            assertEquals("Evaluate items?", userTask.getName());
            assertEquals(Status.Completed.toString(), userTask.getStatus());

            List<WorkItemInstance> workItems = processClient.getWorkItemByProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(workItems);
            assertEquals(1, workItems.size());

            WorkItemInstance workItemInstance = workItems.get(0);
            assertNotNull(workItemInstance);
            assertEquals(processInstanceId, workItemInstance.getProcessInstanceId());
            assertEquals("Email", workItemInstance.getName());
            assertEquals(CONTAINER_ID, workItemInstance.getContainerId());
            assertEquals(0, workItemInstance.getState().intValue());
            assertEquals(5, workItemInstance.getParameters().size());

            assertNotNull(workItemInstance.getId());
            assertNotNull(workItemInstance.getNodeId());
            assertNotNull(workItemInstance.getProcessInstanceId());


            workItemInstance = processClient.getWorkItem(CONTAINER_ID, processInstanceId, workItemInstance.getId());
            assertNotNull(workItemInstance);
            assertEquals(processInstanceId, workItemInstance.getProcessInstanceId());
            assertEquals("Email", workItemInstance.getName());
            assertEquals(CONTAINER_ID, workItemInstance.getContainerId());
            assertEquals(0, workItemInstance.getState().intValue());
            assertEquals(5, workItemInstance.getParameters().size());

            assertNotNull(workItemInstance.getId());
            assertNotNull(workItemInstance.getNodeId());
            assertNotNull(workItemInstance.getProcessInstanceId());

            processClient.abortWorkItem(CONTAINER_ID, processInstanceId, workItemInstance.getId());

            ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(processInstance);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, processInstance.getState().intValue());

        } catch (Exception e){
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            throw e;
        } finally {
            changeUser(TestConfig.getUsername());
        }
    }
    
    @Test
    public void testWorkItemOperationWrongContainer() throws Exception {

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("person", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        try {
            assertNotNull(processInstanceId);
            assertTrue(processInstanceId > 0);

            // Completing human task so we can move in process flow to work item.
            // User task shouldn't be handled as work item because in such case it doesn't behave consistently:
            // i.e. leaving open tasks after finishing process instance.
            List<String> status = Collections.singletonList(Status.Ready.toString());
            List<TaskSummary> taskList = taskClient.findTasksByStatusByProcessInstanceId(processInstanceId, status, 0, 10);

            assertEquals(1, taskList.size());
            TaskSummary taskSummary = taskList.get(0);
            taskClient.startTask(CONTAINER_ID, taskSummary.getId(), USER_YODA);
            taskClient.completeTask(CONTAINER_ID, taskSummary.getId(), USER_YODA, null);

            TaskInstance userTask = taskClient.findTaskById(taskSummary.getId());
            assertNotNull(userTask);
            assertEquals("Evaluate items?", userTask.getName());
            assertEquals(Status.Completed.toString(), userTask.getStatus());
            assertThrows(KieServicesException.class, () -> processClient.getWorkItemByProcessInstance(
                    CONTAINER_ID_RESTART, processInstanceId));
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            changeUser(TestConfig.getUsername());
        }
    }

    @Test
    public void testWorkItemOperationComplete() throws Exception {

        Map<String, Object> parameters = new HashMap<String, Object>();

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        try {
            assertNotNull(processInstanceId);
            assertTrue(processInstanceId.longValue() > 0);

            // Completing human task so we can move in process flow to work item.
            // User task shouldn't be handled as work item because in such case it doesn't behave consistently:
            // i.e. leaving open tasks after finishing process instance.
            List<String> status = Arrays.asList(Status.Ready.toString());
            List<TaskSummary> taskList = taskClient.findTasksByStatusByProcessInstanceId(processInstanceId, status, 0, 10);

            assertEquals(1, taskList.size());
            TaskSummary taskSummary = taskList.get(0);
            taskClient.startTask(CONTAINER_ID, taskSummary.getId(), USER_YODA);
            taskClient.completeTask(CONTAINER_ID, taskSummary.getId(), USER_YODA, null);

            TaskInstance userTask = taskClient.findTaskById(taskSummary.getId());
            assertNotNull(userTask);
            assertEquals("Evaluate items?", userTask.getName());
            assertEquals(Status.Completed.toString(), userTask.getStatus());

            List<WorkItemInstance> workItems = processClient.getWorkItemByProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(workItems);
            assertEquals(1, workItems.size());

            WorkItemInstance workItemInstance = workItems.get(0);
            assertNotNull(workItemInstance);

            processClient.completeWorkItem(CONTAINER_ID, processInstanceId, workItemInstance.getId(), parameters);

            ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(processInstance);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, processInstance.getState().intValue());

        } catch (Exception e){
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            throw e;
        } finally {
            changeUser(TestConfig.getUsername());
        }
    }
    
    @Test
    public void testWorkItemOperationCompleteWrongContainer() throws Exception {

        Map<String, Object> parameters = new HashMap<>();

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        try {
            assertNotNull(processInstanceId);
            assertTrue(processInstanceId > 0);

            // Completing human task so we can move in process flow to work item.
            // User task shouldn't be handled as work item because in such case it doesn't behave consistently:
            // i.e. leaving open tasks after finishing process instance.
            List<String> status = Collections.singletonList(Status.Ready.toString());
            List<TaskSummary> taskList = taskClient.findTasksByStatusByProcessInstanceId(processInstanceId, status, 0, 10);

            assertEquals(1, taskList.size());
            TaskSummary taskSummary = taskList.get(0);
            taskClient.startTask(CONTAINER_ID, taskSummary.getId(), USER_YODA);
            taskClient.completeTask(CONTAINER_ID, taskSummary.getId(), USER_YODA, null);

            TaskInstance userTask = taskClient.findTaskById(taskSummary.getId());
            assertNotNull(userTask);
            assertEquals("Evaluate items?", userTask.getName());
            assertEquals(Status.Completed.toString(), userTask.getStatus());

            List<WorkItemInstance> workItems = processClient.getWorkItemByProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(workItems);
            assertEquals(1, workItems.size());

            WorkItemInstance workItemInstance = workItems.get(0);
            assertNotNull(workItemInstance);

            assertThrows(KieServicesException.class, () -> processClient.completeWorkItem(CONTAINER_ID_RESTART,
                    processInstanceId, workItemInstance.getId(), parameters));
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            changeUser(TestConfig.getUsername());
        }
    }


    @Test
    public void testStartCheckProcessWithCorrelationKey() throws Exception {
        String firstSimpleKey = "first-simple-key";
        String secondSimpleKey = "second-simple-key";
        String stringVarName = "stringData";
        String stringVarValue = "string variable test";

        CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

        CorrelationKey firstKey = correlationKeyFactory.newCorrelationKey(firstSimpleKey);
        CorrelationKey secondKey = correlationKeyFactory.newCorrelationKey(secondSimpleKey);

        Long firstProcessInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, firstKey);
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(stringVarName, stringVarValue);
        Long secondProcessInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, secondKey, parameters);
        try {
            ProcessInstance instance = processClient.getProcessInstance(CONTAINER_ID, firstProcessInstanceId);
            ProcessInstance expected = createEvaluationProcessInstace(firstProcessInstanceId);
            assertProcessInstance(expected, instance);
            assertEquals(firstSimpleKey, instance.getCorrelationKey());

            instance = processClient.getProcessInstance(CONTAINER_ID, secondProcessInstanceId, true);
            expected = createEvaluationProcessInstace(secondProcessInstanceId);
            assertProcessInstance(expected, instance);
            assertEquals(secondSimpleKey, instance.getCorrelationKey());
            assertTrue(instance.getVariables().containsKey(stringVarName));
            assertEquals(stringVarValue, instance.getVariables().get(stringVarName));
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, firstProcessInstanceId);
            processClient.abortProcessInstance(CONTAINER_ID, secondProcessInstanceId);
        }
    }

    @Test
    public void testStartProcessWithCustomTask() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("id", "custom id");
        Long processInstanceId = null;
        try {
            processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_CUSTOM_TASK);

            assertNotNull(processInstanceId);
            assertTrue(processInstanceId.longValue() > 0);

            ProcessInstance pi = queryClient.findProcessInstanceById(processInstanceId);
            assertNotNull(pi);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, pi.getState().intValue());

        } catch(Exception e) {
            if (processInstanceId != null) {
                processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            }
            fail("Exception " + e.getMessage());
        }
    }

    @Test
    @Category({UnstableOnJenkinsPrBuilder.class})
    public void testSignalContainer() {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId > 0);

        try {
            checkAvailableSignals(CONTAINER_ID, processInstanceId);

            Object person = createPersonInstance(USER_JOHN);
            processClient.signal(CONTAINER_ID, "Signal1", person);

            processClient.signal(CONTAINER_ID, "Signal2", "My custom string event");

            ProcessInstance pi = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(pi);
            assertEquals(STATE_COMPLETED, pi.getState().intValue());
        } catch (Exception e) {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            fail(e.getMessage());
        }
    }

    @Test
    @Category({UnstableOnJenkinsPrBuilder.class})
    public void testSignalContainerAlias() {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId > 0);

        try {
            checkAvailableSignals(CONTAINER_ID, processInstanceId);

            Object person = createPersonInstance(USER_JOHN);
            processClient.signal(CONTAINER_ID_ALIAS, "Signal1", person);

            processClient.signal(CONTAINER_ID_ALIAS, "Signal2", "My custom string event");

            ProcessInstance pi = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(pi);
            assertEquals(STATE_COMPLETED, pi.getState().intValue());
        } catch (Exception e) {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            fail(e.getMessage());
        }

    }

    @Test
    public void testSignalStartProcess() throws Exception {
        try {

            List<Integer> status = new ArrayList<Integer>();
            status.add(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE);
            status.add(org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED);
            status.add(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED);

            List<ProcessInstance> processInstances = queryClient.findProcessInstancesByProcessId(PROCESS_ID_SIGNAL_START, status, 0, 10);
            int initial = processInstances.size();

            Object person = createPersonInstance(USER_JOHN);
            processClient.signal(CONTAINER_ID, "start-process", person);

            processInstances = queryClient.findProcessInstancesByProcessId(PROCESS_ID_SIGNAL_START, status, 0, 10);
            assertNotNull(processInstances);
            assertEquals(initial + 1, processInstances.size());

        } catch (Exception e){
            fail(e.getMessage());
        }

    }

    @Test(timeout = 60 * 1000)
    public void testStartProcessInstanceWithAsyncNodes() throws Exception {
        List<String> status = new ArrayList<String>();
        status.add(STATUS.QUEUED.toString());
        status.add(STATUS.RUNNING.toString());
        status.add(STATUS.DONE.toString());
        int originalJobCount = jobServicesClient.getRequestsByStatus(status, 0, 1000).size();

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_ASYNC_SCRIPT);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        try {

            // async node is executed as a job
            List<RequestInfoInstance> jobs = jobServicesClient.getRequestsByStatus(status, 0, 1000);
            assertNotNull(jobs);
            assertEquals(originalJobCount + 1, jobs.size());

            // wait for process instance to be completed
            KieServerSynchronization.waitForProcessInstanceToFinish(processClient, CONTAINER_ID, processInstanceId);

            ProcessInstance pi = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(pi);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, pi.getState().intValue());

        } catch (Exception e){
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            fail(e.getMessage());
        }
    }

    @Test(timeout = 60 * 1000)
    public void testProcessInstanceWithTimer() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("timer", "1s");
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_TIMER, parameters);
        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        try {
            KieServerSynchronization.waitForProcessInstanceToFinish(processClient, CONTAINER_ID, processInstanceId);

            ProcessInstance pi = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(pi);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, pi.getState().intValue());

        } catch (Exception e){
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            fail(e.getMessage());
        }
    }

    @Test
    public void testGetNodeInstances() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {
            List<NodeInstance> instances = processClient.findActiveNodeInstances(CONTAINER_ID, processInstanceId, 0, 10);
            assertNotNull(instances);
            assertEquals(1, instances.size());

            NodeInstance expectedFirstTask = NodeInstance
                    .builder()
                    .name("First task")
                    .containerId(CONTAINER_ID)
                    .nodeType("HumanTaskNode")
                    .completed(false)
                    .processInstanceId(processInstanceId)
                    .build();

            NodeInstance nodeInstance = instances.get(0);
            assertNodeInstance(expectedFirstTask, nodeInstance);
            assertNotNull(nodeInstance.getWorkItemId());
            assertNotNull(nodeInstance.getDate());

            instances = processClient.findCompletedNodeInstances(CONTAINER_ID, processInstanceId, 0, 10);
            assertNotNull(instances);
            assertEquals(1, instances.size());

            NodeInstance expectedStart = NodeInstance
                    .builder()
                    .name("start")
                    .containerId(CONTAINER_ID)
                    .nodeType("StartNode")
                    .completed(true)
                    .processInstanceId(processInstanceId)
                    .build();

            nodeInstance = instances.get(0);
            assertNodeInstance(expectedStart, nodeInstance);
            assertNull(nodeInstance.getWorkItemId());
            assertNotNull(nodeInstance.getDate());

            instances = processClient.findNodeInstances(CONTAINER_ID, processInstanceId, 0, 10);
            assertNotNull(instances);
            assertEquals(3, instances.size());

            nodeInstance = instances.get(0);
            assertNodeInstance(expectedFirstTask, nodeInstance);
            assertNotNull(nodeInstance.getWorkItemId());
            assertNotNull(nodeInstance.getDate());

            nodeInstance = instances.get(1);
            assertNodeInstance(expectedStart, nodeInstance);
            assertNull(nodeInstance.getWorkItemId());
            assertNotNull(nodeInstance.getDate());

            nodeInstance = instances.get(2);
            expectedStart.setCompleted(false);
            assertNodeInstance(expectedStart, nodeInstance);
            assertNull(nodeInstance.getWorkItemId());
            assertNotNull(nodeInstance.getDate());
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testActiveNodeInstancesWrongContainer() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);
        try {
            assertThrows(KieServicesException.class, () -> processClient.findActiveNodeInstances(CONTAINER_ID_RESTART,
                    processInstanceId, 0, 10));
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testCompleteNodeInstancesWrongContainer() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {
            List<NodeInstance> instances = processClient.findActiveNodeInstances(CONTAINER_ID, processInstanceId, 0, 10);
            assertNotNull(instances);
            assertEquals(1, instances.size());

            NodeInstance expectedFirstTask = NodeInstance
                    .builder()
                    .name("First task")
                    .containerId(CONTAINER_ID)
                    .nodeType("HumanTaskNode")
                    .completed(false)
                    .processInstanceId(processInstanceId)
                    .build();
            NodeInstance nodeInstance = instances.get(0);
            assertNodeInstance(expectedFirstTask, nodeInstance);
            assertNotNull(nodeInstance.getWorkItemId());
            assertNotNull(nodeInstance.getDate());

            assertThrows(KieServicesException.class, () -> processClient.findCompletedNodeInstances(
                    CONTAINER_ID_RESTART, processInstanceId, 0, 10));
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testCallActivityProcess() {
        Map<String, Object> parameters = new HashMap<String, Object>();

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_CALL_EVALUATION, parameters);
        try {
            assertNotNull(processInstanceId);
            assertTrue(processInstanceId.longValue() > 0);

            // Process instance is running and is active.
            ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(processInstance);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE, processInstance.getState().intValue());

            List<TaskSummary> tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
            assertEquals(1, tasks.size());

            taskClient.completeAutoProgress(CONTAINER_ID, tasks.get(0).getId(), USER_YODA, null);

            List<ProcessInstance> instances = processClient.findProcessInstancesByParent(CONTAINER_ID, processInstanceId, 0, 10);
            assertEquals(1, instances.size());

            ProcessInstance childInstance = instances.get(0);
            assertNotNull(childInstance);
            assertEquals(PROCESS_ID_EVALUATION, childInstance.getProcessId());
            assertEquals(processInstanceId, childInstance.getParentId());

            List<NodeInstance> activeNodes = queryClient.findActiveNodeInstances(processInstanceId, 0, 10);
            assertEquals(1, activeNodes.size());

            NodeInstance active = activeNodes.get(0);
            assertEquals("Call Evaluation", active.getName());
            assertEquals("SubProcessNode", active.getNodeType());
            assertEquals(childInstance.getId(), active.getReferenceId());

            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);

            // Process instance is now aborted.
            processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
            assertNotNull(processInstance);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED, processInstance.getState().intValue());

            processInstance = processClient.getProcessInstance(CONTAINER_ID, childInstance.getId());
            assertNotNull(processInstance);
            assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_ABORTED, processInstance.getState().intValue());

            // no more active instances
            instances = processClient.findProcessInstancesByParent(CONTAINER_ID, processInstanceId, 0, 10);
            assertEquals(0, instances.size());

            instances = processClient.findProcessInstancesByParent(CONTAINER_ID, processInstanceId, Arrays.asList(3), 0, 10);
            assertEquals(1, instances.size());
        } catch (Exception e) {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            fail(e.getMessage());
        }
    }

    @Test
    public void testFindParentWrongContainer() {
        Map<String, Object> parameters = new HashMap<>();

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_CALL_EVALUATION, parameters);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId > 0);

        // Process instance is running and is active.
        ProcessInstance processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
        assertNotNull(processInstance);
        assertEquals(STATE_ACTIVE, processInstance.getState().intValue());

        List<TaskSummary> tasks = taskClient.findTasksAssignedAsPotentialOwner(USER_YODA, 0, 10);
        assertEquals(1, tasks.size());

        taskClient.completeAutoProgress(CONTAINER_ID, tasks.get(0).getId(), USER_YODA, null);

        List<ProcessInstance> instances = processClient.findProcessInstancesByParent(CONTAINER_ID, processInstanceId, 0,
                10);
        assertEquals(1, instances.size());

        ProcessInstance childInstance = instances.get(0);
        assertNotNull(childInstance);
        assertEquals(PROCESS_ID_EVALUATION, childInstance.getProcessId());
        assertEquals(processInstanceId, childInstance.getParentId());

        List<NodeInstance> activeNodes = queryClient.findActiveNodeInstances(processInstanceId, 0, 10);
        assertEquals(1, activeNodes.size());

        NodeInstance active = activeNodes.get(0);
        assertEquals("Call Evaluation", active.getName());
        assertEquals("SubProcessNode", active.getNodeType());
        assertEquals(childInstance.getId(), active.getReferenceId());

        processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);

        // Process instance is now aborted.
        processInstance = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
        assertNotNull(processInstance);
        assertEquals(STATE_ABORTED, processInstance.getState().intValue());

        processInstance = processClient.getProcessInstance(CONTAINER_ID, childInstance.getId());
        assertNotNull(processInstance);
        assertEquals(STATE_ABORTED, processInstance.getState().intValue());

        assertThrows(KieServicesException.class, () -> processClient.findProcessInstancesByParent(
                CONTAINER_ID_RESTART, processInstanceId, Collections
                        .singletonList(STATE_ABORTED), 0, 10));

    }

    @Test
    public void testFindVariableInstances() {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        try {
            List<VariableInstance> currentState = processClient.findVariablesCurrentState(CONTAINER_ID, processInstanceId);
            assertNotNull(currentState);
            assertEquals(3, currentState.size());

            for (VariableInstance variableInstance : currentState) {
                if ("personData".equals(variableInstance.getVariableName())) {
                    assertVariableInstance(variableInstance, processInstanceId, "personData", "Person{name='john'}");
                } else if ("stringData".equals(variableInstance.getVariableName())) {
                    assertVariableInstance(variableInstance, processInstanceId, "stringData", "waiting for signal");
                } else if ("initiator".equals(variableInstance.getVariableName())) {
                    assertVariableInstance(variableInstance, processInstanceId, "initiator", TestConfig.getUsername());
                } else {
                    fail("Got unexpected variable " + variableInstance.getVariableName());
                }
            }

            List<VariableInstance> varHistory = processClient.findVariableHistory(CONTAINER_ID, processInstanceId, "stringData", 0, 10);
            assertNotNull(varHistory);
            assertEquals(1, varHistory.size());

            VariableInstance variableInstance = varHistory.get(0);
            assertVariableInstance(variableInstance, processInstanceId, "stringData", "waiting for signal");

            processClient.setProcessVariable(CONTAINER_ID, processInstanceId, "stringData", "updated value");

            currentState = processClient.findVariablesCurrentState(CONTAINER_ID, processInstanceId);
            assertNotNull(currentState);
            assertEquals(3, currentState.size());

            for (VariableInstance variable : currentState) {
                if ("personData".equals(variable.getVariableName())) {
                    assertVariableInstance(variable, processInstanceId, "personData", "Person{name='john'}");
                } else if ("stringData".equals(variable.getVariableName())) {
                    assertVariableInstance(variable, processInstanceId, "stringData", "updated value", "waiting for signal");
                } else if ("initiator".equals(variable.getVariableName())) {
                    assertVariableInstance(variable, processInstanceId, "initiator", TestConfig.getUsername());
                } else {
                    fail("Got unexpected variable " + variable.getVariableName());
                }
            }

            varHistory = processClient.findVariableHistory(CONTAINER_ID, processInstanceId, "stringData", 0, 10);
            assertNotNull(varHistory);
            assertEquals(2, varHistory.size());

            variableInstance = varHistory.get(0);
            assertVariableInstance(variableInstance, processInstanceId, "stringData", "updated value", "waiting for signal");

            variableInstance = varHistory.get(1);
            assertVariableInstance(variableInstance, processInstanceId, "stringData", "waiting for signal");

        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }

    }
    
    @Test
    public void testFindVariableInstancesWrongContainer() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);
        try {
            assertThrows(KieServicesException.class, () -> processClient.findVariablesCurrentState(CONTAINER_ID_RESTART,
                    processInstanceId));
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        }
    }

    @Test
    public void testGetProcessInstances() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<ProcessInstance> instances = processClient.findProcessInstances(CONTAINER_ID, 0, 10);
            assertNotNull(instances);
            assertEquals(5, instances.size());

            instances = processClient.findProcessInstances(CONTAINER_ID, 0, 3);
            assertNotNull(instances);
            assertEquals(3, instances.size());

            instances = processClient.findProcessInstances(CONTAINER_ID, 1, 3);
            assertNotNull(instances);
            assertEquals(2, instances.size());
        } finally {
            processClient.abortProcessInstances(CONTAINER_ID, processInstanceIds);
        }
    }

    @Test
    public void testGetProcessInstancesSortedByName() {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        try {
            List<ProcessInstance> instances = queryClient.findProcessInstances(0, 3, SORT_BY_PROCESS_ID, true);
            assertNotNull(instances);
            assertEquals(3, instances.size());
            for (ProcessInstance instance : instances) {
                assertTrue(processInstanceIds.contains(instance.getId()));
                assertEquals(PROCESS_ID_SIGNAL_PROCESS, instance.getProcessId());
            }

            instances = queryClient.findProcessInstances(1, 3, SORT_BY_PROCESS_ID, true);
            assertNotNull(instances);
            assertEquals(2, instances.size());
            for (ProcessInstance instance : instances) {
                assertTrue(processInstanceIds.contains(instance.getId()));
                assertEquals(PROCESS_ID_USERTASK, instance.getProcessId());
            }

            instances = queryClient.findProcessInstances(0, 10, SORT_BY_PROCESS_ID, false);
            assertNotNull(instances);
            assertEquals(5, instances.size());
            for (int i = 0; i < instances.size(); i++) {
                if (i < 2) {
                    assertEquals(PROCESS_ID_USERTASK, instances.get(i).getProcessId());
                } else {
                    assertEquals(PROCESS_ID_SIGNAL_PROCESS, instances.get(i).getProcessId());
                }
            }
        } finally {
            processClient.abortProcessInstances(CONTAINER_ID, processInstanceIds);
        }
    }
    
    @Test
    public void testStartProcessOnDeactivatedContainer() throws Exception {
        
        Object person = createPersonInstance(USER_JOHN);

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("test", USER_MARY);
        parameters.put("number", new Integer(12345));

        List<Object> list = new ArrayList<Object>();
        list.add("item");

        parameters.put("list", list);
        parameters.put("person", person);
        Long processInstanceId = null;
        try {
            processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);

            assertNotNull(processInstanceId);
            assertTrue(processInstanceId.longValue() > 0);

            ServiceResponse<KieContainerResource> reply = client.deactivateContainer(CONTAINER_ID);            
            KieServerAssert.assertSuccess(reply);
            
            assertClientException(
                    () -> processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters),
                    400,
                    "Deployment " + CONTAINER_ID + " is not active");
                     
            // abort is allowed on deactivated container
            processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            
            reply = client.activateContainer(CONTAINER_ID);            
            KieServerAssert.assertSuccess(reply);
            
            // since we activate it again new instance can be started
            processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
            
        } finally {
            if (processInstanceId != null) {
                processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            }
        }
    }

    protected List<Long> createProcessInstances(Map<String, Object> parameters) {
        List<Long> processInstanceIds = new ArrayList<>();

        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters));
        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters));
        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters));
        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters));
        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters));

        Collections.sort(processInstanceIds);
        return processInstanceIds;
    }

    private void assertVariableInstance(VariableInstance variable, Long processInstanceId, String name, String value) {
        assertNotNull(variable);
        assertEquals(processInstanceId, variable.getProcessInstanceId());
        KieServerAssert.assertNullOrEmpty(variable.getOldValue());
        assertEquals(value, variable.getValue());
        assertEquals(name, variable.getVariableName());
    }

    private void assertVariableInstance(VariableInstance variable, Long processInstanceId, String name, String value, String oldValue) {
        assertNotNull(variable);
        assertEquals(processInstanceId, variable.getProcessInstanceId());
        assertEquals(oldValue, variable.getOldValue());
        assertEquals(value, variable.getValue());
        assertEquals(name, variable.getVariableName());
    }

    private ProcessInstance createSignalProcessInstance(Long processInstanceId) {
        return ProcessInstance.builder()
                .id(processInstanceId)
                .state(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE)
                .processId(PROCESS_ID_SIGNAL_PROCESS)
                .processName("signalprocess")
                .processVersion("1.0")
                .containerId(CONTAINER_ID)
                .processInstanceDescription("signalprocess")
                .initiator(TestConfig.getUsername())
                .parentInstanceId(-1l)
                .build();
    }

    private ProcessInstance createEvaluationProcessInstace(Long proccesInstanceId) {
        return ProcessInstance.builder()
                .id(proccesInstanceId)
                .state(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE)
                .processId(PROCESS_ID_EVALUATION)
                .processName("evaluation")
                .processVersion("1.0")
                .initiator(USER_YODA)
                .containerId(CONTAINER_ID)
                .processInstanceDescription("evaluation")
                .parentInstanceId(-1l)
                .build();
    }

    private void assertProcessInstance(ProcessInstance expected, ProcessInstance actual) {
        assertNotNull(actual);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getState(), actual.getState());
        assertEquals(expected.getProcessId(), actual.getProcessId());
        assertEquals(expected.getProcessName(), actual.getProcessName());
        assertEquals(expected.getProcessVersion(), actual.getProcessVersion());
        assertEquals(expected.getContainerId(), actual.getContainerId());
        assertEquals(expected.getProcessInstanceDescription(), actual.getProcessInstanceDescription());
        assertEquals(expected.getInitiator(), actual.getInitiator());
        assertEquals(expected.getParentId(), actual.getParentId());
        assertNotNull(actual.getCorrelationKey());
        assertNotNull(actual.getDate());
    }

    private void assertNodeInstance(NodeInstance expected, NodeInstance actual) {
        assertNotNull(actual);
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getContainerId(), actual.getContainerId());
        assertEquals(expected.getNodeType(), actual.getNodeType());
        assertEquals(expected.getCompleted(), actual.getCompleted());
        assertEquals(expected.getProcessInstanceId(), actual.getProcessInstanceId());
    }

    private void checkAvailableSignals(String containerId, Long processInstanceId) {
        List<String> availableSignals = processClient.getAvailableSignals(containerId, processInstanceId);
        assertNotNull(availableSignals);
        assertEquals(2, availableSignals.size());
        assertTrue(availableSignals.contains("Signal1"));
        assertTrue(availableSignals.contains("Signal2"));
    }

    private void checkAvailableBoundarySignals(String containerId, Long processInstanceId) {
        List<String> availableSignals = processClient.getAvailableSignals(containerId, processInstanceId);
        assertNotNull(availableSignals);
        assertEquals(1, availableSignals.size());
        assertTrue(availableSignals.contains("MySignal"));
    }
}
