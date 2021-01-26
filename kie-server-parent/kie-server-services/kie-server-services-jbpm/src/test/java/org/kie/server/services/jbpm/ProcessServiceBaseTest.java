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

package org.kie.server.services.jbpm;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.jbpm.kie.services.impl.model.ProcessInstanceDesc;
import org.jbpm.kie.services.impl.model.UserTaskInstanceDesc;
import org.jbpm.services.api.DefinitionService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.impl.marshal.MarshallerHelper;
import org.kie.server.services.jbpm.locator.ByProcessInstanceIdContainerLocator;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProcessServiceBaseTest {

    @Mock
    private ProcessService processServiceMock;

    @Mock
    private DefinitionService definitionServiceMock;

    @Mock
    private MarshallerHelper marshallerHelperMock;

    @Mock
    private KieServerRegistry contextMock;

    @Mock
    private RuntimeDataService runtimeDataServiceMock;

    ProcessServiceBase processServiceBase;

    @Before
    public void setup(){
        processServiceBase = new ProcessServiceBase(processServiceMock,
                                                    definitionServiceMock,
                                                    runtimeDataServiceMock,
                                                    contextMock);
        processServiceBase.setMarshallerHelper(marshallerHelperMock);

    }

    @Test
    public void testGetProcessInstanceSetsActiveTasks() {
        String containerId = "container";
        Long processInstanceId = 1L;
        boolean withVars = true;
        String marshallingType = "xstream";

        ProcessInstanceDesc processInstanceDesc = new ProcessInstanceDesc();
        UserTaskInstanceDesc task1 = new UserTaskInstanceDesc(Long.valueOf(1),
                                                                "Ready",
                                                                new Date(),
                                                                "Task1_name",
                                                                "Task1_desc",
                                                                1,
                                                                "Task1_Owner",
                                                                "Task1_Creator",
                                                                "deployment",
                                                                "Process",
                                                                processInstanceId,
                                                                new Date(),
                                                                new Date());
        UserTaskInstanceDesc task2 = new UserTaskInstanceDesc(Long.valueOf(2),
                                                                "Ready",
                                                                new Date(),
                                                                "Task2_name",
                                                                "Task2_desc",
                                                                1,
                                                                "Task2_Owner",
                                                                "Task2_Creator",
                                                                "deployment",
                                                                "Process",
                                                                processInstanceId,
                                                                new Date(),
                                                                new Date());
        processInstanceDesc.setActiveTasks(Arrays.asList(task1,task2));
        processInstanceDesc.setDeploymentId(containerId);


        when(runtimeDataServiceMock.getProcessInstanceById(processInstanceId)).thenReturn(processInstanceDesc);
        when(contextMock.getContainerId(eq(containerId),any(ByProcessInstanceIdContainerLocator.class)))
                .thenReturn(containerId);
        processServiceBase.getProcessInstance(containerId,
                                              processInstanceId,
                                              withVars,
                                              marshallingType);
        ArgumentCaptor<org.kie.server.api.model.instance.ProcessInstance> captor =ArgumentCaptor.forClass(org.kie.server.api.model.instance.ProcessInstance.class);
        verify(marshallerHelperMock).marshal(eq(containerId),eq(marshallingType),captor.capture(),any(ByProcessInstanceIdContainerLocator.class));
        org.kie.server.api.model.instance.ProcessInstance processInstance= captor.getValue();
        verifyProcessInstanceHasActiveTasks(processInstanceDesc,processInstance);


    }

    private void verifyProcessInstanceHasActiveTasks( ProcessInstanceDesc pid, org.kie.server.api.model.instance.ProcessInstance pi){
        assertNotNull(pid);
        assertNotNull(pi);
        assertEquals(pid.getActiveTasks().size(), pi.getActiveUserTasks().getItems().size());

        List<org.jbpm.services.api.model.UserTaskInstanceDesc> userTaskInstanceDescsList= pid.getActiveTasks();
        List<TaskSummary> taskSummaryArray = pi.getActiveUserTasks().getItems();
        for(int i=0; i< pid.getActiveTasks().size();i++){
            verifyTaskInstanceEqualsTaskSummary(userTaskInstanceDescsList.get(i), taskSummaryArray.get(i));
        }
    }

    private void verifyTaskInstanceEqualsTaskSummary(org.jbpm.services.api.model.UserTaskInstanceDesc taskInstance, TaskSummary taskSummary){
        assertEquals(taskInstance.getTaskId(), taskSummary.getId());
        assertEquals(taskInstance.getName(), taskSummary.getName());
        assertEquals(taskInstance.getDescription(), taskSummary.getDescription());
        assertEquals(taskInstance.getActivationTime(), taskSummary.getActivationTime());
        assertEquals(taskInstance.getActualOwner(), taskSummary.getActualOwner());
        assertEquals(taskInstance.getDeploymentId(), taskSummary.getContainerId());
        assertEquals(taskInstance.getCreatedBy(), taskSummary.getCreatedBy());
        assertEquals(taskInstance.getCreatedOn(), taskSummary.getCreatedOn());
        assertEquals(taskInstance.getPriority(), taskSummary.getPriority());
        assertEquals(taskInstance.getProcessId(), taskSummary.getProcessId());
        assertEquals(taskInstance.getProcessInstanceId(), taskSummary.getProcessInstanceId());
        assertEquals(taskInstance.getStatus(), taskSummary.getStatus());
    }


}
