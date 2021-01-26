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

package org.kie.server.services.jbpm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jbpm.services.api.DefinitionService;
import org.jbpm.services.api.DeploymentNotFoundException;
import org.jbpm.services.api.ProcessInstanceNotFoundException;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.internal.KieInternalServices;
import org.kie.internal.process.CorrelationKey;
import org.kie.internal.process.CorrelationKeyFactory;
import org.kie.server.api.model.definition.ProcessStartSpec;
import org.kie.server.api.model.instance.ProcessInstanceList;
import org.kie.server.api.model.instance.WorkItemInstance;
import org.kie.server.api.model.instance.WorkItemInstanceList;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.impl.locator.ContainerLocatorProvider;
import org.kie.server.services.impl.locator.LatestContainerLocator;
import org.kie.server.services.impl.marshal.MarshallerHelper;
import org.kie.server.services.jbpm.locator.ByProcessInstanceIdContainerLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.server.services.jbpm.ConvertUtils.buildQueryContext;
import static org.kie.server.services.jbpm.ConvertUtils.checkSort;
import static org.kie.server.services.jbpm.ConvertUtils.checkStatus;
import static org.kie.server.services.jbpm.ConvertUtils.convertToProcessInstance;
import static org.kie.server.services.jbpm.ConvertUtils.convertToProcessInstanceList;

public class ProcessServiceBase {

    public static final Logger logger = LoggerFactory.getLogger(ProcessServiceBase.class);

    private ProcessService processService;
    private DefinitionService definitionService;
    private RuntimeDataService runtimeDataService;
    private MarshallerHelper marshallerHelper;
    private KieServerRegistry context;

    private CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

    public ProcessServiceBase(ProcessService processService, DefinitionService definitionService, RuntimeDataService runtimeDataService, KieServerRegistry context) {
        this.processService = processService;
        this.definitionService = definitionService;
        this.runtimeDataService = runtimeDataService;
        this.marshallerHelper = new MarshallerHelper(context);
        this.context = context;
    }

    public void setMarshallerHelper(MarshallerHelper marshallerHelper){
        this.marshallerHelper = marshallerHelper;
    }

    public String startProcess(String containerId, String processId, String marshallingType) {
        containerId = context.getContainerId(containerId, ContainerLocatorProvider.get().getLocator());
        // check validity of deployment and process id
        definitionService.getProcessDefinition(containerId, processId);

        logger.debug("Calling start process with id {} on container {} and parameters {}", processId, containerId, null);
        Long processInstanceId = processService.startProcess(containerId, processId);

        // return response
        String response = marshallerHelper.marshal(containerId, marshallingType, processInstanceId);
        return response;
    }


    public String startProcess(String containerId, String processId, String payload, String marshallingType) {
        containerId = context.getContainerId(containerId, ContainerLocatorProvider.get().getLocator());
        // check validity of deployment and process id
        definitionService.getProcessDefinition(containerId, processId);

        logger.debug("About to unmarshal parameters from payload: '{}'", payload);
        Map<String, Object> parameters = marshallerHelper.unmarshal(containerId, payload, marshallingType, Map.class);

        logger.debug("Calling start process with id {} on container {} and parameters {}", processId, containerId, parameters);
        Long processInstanceId = processService.startProcess(containerId, processId, parameters);

        // return response
        return marshallerHelper.marshal(containerId, marshallingType, processInstanceId);
    }

    public String startProcessFromNodeIds(String containerId, String processId, String payload, String marshallingType) {
        containerId = context.getContainerId(containerId, ContainerLocatorProvider.get().getLocator());
        // check validity of deployment and process id
        definitionService.getProcessDefinition(containerId, processId);

        logger.debug("About to unmarshal parameters from payload: '{}'", payload);
        ProcessStartSpec parameters = marshallerHelper.unmarshal(containerId, payload, marshallingType, ProcessStartSpec.class);

        logger.debug("Calling start process with id {} on container {} and parameters {}", processId, containerId, parameters.getVariables());
        Long newProcessInstanceId = processService.startProcessFromNodeIds(containerId, processId, parameters.getVariables(), parameters.getNodeIds().stream().toArray(String[]::new));

        // return response
        return marshallerHelper.marshal(containerId, marshallingType, newProcessInstanceId);
    }

    public String startProcessWithCorrelationKeyFromNodeIds(String containerId, String processId, String correlationKey, String payload, String marshallingType) {
        containerId = context.getContainerId(containerId, ContainerLocatorProvider.get().getLocator());
        // check validity of deployment and process id
        definitionService.getProcessDefinition(containerId, processId);

        logger.debug("About to unmarshal parameters from start spec parameters: '{}'", payload);
        ProcessStartSpec parameters = marshallerHelper.unmarshal(containerId, payload, marshallingType, ProcessStartSpec.class);

        String[] correlationProperties = correlationKey.split(":");
        CorrelationKey actualCorrelationKey = correlationKeyFactory.newCorrelationKey(Arrays.asList(correlationProperties));

        logger.debug("Calling start  from custom nodes process with id {} on container {} and parameters {}", processId, containerId, parameters.getVariables());
        Long newProcessInstanceId = processService.startProcessFromNodeIds(containerId, processId, actualCorrelationKey, parameters.getVariables(), parameters.getNodeIds().stream().toArray(String[]::new));

        // return response
        return marshallerHelper.marshal(containerId, marshallingType, newProcessInstanceId);
    }

    public String startProcessWithCorrelation(String containerId, String processId, String correlationKey, String payload, String marshallingType) {
        containerId = context.getContainerId(containerId, ContainerLocatorProvider.get().getLocator());
        // check validity of deployment and process id
        definitionService.getProcessDefinition(containerId, processId);

        logger.debug("About to unmarshal parameters from start spec parameters: '{}'", payload);
        Map<String, Object> parameters = marshallerHelper.unmarshal(containerId, payload, marshallingType, Map.class);

        String[] correlationProperties = correlationKey.split(":");

        CorrelationKey actualCorrelationKey = correlationKeyFactory.newCorrelationKey(Arrays.asList(correlationProperties));

        logger.debug("Calling start from custom nodes process with id {} on container {} and parameters {}", processId, containerId, parameters);
        Long processInstanceId = processService.startProcess(containerId, processId, actualCorrelationKey, parameters);

        // return response
        return marshallerHelper.marshal(containerId, marshallingType, processInstanceId);

    }


    public Object abortProcessInstance(String containerId, Number processInstanceId) {
        try {
            containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(processInstanceId.longValue()));
            processService.abortProcessInstance(containerId, processInstanceId.longValue());
            return null;
        } catch (IllegalArgumentException e) {
            throw new DeploymentNotFoundException(e.getMessage());
        }
    }


    protected List<Long> convert(List<? extends Number> input) {
        List<Long> result = new ArrayList<Long>();

        for (Number n : input) {
            result.add(n.longValue());
        }

        return result;
    }

    public Object abortProcessInstances(String containerId, List<Long> processInstanceIds) {

        processService.abortProcessInstances(containerId, convert(processInstanceIds));
        return null;

    }

    public void signalProcessInstance(String containerId, Number processInstanceId, String signalName, String marshallingType) {

        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(processInstanceId.longValue()));
        logger.debug("Calling signal '{}' process instance with id {} on container {} and event {}", signalName, processInstanceId, containerId, null);
        processService.signalProcessInstance(containerId, processInstanceId.longValue(), signalName, null);

    }

    public void signalProcessInstance(String containerId, Number processInstanceId, String signalName, String eventPayload, String marshallingType) {

        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(processInstanceId.longValue()));
        logger.debug("About to unmarshal event from payload: '{}'", eventPayload);
        Object event = marshallerHelper.unmarshal(containerId, eventPayload, marshallingType, Object.class);

        logger.debug("Calling signal '{}' process instance with id {} on container {} and event {}", signalName, processInstanceId, containerId, event);
        processService.signalProcessInstance(containerId, processInstanceId.longValue(), signalName, event);

    }

    public void signalProcessInstances(String containerId, List<Long> processInstanceIds, String signalName, String marshallingType) {
        List<Long> ids = convert(processInstanceIds);
        if (ids.isEmpty()) {
            return;
        }
        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(ids.get(0)));
        logger.debug("Calling signal '{}' process instances with id {} on container {} and event {}", signalName, processInstanceIds, containerId, null);
        processService.signalProcessInstances(containerId, convert(processInstanceIds), signalName, null);

    }

    public void signalProcessInstances(String containerId, List<Long> processInstanceIds, String signalName, String eventPayload, String marshallingType) {

        List<Long> ids = convert(processInstanceIds);
        if (ids.isEmpty()) {
            return;
        }
        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(ids.get(0)));
        logger.debug("About to unmarshal event from payload: '{}'", eventPayload);
        Object event = marshallerHelper.unmarshal(containerId, eventPayload, marshallingType, Object.class);

        logger.debug("Calling signal '{}' process instances with id {} on container {} and event {}", signalName, processInstanceIds, containerId, event);
        processService.signalProcessInstances(containerId, ids, signalName, event);

    }

    public void signalProcessInstanceByCorrelationKey(String containerId,
                                                      String correlationKey,
                                                      String signalName,
                                                      String eventPayload,
                                                      String marshallingType) {

        String[] correlationProperties = correlationKey.split(":");
        CorrelationKey actualCorrelationKey = correlationKeyFactory.newCorrelationKey(Arrays.asList(correlationProperties));

        logger.debug("About to unmarshal event from payload: '{}'", eventPayload);
        Object event = marshallerHelper.unmarshal(containerId, eventPayload, marshallingType, Object.class);
        
        logger.debug("Calling signal '{}' process instances with correlation key {} on container {} and event {}", signalName, correlationKey, containerId, event);
        processService.signalProcessInstanceByCorrelationKey(actualCorrelationKey, signalName, event);
    }

    public void signalProcessInstancesByCorrelationKey(String containerId,
                                                       List<String> correlationKeys,
                                                       String signalName,
                                                       String eventPayload,
                                                       String marshallingType) {
        
        logger.debug("About to unmarshal event from payload: '{}'", eventPayload);
        Object event = marshallerHelper.unmarshal(containerId, eventPayload, marshallingType, Object.class);
        
        List<CorrelationKey> keys = correlationKeys.stream().map(e -> correlationKeyFactory.newCorrelationKey(Arrays.asList(e.split(":")))).collect(Collectors.toList());
        logger.debug("Calling signal '{}' process instances with correlation key {} on container {} and event {}", signalName, keys, containerId, event);
        processService.signalProcessInstancesByCorrelationKeys(keys, signalName, event);
    }
    
    public void signal(String containerId, String signalName, String marshallingType) {

        logger.debug("Calling signal '{}' on container {} and event {}", signalName, containerId, null);
        processService.signalEvent(context.getContainerId(containerId, LatestContainerLocator.get()), signalName, null);
    }

    public void signal(String containerId, String signalName, String eventPayload, String marshallingType) {

        logger.debug("About to unmarshal event from payload: '{}'", eventPayload);
        Object event = marshallerHelper.unmarshal(containerId, eventPayload, marshallingType, Object.class);

        logger.debug("Calling signal '{}' on container {} and event {}", signalName, containerId, event);
        processService.signalEvent(context.getContainerId(containerId, LatestContainerLocator.get()), signalName,
                event);

    }

    public String getProcessInstance(String containerId,  Number processInstanceId, boolean withVars, String marshallingType) {
        ProcessInstanceDesc instanceDesc =  runtimeDataService.getProcessInstanceById(processInstanceId.longValue());
        if (instanceDesc == null) {
            throw new ProcessInstanceNotFoundException("Unable to find process instance with id " + processInstanceId);
        }
        
        org.kie.server.api.model.instance.ProcessInstance processInstance = convertToProcessInstance(instanceDesc,
                containerId);

        if (Boolean.TRUE.equals(withVars) && processInstance.getState().equals(ProcessInstance.STATE_ACTIVE)) {
            Map<String, Object> variables = processService.getProcessInstanceVariables(containerId, processInstanceId.longValue());
            processInstance.setVariables(variables);
        }

        logger.debug("About to marshal process instance with id '{}' {}", processInstanceId, processInstance);
        return marshallerHelper.marshal(containerId, marshallingType, processInstance, new ByProcessInstanceIdContainerLocator(processInstanceId.longValue()));
    }

    public void setProcessVariable(String containerId, Number processInstanceId, String varName, String variablePayload, String marshallingType) {
        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(processInstanceId.longValue()));
        logger.debug("About to unmarshal variable from payload: '{}'", variablePayload);
        Object variable = marshallerHelper.unmarshal(containerId, variablePayload, marshallingType, Object.class);

        logger.debug("Setting variable '{}' on process instance with id {} with value {}", varName, processInstanceId, variable);
        processService.setProcessVariable(containerId, processInstanceId.longValue(), varName, variable);

    }

    public void setProcessVariables(String containerId, Number processInstanceId, String variablePayload, String marshallingType) {
        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(processInstanceId.longValue()));
        logger.debug("About to unmarshal variables from payload: '{}'", variablePayload);
        Map<String, Object> variables = marshallerHelper.unmarshal(containerId, variablePayload, marshallingType, Map.class);

        logger.debug("Setting variables '{}' on process instance with id {} with value {}", variables.keySet(), processInstanceId, variables.values());
        processService.setProcessVariables(containerId, processInstanceId.longValue(), variables);
   }


    public String getProcessInstanceVariable(String containerId, Number processInstanceId, String varName, String marshallingType) {
        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(processInstanceId.longValue()));
        Object variable = processService.getProcessInstanceVariable(containerId, processInstanceId.longValue(), varName);

        if (variable == null) {
            throw new IllegalStateException("Unable to find variable '"+ varName + "' within process instance with id " + processInstanceId);
        }

        logger.debug("About to marshal process variable with name '{}' {}", varName, variable);
        String response = marshallerHelper.marshal(containerId, marshallingType, variable);

        return response;

    }

    public String getProcessInstanceVariables(String containerId, Number processInstanceId, String marshallingType) {
        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(processInstanceId.longValue()));
        Map<String, Object> variables = processService.getProcessInstanceVariables(containerId, processInstanceId.longValue());

        logger.debug("About to marshal process variables {}", variables);
        String response = marshallerHelper.marshal(containerId, marshallingType, variables);

        return response;

    }


    public String getAvailableSignals(String containerId, Number processInstanceId, String marshallingType) {
        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(processInstanceId.longValue()));
        Collection<String> signals = processService.getAvailableSignals(containerId, processInstanceId.longValue());

        logger.debug("About to marshal available signals {}", signals);
        String response = marshallerHelper.marshal(containerId, marshallingType, signals);

        return response;
    }


    public void completeWorkItem(String containerId, Number processInstanceId, Number workItemId, String resultPayload, String marshallingType) {
        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(processInstanceId.longValue()));
        logger.debug("About to unmarshal work item result from payload: '{}'", resultPayload);
        Map<String, Object> results = marshallerHelper.unmarshal(containerId, resultPayload, marshallingType, Map.class);

        logger.debug("Completing work item '{}' on process instance id {} with value {}", workItemId, processInstanceId, results);
        processService.completeWorkItem(containerId, processInstanceId.longValue(), workItemId.longValue(), results);

    }


    public void abortWorkItem(String containerId, Number processInstanceId, Number workItemId) {
        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(processInstanceId.longValue()));
        logger.debug("Aborting work item '{}' on process instance id {}", workItemId, processInstanceId);
        processService.abortWorkItem(containerId, processInstanceId.longValue(), workItemId.longValue());
    }


    public String getWorkItem(String containerId, Number processInstanceId, Number workItemId, String marshallingType) {
        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(processInstanceId.longValue()));
        WorkItem workItem = processService.getWorkItem(containerId, processInstanceId.longValue(), workItemId.longValue());

        if (workItem == null) {
            throw new IllegalStateException("Unable to find work item with id " + workItemId);
        }

        WorkItemInstance workItemInstance = WorkItemInstance.builder()
                .id(workItem.getId())
                .nodeInstanceId(((org.drools.core.process.instance.WorkItem) workItem).getNodeInstanceId())
                .processInstanceId(workItem.getProcessInstanceId())
                .containerId(((org.drools.core.process.instance.WorkItem) workItem).getDeploymentId())
                .name(workItem.getName())
                .nodeId(((org.drools.core.process.instance.WorkItem) workItem).getNodeId())
                .parameters(workItem.getParameters())
                .state(workItem.getState())
                .build();

        logger.debug("About to marshal work item {}", workItemInstance);
        String response = marshallerHelper.marshal(containerId, marshallingType, workItemInstance, new ByProcessInstanceIdContainerLocator(processInstanceId.longValue()));

        return response;

    }

    public String getWorkItemByProcessInstance(String containerId, Number processInstanceId, String marshallingType) {

        containerId = context.getContainerId(containerId, new ByProcessInstanceIdContainerLocator(processInstanceId.longValue()));
        List<WorkItem> workItems = processService.getWorkItemByProcessInstance(containerId, processInstanceId.longValue());

        WorkItemInstance[] instances = new WorkItemInstance[workItems.size()];
        int counter = 0;
        for (WorkItem workItem : workItems) {
            WorkItemInstance workItemInstance = WorkItemInstance.builder()
                    .id(workItem.getId())
                    .nodeInstanceId(((org.drools.core.process.instance.WorkItem) workItem).getNodeInstanceId())
                    .processInstanceId(workItem.getProcessInstanceId())
                    .containerId(((org.drools.core.process.instance.WorkItem) workItem).getDeploymentId())
                    .name(workItem.getName())
                    .nodeId(((org.drools.core.process.instance.WorkItem) workItem).getNodeId())
                    .parameters(workItem.getParameters())
                    .state(workItem.getState())
                    .build();
            instances[counter] = workItemInstance;
            counter++;
        }
        WorkItemInstanceList result = new WorkItemInstanceList(instances);
        logger.debug("About to marshal work items {}", result);
        String response = marshallerHelper.marshal(containerId, marshallingType, result);

        return response;
    }
    
    public ProcessInstanceList getProcessInstancesByParent(long parentProcessInstanceId, List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
        return getProcessInstancesByParent(null, parentProcessInstanceId, status, page, pageSize, sort, sortOrder);
    }

    public ProcessInstanceList getProcessInstancesByParent(String containerId, long parentProcessInstanceId, List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
        sort = checkSort(sort);
        status = checkStatus(status);
        Collection<ProcessInstanceDesc> instances = runtimeDataService.getProcessInstancesByParent(parentProcessInstanceId, status, buildQueryContext(page, pageSize, sort, sortOrder));


        logger.debug("Found {} process instances , statuses '{}'", instances.size(), status);

        ProcessInstanceList processInstanceList = convertToProcessInstanceList(instances, containerId);
        logger.debug("Returning result of process instance search: {}", processInstanceList);

        return processInstanceList;
    }
}
