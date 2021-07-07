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

package org.kie.server.remote.rest.jbpm.admin;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

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
import org.jbpm.services.api.DeploymentNotFoundException;
import org.jbpm.services.api.IncorrectDeploymentIDException;
import org.jbpm.services.api.NodeInstanceNotFoundException;
import org.jbpm.services.api.NodeNotFoundException;
import org.jbpm.services.api.ProcessInstanceNotFoundException;
import org.jbpm.services.api.admin.ExecutionErrorNotFoundException;
import org.kie.server.api.model.admin.ExecutionErrorInstance;
import org.kie.server.api.model.admin.ExecutionErrorInstanceList;
import org.kie.server.api.model.admin.MigrationReportInstance;
import org.kie.server.api.model.admin.MigrationReportInstanceList;
import org.kie.server.api.model.admin.ProcessNodeList;
import org.kie.server.api.model.admin.TimerInstanceList;
import org.kie.server.api.model.instance.NodeInstanceList;
import org.kie.server.remote.rest.common.Header;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.jbpm.admin.ProcessAdminServiceBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.server.api.rest.RestURI.ACK_ERRORS_PUT_URI;
import static org.kie.server.api.rest.RestURI.ACK_ERROR_PUT_URI;
import static org.kie.server.api.rest.RestURI.ADMIN_PROCESS_URI;
import static org.kie.server.api.rest.RestURI.CANCEL_NODE_INST_PROCESS_INST_DELETE_URI;
import static org.kie.server.api.rest.RestURI.CONTAINER_ID;
import static org.kie.server.api.rest.RestURI.ERRORS_BY_PROCESS_INST_GET_URI;
import static org.kie.server.api.rest.RestURI.ERRORS_GET_URI;
import static org.kie.server.api.rest.RestURI.ERROR_GET_URI;
import static org.kie.server.api.rest.RestURI.MIGRATE_PROCESS_INSTANCES_PUT_URI;
import static org.kie.server.api.rest.RestURI.MIGRATE_PROCESS_INST_PUT_URI;
import static org.kie.server.api.rest.RestURI.MIGRATE_PROCESS_SUBPROCESS_INST_PUT_URI;
import static org.kie.server.api.rest.RestURI.NODES_PROCESS_INST_GET_URI;
import static org.kie.server.api.rest.RestURI.NODE_INSTANCES_PROCESS_INST_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_INST_ID;
import static org.kie.server.api.rest.RestURI.RETRIGGER_NODE_INST_PROCESS_INST_PUT_URI;
import static org.kie.server.api.rest.RestURI.TIMERS_PROCESS_INST_GET_URI;
import static org.kie.server.api.rest.RestURI.TRIGGER_NODE_PROCESS_INST_POST_URI;
import static org.kie.server.api.rest.RestURI.UPDATE_TIMER_PROCESS_INST_PUT_URI;
import static org.kie.server.remote.rest.common.util.RestUtils.badRequest;
import static org.kie.server.remote.rest.common.util.RestUtils.buildConversationIdHeader;
import static org.kie.server.remote.rest.common.util.RestUtils.createCorrectVariant;
import static org.kie.server.remote.rest.common.util.RestUtils.createResponse;
import static org.kie.server.remote.rest.common.util.RestUtils.errorMessage;
import static org.kie.server.remote.rest.common.util.RestUtils.getContentType;
import static org.kie.server.remote.rest.common.util.RestUtils.getVariant;
import static org.kie.server.remote.rest.common.util.RestUtils.internalServerError;
import static org.kie.server.remote.rest.common.util.RestUtils.noContent;
import static org.kie.server.remote.rest.common.util.RestUtils.notFound;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_EXEC_ERRORS_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_EXEC_ERROR_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_MIGRATION_REPORTS_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_MIGRATION_REPORT_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_PROCESS_INSTANCE_NODES_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_PROCESS_NODES_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_TIMERS_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.MIGRATION_VAR_MAP_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.MIGRATION_VAR_MAP_XML;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.SIMPLE_VAR_MAP_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.SIMPLE_VAR_MAP_XML;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.TIMER_VAR_MAP_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.TIMER_VAR_MAP_XML;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.XML;
import static org.kie.server.remote.rest.jbpm.resources.Messages.CONTAINER_NOT_FOUND;
import static org.kie.server.remote.rest.jbpm.resources.Messages.INCORRECT_CONTAINER_ID;
import static org.kie.server.remote.rest.jbpm.resources.Messages.NODE_INSTANCE_NOT_FOUND;
import static org.kie.server.remote.rest.jbpm.resources.Messages.NODE_NOT_FOUND;
import static org.kie.server.remote.rest.jbpm.resources.Messages.PROCESS_INSTANCE_NOT_FOUND;
import static org.kie.server.remote.rest.jbpm.resources.Messages.TIMER_INSTANCE_NOT_FOUND;

@Api(value="Process instance administration")
@Path("server/" + ADMIN_PROCESS_URI)
public class ProcessAdminResource {

    private static final Logger logger = LoggerFactory.getLogger(ProcessAdminResource.class);

    private ProcessAdminServiceBase processAdminServiceBase;
    private KieServerRegistry context;


    public ProcessAdminResource() {

    }

    public ProcessAdminResource(ProcessAdminServiceBase processAdminServiceBase, KieServerRegistry context) {
        this.processAdminServiceBase = processAdminServiceBase;
        this.context = context;
    }

    @ApiOperation(value="Migrates a specified process instance to a process definition in another KIE container.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"),
            @ApiResponse(code = 404, message = "Container Id not found"), 
            @ApiResponse(code = 201, response = MigrationReportInstance.class, message = "Successfull response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_MIGRATION_REPORT_RESPONSE_JSON)})) })
    @PUT
    @Path(MIGRATE_PROCESS_INST_PUT_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response migrateProcessInstance(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of process instance to be migrated", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId,
            @ApiParam(value = "container id that new process definition belongs to", required = true) @QueryParam("targetContainerId") String targetContainerId, 
            @ApiParam(value = "process definition that process instance should be migrated to", required = true) @QueryParam("targetProcessId") String targetProcessId, 
            @ApiParam(value = "node mapping - unique ids of old definition to new definition given as Map", required = false, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=SIMPLE_VAR_MAP_JSON),
                    @ExampleProperty(mediaType=XML, value=SIMPLE_VAR_MAP_XML)})) String payload) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            MigrationReportInstance reportInstance = processAdminServiceBase.migrateProcessInstance(containerId, processInstanceId, targetContainerId, targetProcessId, payload, type);

            return createCorrectVariant(reportInstance, headers, Response.Status.CREATED, conversationIdHeader);
        } catch (ProcessInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Migrates a specified process instance with all subprocesses to process definition in another KIE container.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"),
            @ApiResponse(code = 404, message = "Container Id not found"), 
            @ApiResponse(code = 201, response = MigrationReportInstanceList.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_MIGRATION_REPORTS_RESPONSE_JSON)})) })
    @PUT
    @Path(MIGRATE_PROCESS_SUBPROCESS_INST_PUT_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response migrateProcessInstanceWithSubprocess(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instances belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "list of identifiers of process instance to be migrated", required = true) @PathParam(PROCESS_INST_ID) Long processInstanceId,
            @ApiParam(value = "container id that new process definition belongs to", required = true) @QueryParam("targetContainerId") String targetContainerId, 
            @ApiParam(value = "process definition that process instances should be migrated to", required = true) @QueryParam("targetProcessId") String targetProcessId, 
            @ApiParam(value = "migration specifcation. It contains process mapping and node mapping - unique ids of old definition to new definition given as Map", required = false, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=MIGRATION_VAR_MAP_JSON),
                    @ExampleProperty(mediaType=XML, value=MIGRATION_VAR_MAP_XML)})) String payload) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            MigrationReportInstanceList reportInstances = processAdminServiceBase.migrateProcessInstanceWithAllSubprocess(containerId, processInstanceId, targetContainerId, payload, type);

            return createCorrectVariant(reportInstances, headers, Response.Status.CREATED, conversationIdHeader);
        } catch (ProcessInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }


    @ApiOperation(value="Migrates multiple process instances to process definition in another KIE container.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"),
            @ApiResponse(code = 404, message = "Container Id not found"), 
            @ApiResponse(code = 201, response = MigrationReportInstanceList.class, message = "Successfull response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_MIGRATION_REPORTS_RESPONSE_JSON)})) })
    @PUT
    @Path(MIGRATE_PROCESS_INSTANCES_PUT_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response migrateProcessInstances(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instances belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "list of identifiers of process instance to be migrated", required = true) @QueryParam(PROCESS_INST_ID) List<Long> processInstanceIds,
            @ApiParam(value = "container id that new process definition belongs to", required = true) @QueryParam("targetContainerId") String targetContainerId, 
            @ApiParam(value = "process definition that process instances should be migrated to", required = true) @QueryParam("targetProcessId") String targetProcessId, 
            @ApiParam(value = "node mapping - unique ids of old definition to new definition given as Map", required = false, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=SIMPLE_VAR_MAP_JSON),
                    @ExampleProperty(mediaType=XML, value=SIMPLE_VAR_MAP_XML)})) String payload) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            MigrationReportInstanceList reportInstances = processAdminServiceBase.migrateProcessInstances(containerId, processInstanceIds, targetContainerId, targetProcessId, payload, type);

            return createCorrectVariant(reportInstances, headers, Response.Status.CREATED, conversationIdHeader);
        } catch (ProcessInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceIds), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }



    @ApiOperation(value="Aborts a specified node instance within a specified process instance.",
            response=Void.class, code=204)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance, node instance or Container Id not found") })
    @DELETE
    @Path(CANCEL_NODE_INST_PROCESS_INST_DELETE_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response cancelNodeInstance(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of process instance that node instance belongs to", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId,
            @ApiParam(value = "identifier of node instance that should be canceled", required = true, example = "567") @PathParam("nodeInstanceId") Long nodeInstanceId) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            processAdminServiceBase.cancelNodeInstance(containerId, processInstanceId, nodeInstanceId);

            return noContent(v, conversationIdHeader);
        } catch (NodeInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(NODE_INSTANCE_NOT_FOUND, nodeInstanceId, processInstanceId), v, conversationIdHeader);
        } catch (ProcessInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (IncorrectDeploymentIDException e) {
            return badRequest(
                    MessageFormat.format(INCORRECT_CONTAINER_ID, processInstanceId, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Re-triggers a specified node instance for a specified process instance. If the node is not active in the process instance, it becomes active upon re-triggering.",
            response=Void.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance, node instance or Container Id not found") })
    @PUT
    @Path(RETRIGGER_NODE_INST_PROCESS_INST_PUT_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response retriggerNodeInstance(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of process instance that node instance belongs to", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId,
            @ApiParam(value = "identifier of node instance that should be retriggered", required = true, example = "567") @PathParam("nodeInstanceId") Long nodeInstanceId) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            processAdminServiceBase.retriggerNodeInstance(containerId, processInstanceId, nodeInstanceId);

            return createResponse("", v, Response.Status.CREATED, conversationIdHeader);
        } catch (NodeInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(NODE_INSTANCE_NOT_FOUND, nodeInstanceId, processInstanceId), v, conversationIdHeader);
        } catch (ProcessInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (IncorrectDeploymentIDException e) {
            return badRequest(
                    MessageFormat.format(INCORRECT_CONTAINER_ID, processInstanceId, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Updates a specified timer for a specified process instance.",
            response=Void.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance, node instance or Container Id not found") })
    @PUT
    @Path(UPDATE_TIMER_PROCESS_INST_PUT_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response updateTimer(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of process instance that timer belongs to", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId,
            @ApiParam(value = "identifier of timer instance to be updated", required = true, example = "99") @PathParam("timerId") Long timerId, 
            @ApiParam(value = "optional flag that indicates if the time expression is relative to the current date or not, defaults to true", required = false) @QueryParam("relative") @DefaultValue("true") boolean relative, 
            @ApiParam(value = "Map of timer expressions - deplay, perios and repeat are allowed values in the map", required = true, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=TIMER_VAR_MAP_JSON),
                    @ExampleProperty(mediaType=XML, value=TIMER_VAR_MAP_XML)})) String payload) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            processAdminServiceBase.updateTimer(containerId, processInstanceId, timerId, relative, payload, type);

            return createResponse("", v, Response.Status.CREATED, conversationIdHeader);
        } catch (NodeInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(TIMER_INSTANCE_NOT_FOUND, timerId, processInstanceId), v, conversationIdHeader);
        } catch (ProcessInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (IncorrectDeploymentIDException e) {
            return badRequest(
                    MessageFormat.format(INCORRECT_CONTAINER_ID, processInstanceId, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Triggers a specified node for a specified process instance. If the node is not active in the process instance, it becomes active upon triggering.",
            response=Void.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance, node instance or Container Id not found") })
    @POST
    @Path(TRIGGER_NODE_PROCESS_INST_POST_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response triggerNode(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of process instance where node should be triggered", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId,
            @ApiParam(value = "identifier of the node to be triggered", required = true, example = "567") @PathParam("nodeId") Long nodeId) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            processAdminServiceBase.triggerNode(containerId, processInstanceId, nodeId);

            return createResponse("", v, Response.Status.CREATED, conversationIdHeader);
        } catch (NodeNotFoundException e) {
            return notFound(
                    MessageFormat.format(NODE_NOT_FOUND, nodeId, processInstanceId), v, conversationIdHeader);
        } catch (ProcessInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (IncorrectDeploymentIDException e) {
            return badRequest(
                    MessageFormat.format(INCORRECT_CONTAINER_ID, processInstanceId, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns all timers for a specified process instance.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"),
            @ApiResponse(code = 404, message = "Container Id not found"), 
            @ApiResponse(code = 200, response = TimerInstanceList.class, message = "Successfull response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_TIMERS_RESPONSE_JSON)})) })
    @GET
    @Path(TIMERS_PROCESS_INST_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getTimerInstances(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of process instance that timer instances should be collected for", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            TimerInstanceList timerInstanceList = processAdminServiceBase.getTimerInstances(containerId, processInstanceId);

            return createCorrectVariant(timerInstanceList, headers, Response.Status.OK, conversationIdHeader);
        } catch (ProcessInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (IncorrectDeploymentIDException e) {
            return badRequest(
                    MessageFormat.format(INCORRECT_CONTAINER_ID, processInstanceId, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns all the active node instances in a specified process instance.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"), 
            @ApiResponse(code = 200, response = NodeInstanceList.class , message = "Successfull response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_PROCESS_INSTANCE_NODES_RESPONSE_JSON)})) })
    @GET
    @Path(NODE_INSTANCES_PROCESS_INST_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getActiveNodeInstances(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of process instance that active nodes instances should be collected for", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            NodeInstanceList nodeInstanceList = processAdminServiceBase.getActiveNodeInstances(containerId, processInstanceId);

            return createCorrectVariant(nodeInstanceList, headers, Response.Status.OK, conversationIdHeader);
        } catch (ProcessInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (IncorrectDeploymentIDException e) {
            return badRequest(
                    MessageFormat.format(INCORRECT_CONTAINER_ID, processInstanceId, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns all nodes in a specified process instance.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"), 
            @ApiResponse(code = 200, response = ProcessNodeList.class, message = "Successfull response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_PROCESS_NODES_RESPONSE_JSON)})) })
    @GET
    @Path(NODES_PROCESS_INST_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getNodes(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of process instance that process nodes should be collected from", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            ProcessNodeList processNodeList = processAdminServiceBase.getProcessNodes(containerId, processInstanceId);

            return createCorrectVariant(processNodeList, headers, Response.Status.OK, conversationIdHeader);
        } catch (ProcessInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (IncorrectDeploymentIDException e) {
            return badRequest(
                    MessageFormat.format(INCORRECT_CONTAINER_ID, processInstanceId, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Acknowledges a specified process execution error (sets acknowledged to true for the error).",
            response=Void.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Execution error or Container Id not found") })
    @PUT
    @Path(ACK_ERROR_PUT_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response acknowledgeError(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that error belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of error to be acknowledged", required = true, example = "xxx-yyy-zzz") @PathParam("errorId") String errorId) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        if(conversationIdHeader == null) {
        	return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v);
        }
        try {

            processAdminServiceBase.acknowledgeError(containerId, Arrays.asList(errorId));
            return createCorrectVariant("", headers, Response.Status.CREATED, conversationIdHeader);
        } catch (ExecutionErrorNotFoundException e) {
            return notFound(e.getMessage(), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Acknowledges multiple process execution errors (sets acknowledged to true for the errors).",
            response=Void.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Execution error or Container Id not found") })
    @PUT
    @Path(ACK_ERRORS_PUT_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response acknowledgeErrors(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that errors belong to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "list of error identifiers to be acknowledged", required = true, example = "xxx-yyy-zzz") @QueryParam("errorId") List<String> errorIds) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        if(conversationIdHeader == null) {
        	return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v);
        }
        try {

            processAdminServiceBase.acknowledgeError(containerId, errorIds);
            return createCorrectVariant("", headers, Response.Status.CREATED, conversationIdHeader);
        } catch (ExecutionErrorNotFoundException e) {
            return notFound(e.getMessage(), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns information about a specified process execution error.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"), 
            @ApiResponse(code = 200, response = ExecutionErrorInstance.class, message = "Successfull response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_EXEC_ERROR_RESPONSE_JSON)})) })
    @GET
    @Path(ERROR_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getExecutionErrorById(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process error belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of error to be loaded", required = true, example = "xxx-yyy-zzz") @PathParam("errorId") String errorId) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        if(conversationIdHeader == null) {
        	return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v);
        }
        try {
            ExecutionErrorInstance executionErrorInstance = processAdminServiceBase.getError(containerId, errorId);

            return createCorrectVariant(executionErrorInstance, headers, Response.Status.OK, conversationIdHeader);
        } catch (ExecutionErrorNotFoundException e) {
            return notFound(e.getMessage(), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns all process execution errors for a specified process instance.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Container Id not found"), 
            @ApiResponse(code = 200, response = ExecutionErrorInstanceList.class, message = "Successfull response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_EXEC_ERRORS_RESPONSE_JSON)})) })
    @GET
    @Path(ERRORS_BY_PROCESS_INST_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getExecutionErrorsByProcessInstance(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of process instance that errors should be collected for", required = true, example = "123") @PathParam(PROCESS_INST_ID) Long processInstanceId,
            @ApiParam(value = "optional flag that indicates if acknowledged errors should also be collected, defaults to false", required = false) @QueryParam("includeAck") @DefaultValue("false") boolean includeAcknowledged, 
            @ApiParam(value = "optional name of the node in the process instance to filter by", required = false) @QueryParam("node") String nodeName,
            @ApiParam(value = "optional pagination - at which page to start, defaults to 0 (meaning first)", required = false) @QueryParam("page") @DefaultValue("0") Integer page, 
            @ApiParam(value = "optional pagination - size of the result, defaults to 10", required = false) @QueryParam("pageSize") @DefaultValue("10") Integer pageSize,
            @ApiParam(value = "optional sort column, no default", required = false) @QueryParam("sort") String sort, 
            @ApiParam(value = "optional sort direction (asc, desc) - defaults to asc", required = false) @QueryParam("sortOrder") @DefaultValue("true") boolean sortOrder) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            ExecutionErrorInstanceList executionErrorInstanceList = processAdminServiceBase.getExecutionErrorsByProcessInstance(containerId, processInstanceId, nodeName,
                    includeAcknowledged, page, pageSize, sort, sortOrder);

            return createCorrectVariant(executionErrorInstanceList, headers, Response.Status.OK, conversationIdHeader);
        } catch (ExecutionErrorNotFoundException e) {
            return notFound(e.getMessage(), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (IncorrectDeploymentIDException e) {
            return badRequest(
                    MessageFormat.format(INCORRECT_CONTAINER_ID, processInstanceId, containerId), v, conversationIdHeader);
        } catch (ProcessInstanceNotFoundException e) {
            return notFound(
                    MessageFormat.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns all process execution errors for a specified KIE container.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance or Container Id not found"), 
            @ApiResponse(code = 200, response = ExecutionErrorInstanceList.class, message = "Successfull response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_EXEC_ERRORS_RESPONSE_JSON)})) })
    @GET
    @Path(ERRORS_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getExecutionErrors(@javax.ws.rs.core.Context HttpHeaders headers,
            @ApiParam(value = "container id that errors belong to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
            @ApiParam(value = "optional flag that indicates if acknowledged errors should also be collected, defaults to false", required = false) @QueryParam("includeAck") @DefaultValue("false") boolean includeAcknowledged,
            @ApiParam(value = "optional pagination - at which page to start, defaults to 0 (meaning first)", required = false) @QueryParam("page") @DefaultValue("0") Integer page, 
            @ApiParam(value = "optional pagination - size of the result, defaults to 10", required = false) @QueryParam("pageSize") @DefaultValue("10") Integer pageSize,
            @ApiParam(value = "optional sort column, no default", required = false) @QueryParam("sort") String sort, 
            @ApiParam(value = "optional sort direction (asc, desc) - defaults to asc", required = false) @QueryParam("sortOrder") @DefaultValue("true") boolean sortOrder) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        if(conversationIdHeader == null) {
        	return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v);
        }
        try {
            ExecutionErrorInstanceList executionErrorInstanceList = processAdminServiceBase.getExecutionErrors(containerId, includeAcknowledged, page, pageSize, sort, sortOrder);

            return createCorrectVariant(executionErrorInstanceList, headers, Response.Status.OK, conversationIdHeader);
        } catch (ExecutionErrorNotFoundException e) {
            return notFound(e.getMessage(), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(
                    MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }
}
