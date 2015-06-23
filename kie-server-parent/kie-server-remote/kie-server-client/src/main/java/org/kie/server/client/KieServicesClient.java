/*
 * Copyright 2015 JBoss Inc
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

package org.kie.server.client;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.kie.api.command.Command;
import org.kie.api.task.model.Task;
import org.kie.internal.process.CorrelationKey;
import org.kie.server.api.commands.CommandScript;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.KieScannerResource;
import org.kie.server.api.model.KieServerConfig;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.ServiceResponsesList;
import org.kie.server.api.model.definition.AssociatedEntitiesDefinition;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.api.model.definition.ProcessDefinitionList;
import org.kie.server.api.model.definition.ServiceTasksDefinition;
import org.kie.server.api.model.definition.SubProcessesDefinition;
import org.kie.server.api.model.definition.TaskInputsDefinition;
import org.kie.server.api.model.definition.TaskOutputsDefinition;
import org.kie.server.api.model.definition.UserTaskDefinitionList;
import org.kie.server.api.model.definition.VariablesDefinition;
import org.kie.server.api.model.instance.NodeInstance;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.ProcessInstanceList;
import org.kie.server.api.model.instance.TaskAttachment;
import org.kie.server.api.model.instance.TaskComment;
import org.kie.server.api.model.instance.TaskEventInstance;
import org.kie.server.api.model.instance.TaskInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.api.model.instance.TaskSummaryList;
import org.kie.server.api.model.instance.VariableInstance;
import org.kie.server.api.model.instance.WorkItemInstance;

public interface KieServicesClient {
    ServiceResponse<KieServerInfo> register(String controllerEndpoint, KieServerConfig kieServerConfig);

    ServiceResponse<KieServerInfo> getServerInfo();

    ServiceResponse<KieContainerResourceList> listContainers();

    ServiceResponse<KieContainerResource> createContainer(String id, KieContainerResource resource);

    ServiceResponse<KieContainerResource> getContainerInfo(String id);

    ServiceResponse<Void> disposeContainer(String id);

    ServiceResponse<String> executeCommands(String id, String payload);

    ServiceResponse<String> executeCommands(String id, Command<?> cmd);

    ServiceResponsesList executeScript(CommandScript script);

    ServiceResponse<KieScannerResource> getScannerInfo(String id);

    ServiceResponse<KieScannerResource> updateScanner(String id, KieScannerResource resource);

    ServiceResponse<ReleaseId> updateReleaseId(String id, ReleaseId releaseId);

    // process definition
    ProcessDefinition getProcessDefinition(String containerId, String processId);

    SubProcessesDefinition getReusableSubProcessDefinitions(String containerId, String processId);

    VariablesDefinition getProcessVariableDefinitions(String containerId, String processId);

    ServiceTasksDefinition getServiceTaskDefinitions(String containerId, String processId);

    AssociatedEntitiesDefinition getAssociatedEntityDefinitions(String containerId, String processId);

    UserTaskDefinitionList getUserTaskDefinitions(String containerId, String processId);

    TaskInputsDefinition getUserTaskInputDefinitions(String containerId, String processId, String taskName);

    TaskOutputsDefinition getUserTaskOutputDefinitions(String containerId, String processId, String taskName);

    // process operations
    Long startProcess(String containerId, String processId);

    Long startProcess(String containerId, String processId, Map<String, Object> variables);

    Long startProcess(String containerId, String processId, CorrelationKey correlationKey);

    Long startProcess(String containerId, String processId, CorrelationKey correlationKey, Map<String, Object> variables);

    void abortProcessInstance(String containerId, Long processInstanceId);

    void abortProcessInstances(String containerId, List<Long> processInstanceIds);

    Object getProcessInstanceVariable(String containerId, Long processInstanceId, String variableName);

    <T> T getProcessInstanceVariable(String containerId, Long processInstanceId, String variableName, Class<T> type);

    Map<String, Object> getProcessInstanceVariables(String containerId, Long processInstanceId);

    void signalProcessInstance(String containerId, Long processInstanceId, String signalName, Object event);

    void signalProcessInstances(String containerId, List<Long> processInstanceId, String signalName, Object event);

    List<String> getAvailableSignals(String containerId, Long processInstanceId);

    void setProcessVariable(String containerId, Long processInstanceId, String variableId, Object value);

    void setProcessVariables(String containerId, Long processInstanceId, Map<String, Object> variables);

    ProcessInstance getProcessInstance(String containerId, Long processInstanceId);

    ProcessInstance getProcessInstance(String containerId, Long processInstanceId, boolean withVars);

    void completeWorkItem(String containerId, Long processInstanceId, Long id, Map<String, Object> results);

    void abortWorkItem(String containerId, Long processInstanceId, Long id);

    WorkItemInstance getWorkItem(String containerId, Long processInstanceId, Long id);

    List<WorkItemInstance> getWorkItemByProcessInstance(String containerId, Long processInstanceId);

    // task operations
    void activateTask(String containerId, Long taskId, String userId);

    void claimTask(String containerId, Long taskId, String userId);

    void completeTask(String containerId, Long taskId, String userId, Map<String, Object> params);

    void delegateTask(String containerId, Long taskId, String userId, String targetUserId);

    void exitTask(String containerId, Long taskId, String userId);

    void failTask(String containerId, Long taskId, String userId, Map<String, Object> params);

    void forwardTask(String containerId, Long taskId, String userId, String targetEntityId);

    void releaseTask(String containerId, Long taskId, String userId);

    void resumeTask(String containerId, Long taskId, String userId);

    void skipTask(String containerId, Long taskId, String userId);

    void startTask(String containerId, Long taskId, String userId);

    void stopTask(String containerId, Long taskId, String userId);

    void suspendTask(String containerId, Long taskId, String userId);

    void nominateTask(String containerId, Long taskId, String userId, List<String> potentialOwners);

    void setTaskPriority(String containerId, Long taskId, int priority);

    void setTaskExpirationDate(String containerId, Long taskId, Date date);

    void setTaskSkipable(String containerId, Long taskId, boolean skipable);

    void setTaskName(String containerId, Long taskId, String name);

    void setTaskDescription(String containerId, Long taskId, String description);

    Long saveTaskContent(String containerId, Long taskId, Map<String, Object> values);

    Map<String, Object> getTaskOutputContentByTaskId(String containerId, Long taskId);

    Map<String, Object> getTaskInputContentByTaskId(String containerId, Long taskId);

    void deleteTaskContent(String containerId, Long taskId, Long contentId);

    Long addTaskComment(String containerId, Long taskId, String text, String addedBy, Date addedOn);

    void deleteTaskComment(String containerId, Long taskId, Long commentId);

    List<TaskComment> getTaskCommentsByTaskId(String containerId, Long taskId);

    TaskComment getTaskCommentById(String containerId, Long taskId, Long commentId);

    Long addTaskAttachment(String containerId, Long taskId, String userId, Object attachment);

    void deleteTaskAttachment(String containerId, Long taskId, Long attachmentId);

    TaskAttachment getTaskAttachmentById(String containerId, Long taskId, Long attachmentId);

    Object getTaskAttachmentContentById(String containerId, Long taskId, Long attachmentId);

    List<TaskAttachment> getTaskAttachmentsByTaskId(String containerId, Long taskId);

    TaskInstance getTaskInstance(String containerId, Long taskId);

    TaskInstance getTaskInstance(String containerId, Long taskId, boolean withInputs, boolean withOutputs, boolean withAssignments);

    // runtime data searches
    ProcessDefinition findProcessByContainerIdProcessId(String containerId, String processId);

    List<ProcessDefinition> findProcessesById(String processId);

    List<ProcessDefinition> findProcesses(Integer page, Integer pageSize);

    List<ProcessDefinition> findProcesses(String filter, Integer page, Integer pageSize);

    List<ProcessDefinition> findProcessesByContainerId(String containerId, Integer page, Integer pageSize);


    List<ProcessInstance> findProcessInstances(Integer page, Integer pageSize);

    List<ProcessInstance> findProcessInstancesByProcessId(String processId, List<Integer> status, Integer page, Integer pageSize);

    List<ProcessInstance> findProcessInstancesByProcessName(String processName, List<Integer> status, Integer page, Integer pageSize);

    List<ProcessInstance> findProcessInstancesByContainerId(String containerId, List<Integer> status, Integer page, Integer pageSize);

    List<ProcessInstance> findProcessInstancesByStatus(List<Integer> status, Integer page, Integer pageSize);

    List<ProcessInstance> findProcessInstancesByInitiator(String initiator, List<Integer> status, Integer page, Integer pageSize);

    ProcessInstance findProcessInstanceById(Long processInstanceId);

    ProcessInstance findProcessInstanceByCorrelationKey(CorrelationKey correlationKey);

    NodeInstance findNodeInstanceByWorkItemId(Long processInstanceId, Long workItemId);

    List<NodeInstance> findActiveNodeInstances(Long processInstanceId, Integer page, Integer pageSize);

    List<NodeInstance> findCompletedNodeInstances(Long processInstanceId, Integer page, Integer pageSize);

    List<NodeInstance> findNodeInstances(Long processInstanceId, Integer page, Integer pageSize);

    List<VariableInstance> findVariablesCurrentState(Long processInstanceId);

    List<VariableInstance> findVariableHistory(Long processInstanceId, String variableName, Integer page, Integer pageSize);

    // task searches
    TaskInstance findTaskByWorkItemId(Long workItemId);

    TaskInstance findTaskById(Long taskId);

    List<TaskSummary> findTasksAssignedAsBusinessAdministrator(String userId, Integer page, Integer pageSize);

    List<TaskSummary> findTasksAssignedAsBusinessAdministrator(String userId, List<String> status, Integer page, Integer pageSize);

    List<TaskSummary> findTasksAssignedAsPotentialOwner(String userId, Integer page, Integer pageSize);

    List<TaskSummary> findTasksAssignedAsPotentialOwner(String userId, List<String> status, Integer page, Integer pageSize);

    List<TaskSummary> findTasksAssignedAsPotentialOwner(String userId, List<String> groups, List<String> status, Integer page, Integer pageSize);

    List<TaskSummary> findTasksOwned(String userId, Integer page, Integer pageSize);

    List<TaskSummary> findTasksOwned(String userId, List<String> status, Integer page, Integer pageSize);

    List<TaskSummary> findTasksByStatusByProcessInstanceId(Long processInstanceId, List<String> status, Integer page, Integer pageSize);

    List<TaskSummary> findTasks(String userId, Integer page, Integer pageSize);

    List<TaskEventInstance> findTaskEvents(Long taskId, Integer page, Integer pageSize);
}
