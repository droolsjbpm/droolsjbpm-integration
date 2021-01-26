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

package org.kie.server.client.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.kie.internal.process.CorrelationKey;
import org.kie.server.api.commands.CommandScript;
import org.kie.server.api.commands.DescriptorCommand;
import org.kie.server.api.model.KieServerCommand;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.Wrapped;
import org.kie.server.api.model.definition.AssociatedEntitiesDefinition;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.api.model.definition.ProcessStartSpec;
import org.kie.server.api.model.definition.ServiceTasksDefinition;
import org.kie.server.api.model.definition.SubProcessesDefinition;
import org.kie.server.api.model.definition.TaskInputsDefinition;
import org.kie.server.api.model.definition.TaskOutputsDefinition;
import org.kie.server.api.model.definition.UserTaskDefinitionList;
import org.kie.server.api.model.definition.VariablesDefinition;
import org.kie.server.api.model.instance.NodeInstance;
import org.kie.server.api.model.instance.NodeInstanceList;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.ProcessInstanceList;
import org.kie.server.api.model.instance.VariableInstance;
import org.kie.server.api.model.instance.VariableInstanceList;
import org.kie.server.api.model.instance.WorkItemInstance;
import org.kie.server.api.model.instance.WorkItemInstanceList;
import org.kie.server.api.rest.RestURI;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.ProcessServicesClient;

import static java.util.Collections.singletonList;
import static org.kie.server.api.rest.RestURI.ABORT_PROCESS_INSTANCES_DEL_URI;
import static org.kie.server.api.rest.RestURI.ABORT_PROCESS_INST_DEL_URI;
import static org.kie.server.api.rest.RestURI.CONTAINER_ID;
import static org.kie.server.api.rest.RestURI.CORRELATION_KEY;
import static org.kie.server.api.rest.RestURI.PROCESS_DEF_ASSOCIATED_ENTITIES_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_DEF_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_DEF_SERVICE_TASKS_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_DEF_SUBPROCESS_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_DEF_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_DEF_USER_TASKS_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_DEF_USER_TASK_INPUT_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_DEF_USER_TASK_OUTPUT_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_DEF_VARIABLES_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_ID;
import static org.kie.server.api.rest.RestURI.PROCESS_INSTANCES_BY_CONTAINER_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_INSTANCES_BY_PARENT_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_INSTANCES_NODE_INSTANCES_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_INSTANCE_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_INSTANCE_SIGNALS_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_INSTANCE_VARS_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_INSTANCE_VARS_POST_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_INSTANCE_VAR_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_INSTANCE_VAR_INSTANCES_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_INSTANCE_VAR_INSTANCE_BY_VAR_NAME_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_INSTANCE_VAR_PUT_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_INSTANCE_WORK_ITEMS_BY_PROC_INST_ID_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_INSTANCE_WORK_ITEM_ABORT_PUT_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_INSTANCE_WORK_ITEM_BY_ID_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_INSTANCE_WORK_ITEM_COMPLETE_PUT_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_INST_ID;
import static org.kie.server.api.rest.RestURI.PROCESS_URI;
import static org.kie.server.api.rest.RestURI.SIGNAL_NAME;
import static org.kie.server.api.rest.RestURI.SIGNAL_PROCESS_BY_CORRELATION_KEY_POST_URI;
import static org.kie.server.api.rest.RestURI.SIGNAL_PROCESS_INSTANCES_PORT_URI;
import static org.kie.server.api.rest.RestURI.SIGNAL_PROCESS_INST_POST_URI;
import static org.kie.server.api.rest.RestURI.START_PROCESS_FROM_NODES_POST_URI;
import static org.kie.server.api.rest.RestURI.START_PROCESS_FROM_NODES_WITH_CORRELATION_KEY_POST_URI;
import static org.kie.server.api.rest.RestURI.START_PROCESS_POST_URI;
import static org.kie.server.api.rest.RestURI.START_PROCESS_WITH_CORRELATION_KEY_POST_URI;
import static org.kie.server.api.rest.RestURI.TASK_NAME;
import static org.kie.server.api.rest.RestURI.VAR_NAME;
import static org.kie.server.api.rest.RestURI.WORK_ITEM_ID;
import static org.kie.server.api.rest.RestURI.build;

public class ProcessServicesClientImpl extends AbstractKieServicesClientImpl implements ProcessServicesClient {

    public ProcessServicesClientImpl(KieServicesConfiguration config) {
        super(config);
    }

    public ProcessServicesClientImpl(KieServicesConfiguration config, ClassLoader classLoader) {
        super(config, classLoader);
    }

    @Override
    public ProcessDefinition getProcessDefinition(String containerId, String processId) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_ID, processId);

            return makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_DEF_URI + "/" + PROCESS_DEF_GET_URI, valuesMap),
                    ProcessDefinition.class);
        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand) new DescriptorCommand("DefinitionService", "getProcessDefinition", new Object[]{containerId, processId})));
            ServiceResponse<ProcessDefinition> response = (ServiceResponse<ProcessDefinition>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            return response.getResult();
        }
    }

    @Override
    public SubProcessesDefinition getReusableSubProcessDefinitions(String containerId, String processId) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_ID, processId);

            return makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_DEF_URI + "/" + PROCESS_DEF_SUBPROCESS_GET_URI, valuesMap),
                    SubProcessesDefinition.class);

        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand) new DescriptorCommand("DefinitionService", "getReusableSubProcesses", new Object[]{containerId, processId})));
            ServiceResponse<SubProcessesDefinition> response = (ServiceResponse<SubProcessesDefinition>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            return response.getResult();
        }
    }

    @Override
    public VariablesDefinition getProcessVariableDefinitions(String containerId, String processId) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_ID, processId);

            return makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_DEF_URI + "/" + PROCESS_DEF_VARIABLES_GET_URI, valuesMap),
                    VariablesDefinition.class);

        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand) new DescriptorCommand("DefinitionService", "getProcessVariables", new Object[]{containerId, processId})));
            ServiceResponse<VariablesDefinition> response = (ServiceResponse<VariablesDefinition>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            return response.getResult();
        }
    }

    @Override
    public ServiceTasksDefinition getServiceTaskDefinitions(String containerId, String processId) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_ID, processId);

            return makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_DEF_URI + "/" + PROCESS_DEF_SERVICE_TASKS_GET_URI, valuesMap),
                    ServiceTasksDefinition.class);

        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand) new DescriptorCommand("DefinitionService", "getServiceTasks", new Object[]{containerId, processId})));
            ServiceResponse<ServiceTasksDefinition> response = (ServiceResponse<ServiceTasksDefinition>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            return response.getResult();
        }
    }

    @Override
    public AssociatedEntitiesDefinition getAssociatedEntityDefinitions(String containerId, String processId) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_ID, processId);

            return makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_DEF_URI + "/" + PROCESS_DEF_ASSOCIATED_ENTITIES_GET_URI, valuesMap),
                    AssociatedEntitiesDefinition.class);

        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand) new DescriptorCommand("DefinitionService", "getAssociatedEntities", new Object[]{containerId, processId})));
            ServiceResponse<AssociatedEntitiesDefinition> response = (ServiceResponse<AssociatedEntitiesDefinition>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            return response.getResult();
        }
    }

    @Override
    public UserTaskDefinitionList getUserTaskDefinitions(String containerId, String processId) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_ID, processId);

            return makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_DEF_URI + "/" + PROCESS_DEF_USER_TASKS_GET_URI, valuesMap),
                    UserTaskDefinitionList.class);

        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand) new DescriptorCommand("DefinitionService", "getTasksDefinitions", new Object[]{containerId, processId})));
            ServiceResponse<UserTaskDefinitionList> response = (ServiceResponse<UserTaskDefinitionList>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            return response.getResult();
        }
    }

    @Override
    public TaskInputsDefinition getUserTaskInputDefinitions(String containerId, String processId, String taskName) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_ID, processId);
            valuesMap.put(TASK_NAME, encode(taskName));

            return makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_DEF_URI + "/" + PROCESS_DEF_USER_TASK_INPUT_GET_URI, valuesMap),
                    TaskInputsDefinition.class);

        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand) new DescriptorCommand("DefinitionService", "getTaskInputMappings", new Object[]{containerId, processId, taskName})));
            ServiceResponse<TaskInputsDefinition> response = (ServiceResponse<TaskInputsDefinition>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            return response.getResult();
        }
    }

    @Override
    public TaskOutputsDefinition getUserTaskOutputDefinitions(String containerId, String processId, String taskName) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_ID, processId);
            valuesMap.put(TASK_NAME, encode(taskName));

            return makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_DEF_URI + "/" + PROCESS_DEF_USER_TASK_OUTPUT_GET_URI, valuesMap),
                    TaskOutputsDefinition.class);

        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand) new DescriptorCommand("DefinitionService", "getTaskOutputMappings", new Object[]{containerId, processId, taskName})));
            ServiceResponse<TaskOutputsDefinition> response = (ServiceResponse<TaskOutputsDefinition>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            return response.getResult();
        }
    }

    @Override
    public Long startProcess(String containerId, String processId) {
        return startProcess(containerId, processId, new HashMap<String, Object>());
    }

    @Override
    public Long startProcess(String containerId, String processId, Map<String, Object> variables) {
        Object result = null;

        if( config.isRest() ) {

            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_ID, processId);

            result = makeHttpPostRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + START_PROCESS_POST_URI, valuesMap), variables,
                    Object.class);

        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "startProcess", serialize(safeMap(variables)), marshaller.getFormat().getType(), new Object[]{containerId, processId}) ) );
            ServiceResponse<String> response = (ServiceResponse<String>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            result = deserialize(response.getResult(), Object.class);
        }

        if (result instanceof Wrapped) {
            return (Long) ((Wrapped) result).unwrap();
        }

        return ((Number) result).longValue();
    }


    @Override
    public Long startProcessFromNodeIds(String containerId, String processId, Map<String, Object> variables, String... nodes) {
        Object result = null;
        ProcessStartSpec spec = new ProcessStartSpec();
        spec.setVariables(variables);
        spec.setNodeIds(Arrays.asList(nodes));
        if (config.isRest()) {

            Map<String, Object> valuesMap = new HashMap<>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_ID, processId);


            result = makeHttpPostRequestAndCreateCustomResponse(build(loadBalancer.getUrl(), PROCESS_URI + "/" + START_PROCESS_FROM_NODES_POST_URI, valuesMap), spec, Object.class);

        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand) new DescriptorCommand("ProcessService", "startProcessFromNodeIds",
                                                                                                            serialize(spec), marshaller.getFormat().getType(),
                                                                                                            containerId, processId)));
            ServiceResponse<String> response = (ServiceResponse<String>) executeJmsCommand(script, DescriptorCommand.class.getName(), "BPM", containerId).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            result = deserialize(response.getResult(), Object.class);
        }

        if (result instanceof Wrapped) {
            return (Long) ((Wrapped) result).unwrap();
        }

        return ((Number) result).longValue();
    }

    @Override
    public Long startProcessFromNodeIds(String containerId, String processId, CorrelationKey correlationKey, Map<String, Object> variables, String... nodes) {
        Object result = null;

        ProcessStartSpec spec = new ProcessStartSpec();
        spec.setVariables(variables);
        spec.setNodeIds(Arrays.asList(nodes));

        if (config.isRest()) {

            Map<String, Object> valuesMap = new HashMap<>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_ID, processId);
            valuesMap.put(CORRELATION_KEY, correlationKey.toExternalForm());


            result = makeHttpPostRequestAndCreateCustomResponse(build(loadBalancer.getUrl(),
                                                                      PROCESS_URI + "/" + START_PROCESS_FROM_NODES_WITH_CORRELATION_KEY_POST_URI, valuesMap),
                                                                spec, Object.class);

        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand) new DescriptorCommand("ProcessService", "startProcessWithCorrelationKeyFromNodeIds",
                                                                                                            serialize(spec), marshaller.getFormat().getType(),
                                                                                                            containerId, processId, correlationKey.toExternalForm())));
            ServiceResponse<String> response = (ServiceResponse<String>) executeJmsCommand(script, DescriptorCommand.class.getName(), "BPM", containerId).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            result = deserialize(response.getResult(), Object.class);
        }

        if (result instanceof Wrapped) {
            return (Long) ((Wrapped) result).unwrap();
        }

        return ((Number) result).longValue();
    }


    @Override
    public Long startProcess(String containerId, String processId, CorrelationKey correlationKey) {
        return startProcess(containerId, processId, correlationKey, null);
    }

    @Override
    public Long startProcess(String containerId, String processId, CorrelationKey correlationKey, Map<String, Object> variables) {
        Object result = null;
        if( config.isRest() ) {

            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_ID, processId);
            valuesMap.put(CORRELATION_KEY, correlationKey.toExternalForm());

            result = makeHttpPostRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + START_PROCESS_WITH_CORRELATION_KEY_POST_URI, valuesMap), variables,
                    Object.class);

        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "startProcessWithCorrelation", serialize(safeMap(variables)), marshaller.getFormat().getType(), new Object[]{containerId, processId, correlationKey.toExternalForm()}) ) );
            ServiceResponse<String> response = (ServiceResponse<String>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            result = deserialize(response.getResult(), Object.class);
        }

        if (result instanceof Wrapped) {
            return (Long) ((Wrapped) result).unwrap();
        }

        return ((Number) result).longValue();
    }

    @Override
    public void abortProcessInstance(String containerId, Long processInstanceId) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);

            makeHttpDeleteRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + ABORT_PROCESS_INST_DEL_URI, valuesMap),
                    null);

        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "abortProcessInstance", new Object[]{containerId, processInstanceId})));
            ServiceResponse<?> response = (ServiceResponse<?>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);
            throwExceptionOnFailure(response);
        }
    }

    @Override
    public void abortProcessInstances(String containerId, List<Long> processInstanceIds) {
        if( config.isRest() ) {
            String queryStr = buildQueryString("instanceId", processInstanceIds);

            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);

            makeHttpDeleteRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + ABORT_PROCESS_INSTANCES_DEL_URI, valuesMap) + queryStr,
                    null);

        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "abortProcessInstances", new Object[]{containerId, processInstanceIds})));
            ServiceResponse<?> response = (ServiceResponse<?>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);
            throwExceptionOnFailure(response);
        }
    }

    @Override
    public Object getProcessInstanceVariable(String containerId, Long processInstanceId, String variableName) {
        return getProcessInstanceVariable(containerId, processInstanceId, variableName, Object.class);
    }

    @Override
    public <T> T getProcessInstanceVariable(String containerId, Long processInstanceId, String variableName, Class<T> type) {
        Object result = null;
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);
            valuesMap.put(VAR_NAME, variableName);

            result = makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCE_VAR_GET_URI, valuesMap), type);

        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "getProcessInstanceVariable",  marshaller.getFormat().getType(), new Object[]{containerId, processInstanceId, variableName}) ) );
            ServiceResponse<String> response = (ServiceResponse<String>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            result = deserialize(response.getResult(), type);
        }

        if (result instanceof Wrapped) {
            return (T) ((Wrapped) result).unwrap();
        }

        return (T) result;
    }

    @Override
    public Map<String, Object> getProcessInstanceVariables(String containerId, Long processInstanceId) {
        Object variables = null;
        if( config.isRest() ) {

            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);

            variables = makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCE_VARS_GET_URI, valuesMap),
                    Object.class);

        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "getProcessInstanceVariables", marshaller.getFormat().getType(), new Object[]{containerId, processInstanceId}) ) );
            ServiceResponse<String> response = (ServiceResponse<String>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            variables = deserialize(response.getResult(), Object.class);
        }

        if (variables instanceof Wrapped) {
            return (Map) ((Wrapped) variables).unwrap();
        }

        return (Map) variables;
    }

    @Override
    public void signalProcessInstance(String containerId, Long processInstanceId, String signalName, Object event) {

        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);
            valuesMap.put(SIGNAL_NAME, signalName);

            Map<String, String> headers = new HashMap<String, String>();

            makeHttpPostRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + SIGNAL_PROCESS_INST_POST_URI, valuesMap), event, String.class, headers);
        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "signalProcessInstance", serialize(event), marshaller.getFormat().getType(), new Object[]{containerId, processInstanceId, signalName})));
            ServiceResponse<?> response = (ServiceResponse<?>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);
            throwExceptionOnFailure(response);
        }
    }

    @Override
    public void signalProcessInstances(String containerId, List<Long> processInstanceIds, String signalName, Object event) {

        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(SIGNAL_NAME, signalName);

            String queryStr = buildQueryString("instanceId", processInstanceIds);


            Map<String, String> headers = new HashMap<String, String>();
            makeHttpPostRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + SIGNAL_PROCESS_INSTANCES_PORT_URI, valuesMap) + queryStr
                    , event, String.class, headers);
        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "signalProcessInstances", serialize(event), marshaller.getFormat().getType(), new Object[]{containerId, processInstanceIds, signalName})));
            ServiceResponse<?> response = (ServiceResponse<?>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);
            throwExceptionOnFailure(response);
        }
    }

    @Override
    public void signalProcessInstanceByCorrelationKey(String containerId,
                                                      CorrelationKey correlationKey,
                                                      String signalName,
                                                      Object event) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(CORRELATION_KEY, correlationKey.toExternalForm());
            valuesMap.put(SIGNAL_NAME, signalName);

            Map<String, String> headers = new HashMap<String, String>();

            makeHttpPostRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + SIGNAL_PROCESS_BY_CORRELATION_KEY_POST_URI, valuesMap), event, String.class, headers);
        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "signalProcessInstanceByCorrelationKey", serialize(event), marshaller.getFormat().getType(), new Object[]{containerId, correlationKey.toExternalForm(), signalName})));
            ServiceResponse<?> response = (ServiceResponse<?>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);
            throwExceptionOnFailure(response);
        }
    }

    @Override
    public void signalProcessInstancesByCorrelationKeys(String containerId,
                                                        List<CorrelationKey> correlationKeys,
                                                        String signalName,
                                                        Object event) {
        List<String> keys = correlationKeys.stream().map(CorrelationKey::toExternalForm).collect(Collectors.toList());
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(SIGNAL_NAME, signalName);


            String queryStr = buildQueryString("correlationKeys", keys);


            Map<String, String> headers = new HashMap<String, String>();
            makeHttpPostRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + SIGNAL_PROCESS_INSTANCES_PORT_URI, valuesMap) + queryStr
                    , event, String.class, headers);
        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "signalProcessInstancesByCorrelationKey", serialize(event), marshaller.getFormat().getType(), new Object[]{containerId, keys, signalName})));
            ServiceResponse<?> response = (ServiceResponse<?>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);
            throwExceptionOnFailure(response);
        }
    }

    @Override
    public void signal(String containerId, String signalName, Object event) {

        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(SIGNAL_NAME, signalName);

            Map<String, String> headers = new HashMap<String, String>();
            makeHttpPostRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + SIGNAL_PROCESS_INSTANCES_PORT_URI, valuesMap), event, String.class, headers);
        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "signal", serialize(event), marshaller.getFormat().getType(), new Object[]{containerId, signalName})));
            ServiceResponse<?> response = (ServiceResponse<?>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);
            throwExceptionOnFailure(response);
        }
    }

    @Override
    public List<String> getAvailableSignals(String containerId, Long processInstanceId) {
        Object signals = null;
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);

            signals = makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCE_SIGNALS_GET_URI, valuesMap), Object.class);


        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "getAvailableSignals", marshaller.getFormat().getType(), new Object[]{containerId, processInstanceId}) ) );
            ServiceResponse<String> response = (ServiceResponse<String>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            signals = deserialize(response.getResult(), Object.class);
        }

        if (signals instanceof Wrapped) {
            return (List<String>) ((Wrapped)signals).unwrap();
        }

        return (List<String>) signals;
    }

    @Override
    public void setProcessVariable(String containerId, Long processInstanceId, String variableId, Object value) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);
            valuesMap.put(VAR_NAME, variableId);
            makeHttpPutRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCE_VAR_PUT_URI, valuesMap), value, String.class, getHeaders(null));
        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "setProcessVariable", serialize(value), marshaller.getFormat().getType(), new Object[]{containerId, processInstanceId, variableId})));
            ServiceResponse<?> response = (ServiceResponse<?>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);
            throwExceptionOnFailure(response);
        }
    }

    @Override
    public void setProcessVariables(String containerId, Long processInstanceId, Map<String, Object> variables) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);

            makeHttpPostRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCE_VARS_POST_URI, valuesMap), variables,
                    String.class);

        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "setProcessVariables", serialize(variables), marshaller.getFormat().getType(), new Object[]{containerId, processInstanceId})));
            ServiceResponse<?> response = (ServiceResponse<?>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);
            throwExceptionOnFailure(response);
        }
    }

    @Override
    public ProcessInstance getProcessInstance(String containerId, Long processInstanceId) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);

            return makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCE_GET_URI, valuesMap) , ProcessInstance.class);

        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "getProcessInstance", marshaller.getFormat().getType(), new Object[]{containerId, processInstanceId, false} )) );
            ServiceResponse<String> response = (ServiceResponse<String>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            return deserialize(response.getResult(), ProcessInstance.class);
        }
    }

    @Override
    public ProcessInstance getProcessInstance(String containerId, Long processInstanceId, boolean withVars) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);

            return makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCE_GET_URI, valuesMap) + "?withVars=" + withVars , ProcessInstance.class);

        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "getProcessInstance", marshaller.getFormat().getType(), new Object[]{containerId, processInstanceId, withVars}) ) );
            ServiceResponse<String> response = (ServiceResponse<String>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            return deserialize(response.getResult(), ProcessInstance.class);
        }
    }

    @Override
    public void completeWorkItem(String containerId, Long processInstanceId, Long id, Map<String, Object> results) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);
            valuesMap.put(WORK_ITEM_ID, id);

            makeHttpPutRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCE_WORK_ITEM_COMPLETE_PUT_URI, valuesMap), results,
                    String.class, getHeaders(null));
        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "completeWorkItem", serialize(safeMap(results)), marshaller.getFormat().getType(), new Object[]{containerId, processInstanceId, id})));
            ServiceResponse<?> response = (ServiceResponse<?>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);
            throwExceptionOnFailure(response);
        }
    }

    @Override
    public void abortWorkItem(String containerId, Long processInstanceId, Long id) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);
            valuesMap.put(WORK_ITEM_ID, id);

            makeHttpPutRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCE_WORK_ITEM_ABORT_PUT_URI, valuesMap), null,
                    String.class, getHeaders(null));
        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "abortWorkItem",  new Object[]{containerId, processInstanceId, id})));
            ServiceResponse<?> response = (ServiceResponse<?>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);
            throwExceptionOnFailure(response);
        }
    }

    @Override
    public WorkItemInstance getWorkItem(String containerId, Long processInstanceId, Long id) {
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);
            valuesMap.put(WORK_ITEM_ID, id);

            return makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCE_WORK_ITEM_BY_ID_GET_URI, valuesMap), WorkItemInstance.class);


        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "getWorkItem", marshaller.getFormat().getType(), new Object[]{containerId, processInstanceId, id}) ) );
            ServiceResponse<String> response = (ServiceResponse<String>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            return deserialize(response.getResult(), WorkItemInstance.class);
        }
    }

    @Override
    public List<WorkItemInstance> getWorkItemByProcessInstance(String containerId, Long processInstanceId) {
        WorkItemInstanceList list = null;
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);

            list = makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCE_WORK_ITEMS_BY_PROC_INST_ID_GET_URI, valuesMap), WorkItemInstanceList.class);



        } else {
            CommandScript script = new CommandScript(singletonList(
                    (KieServerCommand) new DescriptorCommand( "ProcessService", "getWorkItemByProcessInstance", marshaller.getFormat().getType(), new Object[]{containerId, processInstanceId}) ) );
            ServiceResponse<String> response = (ServiceResponse<String>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM", containerId ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            list = deserialize(response.getResult(), WorkItemInstanceList.class);
        }

        if (list != null && list.getWorkItems() != null) {
            return Arrays.asList(list.getWorkItems());
        }

        return Collections.emptyList();
    }


    @Override
    public List<NodeInstance> findActiveNodeInstances(String containerId, Long processInstanceId, Integer page, Integer pageSize) {
        NodeInstanceList result = null;

        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);

            String queryString = getPagingQueryString("?activeOnly=true", page, pageSize);

            result = makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCES_NODE_INSTANCES_GET_URI, valuesMap) + queryString, NodeInstanceList.class);


        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand)
                    new DescriptorCommand( "QueryService", "getProcessInstanceHistory", new Object[]{containerId, processInstanceId, true, false, page, pageSize}) ) );
            ServiceResponse<NodeInstanceList> response = (ServiceResponse<NodeInstanceList>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM" ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            result = response.getResult();
        }

        if (result != null && result.getNodeInstances() != null) {
            return Arrays.asList(result.getNodeInstances());
        }

        return Collections.emptyList();
    }

    @Override
    public List<NodeInstance> findCompletedNodeInstances(String containerId, Long processInstanceId, Integer page, Integer pageSize) {
        NodeInstanceList result = null;
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);

            String queryString = getPagingQueryString("?completedOnly=true", page, pageSize);

            result = makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCES_NODE_INSTANCES_GET_URI, valuesMap) + queryString, NodeInstanceList.class);


        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand)
                    new DescriptorCommand( "QueryService", "getProcessInstanceHistory", new Object[]{containerId, processInstanceId, false, true, page, pageSize}) ) );
            ServiceResponse<NodeInstanceList> response = (ServiceResponse<NodeInstanceList>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM" ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            result = response.getResult();
        }


        if (result != null && result.getNodeInstances() != null) {
            return Arrays.asList(result.getNodeInstances());
        }

        return Collections.emptyList();
    }

    @Override
    public List<NodeInstance> findNodeInstancesByType(String containerId, Long processInstanceId, String entryType, Integer page, Integer pageSize) {
        NodeInstanceList result = null;
        if (config.isRest()) {
            Map<String, Object> valuesMap = new HashMap<>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);

            String queryString = getPagingQueryString("?" + RestURI.PROCESS_INST_HISTORY_TYPE + "=" + entryType, page, pageSize);

            result = makeHttpGetRequestAndCreateCustomResponse(build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCES_NODE_INSTANCES_GET_URI, valuesMap) + queryString, NodeInstanceList.class);

        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand) new DescriptorCommand("QueryService", "getProcessInstanceFullHistoryByType", new Object[] {
                                                                                                            containerId, processInstanceId, entryType, page, pageSize})));
            ServiceResponse<NodeInstanceList> response = (ServiceResponse<NodeInstanceList>) executeJmsCommand(script, DescriptorCommand.class.getName(), "BPM").getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            result = response.getResult();
        }

        if (result != null && result.getNodeInstances() != null) {
            return Arrays.asList(result.getNodeInstances());
        }

        return Collections.emptyList();
    }

    @Override
    public List<NodeInstance> findNodeInstances(String containerId, Long processInstanceId, Integer page, Integer pageSize) {
        NodeInstanceList result = null;
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);

            String queryString = getPagingQueryString("", page, pageSize);

            result = makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCES_NODE_INSTANCES_GET_URI, valuesMap) + queryString, NodeInstanceList.class);


        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand)
                    new DescriptorCommand( "QueryService", "getProcessInstanceHistory", new Object[]{containerId, processInstanceId, true, true, page, pageSize}) ) );
            ServiceResponse<NodeInstanceList> response = (ServiceResponse<NodeInstanceList>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM" ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            result = response.getResult();
        }

        if (result != null && result.getNodeInstances() != null) {
            return Arrays.asList(result.getNodeInstances());
        }

        return Collections.emptyList();
    }

    @Override
    public List<VariableInstance> findVariablesCurrentState(String containerId, Long processInstanceId) {
        VariableInstanceList result = null;

        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);

            result = makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCE_VAR_INSTANCES_GET_URI, valuesMap), VariableInstanceList.class);



        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand)
            new DescriptorCommand("QueryService", "getVariablesCurrentState", new Object[]{containerId, processInstanceId})));
            ServiceResponse<VariableInstanceList> response = (ServiceResponse<VariableInstanceList>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM" ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            result = response.getResult();
        }

        if (result != null && result.getVariableInstances() != null) {
            return Arrays.asList(result.getVariableInstances());
        }

        return Collections.emptyList();
    }

    @Override
    public List<VariableInstance> findVariableHistory(String containerId, Long processInstanceId, String variableName, Integer page, Integer pageSize) {
        VariableInstanceList result = null;

        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, processInstanceId);
            valuesMap.put(VAR_NAME, variableName);

            String queryString = getPagingQueryString("", page, pageSize);

            result = makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCE_VAR_INSTANCE_BY_VAR_NAME_GET_URI, valuesMap) + queryString, VariableInstanceList.class);


        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand)
                    new DescriptorCommand( "QueryService", "getVariableHistory", new Object[]{containerId, processInstanceId, variableName, page, pageSize}) ) );
            ServiceResponse<VariableInstanceList> response = (ServiceResponse<VariableInstanceList>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM" ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            result = response.getResult();
        }


        if (result != null && result.getVariableInstances() != null) {
            return Arrays.asList(result.getVariableInstances());
        }

        return Collections.emptyList();
    }

    @Override
    public List<ProcessInstance> findProcessInstancesByParent(String containerId, Long parentProcessInstanceId, Integer page, Integer pageSize) {
        return findProcessInstancesByParent(containerId, parentProcessInstanceId, null, page, pageSize);
    }

    @Override
    public List<ProcessInstance> findProcessInstancesByParent(String containerId, Long parentProcessInstanceId, List<Integer> status, Integer page, Integer pageSize) {
        return findProcessInstancesByParent(containerId, parentProcessInstanceId, status, page, pageSize, "", true);
    }

    @Override
    public List<ProcessInstance> findProcessInstancesByParent(String containerId, Long parentProcessInstanceId, List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
        ProcessInstanceList result = null;
        if( config.isRest() ) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);
            valuesMap.put(PROCESS_INST_ID, parentProcessInstanceId);

            String statusQueryString = getAdditionalParams("?sort="+sort+"&sortOrder="+sortOrder, "status", status);
            String queryString = getPagingQueryString(statusQueryString, page, pageSize);

            result = makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCES_BY_PARENT_GET_URI, valuesMap) + queryString, ProcessInstanceList.class);
        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand)
                    new DescriptorCommand( "ProcessService", "getProcessInstancesByParent", new Object[]{containerId, parentProcessInstanceId, safeList(status), page, pageSize, sort, sortOrder}) ) );
            ServiceResponse<ProcessInstanceList> response = (ServiceResponse<ProcessInstanceList>) executeJmsCommand( script, DescriptorCommand.class.getName(), "BPM" ).getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            result = response.getResult();
        }


        if (result != null && result.getProcessInstances() != null) {
            return Arrays.asList(result.getProcessInstances());
        }

        return Collections.emptyList();
    }

    @Override
    public List<ProcessInstance> findProcessInstances(String containerId, Integer page, Integer pageSize) {
        return findProcessInstances(containerId, page, pageSize, "", true);
    }

    @Override
    public List<ProcessInstance> findProcessInstances(String containerId, Integer page, Integer pageSize, String sort, boolean sortOrder) {
        ProcessInstanceList result = null;
        if (config.isRest()) {
            Map<String, Object> valuesMap = new HashMap<String, Object>();
            valuesMap.put(CONTAINER_ID, containerId);

            String queryString = getPagingQueryString("?sort=" + sort + "&sortOrder=" + sortOrder, page, pageSize);

            result = makeHttpGetRequestAndCreateCustomResponse(
                    build(loadBalancer.getUrl(), PROCESS_URI + "/" + PROCESS_INSTANCES_BY_CONTAINER_GET_URI, valuesMap) + queryString, ProcessInstanceList.class);
        } else {
            CommandScript script = new CommandScript(singletonList((KieServerCommand)
                    new DescriptorCommand("QueryService", "getProcessInstances", new Object[]{new ArrayList(), "", "", page, pageSize, sort, sortOrder})));
            ServiceResponse<ProcessInstanceList> response = (ServiceResponse<ProcessInstanceList>) executeJmsCommand(script, DescriptorCommand.class.getName(), "BPM").getResponses().get(0);

            throwExceptionOnFailure(response);
            if (shouldReturnWithNullResponse(response)) {
                return null;
            }
            result = response.getResult();
        }

        if (result != null && result.getProcessInstances() != null) {
            return Arrays.asList(result.getProcessInstances());
        }

        return Collections.emptyList();
    }

}
