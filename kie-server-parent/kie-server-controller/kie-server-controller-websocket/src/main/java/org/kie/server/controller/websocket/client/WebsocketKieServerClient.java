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

import javax.websocket.Session;

import org.kie.api.command.Command;
import org.kie.internal.process.CorrelationKey;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.commands.CommandScript;
import org.kie.server.api.commands.CreateContainerCommand;
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
import org.kie.server.api.model.definition.QueryDefinition;
import org.kie.server.api.model.definition.QueryFilterSpec;
import org.kie.server.api.model.instance.NodeInstance;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.VariableInstance;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.jms.ResponseHandler;
import org.kie.server.controller.websocket.WebsocketSessionManager;
import org.kie.server.controller.websocket.WebsocketUtils;
import org.kie.server.controller.websocket.common.handlers.WebsocketServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class WebsocketKieServerClient implements KieServicesClient {
    
    private static final Logger logger = LoggerFactory.getLogger(WebsocketKieServerClient.class);
    
    private WebsocketSessionManager manager = WebsocketSessionManager.getInstance();
    private String url;
    private KieServerInfo serverInfo;
    
    public WebsocketKieServerClient(String url) {
        this.url = url;
        this.serverInfo = manager.getServerInfoByUrl(url);
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
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public void setResponseHandler(ResponseHandler responseHandler) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public void replaceQuery(QueryDefinition queryDefinition) {
                    CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new DescriptorCommand("QueryDataService", "replaceQuery",  
                            WebsocketUtils.marshal(MarshallingFormat.JSON.toString(), queryDefinition), MarshallingFormat.JSON.toString(), new Object[]{queryDefinition.getName()})));
                    sendCommandToAllSessions(script, new WebsocketServiceResponse(true, (message) -> {
                        WebsocketUtils.unmarshal(message, MarshallingFormat.JSON.getType(), ServiceResponsesList.class);            
                        return null;
                    })).getResponses();    
                }
                
                @Override
                public void registerQuery(QueryDefinition queryDefinition) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public <T> List<T> query(String queryName, String mapper, String builder, Map<String, Object> parameters, Integer page, Integer pageSize, Class<T> resultType) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public <T> List<T> query(String queryName, String mapper, QueryFilterSpec filterSpec, Integer page, Integer pageSize, Class<T> resultType) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public <T> List<T> query(String queryName, String mapper, String orderBy, Integer page, Integer pageSize, Class<T> resultType) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public <T> List<T> query(String queryName, String mapper, Integer page, Integer pageSize, Class<T> resultType) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public QueryDefinition getQuery(String queryName) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<QueryDefinition> getQueries(Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<VariableInstance> findVariablesCurrentState(Long processInstanceId) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<VariableInstance> findVariableHistory(Long processInstanceId, String variableName, Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessDefinition> findProcessesById(String processId) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessDefinition> findProcessesByContainerId(String containerId, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessDefinition> findProcessesByContainerId(String containerId, Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessDefinition> findProcesses(String filter, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessDefinition> findProcesses(Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessDefinition> findProcesses(String filter, Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessDefinition> findProcesses(Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByVariableAndValue(String variableName, String variableValue, List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByVariableAndValue(String variableName, String variableValue, List<Integer> status, Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByVariable(String variableName, List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByVariable(String variableName, List<Integer> status, Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByStatus(List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByStatus(List<Integer> status, Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByProcessName(String processName, List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByProcessName(String processName, List<Integer> status, Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByProcessId(String processId, List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByProcessId(String processId, List<Integer> status, Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByInitiator(String initiator, List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByInitiator(String initiator, List<Integer> status, Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByCorrelationKey(CorrelationKey correlationKey, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByCorrelationKey(CorrelationKey correlationKey, Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByContainerId(String containerId, List<Integer> status, Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstancesByContainerId(String containerId, List<Integer> status, Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstances(Integer page, Integer pageSize, String sort, boolean sortOrder) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<ProcessInstance> findProcessInstances(Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public ProcessInstance findProcessInstanceById(Long processInstanceId, boolean withVars) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public ProcessInstance findProcessInstanceById(Long processInstanceId) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public ProcessInstance findProcessInstanceByCorrelationKey(CorrelationKey correlationKey) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public ProcessDefinition findProcessByContainerIdProcessId(String containerId, String processId) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<NodeInstance> findNodeInstances(Long processInstanceId, Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public NodeInstance findNodeInstanceByWorkItemId(Long processInstanceId, Long workItemId) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<NodeInstance> findCompletedNodeInstances(Long processInstanceId, Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
                
                @Override
                public List<NodeInstance> findActiveNodeInstances(Long processInstanceId, Integer page, Integer pageSize) {
                    throw new UnsupportedOperationException("Not supported for websocket implementation");
                }
            };
        }
        
        throw new UnsupportedOperationException("Not supported for websocket implementation");
    }    
    
    protected ServiceResponsesList sendCommand(CommandScript script, WebsocketServiceResponse response) {
        logger.debug("About to send command {} to kie server located at {}", script, url);
        List<Session> sessions = manager.getByUrl(url);
        
        Session session = sessions.get(0);
        
        logger.debug("Websocket session ({}) is open {}", session.getId(), session.isOpen());
        String content = WebsocketUtils.marshal(MarshallingFormat.JSON.getType(), script);
        logger.debug("Content to be sent over websocket '{}'", content);
        try {
            manager.getHandler(session.getId()).addHandler(response);
            
            session.getBasicRemote().sendText(content);
            logger.debug("Message successfully sent to kie server");
            return new ServiceResponsesList(Arrays.asList(response));
        } catch (IOException e) {
           throw new RuntimeException(e);
        }
        
    }
    
    protected ServiceResponsesList sendCommandToAllSessions(CommandScript script, WebsocketServiceResponse response) {
        logger.debug("About to send command {} to kie server located at {}", script, url);
        List<Session> sessions = manager.getByUrl(url);
        List<ServiceResponse<?>> responses = new ArrayList<>();
        ServiceResponsesList result = new ServiceResponsesList(responses);
               
        for (Session session : sessions) {
        
            logger.debug("Websocket session ({}) is open {}", session.getId(), session.isOpen());
            String content = WebsocketUtils.marshal(MarshallingFormat.JSON.getType(), script);
            logger.debug("Content to be sent over websocket '{}'", content);
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
        ServiceResponse<KieServerInfo> response = (ServiceResponse<KieServerInfo>) sendCommand(script, new WebsocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebsocketUtils.unmarshal(message, MarshallingFormat.JSON.getType(), ServiceResponsesList.class);            
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
        ServiceResponse<KieContainerResourceList> response = (ServiceResponse<KieContainerResourceList>) sendCommand(script, new WebsocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebsocketUtils.unmarshal(message, MarshallingFormat.JSON.getType(), ServiceResponsesList.class);            
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response;
    }

    @Override
    public ServiceResponse<KieContainerResource> createContainer(String id, KieContainerResource resource) {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new CreateContainerCommand(resource)));
        ServiceResponse<KieContainerResource> response = (ServiceResponse<KieContainerResource>) sendCommandToAllSessions(script, new WebsocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebsocketUtils.unmarshal(message, MarshallingFormat.JSON.getType(), ServiceResponsesList.class);            
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response;
    }

    @Override
    public ServiceResponse<KieContainerResource> getContainerInfo(String id) {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new GetContainerInfoCommand(id)));
        ServiceResponse<KieContainerResource> response = (ServiceResponse<KieContainerResource>) sendCommand(script, new WebsocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebsocketUtils.unmarshal(message, MarshallingFormat.JSON.getType(), ServiceResponsesList.class);            
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response;
    }

    @Override
    public ServiceResponse<Void> disposeContainer(String id) {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new DisposeContainerCommand(id)));
        ServiceResponse<Void> response = (ServiceResponse<Void>) sendCommandToAllSessions(script, new WebsocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebsocketUtils.unmarshal(message, MarshallingFormat.JSON.getType(), ServiceResponsesList.class);            
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response;
    }

    @Override
    public ServiceResponsesList executeScript(CommandScript script) {
        throw new UnsupportedOperationException("Not supported for websocket implementation");
    }

    @Override
    public ServiceResponse<KieScannerResource> getScannerInfo(String id) {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new GetScannerInfoCommand(id)));
        ServiceResponse<KieScannerResource> response = (ServiceResponse<KieScannerResource>) sendCommand(script, new WebsocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebsocketUtils.unmarshal(message, MarshallingFormat.JSON.getType(), ServiceResponsesList.class);            
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response;
    }

    @Override
    public ServiceResponse<KieScannerResource> updateScanner(String id, KieScannerResource resource) {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new UpdateScannerCommand(id, resource)));
        ServiceResponse<KieScannerResource> response = (ServiceResponse<KieScannerResource>) sendCommandToAllSessions(script, new WebsocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebsocketUtils.unmarshal(message, MarshallingFormat.JSON.getType(), ServiceResponsesList.class);            
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response; 
    }

    @Override
    public ServiceResponse<ReleaseId> getReleaseId(String containerId) {
        CommandScript script = new CommandScript(Collections.singletonList(new GetReleaseIdCommand(containerId)));
        ServiceResponse<ReleaseId> response = (ServiceResponse<ReleaseId>) sendCommand(script, new WebsocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebsocketUtils.unmarshal(message, MarshallingFormat.JSON.getType(), ServiceResponsesList.class);            
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response; 
    }

    @Override
    public ServiceResponse<ReleaseId> updateReleaseId(String id, ReleaseId releaseId) {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new UpdateReleaseIdCommand(id, releaseId)));
        ServiceResponse<ReleaseId> response = (ServiceResponse<ReleaseId>) sendCommandToAllSessions(script, new WebsocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebsocketUtils.unmarshal(message, MarshallingFormat.JSON.getType(), ServiceResponsesList.class);            
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response; 
    }

    @Override
    public ServiceResponse<KieServerStateInfo> getServerState() {
        CommandScript script = new CommandScript(Collections.singletonList((KieServerCommand) new GetServerStateCommand()));
        ServiceResponse<KieServerStateInfo> response = (ServiceResponse<KieServerStateInfo>) sendCommand(script, new WebsocketServiceResponse(true, (message) -> {
            ServiceResponsesList list = WebsocketUtils.unmarshal(message, MarshallingFormat.JSON.getType(), ServiceResponsesList.class);            
            return list.getResponses().get(0);
        })).getResponses().get(0);
        
        return response; 
    }

    @Override
    public ServiceResponse<String> executeCommands(String id, String payload) {
        throw new UnsupportedOperationException("Not supported for websocket implementation");
    }

    @Override
    public ServiceResponse<String> executeCommands(String id, Command<?> cmd) {
        throw new UnsupportedOperationException("Not supported for websocket implementation");
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

}
