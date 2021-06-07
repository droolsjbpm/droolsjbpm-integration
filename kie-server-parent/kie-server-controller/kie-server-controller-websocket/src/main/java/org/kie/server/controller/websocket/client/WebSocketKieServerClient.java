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

package org.kie.server.controller.websocket.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.websocket.Session;
import org.kie.api.command.Command;
import org.kie.internal.process.CorrelationKey;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.commands.ActivateContainerCommand;
import org.kie.server.api.commands.CommandScript;
import org.kie.server.api.commands.CreateContainerCommand;
import org.kie.server.api.commands.DeactivateContainerCommand;
import org.kie.server.api.commands.DescriptorCommand;
import org.kie.server.api.commands.DisposeContainerCommand;
import org.kie.server.api.commands.GetContainerInfoCommand;
import org.kie.server.api.commands.GetReleaseIdCommand;
import org.kie.server.api.commands.GetScannerInfoCommand;
import org.kie.server.api.commands.GetServerInfoCommand;
import org.kie.server.api.commands.GetServerStateCommand;
import org.kie.server.api.commands.ListContainersCommand;
import org.kie.server.api.commands.UpdateReleaseIdCommand;
import org.kie.server.api.commands.UpdateScannerCommand;
import org.kie.server.api.exception.KieServicesException;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceFilter;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.KieScannerResource;
import org.kie.server.api.model.KieServerCommand;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.KieServerStateInfo;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.ServiceResponsesList;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.api.model.definition.ProcessInstanceQueryFilterSpec;
import org.kie.server.api.model.definition.QueryDefinition;
import org.kie.server.api.model.definition.QueryFilterSpec;
import org.kie.server.api.model.definition.SearchQueryFilterSpec;
import org.kie.server.api.model.definition.TaskQueryFilterSpec;
import org.kie.server.api.model.instance.NodeInstance;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.ProcessInstanceCustomVars;
import org.kie.server.api.model.instance.ProcessInstanceUserTaskWithVariables;
import org.kie.server.api.model.instance.TaskInstance;
import org.kie.server.api.model.instance.VariableInstance;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.jms.ResponseHandler;
import org.kie.server.controller.websocket.WebSocketSessionManager;
import org.kie.server.controller.websocket.common.WebSocketUtils;
import org.kie.server.controller.websocket.common.handlers.WebSocketServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class WebSocketKieServerClient implements KieServicesClient {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketKieServerClient.class);
    
    private WebSocketSessionManager manager = WebSocketSessionManager.getInstance();
    private String url;
    private KieServerInfo serverInfo;
    
    public WebSocketKieServerClient(String url) {
        this.url = url;
        this.serverInfo = manager.getServerInfoByUrl(url);
    }

    private static <T> T throwUnsupportedException(){
        throw new UnsupportedOperationException("Not supported for Web Socket implementation");
    }

    @Override
    public <T> T getServicesClient(Class<T> serviceClient) {
        if (QueryServicesClient.class.isAssignableFrom(serviceClient)) {
            if (!serverInfo.getCapabilities().contains(KieServerConstants.CAPABILITY_BPM)) {
                throw new KieServicesException("Server that this client is connected to has no capabilities to handle " + serviceClient.getSimpleName());
            }
            return (T) new QueryServicesClient() {
                
                
                @Override
                public void unregisterQuery(String queryName) {
                    throwUnsupportedException();
                }
                
                @Override
                public void setResponseHandler(ResponseHandler responseHandler) {
                    throwUnsupportedException();
                }
                
                @Override
                public QueryDefinition replaceQuery(QueryDefinition queryDefinition) {
                    CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new DescriptorCommand("QueryDataService", "replaceQuery",
                                                                                                                                WebSocketUtils.marshal(queryDefinition), MarshallingFormat.JSON.toString(), new Object[]{queryDefinition.getName()})));
                    sendCommandToAllSessions(script, new WebSocketServiceResponse(true, (message) -> {
                        WebSocketUtils.unmarshal(message, ServiceResponsesList.class);
                        return null;
                    })).getResponses();  
                    
                    return queryDefinition;
                }
                
                @Override
                public QueryDefinition registerQuery(QueryDefinition queryDefinition) {
                    return throwUnsupportedException();
                }
                
                @Override
                public <T> List<T> query(String queryName, String mapper, String builder, Map<String, Object> parameters, Integer page, Integer pageSize, Class<T> resultType) {
                    return throwUnsupportedException();
                }
                
                @Override
                public <T> List<T> query(String queryName, String mapper, QueryFilterSpec filterSpec, Integer page, Integer pageSize, Class<T> resultType) {
                    return throwUnsupportedException();
                }
                
                @Override
                public <T> List<T> query(String queryName, String mapper, String orderBy, Integer page, Integer pageSize, Class<T> resultType) {
                    return throwUnsupportedException();
                }
                
                @Override
                public <T> List<T> query(String queryName, String mapper, Integer page, Integer pageSize, Class<T> resultType) {
                    return throwUnsupportedException();
                }

                @Override
                public <T> List<T> query(String containerId, String queryName, String mapper, String builder, Map<String, Object> parameters, Integer page, Integer pageSize, Class<T> resultType) {
                    return throwUnsupportedException();
                }
                
                @Override
                public QueryDefinition getQuery(String queryName) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<QueryDefinition> getQueries(Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<VariableInstance> findVariablesCurrentState(Long processInstanceId) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<VariableInstance> findVariableHistory(Long processInstanceId, String variableName, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessDefinition> findProcessesById(String processId) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessDefinition> findProcessesByContainerId(String containerId, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessDefinition> findProcessesByContainerId(String containerId, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessDefinition> findProcesses(String filter, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessDefinition> findProcesses(Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessDefinition> findProcesses(String filter, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessDefinition> findProcesses(Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByVariableAndValue(String variableName, String variableValue, List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByVariableAndValue(String variableName, String variableValue, List<Integer> status, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByVariable(String variableName, List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByVariable(String variableName, List<Integer> status, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByStatus(List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByStatus(List<Integer> status, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByProcessName(String processName, List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByProcessName(String processName, List<Integer> status, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByProcessId(String processId, List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByProcessId(String processId, List<Integer> status, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByInitiator(String initiator, List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByInitiator(String initiator, List<Integer> status, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByCorrelationKey(CorrelationKey correlationKey, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByCorrelationKey(CorrelationKey correlationKey, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByContainerId(String containerId, List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByContainerId(String containerId, List<Integer> status, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstances(Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<ProcessInstance> findProcessInstances(Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
                
                @Override
                public ProcessInstance findProcessInstanceById(Long processInstanceId, boolean withVars) {
                    return throwUnsupportedException();
                }
                
                @Override
                public ProcessInstance findProcessInstanceById(Long processInstanceId) {
                    return throwUnsupportedException();
                }
                
                @Override
                public ProcessInstance findProcessInstanceByCorrelationKey(CorrelationKey correlationKey) {
                    return throwUnsupportedException();
                }
                
                @Override
                public ProcessDefinition findProcessByContainerIdProcessId(String containerId, String processId) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<NodeInstance> findNodeInstances(Long processInstanceId, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
                
                @Override
                public NodeInstance findNodeInstanceByWorkItemId(Long processInstanceId, Long workItemId) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<NodeInstance> findCompletedNodeInstances(Long processInstanceId, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
                
                @Override
                public List<NodeInstance> findActiveNodeInstances(Long processInstanceId, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }

                @Override
                public List<ProcessInstance> findProcessInstancesWithFilters(String queryName, ProcessInstanceQueryFilterSpec filterSpec, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }

                @Override
                public List<TaskInstance> findHumanTasksWithFilters(String queryName, TaskQueryFilterSpec filterSpec, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }

                @Override
                public List<ProcessInstance> findProcessInstancesByProcessIdAndInitiator(String processId, String initiator, List<Integer> status, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }

                @Override
                public List<ProcessInstance> findProcessInstancesByProcessIdAndInitiator(String processId, String initiator, List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    return throwUnsupportedException();
                }

                @Override
                public List<ProcessInstanceCustomVars> queryProcessesByVariables(SearchQueryFilterSpec spec, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }

                @Override
                public List<ProcessInstanceUserTaskWithVariables> queryUserTaskByVariables(SearchQueryFilterSpec spec, Integer page, Integer pageSize) {
                    return throwUnsupportedException();
                }
            };
        }

        return throwUnsupportedException();
    }
    
    protected ServiceResponsesList sendCommand(CommandScript script, WebSocketServiceResponse response) {
        logger.debug("About to send command {} to kie server located at {}", script, url);
        List<Session> sessions = manager.getByUrl(url);
        
        Session session = sessions.get(0);
        
        logger.debug("Web Socket session ({}) is open {}", session.getId(), session.isOpen());
        String content = WebSocketUtils.marshal(script);
        logger.debug("Content to be sent over Web Socket '{}'", content);
        try {
            manager.getHandler(session.getId()).addHandler(response);
            
            session.getBasicRemote().sendText(content);
            logger.debug("Message successfully sent to kie server");
            return new ServiceResponsesList(Arrays.asList(response));
        } catch (IOException e) {
           throw new RuntimeException(e);
        }
        
    }
    
    protected ServiceResponsesList sendCommandToAllSessions(CommandScript script, WebSocketServiceResponse response) {
        logger.debug("About to send command {} to kie server located at {}", script, url);
        List<Session> sessions = manager.getByUrl(url);
        List<ServiceResponse<?>> responses = new ArrayList<>();
        ServiceResponsesList result = new ServiceResponsesList(responses);
               
        for (Session session : sessions) {
        
            logger.debug("Web Socket session ({}) is open {}", session.getId(), session.isOpen());
            String content = WebSocketUtils.marshal(script);
            logger.debug("Content to be sent over Web Socket '{}'", content);
            try {
                manager.getHandler(session.getId()).addHandler(response);
                
                session.getBasicRemote().sendText(content);
                logger.debug("Message successfully sent to kie server");
                responses.add(response);
            } catch (IOException e) {
               throw new RuntimeException(e);
            }
        
        }
        return result;
    }

    
    @Override
    public ServiceResponse<KieServerInfo> getServerInfo() {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new GetServerInfoCommand()));
        ServiceResponse<KieServerInfo> response = (ServiceResponse<KieServerInfo>) sendCommand(script, new WebSocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebSocketUtils.unmarshal(message, ServiceResponsesList.class);
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response;
    }


    @Override
    public ServiceResponse<KieContainerResourceList> listContainers() {
        return listContainers(KieContainerResourceFilter.ACCEPT_ALL);
    }

    @Override
    public ServiceResponse<KieContainerResourceList> listContainers(KieContainerResourceFilter containerFilter) {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new ListContainersCommand(containerFilter)));
        ServiceResponse<KieContainerResourceList> response = (ServiceResponse<KieContainerResourceList>) sendCommand(script, new WebSocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebSocketUtils.unmarshal(message, ServiceResponsesList.class);
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response;
    }

    @Override
    public ServiceResponse<KieContainerResource> createContainer(String id, KieContainerResource resource) {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new CreateContainerCommand(resource)));
        ServiceResponse<KieContainerResource> response = (ServiceResponse<KieContainerResource>) sendCommandToAllSessions(script, new WebSocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebSocketUtils.unmarshal(message, ServiceResponsesList.class);
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response;
    }

    @Override
    public ServiceResponse<KieContainerResource> getContainerInfo(String id) {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new GetContainerInfoCommand(id)));
        ServiceResponse<KieContainerResource> response = (ServiceResponse<KieContainerResource>) sendCommand(script, new WebSocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebSocketUtils.unmarshal(message, ServiceResponsesList.class);
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response;
    }

    @Override
    public ServiceResponse<Void> disposeContainer(String id) {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new DisposeContainerCommand(id)));
        ServiceResponse<Void> response = (ServiceResponse<Void>) sendCommandToAllSessions(script, new WebSocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebSocketUtils.unmarshal(message, ServiceResponsesList.class);
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response;
    }

    @Override
    public ServiceResponsesList executeScript(CommandScript script) {
        return throwUnsupportedException();
    }

    @Override
    public ServiceResponse<KieScannerResource> getScannerInfo(String id) {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new GetScannerInfoCommand(id)));
        ServiceResponse<KieScannerResource> response = (ServiceResponse<KieScannerResource>) sendCommand(script, new WebSocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebSocketUtils.unmarshal(message, ServiceResponsesList.class);
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response;
    }

    @Override
    public ServiceResponse<KieScannerResource> updateScanner(String id, KieScannerResource resource) {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new UpdateScannerCommand(id, resource)));
        ServiceResponse<KieScannerResource> response = (ServiceResponse<KieScannerResource>) sendCommandToAllSessions(script, new WebSocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebSocketUtils.unmarshal(message, ServiceResponsesList.class);
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response; 
    }

    @Override
    public ServiceResponse<ReleaseId> getReleaseId(String containerId) {
        CommandScript script = new CommandScript(Collections.singletonList(new GetReleaseIdCommand(containerId)));
        ServiceResponse<ReleaseId> response = (ServiceResponse<ReleaseId>) sendCommand(script, new WebSocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebSocketUtils.unmarshal(message, ServiceResponsesList.class);
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response; 
    }

    @Override
    public ServiceResponse<ReleaseId> updateReleaseId(String id, ReleaseId releaseId) {
        return updateReleaseId(id, releaseId, false);
    }

    @Override
    public ServiceResponse<ReleaseId> updateReleaseId(String id, ReleaseId releaseId, boolean resetBeforeUpdate) {
        CommandScript script = new CommandScript(Collections.singletonList(new UpdateReleaseIdCommand(id, releaseId, resetBeforeUpdate)));
        ServiceResponse<ReleaseId> response = (ServiceResponse<ReleaseId>) sendCommandToAllSessions(script, new WebSocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebSocketUtils.unmarshal(message, ServiceResponsesList.class);
            return list.getResponses().get(0);
        })).getResponses().get(0);

        return response;
    }

    @Override
    public ServiceResponse<KieServerStateInfo> getServerState() {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new GetServerStateCommand()));
        ServiceResponse<KieServerStateInfo> response = (ServiceResponse<KieServerStateInfo>) sendCommand(script, new WebSocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebSocketUtils.unmarshal(message, ServiceResponsesList.class);
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response; 
    }

    @Override
    public ServiceResponse<String> executeCommands(String id, String payload) {
        return throwUnsupportedException();
    }

    @Override
    public ServiceResponse<String> executeCommands(String id, Command<?> cmd) {
        return throwUnsupportedException();
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        // no-op
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }

    @Override
    public String getConversationId() {
        return null;
    }

    @Override
    public void completeConversation() {
        // no-op
    }

    @Override
    public void setResponseHandler(ResponseHandler responseHandler) {
        // no-op
    }

    @Override
    public ServiceResponse<KieContainerResource> activateContainer(String id) {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new ActivateContainerCommand(id)));
        ServiceResponse<KieContainerResource> response = (ServiceResponse<KieContainerResource>) sendCommandToAllSessions(script, new WebSocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebSocketUtils.unmarshal(message, ServiceResponsesList.class);
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response;
    }

    @Override
    public ServiceResponse<KieContainerResource> deactivateContainer(String id) {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new DeactivateContainerCommand(id)));
        ServiceResponse<KieContainerResource> response = (ServiceResponse<KieContainerResource>) sendCommandToAllSessions(script, new WebSocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebSocketUtils.unmarshal(message, ServiceResponsesList.class);
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response;
    }
    
    @Override
    public void close() {
        // no-op
    }

}
