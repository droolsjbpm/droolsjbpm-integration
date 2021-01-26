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

package org.kie.server.remote.rest.jbpm;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import org.jbpm.services.api.DeploymentNotActiveException;
import org.jbpm.services.api.DeploymentNotFoundException;
import org.jbpm.services.api.ProcessDefinitionNotFoundException;
import org.jbpm.services.api.ProcessInstanceNotFoundException;
import org.jbpm.services.api.WorkItemNotFoundException;
import org.kie.server.api.model.definition.ProcessDefinitionList;
import org.kie.server.api.model.instance.NodeInstanceList;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.ProcessInstanceList;
import org.kie.server.api.model.instance.VariableInstanceList;
import org.kie.server.api.model.instance.WorkItemInstance;
import org.kie.server.api.model.instance.WorkItemInstanceList;
import org.kie.server.remote.rest.common.Header;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.jbpm.ProcessServiceBase;
import org.kie.server.services.jbpm.RuntimeDataServiceBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.server.api.rest.RestURI.ABORT_PROCESS_INSTANCES_DEL_URI;
import static org.kie.server.api.rest.RestURI.ABORT_PROCESS_INST_DEL_URI;
import static org.kie.server.api.rest.RestURI.CONTAINER_ID;
import static org.kie.server.api.rest.RestURI.CORRELATION_KEY;
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
import static org.kie.server.api.rest.RestURI.PROCESS_INST_HISTORY_TYPE;
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
import static org.kie.server.remote.rest.common.util.RestUtils.badRequest;
import static org.kie.server.remote.rest.common.util.RestUtils.buildConversationIdHeader;
import static org.kie.server.remote.rest.common.util.RestUtils.createCorrectVariant;
import static org.kie.server.remote.rest.common.util.RestUtils.createResponse;
import static org.kie.server.remote.rest.common.util.RestUtils.errorMessage;
import static org.kie.server.remote.rest.common.util.RestUtils.forbidden;
import static org.kie.server.remote.rest.common.util.RestUtils.getContentType;
import static org.kie.server.remote.rest.common.util.RestUtils.getVariant;
import static org.kie.server.remote.rest.common.util.RestUtils.internalServerError;
import static org.kie.server.remote.rest.common.util.RestUtils.noContent;
import static org.kie.server.remote.rest.common.util.RestUtils.notFound;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_PROCESS_DEFS_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_PROCESS_INSTANCES_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_PROCESS_INSTANCE_NODES_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_PROCESS_INSTANCE_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_PROCESS_INSTANCE_SIGNALS_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_PROCESS_INSTANCE_VARS_LOG_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_PROCESS_INSTANCE_VARS_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_PROCESS_INSTANCE_VAR_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_PROCESS_INSTANCE_WORK_ITEMS_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_PROCESS_INSTANCE_WORK_ITEM_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.LONG_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.LONG_RESPONSE_XML;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.VAR_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.VAR_MAP_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.VAR_MAP_XML;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.VAR_XML;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.XML;
import static org.kie.server.remote.rest.jbpm.resources.Messages.CONTAINER_NOT_FOUND;
import static org.kie.server.remote.rest.jbpm.resources.Messages.CREATE_RESPONSE_ERROR;
import static org.kie.server.remote.rest.jbpm.resources.Messages.PROCESS_DEFINITION_NOT_FOUND;
import static org.kie.server.remote.rest.jbpm.resources.Messages.PROCESS_INSTANCE_NOT_FOUND;
import static org.kie.server.remote.rest.jbpm.resources.Messages.WORK_ITEM_NOT_FOUND;

@Api(value="Process instances")
@Path("server/" + PROCESS_URI)
public class ProcessResource  {

    public static final Logger logger = LoggerFactory.getLogger(ProcessResource.class);

    private ProcessServiceBase processServiceBase;
    private RuntimeDataServiceBase runtimeDataServiceBase;
    private KieServerRegistry context;

    public ProcessResource() {

    }

    public ProcessResource(ProcessServiceBase processServiceBase, RuntimeDataServiceBase runtimeDataServiceBase, KieServerRegistry context) {
        this.processServiceBase = processServiceBase;
        this.runtimeDataServiceBase = runtimeDataServiceBase;
        this.context = context;
    }

    protected static String getRelativePath(HttpServletRequest httpRequest) {
        String url =  httpRequest.getRequestURI();
        url = url.replaceAll( ".*/rest", "");
        return url;
    }

    @ApiOperation(value="Starts a new process instance of a specified process.")
    @ApiResponses(value = { 
            @ApiResponse(code = 201, response=Long.class, message = "Process instance started", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=LONG_RESPONSE_JSON),
                    @ExampleProperty(mediaType=XML, value=LONG_RESPONSE_XML)})),
            @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process ID or Container Id not found"),
            @ApiResponse(code = 403, message = "User does not have permission to access this asset")})
    @POST
    @Path(START_PROCESS_POST_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response startProcess(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id where the process definition resides", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "process id that new instance should be created from", required = true, example = "evaluation") @PathParam(PROCESS_ID) String processId, 
            @ApiParam(value = "optional map of process variables", required = false, examples=@Example(value= {
                                    @ExampleProperty(mediaType=JSON, value=VAR_MAP_JSON),
                                    @ExampleProperty(mediaType=XML, value=VAR_MAP_XML)})) @DefaultValue("") String payload) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        
        try {
            String response = processServiceBase.startProcess(containerId, processId, payload, type);

            logger.debug("Returning CREATED response with content '{}'", response);
            return createResponse(response, v, Response.Status.CREATED, conversationIdHeader);
        } catch (DeploymentNotActiveException e) {
            return badRequest(
                    e.getMessage(), v);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v);
        } catch (ProcessDefinitionNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_DEFINITION_NOT_FOUND, processId, containerId), v);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(
                    MessageFormat.format(CREATE_RESPONSE_ERROR, e.getMessage()), v);
        }
    }

    @ApiOperation(value = "Starts a new process instance from the specific nodes")
    @ApiResponses(value = {@ApiResponse(code = 201, response = Long.class, message = "Process instance created",
                                        examples = @Example(value = {@ExampleProperty(mediaType = JSON, value = LONG_RESPONSE_JSON),
                                                                     @ExampleProperty(mediaType = XML, value = LONG_RESPONSE_XML)})),
            @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process ID or Container Id not found"),
            @ApiResponse(code = 403, message = "User does not have permission to access this asset")})
    @POST
    @Path(START_PROCESS_FROM_NODES_POST_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response startProcessFromNodeIds(@javax.ws.rs.core.Context HttpHeaders headers,
            @ApiParam(value = "container id where the process definition resides", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "process instance id that new instance should be created from", required = true, example = "evaluation") @PathParam(PROCESS_ID) String processId, 
                                            @ApiParam(value = "optional map of process variables", required = false) @DefaultValue("") String payload) {

        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        
        try {
            String response = processServiceBase.startProcessFromNodeIds(containerId, processId, payload, type);

            logger.debug("Returning CREATED response with content '{}'", response);
            return createResponse(response, v, Response.Status.CREATED, conversationIdHeader);
        } catch (DeploymentNotActiveException e) {
            return badRequest(
                    e.getMessage(), v);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v);
        } catch (ProcessDefinitionNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_DEFINITION_NOT_FOUND, processId, containerId), v);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(
                    MessageFormat.format(CREATE_RESPONSE_ERROR, e.getMessage()), v);
        }
    }

    @ApiOperation(value = "Starts a new process instance from the specific nodes")
    @ApiResponses(value = {@ApiResponse(code = 201, response = Long.class, message = "Process instance created",
                                        examples = @Example(value = {@ExampleProperty(mediaType = JSON, value = LONG_RESPONSE_JSON),
                                                                     @ExampleProperty(mediaType = XML, value = LONG_RESPONSE_XML)})),
                           @ApiResponse(code = 500, message = "Unexpected error"),
                           @ApiResponse(code = 404, message = "Process ID or Container Id not found"),
                           @ApiResponse(code = 403, message = "User does not have permission to access this asset")})
    @POST
    @Path(START_PROCESS_FROM_NODES_WITH_CORRELATION_KEY_POST_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response startProcessWithCorrelationKeyFromNodeIds(@javax.ws.rs.core.Context HttpHeaders headers,
                                            @ApiParam(value = "container id where the process definition resides", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
                                            @ApiParam(value = "process instance id that new instance should be created from", required = true, example = "evaluation") @PathParam(PROCESS_ID) String processId,
                                                              @ApiParam(value = "correlation key that should be used for creating the process", required = true,
                                                                        example = "evaluation") @PathParam(CORRELATION_KEY) String correlationKey,
                                                              @ApiParam(value = "start process specifications", required = false) @DefaultValue("") String payload) {

        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);

        try {
            String response = processServiceBase.startProcessWithCorrelationKeyFromNodeIds(containerId, processId, correlationKey, payload, type);

            logger.debug("Returning CREATED response with content '{}'", response);
            return createResponse(response, v, Response.Status.CREATED, conversationIdHeader);
        } catch (DeploymentNotActiveException e) {
            return badRequest(
                              e.getMessage(), v);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                            MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v);
        } catch (ProcessDefinitionNotFoundException e) {
            return notFound(
                            MessageFormat.format(PROCESS_DEFINITION_NOT_FOUND, processId, containerId), v);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(
                                       MessageFormat.format(CREATE_RESPONSE_ERROR, e.getMessage()), v);
        }
    }

    @ApiOperation(value="Starts a new process instance of a specified process and assigns a new correlation key to the process instance.")
    @ApiResponses(value = { @ApiResponse(code = 201, response=Long.class, message = "Process instance started", examples=@Example(value= {
            @ExampleProperty(mediaType=JSON, value=LONG_RESPONSE_JSON),
            @ExampleProperty(mediaType=XML, value=LONG_RESPONSE_XML)})),
            @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process ID or Container Id not found"),
            @ApiResponse(code = 403, message = "User does not have permission to access this asset")})
    @POST
    @Path(START_PROCESS_WITH_CORRELATION_KEY_POST_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response startProcessWithCorrelation(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id where the process definition resides", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "process id that new instance should be created from", required = true, example = "evaluation") @PathParam(PROCESS_ID) String processId,
            @ApiParam(value = "correlation key to be assigned to process instance", required = true, example = "john-evaluation-2019") @PathParam("correlationKey") String correlationKey, 
            @ApiParam(value = "optional map of process variables", required = false, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=VAR_MAP_JSON),
                    @ExampleProperty(mediaType=XML, value=VAR_MAP_XML)}))  @DefaultValue("") String payload) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);

        try {
            String response = processServiceBase.startProcessWithCorrelation(containerId, processId, correlationKey, payload, type);

            logger.debug("Returning CREATED response with content '{}'", response);
            return createResponse(response, v, Response.Status.CREATED, conversationIdHeader);
        } catch (DeploymentNotActiveException e) {
            return badRequest(
                    e.getMessage(), v);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v);
        } catch (ProcessDefinitionNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_DEFINITION_NOT_FOUND, processId, containerId), v);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(
                    MessageFormat.format(CREATE_RESPONSE_ERROR, e.getMessage()), v);
        }
    }

    @ApiOperation(value = "Aborts a specified process instance in a specified KIE container.", response = Void.class, code = 204)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
                            @ApiResponse(code = 404, message = "Process instance or Container Id not found"),
                            @ApiResponse(code = 403, message = "User does not have permission to access this asset")})
    @DELETE
    @Path(ABORT_PROCESS_INST_DEL_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response abortProcessInstance(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of the process instance to be aborted", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            processServiceBase.abortProcessInstance(containerId, processInstanceId);
            // produce 204 NO_CONTENT response code
            return noContent(v, conversationIdHeader);
        } catch (ProcessInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }


    @ApiOperation(value = "Aborts multiple specified process instances in a specified KIE container.", response = Void.class, code = 204)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"),
            @ApiResponse(code = 403, message = "User does not have permission to access this asset")})
    @DELETE
    @Path(ABORT_PROCESS_INSTANCES_DEL_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response abortProcessInstances(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "list of identifiers of the process instances to be aborted", required = true) @QueryParam("instanceId") List<Long> processInstanceIds) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            processServiceBase.abortProcessInstances(containerId, processInstanceIds);
            // produce 204 NO_CONTENT response code
            return noContent(v, conversationIdHeader);
        } catch (ProcessInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceIds), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Signals a specified process instance with a specified signal name and optional signal data.",
            response=Void.class, code=200)
            @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"),
            @ApiResponse(code = 403, message = "User does not have permission to access this asset")})
    @POST
    @Path(SIGNAL_PROCESS_INST_POST_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response signalProcessInstance(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
            @ApiParam(value = "identifier of the process instance to be signaled", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId, 
            @ApiParam(value = "signal name to be send to process instance", required = true, example = "EventReceived") @PathParam(SIGNAL_NAME) String signalName, 
            @ApiParam(value = "optional event data - any type can be provided", required = false, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=VAR_JSON),
                    @ExampleProperty(mediaType=XML, value=VAR_XML)}))  String eventPayload) {

        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            processServiceBase.signalProcessInstance(containerId, processInstanceId, signalName, eventPayload, type);

            return createResponse(null, v, Response.Status.OK, conversationIdHeader);

        } catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Signals a specified process instance by correlation key with a specified signal name and optional signal data.",
            response=Void.class, code=200)
            @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"),
            @ApiResponse(code = 403, message = "User does not have permission to access this asset")})
    @POST
    @Path(SIGNAL_PROCESS_BY_CORRELATION_KEY_POST_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response signalProcessInstanceByCorrelationKey(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
            @ApiParam(value = "correlation key of the process instance to be signaled", required = true, example = "123") @PathParam(CORRELATION_KEY) String correlationKey, 
            @ApiParam(value = "signal name to be sent to process instance", required = true, example = "EventReceived") @PathParam(SIGNAL_NAME) String signalName, 
            @ApiParam(value = "optional event data - any type can be provided", required = false, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=VAR_JSON),
                    @ExampleProperty(mediaType=XML, value=VAR_XML)}))  String eventPayload) {

        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            processServiceBase.signalProcessInstanceByCorrelationKey(containerId, correlationKey, signalName, eventPayload, type);
            return createResponse(null, v, Response.Status.OK, conversationIdHeader);

        } catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, correlationKey), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Signals multiple process instances with a specified signal name.",
            response=Void.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"),
            @ApiResponse(code = 403, message = "User does not have permission to access this asset")})
    @POST
    @Path(SIGNAL_PROCESS_INSTANCES_PORT_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response signalProcessInstances(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
            @ApiParam(value = "list of identifiers of the process instances to be signaled", required = false) @QueryParam("instanceId") List<Long> processInstanceIds, 
            @ApiParam(value = "list of correlationKeys of the process instances to be signaled", required = false) @QueryParam("correlationKey") List<String> correlationKeys, 
            @ApiParam(value = "signal name to be send to process instance", required = true, example = "EventReceived") @PathParam(SIGNAL_NAME) String signalName, 
            @ApiParam(value = "optional event data - any type can be provided", required = false, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=VAR_JSON),
                    @ExampleProperty(mediaType=XML, value=VAR_XML)})) String eventPayload) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            if (processInstanceIds != null && !processInstanceIds.isEmpty()) {
                logger.debug("Signaling given process instances - {}", processInstanceIds);
                processServiceBase.signalProcessInstances(containerId, processInstanceIds, signalName, eventPayload, type);
            } else if ( correlationKeys != null && !correlationKeys.isEmpty()){
                logger.debug("Signaling given process instances by correlation key - {}", correlationKeys);
                processServiceBase.signalProcessInstancesByCorrelationKey(containerId, correlationKeys, signalName, eventPayload, type);
            } else {
                logger.debug("No process instances given, signal container..");
                processServiceBase.signal(containerId, signalName, eventPayload, type);
            }

            return createResponse("", v, Response.Status.OK, conversationIdHeader);

        } catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceIds), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns information about a specified process instance in a specified KIE container.",
            response=ProcessInstance.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"),
            @ApiResponse(code = 200, response = ProcessInstance.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_PROCESS_INSTANCE_RESPONSE_JSON)})) })
    @GET
    @Path(PROCESS_INSTANCE_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getProcessInstance(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
            @ApiParam(value = "identifier of the process instance to be fetched", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId, 
            @ApiParam(value = "indicates if process instance variables should be loaded or not", required = false) @QueryParam("withVars") boolean withVars) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            String response = processServiceBase.getProcessInstance(containerId, processInstanceId, withVars, type);

            logger.debug("Returning OK response with content '{}'", response);            
            return createResponse(response, v, Response.Status.OK, conversationIdHeader);

        } catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v);
        }
    }

    @ApiOperation(value="Creates or updates a variable for a specified process instance.",
            response=Void.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"),
            @ApiResponse(code = 403, message = "User does not have permission to access this asset")})
    @PUT
    @Path(PROCESS_INSTANCE_VAR_PUT_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response setProcessVariable(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
            @ApiParam(value = "identifier of the process instance to be updated", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId, 
            @ApiParam(value = "name of the variable to be set/updated", required = true, example = "name") @PathParam("varName") String varName, 
            @ApiParam(value = "variable data - any type can be provided", required = true, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=VAR_JSON),
                    @ExampleProperty(mediaType=XML, value=VAR_XML)})) String variablePayload) {

        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            processServiceBase.setProcessVariable(containerId, processInstanceId, varName, variablePayload, type);
            return createResponse("", v, Response.Status.CREATED, conversationIdHeader);

        } catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Updates the values of one or more variable for a specified process instance. The request is a map in which the key is the variable name and the value is the new variable value.",
            response=Void.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"),
            @ApiResponse(code = 403, message = "User does not have permission to access this asset")})
    @POST
    @Path(PROCESS_INSTANCE_VARS_POST_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response setProcessVariables(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
            @ApiParam(value = "identifier of the process instance to be updated", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId, 
            @ApiParam(value = "variable data give as map", required = true, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=VAR_MAP_JSON),
                    @ExampleProperty(mediaType=XML, value=VAR_MAP_XML)}))  String variablePayload) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            processServiceBase.setProcessVariables(containerId, processInstanceId, variablePayload, type);
            return createResponse("", v, Response.Status.CREATED, conversationIdHeader);

        } catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns the value of a specified variable in a specified process instance.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"),
            @ApiResponse(code = 403, message = "User does not have permission to access this asset"),
            @ApiResponse(code = 200, response = Object.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_PROCESS_INSTANCE_VAR_RESPONSE_JSON)}) )})
    @GET
    @Path(PROCESS_INSTANCE_VAR_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Object getProcessInstanceVariable(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
            @ApiParam(value = "identifier of the process instance that variable should be retrieved from", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId, 
            @ApiParam(value = "variable name to be retrieved", required = true, example = "person") @PathParam("varName") String varName) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            String response = processServiceBase.getProcessInstanceVariable(containerId, processInstanceId, varName, type);

            logger.debug("Returning OK response with content '{}'", response);
            return createResponse(response, v, Response.Status.OK, conversationIdHeader);

        } catch (ProcessInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Retrieves all variables for a specified process instance as a map in which the key is the variable name and the value is the variable value.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"), 
            @ApiResponse(code = 403, message = "User does not have permission to access this asset"),
            @ApiResponse(code = 200, response = Map.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_PROCESS_INSTANCE_VARS_RESPONSE_JSON)})) })
    @GET
    @Path(PROCESS_INSTANCE_VARS_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getProcessInstanceVariables(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
            @ApiParam(value = "identifier of the process instance that variables should be retrieved from", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            String response = processServiceBase.getProcessInstanceVariables(containerId, processInstanceId, type);

            logger.debug("Returning OK response with content '{}'", response);
            return createResponse(response, v, Response.Status.OK, conversationIdHeader);

        } catch (ProcessInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }

    }

    @ApiOperation(value="Returns all available signal names for a specified process instance.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"), 
            @ApiResponse(code = 403, message = "User does not have permission to access this asset"),
            @ApiResponse(code = 200, response= String.class, responseContainer= "List", message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_PROCESS_INSTANCE_SIGNALS_RESPONSE_JSON)})) })
    @GET
    @Path(PROCESS_INSTANCE_SIGNALS_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getAvailableSignals(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
            @ApiParam(value = "identifier of the process instance that signals should be collected for", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            String response = processServiceBase.getAvailableSignals(containerId, processInstanceId, type);

            logger.debug("Returning OK response with content '{}'", response);

            return createResponse(response, v, Response.Status.OK, conversationIdHeader);
        }  catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }

    }

    @ApiOperation(value="Completes a specified work item for a specified process instance.",
            response=Void.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance, Work Item or Container Id not found"), 
            @ApiResponse(code = 403, message = "User does not have permission to access this asset") })
    @PUT
    @Path(PROCESS_INSTANCE_WORK_ITEM_COMPLETE_PUT_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response completeWorkItem(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
            @ApiParam(value = "identifier of the process instance that work item belongs to", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId, 
            @ApiParam(value = "identifier of the work item to complete", required = true, example = "567") @PathParam("workItemId") Long workItemId, 
            @ApiParam(value = "optional outcome data give as map", required = false, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=VAR_MAP_JSON),
                    @ExampleProperty(mediaType=XML, value=VAR_MAP_XML)})) String resultPayload) {

        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            processServiceBase.completeWorkItem(containerId, processInstanceId, workItemId, resultPayload, type);

            return createResponse("", v, Response.Status.CREATED, conversationIdHeader);

        } catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }


    @ApiOperation(value="Aborts a specified work item for a specified process instance.",
            response=Void.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance, Work Item or Container Id not found"),
            @ApiResponse(code = 403, message = "User does not have permission to access this asset")})
    @PUT
    @Path(PROCESS_INSTANCE_WORK_ITEM_ABORT_PUT_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response abortWorkItem(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
            @ApiParam(value = "identifier of the process instance that work item belongs to", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId, 
            @ApiParam(value = "identifier of the work item to abort", required = true, example = "567") @PathParam("workItemId") Long workItemId) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            processServiceBase.abortWorkItem(containerId, processInstanceId, workItemId);

            return createResponse("", v, Response.Status.CREATED, conversationIdHeader);

        } catch (WorkItemNotFoundException e) {
            return notFound(MessageFormat.format(WORK_ITEM_NOT_FOUND, workItemId), v, conversationIdHeader);
        } catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns information about a specified work item for a specified process instance.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance, Work Item or Container Id not found"), 
            @ApiResponse(code = 403, message = "User does not have permission to access this asset"),
            @ApiResponse(code = 200, response = WorkItemInstance.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_PROCESS_INSTANCE_WORK_ITEM_RESPONSE_JSON)})) })
    @GET
    @Path(PROCESS_INSTANCE_WORK_ITEM_BY_ID_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getWorkItem(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
            @ApiParam(value = "identifier of the process instance that work item belongs to", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId, 
            @ApiParam(value = "identifier of the work item to retrieve", required = true, example = "567") @PathParam("workItemId") Long workItemId) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            String response = processServiceBase.getWorkItem(containerId, processInstanceId, workItemId, type);

            logger.debug("Returning OK response with content '{}'", response);
            return createResponse(response, v, Response.Status.OK, conversationIdHeader);
        }  catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        }  catch (WorkItemNotFoundException e) {
            return notFound(MessageFormat.format(WORK_ITEM_NOT_FOUND, workItemId), v, conversationIdHeader);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }

    }

    @ApiOperation(value="Returns all work items for a specified process instance.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance, Work Item or Container Id not found"), 
            @ApiResponse(code = 403, message = "User does not have permission to access this asset"),
            @ApiResponse(code = 200, response = WorkItemInstanceList.class,  message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_PROCESS_INSTANCE_WORK_ITEMS_RESPONSE_JSON)})) })
    @GET
    @Path(PROCESS_INSTANCE_WORK_ITEMS_BY_PROC_INST_ID_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getWorkItemByProcessInstance(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
            @ApiParam(value = "identifier of the process instance that work items belong to", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            String response = processServiceBase.getWorkItemByProcessInstance(containerId, processInstanceId, type);

            logger.debug("Returning OK response with content '{}'", response);
            return createResponse(response, v, Response.Status.OK, conversationIdHeader);
        }  catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (SecurityException e) {
            return forbidden(errorMessage(e, e.getMessage()), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns a list of process instances in a specified KIE container.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
                            @ApiResponse(code = 404, message = "Container Id not found"), 
                            @ApiResponse(code = 200, response = ProcessInstanceList.class, message = "Successful response", examples=@Example(value= {
                                    @ExampleProperty(mediaType=JSON, value=GET_PROCESS_INSTANCES_RESPONSE_JSON)}))})
    @GET
    @Path(PROCESS_INSTANCES_BY_CONTAINER_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getProcessInstancesByDeploymentId(@Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "optional process instance status (active, completed, aborted) - defaults ot active (1) only", required = false, allowableValues="1,2,3") @QueryParam("status") List<Integer> status,
            @ApiParam(value = "optional pagination - at which page to start, defaults to 0 (meaning first)", required = false) @QueryParam("page") @DefaultValue("0") Integer page, 
            @ApiParam(value = "optional pagination - size of the result, defaults to 10", required = false) @QueryParam("pageSize") @DefaultValue("10") Integer pageSize,
            @ApiParam(value = "optional sort column, no default", required = false) @QueryParam("sort") String sort, 
            @ApiParam(value = "optional sort direction (asc, desc) - defaults to asc", required = false) @QueryParam("sortOrder") @DefaultValue("true") boolean sortOrder) {

        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            ProcessInstanceList processInstanceList = runtimeDataServiceBase.getProcessInstancesByDeploymentId(containerId, status, page, pageSize, sort, sortOrder);
            logger.debug("Returning result of process instance search: {}", processInstanceList);
    
            return createCorrectVariant(processInstanceList, headers, Response.Status.OK, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns a list of process definitions in a specified KIE container.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
                            @ApiResponse(code = 404, message = "Container Id not found"), 
                            @ApiResponse(code = 200, response = ProcessDefinitionList.class, message = "Successful response", examples=@Example(value= {
                                    @ExampleProperty(mediaType=JSON, value=GET_PROCESS_DEFS_RESPONSE_JSON)}))})
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getProcessesByDeploymentId(@Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
            @ApiParam(value = "optional pagination - at which page to start, defaults to 0 (meaning first)", required = false) @QueryParam("page") @DefaultValue("0") Integer page, 
            @ApiParam(value = "optional pagination - size of the result, defaults to 10", required = false) @QueryParam("pageSize") @DefaultValue("10") Integer pageSize,
            @ApiParam(value = "optional sort column, no default", required = false) @QueryParam("sort") String sort, 
            @ApiParam(value = "optional sort direction (asc, desc) - defaults to asc", required = false) @QueryParam("sortOrder") @DefaultValue("true") boolean sortOrder) {

        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            ProcessDefinitionList processDefinitionList = runtimeDataServiceBase.getProcessesByDeploymentIdCheckContainer(containerId, page, pageSize, sort, sortOrder);
            logger.debug("Returning result of process definition search: {}", processDefinitionList);
    
            return createCorrectVariant(processDefinitionList, headers, Response.Status.OK, conversationIdHeader);
            
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns node instances for the specified process instance.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
                            @ApiResponse(code = 404, message = "Process Instance or Container Id not found"), 
                            @ApiResponse(code = 200, response = NodeInstanceList.class, message = "Successful response", examples=@Example(value= {
                                    @ExampleProperty(mediaType=JSON, value=GET_PROCESS_INSTANCE_NODES_RESPONSE_JSON)}))})
    @GET
    @Path(PROCESS_INSTANCES_NODE_INSTANCES_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getProcessInstanceHistory(@Context HttpHeaders headers, @PathParam(CONTAINER_ID) 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") String containerId, 
            @ApiParam(value = "identifier of the process instance that history should be collected for", required = true, example = "123") @PathParam(PROCESS_INST_ID) long processInstanceId,
            @ApiParam(value = "instructs if active nodes only should be collected, defaults to false", required = false) @QueryParam("activeOnly")Boolean active, 
            @ApiParam(value = "instructs if completed nodes only should be collected, defaults to false", required = false) @QueryParam("completedOnly")Boolean completed,
                                              @ApiParam(value = "entry type from the history", required = false,
                                                        example = "123") @QueryParam(PROCESS_INST_HISTORY_TYPE) String processInstHistoryType,
            @ApiParam(value = "optional pagination - at which page to start, defaults to 0 (meaning first)", required = false) @QueryParam("page") @DefaultValue("0") Integer page, 
            @ApiParam(value = "optional pagination - size of the result, defaults to 10", required = false) @QueryParam("pageSize") @DefaultValue("10") Integer pageSize) {
        
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            NodeInstanceList nodeInstanceList = null;
            if (processInstHistoryType == null) {
                nodeInstanceList = runtimeDataServiceBase.getProcessInstanceHistory(containerId, processInstanceId, active, completed, page, pageSize);
            } else {
                nodeInstanceList = runtimeDataServiceBase.getProcessInstanceFullHistoryByType(containerId, processInstanceId, processInstHistoryType, page, pageSize);
            }
            logger.debug("Returning result of node instances search: {}", nodeInstanceList);
            return createCorrectVariant(nodeInstanceList, headers, Response.Status.OK, conversationIdHeader);
        }  catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns the current variable values of a specified process instance in a specified KIE container.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
                            @ApiResponse(code = 404, message = "Process Instance or Container Id not found"), 
                            @ApiResponse(code = 200, response = VariableInstanceList.class, message = "Successful response", examples=@Example(value= {
                                    @ExampleProperty(mediaType=JSON, value=GET_PROCESS_INSTANCE_VARS_LOG_RESPONSE_JSON)}))})
    @GET
    @Path(PROCESS_INSTANCE_VAR_INSTANCES_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getVariablesCurrentState(@Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of the process instance that variables state should be collected for", required = true, example = "123") @PathParam(PROCESS_INST_ID) long processInstanceId) {

        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            VariableInstanceList variableInstanceList = runtimeDataServiceBase.getVariablesCurrentState(containerId, processInstanceId);
            logger.debug("Returning result of variables search: {}", variableInstanceList);
    
            return createCorrectVariant(variableInstanceList, headers, Response.Status.OK, conversationIdHeader);
        }  catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns the history of a specified variable in a specified process instance.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
                            @ApiResponse(code = 404, message = "Process Instance or Container Id not found"), 
                            @ApiResponse(code = 200, response = VariableInstanceList.class, message = "Successful response", examples=@Example(value= {
                                    @ExampleProperty(mediaType=JSON, value=GET_PROCESS_INSTANCE_VARS_LOG_RESPONSE_JSON)}))})
    @GET
    @Path(PROCESS_INSTANCE_VAR_INSTANCE_BY_VAR_NAME_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getVariableHistory(@Context HttpHeaders headers,  
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of the process instance that variable history should be collected for", required = true, example = "123") @PathParam(PROCESS_INST_ID) long processInstanceId,
            @ApiParam(value = "name of the variables that history should be collected for", required = true, example = "person") @PathParam("varName") String variableName,
            @ApiParam(value = "optional pagination - at which page to start, defaults to 0 (meaning first)", required = false) @QueryParam("page") @DefaultValue("0") Integer page, 
            @ApiParam(value = "optional pagination - size of the result, defaults to 10", required = false) @QueryParam("pageSize") @DefaultValue("10") Integer pageSize) {

        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        Variant v = getVariant(headers);
        try {
            VariableInstanceList variableInstanceList = runtimeDataServiceBase.getVariableHistory(containerId, processInstanceId, variableName, page, pageSize);
            logger.debug("Returning result of variable '{}; history search: {}", variableName, variableInstanceList);
    
            return createCorrectVariant(variableInstanceList, headers, Response.Status.OK, conversationIdHeader);
        }  catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns a list of process instances for which a specified process instance is a parent process instance")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"), 
            @ApiResponse(code = 200, response = ProcessInstanceList.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_PROCESS_INSTANCES_RESPONSE_JSON)}))})
    @GET
    @Path(PROCESS_INSTANCES_BY_PARENT_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getProcessInstances(@Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of the parent process instance that process instances should be collected for", required = true, example = "123") @PathParam(PROCESS_INST_ID) long parentProcessInstanceId,
            @ApiParam(value = "optional process instance status (active, completed, aborted) - defaults ot active (1) only", required = false, allowableValues="1,2,3") @QueryParam("status") List<Integer> status,
            @ApiParam(value = "optional pagination - at which page to start, defaults to 0 (meaning first)", required = false) @QueryParam("page") @DefaultValue("0") Integer page, 
            @ApiParam(value = "optional pagination - size of the result, defaults to 10", required = false) @QueryParam("pageSize") @DefaultValue("10") Integer pageSize,
            @ApiParam(value = "optional sort column, no default", required = false) @QueryParam("sort") String sort, 
            @ApiParam(value = "optional sort direction (asc, desc) - defaults to asc", required = false) @QueryParam("sortOrder") @DefaultValue("true") boolean sortOrder) {

        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        ProcessInstanceList processInstanceList = processServiceBase.getProcessInstancesByParent(containerId, parentProcessInstanceId, status, page, pageSize, sort, sortOrder);
        logger.debug("Returning result of process instance search: {}", processInstanceList);

        return createCorrectVariant(processInstanceList, headers, Response.Status.OK, conversationIdHeader);
    }

}
