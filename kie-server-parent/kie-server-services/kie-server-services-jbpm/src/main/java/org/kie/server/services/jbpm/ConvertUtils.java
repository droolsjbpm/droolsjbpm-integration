/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jbpm.services.api.ProcessInstanceNotFoundException;
import org.jbpm.services.api.model.NodeInstanceDesc;
import org.jbpm.services.api.model.ProcessDefinition;
import org.jbpm.services.api.model.ProcessInstanceCustomDesc;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.jbpm.services.api.model.ProcessInstanceWithVarsDesc;
import org.jbpm.services.api.model.UserTaskInstanceDesc;
import org.jbpm.services.api.model.UserTaskInstanceWithPotOwnerDesc;
import org.jbpm.services.api.model.UserTaskInstanceWithVarsDesc;
import org.jbpm.services.api.model.VariableDesc;
import org.kie.api.runtime.query.QueryContext;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.query.QueryFilter;
import org.kie.internal.runtime.error.ExecutionError;
import org.kie.server.api.model.admin.ExecutionErrorInstance;
import org.kie.server.api.model.admin.ExecutionErrorInstanceList;
import org.kie.server.api.model.definition.ProcessDefinitionList;
import org.kie.server.api.model.definition.QueryDefinition;
import org.kie.server.api.model.definition.QueryDefinitionList;
import org.kie.server.api.model.definition.QueryParam;
import org.kie.server.api.model.instance.NodeInstance;
import org.kie.server.api.model.instance.NodeInstanceList;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.ProcessInstanceCustomVars;
import org.kie.server.api.model.instance.ProcessInstanceCustomVarsList;
import org.kie.server.api.model.instance.ProcessInstanceList;
import org.kie.server.api.model.instance.ProcessInstanceUserTaskWithVariables;
import org.kie.server.api.model.instance.ProcessInstanceUserTaskWithVariablesList;
import org.kie.server.api.model.instance.TaskInstance;
import org.kie.server.api.model.instance.TaskInstanceList;
import org.kie.server.api.model.instance.TaskSummaryList;
import org.kie.server.api.model.instance.TaskWithProcessDescription;
import org.kie.server.api.model.instance.TaskWithProcessDescriptionList;
import org.kie.server.api.model.instance.VariableInstance;
import org.kie.server.api.model.instance.VariableInstanceList;
import org.kie.server.services.api.ContainerLocator;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.jbpm.locator.ByProcessInstanceIdContainerLocator;

import static java.util.stream.Collectors.toList;
import static org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE;

public class ConvertUtils {

    private ConvertUtils() {}

    private static Map<Long, ContainerLocator> containerCache = Collections.synchronizedMap(new WeakHashMap<>());

    public static ProcessInstanceList convertToProcessInstanceList(Collection<ProcessInstanceDesc> instances) {
        return convertToProcessInstanceList(instances, null);
    }

    public static ProcessInstanceList convertToProcessInstanceList(Collection<ProcessInstanceDesc> instances,
                                                                   String containerId) {
        if (instances == null) {
            return new ProcessInstanceList(new ProcessInstance[0]);
        }

        List<ProcessInstance> processInstances = new ArrayList<>(instances.size());
        for (ProcessInstanceDesc pi : instances) {
            org.kie.server.api.model.instance.ProcessInstance instance = convertToProcessInstance(pi, containerId);

            processInstances.add(instance);
        }

        return new ProcessInstanceList(processInstances);
    }

    public static ProcessInstanceList convertToProcessInstanceWithVarsList(Collection<ProcessInstanceWithVarsDesc> instances) {
        if (instances == null) {
            return new ProcessInstanceList(new org.kie.server.api.model.instance.ProcessInstance[0]);
        }

        List<ProcessInstance> processInstances = new ArrayList<>(instances.size());
        for (ProcessInstanceWithVarsDesc pi : instances) {
            org.kie.server.api.model.instance.ProcessInstance instance = convertToProcessInstance(pi);
            if (instance != null) {
                instance.setVariables(pi.getVariables());
            }
            processInstances.add(instance);
        }
        return new ProcessInstanceList(processInstances);
    }

    public static org.kie.server.api.model.instance.ProcessInstance convertToProcessInstance(ProcessInstanceDesc pi) {
        return convertToProcessInstance(pi, null);
    }

    private static void checkContainerId(long processInstanceId, String containerId) {
        // check process instance belong to container id
        if (containerId != null) {
            try {
                if (!containerCache.computeIfAbsent(processInstanceId, ByProcessInstanceIdContainerLocator::new)
                        .locateContainer(containerId, Collections.emptyList()).equals(containerId)) {
                    throwException(processInstanceId, containerId);
                }
            } catch (IllegalArgumentException ex) {
                throwException(processInstanceId, containerId, ex);
            }
        }

    }

    public static org.kie.server.api.model.instance.ProcessInstance convertToProcessInstance(ProcessInstanceDesc pi,
                                                                                             String containerId) {
        if (pi == null) {
            return null;
        }

        checkContainerId(pi.getId(), containerId);
        org.kie.server.api.model.instance.ProcessInstance instance = org.kie.server.api.model.instance.ProcessInstance
                .builder()
                .id(pi.getId())
                .processId(pi.getProcessId())
                .processName(pi.getProcessName())
                .processVersion(pi.getProcessVersion())
                .containerId(pi.getDeploymentId())
                .processInstanceDescription(pi.getProcessInstanceDescription())
                .correlationKey(pi.getCorrelationKey())
                .parentInstanceId(pi.getParentId())
                .date(pi.getDataTimeStamp())
                .initiator(pi.getInitiator())
                .state(pi.getState())
                .slaCompliance(pi.getSlaCompliance())
                .slaDueDate(pi.getSlaDueDate())
                .build();

        if (pi.getActiveTasks() != null && !pi.getActiveTasks().isEmpty()) {
            org.kie.server.api.model.instance.TaskSummary[] tasks = new org.kie.server.api.model.instance.TaskSummary[pi.getActiveTasks().size()];
            int counter = 0;
            for (UserTaskInstanceDesc taskSummary : pi.getActiveTasks()) {
                org.kie.server.api.model.instance.TaskSummary task = org.kie.server.api.model.instance.TaskSummary.builder()
                        .id(taskSummary.getTaskId())
                        .name(taskSummary.getName())
                        .description(taskSummary.getDescription())
                        .activationTime(taskSummary.getActivationTime())
                        .actualOwner(taskSummary.getActualOwner())
                        .containerId(taskSummary.getDeploymentId())
                        .createdBy(taskSummary.getCreatedBy())
                        .createdOn(taskSummary.getCreatedOn())
                        .priority(taskSummary.getPriority())
                        .processId(taskSummary.getProcessId())
                        .processInstanceId(taskSummary.getProcessInstanceId())
                        .status(taskSummary.getStatus())
                        .subject(taskSummary.getSubject())
                        .build();
                tasks[counter] = task;
                counter++;
            }
            instance.setActiveUserTasks(new TaskSummaryList(tasks));
        }

        return instance;
    }
    
    public static ProcessInstanceCustomVarsList convertToProcessInstanceCustomVarsList(Collection<ProcessInstanceCustomDesc> instances) {
        if (instances == null) {
            return new ProcessInstanceCustomVarsList(new ProcessInstanceCustomVars[0]);
        }

        List<ProcessInstanceCustomVars> processInstances = new ArrayList<>(instances.size());
        for (ProcessInstanceCustomDesc pi : instances) {
            ProcessInstanceCustomVars instance = convertToProcessInstanceCustomVars(pi);
            processInstances.add(instance);
        }
        return new ProcessInstanceCustomVarsList(processInstances);
    }
    
    public static ProcessInstanceCustomVars convertToProcessInstanceCustomVars(ProcessInstanceCustomDesc pi) {
        return pi == null ? null : ProcessInstanceCustomVars.builder()
                .id(pi.getId())
                .processId(pi.getProcessId())
                .processName(pi.getProcessName())
                .processVersion(pi.getProcessVersion())
                .containerId(pi.getDeploymentId())
                .processInstanceDescription(pi.getProcessInstanceDescription())
                .correlationKey(pi.getCorrelationKey())
                .parentInstanceId(pi.getParentId())
                .date(pi.getDataTimeStamp())
                .initiator(pi.getInitiator())
                .state(pi.getState())
                .lastModificationDate(pi.getLastModificationDate())
                .variables(pi.getVariables())
                .build();
    }

    public static ProcessDefinitionList convertToProcessList(Collection<ProcessDefinition> definitions) {
        if (definitions == null) {
            return new ProcessDefinitionList(new org.kie.server.api.model.definition.ProcessDefinition[0]);
        }
        List<org.kie.server.api.model.definition.ProcessDefinition> processes = new ArrayList<>(definitions.size());
        for (ProcessDefinition pd : definitions) {
            org.kie.server.api.model.definition.ProcessDefinition definition = convertToProcess(pd);
            processes.add(definition);
        }
        return new ProcessDefinitionList(processes);
    }

    public static org.kie.server.api.model.definition.ProcessDefinition convertToProcess(ProcessDefinition processDesc) {
        return processDesc == null ? null : org.kie.server.api.model.definition.ProcessDefinition.builder()
                .id(processDesc.getId())
                .name(processDesc.getName())
                .packageName(processDesc.getPackageName())
                .version(processDesc.getVersion())
                .containerId(processDesc.getDeploymentId())
                .dynamic(processDesc.isDynamic())
                .build();
    }

    public static ExecutionErrorInstanceList convertToErrorInstanceList(List<ExecutionError> executionErrors) {
        if (executionErrors == null) {
            return new ExecutionErrorInstanceList(new ExecutionErrorInstance[0]);
        }

        List<ExecutionErrorInstance> executionErrorInstances = new ArrayList<>(executionErrors.size());
        for (ExecutionError error : executionErrors) {
            executionErrorInstances.add(convertToErrorInstance(error));
        }

        return new ExecutionErrorInstanceList(executionErrorInstances);
    }

    public static ExecutionErrorInstance convertToErrorInstance(ExecutionError executionError) {
        return executionError == null ? null : ExecutionErrorInstance.builder()
                .error(executionError.getError())
                .errorId(executionError.getErrorId())
                .errorDate(executionError.getErrorDate())
                .processInstanceId(executionError.getProcessInstanceId())
                .acknowledged(executionError.isAcknowledged())
                .acknowledgedAt(executionError.getAcknowledgedAt())
                .acknowledgedBy(executionError.getAcknowledgedBy())
                .activityId(executionError.getActivityId())
                .activityName(executionError.getActivityName())
                .jobId(executionError.getJobId())
                .containerId(executionError.getDeploymentId())
                .message(executionError.getErrorMessage())
                .processId(executionError.getProcessId())
                .type(executionError.getType())
                .build();
    }

    public static QueryContext buildQueryContext(Integer page, Integer pageSize) {
        return new QueryContext(page * pageSize, pageSize);
    }

    public static QueryContext buildQueryContext(Integer page, Integer pageSize, String orderBy, boolean asc) {
        if (orderBy != null && !orderBy.isEmpty()) {
            return new QueryContext(page * pageSize, pageSize, orderBy, asc);
        }

        return new QueryContext(page * pageSize, pageSize);
    }

    public static QueryFilter buildQueryFilter(Integer page, Integer pageSize) {
        return new QueryFilter(page * pageSize, pageSize);
    }

    public static QueryFilter buildQueryFilter(Integer page, Integer pageSize, String orderBy, boolean asc) {
        QueryFilter queryFilter = null;
        if (orderBy != null && !orderBy.isEmpty()) {
            queryFilter = new QueryFilter(page * pageSize, pageSize, orderBy, asc);
        } else {
            queryFilter = new QueryFilter(page * pageSize, pageSize);
        }

        return queryFilter;
    }

    public static QueryFilter buildTaskByNameQueryFilter(Integer page, Integer pageSize, String orderBy, boolean asc, String filter) {
        QueryFilter queryFilter = null;
        if (orderBy != null && !orderBy.isEmpty()) {
            queryFilter = new QueryFilter(page * pageSize, pageSize, orderBy, asc);
        } else {
            queryFilter = new QueryFilter(page * pageSize, pageSize);
        }

        if (filter != null && !filter.isEmpty()) {
            Map<String, Object> params = new HashMap<>();
            params.put("taskName", filter);
            queryFilter.setFilterParams("t.name like :taskName");
            queryFilter.setParams(params);
        }

        return queryFilter;
    }

    public static List<Status> buildTaskStatuses(List<String> status) {
        if (status == null || status.isEmpty()) {
            return Collections.emptyList();
        }
        List<Status> taskStatuses = new ArrayList<>();
        for (String s : status) {
            taskStatuses.add(Status.valueOf(s));
        }
        return taskStatuses;
    }

    public static NodeInstance convertToNodeInstance(NodeInstanceDesc nodeInstanceDesc) {
        return convertToNodeInstance(nodeInstanceDesc, null);
    }

    public static NodeInstance convertToNodeInstance(NodeInstanceDesc nodeInstanceDesc,
                                                     String containerId) {
        checkContainerId(nodeInstanceDesc.getProcessInstanceId(), containerId);
        return NodeInstance.builder()
                .id(nodeInstanceDesc.getId())
                .name(nodeInstanceDesc.getName())
                .nodeId(nodeInstanceDesc.getNodeId())
                .nodeType(nodeInstanceDesc.getNodeType())
                .processInstanceId(nodeInstanceDesc.getProcessInstanceId())
                .containerId(nodeInstanceDesc.getDeploymentId())
                .workItemId(nodeInstanceDesc.getWorkItemId())
                .completed(nodeInstanceDesc.isCompleted())
                .connection(nodeInstanceDesc.getConnection())
                .date(nodeInstanceDesc.getDataTimeStamp())
                .referenceId(nodeInstanceDesc.getReferenceId())
                .slaCompliance(nodeInstanceDesc.getSlaCompliance())
                .slaDueDate(nodeInstanceDesc.getSlaDueDate())
                .build();
    }

    public static NodeInstanceList convertToNodeInstanceList(Collection<NodeInstanceDesc> definitions,
                                                             String containerId) {
        if (definitions == null) {
            return new NodeInstanceList(new NodeInstance[0]);
        }

        List<NodeInstance> processes = new ArrayList<>(definitions.size());
        for (NodeInstanceDesc ni : definitions) {
            processes.add(convertToNodeInstance(ni, containerId));
        }
        return new NodeInstanceList(processes);
    }

    public static VariableInstance convertToVariable(VariableDesc variableDesc) {
        return convertToVariable(variableDesc, null);
    }


    public static VariableInstance convertToVariable(VariableDesc variableDesc,
                                                     String containerId) {
        checkContainerId(variableDesc.getProcessInstanceId(), containerId);
        return VariableInstance.builder()
                .name(variableDesc.getVariableId())
                .processInstanceId(variableDesc.getProcessInstanceId())
                .value(variableDesc.getNewValue())
                .oldValue(variableDesc.getOldValue())
                .date(variableDesc.getDataTimeStamp())
                .build();
    }

    public static VariableInstanceList convertToVariablesList(Collection<VariableDesc> variables,
                                                              String containerId,
                                                              KieServerRegistry registry) {
        if (variables == null) {
            return new VariableInstanceList(new VariableInstance[0]);
        }
        List<VariableInstance> processes = new ArrayList<>(variables.size());
        for (VariableDesc vi : variables) {
            processes.add(convertToVariable(vi, containerId));
        }
        return new VariableInstanceList(processes);
    }

    public static TaskInstanceList convertToTaskInstanceWithVarsList(Collection<UserTaskInstanceWithVarsDesc> instances) {
        if (instances == null) {
            return new TaskInstanceList(new org.kie.server.api.model.instance.TaskInstance[0]);
        }

        List<TaskInstance> taskInstances = new ArrayList<>(instances.size());
        for (UserTaskInstanceWithVarsDesc task : instances) {
            org.kie.server.api.model.instance.TaskInstance instance = convertToTask(task);

            instance.setInputData(task.getVariables());

            taskInstances.add(instance);
        }

        return new TaskInstanceList(taskInstances);
    }

    public static TaskInstanceList convertToTaskInstanceList(Collection<UserTaskInstanceDesc> instances) {
        if (instances == null) {
            return new TaskInstanceList(new org.kie.server.api.model.instance.TaskInstance[0]);
        }

        List<TaskInstance> taskInstances = new ArrayList<>(instances.size());
        for (UserTaskInstanceDesc task : instances) {
            org.kie.server.api.model.instance.TaskInstance instance = convertToTask(task);
            taskInstances.add(instance);
        }

        return new TaskInstanceList(taskInstances);
    }

    public static TaskInstance convertToTask(UserTaskInstanceDesc userTask) {
        return TaskInstance.builder()
                .id(userTask.getTaskId())
                .name(userTask.getName())
                .processInstanceId(userTask.getProcessInstanceId())
                .processId(userTask.getProcessId())
                .activationTime(userTask.getActivationTime())
                .actualOwner(userTask.getActualOwner())
                .containerId(userTask.getDeploymentId())
                .createdBy(userTask.getCreatedBy())
                .createdOn(userTask.getCreatedOn())
                .description(userTask.getDescription())
                .expirationTime(userTask.getDueDate())
                .status(userTask.getStatus())
                .priority(userTask.getPriority())
                .workItemId(userTask.getWorkItemId())
                .slaCompliance(userTask.getSlaCompliance())
                .slaDueDate(userTask.getSlaDueDate())
                .formName(userTask.getFormName())
                .subject(userTask.getSubject())
                .processType(userTask.getProcessType())
                .correlationKey(userTask.getCorrelationKey())
                .build();
    }
    
    public static TaskWithProcessDescriptionList convertToTaskInstanceListPO(Collection<UserTaskInstanceWithPotOwnerDesc> instances) {
        if (instances == null) {
            return new TaskWithProcessDescriptionList(new org.kie.server.api.model.instance.TaskWithProcessDescription[0]);
        }

        List<TaskWithProcessDescription> taskInstances = new ArrayList<>(instances.size());
        for (UserTaskInstanceWithPotOwnerDesc task : instances) {
            org.kie.server.api.model.instance.TaskWithProcessDescription instance = convertToTaskPO(task);
            taskInstances.add(instance);
        }

        return new TaskWithProcessDescriptionList(taskInstances);
    }
    
    public static TaskWithProcessDescription convertToTaskPO(UserTaskInstanceWithPotOwnerDesc userTask) {
        return TaskWithProcessDescription.builder()
                .id(userTask.getTaskId())
                .name(userTask.getName())
                .processInstanceId(userTask.getProcessInstanceId())
                .processId(userTask.getProcessId())
                .activationTime(userTask.getActivationTime())
                .actualOwner(userTask.getActualOwner())
                .containerId(userTask.getDeploymentId())
                .createdBy(userTask.getCreatedBy())
                .createdOn(userTask.getCreatedOn())
                .description(userTask.getDescription())
                .formName(userTask.getFormName())
                .expirationTime(userTask.getDueDate())
                .status(userTask.getStatus())
                .priority(userTask.getPriority())
                .subject(userTask.getSubject())
                .potentialOwners(userTask.getPotentialOwners())
                .correlationKey(userTask.getCorrelationKey())
                .lastModificationDate(userTask.getLastModificationDate())
                .lastModificationUser(userTask.getLastModificationUser())
                .inputData(userTask.getInputdata())
                .outputData(userTask.getOutputdata())
                .processInstanceDescription(userTask.getProcessInstanceDescription())
                .subject(userTask.getSubject())
                .build();
    }

    public static TaskSummaryList convertToTaskSummaryList(Collection<TaskSummary> tasks) {
        if (tasks == null) {
            return new TaskSummaryList(new org.kie.server.api.model.instance.TaskSummary[0]);
        }
        org.kie.server.api.model.instance.TaskSummary[] instances = new org.kie.server.api.model.instance.TaskSummary[tasks.size()];
        int counter = 0;
        for (TaskSummary taskSummary : tasks) {

            instances[counter] = convertToTaskSummary(taskSummary);
            counter++;
        }

        return new TaskSummaryList(instances);
    }

    public static org.kie.server.api.model.instance.TaskSummary convertToTaskSummary(TaskSummary taskSummary) {
        return org.kie.server.api.model.instance.TaskSummary.builder()
                .id(taskSummary.getId())
                .name(taskSummary.getName())
                .description(taskSummary.getDescription())
                .subject(taskSummary.getSubject())
                .taskParentId(taskSummary.getParentId())
                .activationTime(taskSummary.getActivationTime())
                .actualOwner(taskSummary.getActualOwnerId())
                .containerId(taskSummary.getDeploymentId())
                .createdBy(taskSummary.getCreatedById())
                .createdOn(taskSummary.getCreatedOn())
                .expirationTime(taskSummary.getExpirationTime())
                .priority(taskSummary.getPriority())
                .processId(taskSummary.getProcessId())
                .processInstanceId(taskSummary.getProcessInstanceId())
                .status(taskSummary.getStatusId())
                .skipable(taskSummary.isSkipable())
                .subject(taskSummary.getSubject())
                .correlationKey(taskSummary.getCorrelationKey())
                .processType(taskSummary.getProcessType()).build();
    }

    public static QueryDefinition convertQueryDefinition(org.jbpm.services.api.query.model.QueryDefinition queryDefinition) {
        return queryDefinition == null ? null : QueryDefinition.builder()
                                                               .name(queryDefinition.getName())
                                                               .expression(queryDefinition.getExpression())
                                                               .source(queryDefinition.getSource())
                                                               .target(queryDefinition.getTarget().toString())
                                                               .columns(queryDefinition.getColumns())
                                                               .build();
    }

    public static QueryDefinitionList convertToQueryDefinitionList(Collection<org.jbpm.services.api.query.model.QueryDefinition> definitions) {
        if (definitions == null) {
            return new QueryDefinitionList(new QueryDefinition[0]);
        }
        QueryDefinition[] instances = new QueryDefinition[definitions.size()];
        int counter = 0;
        for (org.jbpm.services.api.query.model.QueryDefinition queryDefinition : definitions) {

            instances[counter] = convertQueryDefinition(queryDefinition);
            counter++;
        }

        return new QueryDefinitionList(instances);
    }

    public static ProcessInstanceCustomVarsList convertToProcessInstanceCustomVarsList(List<ProcessInstanceWithVarsDesc> data) {
        List<ProcessInstanceCustomVars> processInstances = new ArrayList<>();
        for (ProcessInstanceWithVarsDesc proc : data) {
            ProcessInstanceCustomVars tmp = new ProcessInstanceCustomVars();
            tmp.setId(proc.getId());
            tmp.setVariables(proc.getVariables());
            tmp.setProcessId(proc.getProcessId());
            tmp.setCorrelationKey(proc.getCorrelationKey());
            tmp.setContainerId(proc.getDeploymentId());
            tmp.setProcessName(proc.getProcessName());
            tmp.setProcessVersion(proc.getProcessVersion());
            tmp.setDate(proc.getDataTimeStamp());
            tmp.setInitiator(proc.getInitiator());
            tmp.setState(proc.getState());
            processInstances.add(tmp);
        }
        ProcessInstanceCustomVarsList result = new ProcessInstanceCustomVarsList();
        result.setProcessInstances(processInstances.stream().toArray(ProcessInstanceCustomVars[]::new));
        return result;
    }

    public static ProcessInstanceUserTaskWithVariablesList convertToUserTaskWithVariablesList(List<UserTaskInstanceWithPotOwnerDesc> queryUserTasksByVariables) {
        List<ProcessInstanceUserTaskWithVariables> data = new ArrayList<>();
        for (UserTaskInstanceWithPotOwnerDesc desc : queryUserTasksByVariables) {
            ProcessInstanceUserTaskWithVariables var = new ProcessInstanceUserTaskWithVariables();
            var.setId(desc.getTaskId());
            var.setName(desc.getName());
            var.setCorrelationKey(desc.getCorrelationKey());
            var.setActualOwner(desc.getActualOwner());
            var.setProcessDefinitionId(desc.getProcessId());
            var.setPotentialOwners(desc.getPotentialOwners());
            var.setProcessInstanceId(desc.getProcessInstanceId());
            var.setProcessVariables(desc.getProcessVariables());
            var.setInputVariables(desc.getInputdata());
            var.setStatus(desc.getStatus());
            data.add(var);
        }
        ProcessInstanceUserTaskWithVariablesList result = new ProcessInstanceUserTaskWithVariablesList();
        result.setUserTaskWithVariables(data.parallelStream().toArray(ProcessInstanceUserTaskWithVariables[]::new));
        return result;
    }

    public static List<org.jbpm.services.api.query.model.QueryParam> convertToServiceApiQueryParam(List<QueryParam> param) {
        return param.stream().map(e -> new org.jbpm.services.api.query.model.QueryParam(e.getColumn(), e.getOperator(), e.getValue())).collect(toList());
    }

    public static String nullEmpty(String value) {
        if (value != null && value.isEmpty()) {
            return null;
        }

        return value;
    }
    
    public static String checkSort(String sort) {
        return sort == null || sort.isEmpty() ? "ProcessInstanceId" : sort;
    }

    public static List<Integer> checkStatus(List<Integer> status) {
        return status == null || status.isEmpty() ? Collections.singletonList(STATE_ACTIVE) : status;
    }

    public static void throwException(long processInstanceId, String containerId) {
        throw new ProcessInstanceNotFoundException(errorMessage(processInstanceId, containerId));
    }

    public static void throwException(long processInstanceId, String containerId, Throwable ex) {
        throw new ProcessInstanceNotFoundException(errorMessage(processInstanceId, containerId), ex);
    }

    private static String errorMessage(long processInstanceId, String containerId) {
        return "process instance " + processInstanceId + " does not belong to container " + containerId;
    }
}
