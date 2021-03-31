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
import org.jbpm.services.api.ProcessInstanceNotFoundException;
import org.jbpm.services.api.TaskNotFoundException;
import org.jbpm.services.api.admin.ExecutionErrorNotFoundException;
import org.kie.server.api.model.admin.ExecutionErrorInstance;
import org.kie.server.api.model.admin.ExecutionErrorInstanceList;
import org.kie.server.api.model.admin.TaskNotificationList;
import org.kie.server.api.model.admin.TaskReassignmentList;
import org.kie.server.remote.rest.common.Header;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.jbpm.admin.UserTaskAdminServiceBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.server.api.rest.RestURI.ACK_ERRORS_PUT_URI;
import static org.kie.server.api.rest.RestURI.ACK_ERROR_PUT_URI;
import static org.kie.server.api.rest.RestURI.ADMIN_TASK_URI;
import static org.kie.server.api.rest.RestURI.CONTAINER_ID;
import static org.kie.server.api.rest.RestURI.ERRORS_BY_TASK_ID_GET_URI;
import static org.kie.server.api.rest.RestURI.ERRORS_GET_URI;
import static org.kie.server.api.rest.RestURI.ERROR_GET_URI;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_ADMINS_GROUPS_DELETE_URI;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_ADMINS_USERS_DELETE_URI;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_ADMINS_USERS_URI;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_EXL_OWNERS_GROUPS_DELETE_URI;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_EXL_OWNERS_USERS_DELETE_URI;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_EXL_OWNERS_USERS_URI;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_ID;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_INPUTS_URI;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_NOTIFICATIONS_URI;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_NOTIFICATION_DELETE_URI;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_OUTPUTS_URI;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_POT_OWNERS_GROUPS_DELETE_URI;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_POT_OWNERS_USERS_DELETE_URI;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_POT_OWNERS_USERS_URI;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_REASSIGNMENTS_URI;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_REASSIGNMENT_DELETE_URI;
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
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.EMAIL_NOTIFICATION_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.EMAIL_NOTIFICATION_XML;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_EXEC_ERRORS_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_EXEC_ERROR_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_TASK_NOTIFICATIONS_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.GET_TASK_REASSIGNMENTS_RESPONSE_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.INTEGER_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.ORG_ENTITIES_LIST_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.ORG_ENTITIES_LIST_XML;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.VAR_MAP_JSON;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.VAR_MAP_XML;
import static org.kie.server.remote.rest.jbpm.docs.ParameterSamples.XML;
import static org.kie.server.remote.rest.jbpm.resources.Messages.CONTAINER_NOT_FOUND;
import static org.kie.server.remote.rest.jbpm.resources.Messages.TASK_INSTANCE_NOT_FOUND;

@Api(value="Task instance administration")
@Path("server/" + ADMIN_TASK_URI)
public class UserTaskAdminResource {

    private static final Logger logger = LoggerFactory.getLogger(ProcessAdminResource.class);

    private UserTaskAdminServiceBase userTaskAdminServiceBase;
    private KieServerRegistry context;

    private static final int POT_OWNER = 1;
    private static final int EXL_OWNER = 2;
    private static final int ADMIN = 3;

    public UserTaskAdminResource() {

    }

    public UserTaskAdminResource(UserTaskAdminServiceBase userTaskAdminServiceBase, KieServerRegistry context) {
        this.userTaskAdminServiceBase = userTaskAdminServiceBase;
        this.context = context;
    }

    @ApiOperation(value="Adds users and groups as potential owners to a specified task instance.",
            response=Void.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found") })
    @PUT
    @Path(TASK_INSTANCE_POT_OWNERS_USERS_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response addPotentialOwners(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "optional user id to be used instead of authenticated user - only when bypass authenticated user is enabled", required = false) @QueryParam("user") String userId,
            @ApiParam(value = "optional flag that indicates if existing potential owners should be removed, defaults to false", required = false) @QueryParam("remove") @DefaultValue("false") boolean removeExisting, 
            @ApiParam(value = "list of users/groups to be added as potential owners, as OrgEntities type", required = true, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=ORG_ENTITIES_LIST_JSON),
                    @ExampleProperty(mediaType=XML, value=ORG_ENTITIES_LIST_XML)})) String payload) {

        return addToTask(headers, userId, containerId, tInstanceId, removeExisting, payload, POT_OWNER);
    }

    @ApiOperation(value="Adds users and groups to be excluded from being owners for a specified task instance.",
            response=Void.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found") })
    @PUT
    @Path(TASK_INSTANCE_EXL_OWNERS_USERS_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response addExcludedOwners(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "optional user id to be used instead of authenticated user - only when bypass authenticated user is enabled", required = false) @QueryParam("user") String userId,
            @ApiParam(value = "optional flag that indicates if existing excluded owners should be removed, defaults to false", required = false) @QueryParam("remove") @DefaultValue("false") boolean removeExisting, 
            @ApiParam(value = "list of users/groups to be added as excluded owners, as OrgEntities type", required = true, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=ORG_ENTITIES_LIST_JSON),
                    @ExampleProperty(mediaType=XML, value=ORG_ENTITIES_LIST_XML)})) String payload) {

        return addToTask(headers, userId,  containerId, tInstanceId, removeExisting, payload, EXL_OWNER);
    }

    @ApiOperation(value="Adds business administrator users or groups to a specified task instance.",
            response=Void.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found") })
    @PUT
    @Path(TASK_INSTANCE_ADMINS_USERS_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response addAdmins(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "optional user id to be used instead of authenticated user - only when bypass authenticated user is enabled", required = false) @QueryParam("user") String userId,
            @ApiParam(value = "optional flag that indicates if existing business admins should be removed, defaults to false", required = false) @QueryParam("remove") @DefaultValue("false") boolean removeExisting, 
            @ApiParam(value = "list of users/groups to be added as business admins, as OrgEntities type", required = true, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=ORG_ENTITIES_LIST_JSON),
                    @ExampleProperty(mediaType=XML, value=ORG_ENTITIES_LIST_XML)})) String payload) {

        return addToTask(headers, userId, containerId, tInstanceId, removeExisting, payload, ADMIN);
    }

    
    @ApiOperation(value="Deletes specified users previously added as potential owners for a specified task instance.",
            response=Void.class, code=204)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found") })
    @DELETE
    @Path(TASK_INSTANCE_POT_OWNERS_USERS_DELETE_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response removePotentialOwnersUsers(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "optional user id to be used instead of authenticated user - only when bypass authenticated user is enabled", required = false) @QueryParam("user") String userId,
            @ApiParam(value = "list of users to be removed from potantial owners list", required = true, example = "john") @PathParam("entityId") String users) {

        return removeFromTask(headers, userId, containerId, tInstanceId, true, users, POT_OWNER);
    }

    @ApiOperation(value="Deletes specified users previously added as excluded owners for a specified task instance. ",
            response=Void.class, code=204)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found") })
    @DELETE
    @Path(TASK_INSTANCE_EXL_OWNERS_USERS_DELETE_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response removeExcludedOwnersUsers(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "optional user id to be used instead of authenticated user - only when bypass authenticated user is enabled", required = false) @QueryParam("user") String userId,
            @ApiParam(value = "list of users to be removed from excluded owners list", required = true, example = "john") @PathParam("entityId") String users) {

        return removeFromTask(headers, userId, containerId, tInstanceId, true, users, EXL_OWNER);
    }

    @ApiOperation(value="Deletes specified users previously added as business administrators for a specified task instance.",
            response=Void.class, code=204)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found") })
    @DELETE
    @Path(TASK_INSTANCE_ADMINS_USERS_DELETE_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response removeAdminsUsers(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "optional user id to be used instead of authenticated user - only when bypass authenticated user is enabled", required = false) @QueryParam("user") String userId,
            @ApiParam(value = "list of users to be removed from business admin list", required = true, example = "john") @PathParam("entityId") String users) {

        return removeFromTask(headers, userId, containerId, tInstanceId, true, users, ADMIN);
    }

    @ApiOperation(value="Deletes specified groups previously added as potential owners for a specified task instance.",
            response=Void.class, code=204)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found") })
    @DELETE
    @Path(TASK_INSTANCE_POT_OWNERS_GROUPS_DELETE_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response removePotentialOwnersGroups(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "optional user id to be used instead of authenticated user - only when bypass authenticated user is enabled", required = false) @QueryParam("user") String userId,
            @ApiParam(value = "list of groups to be removed from potantial owners list", required = true, example = "HR") @PathParam("entityId") String groups) {

        return removeFromTask(headers, userId, containerId, tInstanceId, false, groups, POT_OWNER);
    }

    @ApiOperation(value="Deletes specified groups previously added as excluded owners for a specified task instance.",
            response=Void.class, code=204)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found") })
    @DELETE
    @Path(TASK_INSTANCE_EXL_OWNERS_GROUPS_DELETE_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response removeExcludedOwnersGroups(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "optional user id to be used instead of authenticated user - only when bypass authenticated user is enabled", required = false) @QueryParam("user") String userId,
            @ApiParam(value = "list of groups to be removed from excluded owners list", required = true, example = "HR") @PathParam("entityId") String groups) {

        return removeFromTask(headers, userId, containerId, tInstanceId, false, groups, EXL_OWNER);
    }

    @ApiOperation(value="Deletes specified groups previously added as business administrators for a specified task instance.",
            response=Void.class, code=204)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found") })
    @DELETE
    @Path(TASK_INSTANCE_ADMINS_GROUPS_DELETE_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response removeAdminsGroups(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true) @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true) @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "optional user id to be used instead of authenticated user - only when bypass authenticated user is enabled", required = false) @QueryParam("user") String userId,
            @ApiParam(value = "list of groups to be removed from business admin list", required = true, example = "HR") @PathParam("entityId") String groups) {

        return removeFromTask(headers, userId, containerId, tInstanceId, false, groups, ADMIN);
    }

    @ApiOperation(value="Adds input data to a specified task instance.",
            response=Void.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found") })
    @PUT
    @Path(TASK_INSTANCE_INPUTS_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response addTaskInputs(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "map of data to be set as task inputs, as Map", required = true, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=VAR_MAP_JSON),
                    @ExampleProperty(mediaType=XML, value=VAR_MAP_XML)})) String payload) {

        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            userTaskAdminServiceBase.addTaskInputs(containerId, tInstanceId, payload, type);
            return createResponse("", v, Response.Status.CREATED, conversationIdHeader);
        } catch (TaskNotFoundException e) {
            return notFound(MessageFormat.format(TASK_INSTANCE_NOT_FOUND, tInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Deletes input data by parameter name from a specified task instance.",
            response=Void.class, code=204)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found") })
    @DELETE
    @Path(TASK_INSTANCE_INPUTS_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response removeTaskInputs(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "one or more names of task inputs to be removed", required = true) @QueryParam("name") List<String> inputNames) {
        Variant v = getVariant(headers);

        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            userTaskAdminServiceBase.removeTaskInputs(containerId, tInstanceId, inputNames);
            return noContent(v, conversationIdHeader);
        } catch (TaskNotFoundException e) {
            return notFound(MessageFormat.format(TASK_INSTANCE_NOT_FOUND, tInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Deletes output data by parameter name from a specified task instance.",
            response=Void.class, code=204)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found") })
    @DELETE
    @Path(TASK_INSTANCE_OUTPUTS_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response removeTaskOutputs(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "one or more names of task outputs to be removed", required = true) @QueryParam("name") List<String> outputNames) {
        Variant v = getVariant(headers);

        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            userTaskAdminServiceBase.removeTaskOutputs(containerId, tInstanceId, outputNames);
            return noContent(v, conversationIdHeader);
        } catch (TaskNotFoundException e) {
            return notFound(MessageFormat.format(TASK_INSTANCE_NOT_FOUND, tInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Schedules a specified task instance to be reassigned to specified users or groups and returns the ID of the reassignment.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found"), 
            @ApiResponse(code = 201, response = Long.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=INTEGER_JSON)})) })
    @POST
    @Path(TASK_INSTANCE_REASSIGNMENTS_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response reassign(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "time expression for reassignmnet", required = true) @QueryParam("expiresAt") String expiresAt, 
            @ApiParam(value = "optional flag that indicates the type of reassignment, either whenNotStarted or whenNotCompleted must be set", required = false) @QueryParam("whenNotStarted") @DefaultValue("false") boolean whenNotStarted, 
            @ApiParam(value = "optional flag that indicates the type of reassignment, either whenNotStarted or whenNotCompleted must be set", required = false) @QueryParam("whenNotCompleted") @DefaultValue("false") boolean whenNotCompleted, 
            @ApiParam(value = "list of users/groups that task should be reassined to, as OrgEntities type", required = true, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=ORG_ENTITIES_LIST_JSON),
                    @ExampleProperty(mediaType=XML, value=ORG_ENTITIES_LIST_XML)})) String payload) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);

        if (expiresAt == null) {
            return badRequest("'expiresAt' query parameter is mandatory", v, conversationIdHeader);
        }

        try {
            if (!whenNotCompleted && !whenNotStarted) {
                return badRequest("At least one query parameters must be set to true - 'whenNotStarted' or 'whenNotCompleted'", v, conversationIdHeader);
            }
            String id = null;
            if (whenNotStarted) {
                id = userTaskAdminServiceBase.reassignWhenNotStarted(containerId, tInstanceId, expiresAt, payload, type);
            }
            if (whenNotCompleted) {
                id = userTaskAdminServiceBase.reassignWhenNotCompleted(containerId, tInstanceId, expiresAt, payload, type);
            }
            return createResponse(id, v, Response.Status.CREATED, conversationIdHeader);
        } catch (TaskNotFoundException e) {
            return notFound(MessageFormat.format(TASK_INSTANCE_NOT_FOUND, tInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (RuntimeException e) {
            return badRequest(e.getMessage(), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Creates an email notification for the specified task instance and returns the ID of the new notification.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found"), 
            @ApiResponse(code = 201, response = Long.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=INTEGER_JSON)})) })
    @POST
    @Path(TASK_INSTANCE_NOTIFICATIONS_URI)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response notify(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "time expression for notification", required = true) @QueryParam("expiresAt") String expiresAt, 
            @ApiParam(value = "optional flag that indicates the type of notification, either whenNotStarted or whenNotCompleted must be set", required = false) @QueryParam("whenNotStarted") @DefaultValue("false") boolean whenNotStarted, 
            @ApiParam(value = "optional flag that indicates the type of notification, either whenNotStarted or whenNotCompleted must be set", required = false) @QueryParam("whenNotCompleted") @DefaultValue("false") boolean whenNotCompleted, 
            @ApiParam(value = "email notification details, as EmailNotification type", required = true, examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=EMAIL_NOTIFICATION_JSON),
                    @ExampleProperty(mediaType=XML, value=EMAIL_NOTIFICATION_XML)})) String payload) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            if (expiresAt == null) {
                return badRequest("'expiresAt' query parameter is mandatory", v, conversationIdHeader);
            }

            if (!whenNotCompleted && !whenNotStarted) {
                return badRequest("At least one query parameters must be set to true - 'whenNotStarted' or 'whenNotCompleted'", v, conversationIdHeader);
            }
            String id = null;
            if (whenNotStarted) {
                id = userTaskAdminServiceBase.notifyWhenNotStarted(containerId, tInstanceId, expiresAt, payload, type);
            }
            if (whenNotCompleted) {
                id = userTaskAdminServiceBase.notifyWhenNotCompleted(containerId, tInstanceId, expiresAt, payload, type);
            }
            return createResponse(id, v, Response.Status.CREATED, conversationIdHeader);
        } catch (TaskNotFoundException e) {
            return notFound(MessageFormat.format(TASK_INSTANCE_NOT_FOUND, tInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Deletes a specified reassignment for a specified task instance.",
            response=Void.class, code=204)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found") })
    @DELETE
    @Path(TASK_INSTANCE_REASSIGNMENT_DELETE_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response cancelReassignment(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "identifier of reassignment to be canceled", required = true, example = "567") @PathParam("reassignmentId") Long reassignmentId) {
        Variant v = getVariant(headers);

        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            userTaskAdminServiceBase.cancelReassignment(containerId, tInstanceId, reassignmentId);
            return noContent(v, conversationIdHeader);
        } catch (TaskNotFoundException e) {
            return notFound(MessageFormat.format(TASK_INSTANCE_NOT_FOUND, tInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Deletes a specified email notification from a specified task instance.",
            response=Void.class, code=204)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found") })
    @DELETE
    @Path(TASK_INSTANCE_NOTIFICATION_DELETE_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response cancelNotification(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "identifier of notification to be canceled", required = true, example = "567") @PathParam("notificationId") Long notificationId) {
        Variant v = getVariant(headers);

        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            userTaskAdminServiceBase.cancelNotification(containerId, tInstanceId, notificationId);
            return noContent(v, conversationIdHeader);
        } catch (TaskNotFoundException e) {
            return notFound(MessageFormat.format(TASK_INSTANCE_NOT_FOUND, tInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns task reassignments for a specified task instance.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found"), 
            @ApiResponse(code = 200, response = TaskReassignmentList.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_TASK_REASSIGNMENTS_RESPONSE_JSON)})) })
    @GET
    @Path(TASK_INSTANCE_REASSIGNMENTS_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getTaskReassignments(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "optional flag that indicates if active only reassignmnets should be collected, defaults to true", required = false) @QueryParam("activeOnly") @DefaultValue("true") boolean activeOnly) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            TaskReassignmentList taskReassignmentList = userTaskAdminServiceBase.getTaskReassignments(containerId, tInstanceId, activeOnly);

            return createCorrectVariant(taskReassignmentList, headers, Response.Status.OK, conversationIdHeader);
        } catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format(TASK_INSTANCE_NOT_FOUND, tInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns notifications created for a specified task instance.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found"), 
            @ApiResponse(code = 200, response = TaskNotificationList.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_TASK_NOTIFICATIONS_RESPONSE_JSON)})) })
    @GET
    @Path(TASK_INSTANCE_NOTIFICATIONS_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getTaskNotifications(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance to be updated", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long tInstanceId, 
            @ApiParam(value = "optional flag that indicates if active only notifications should be collected, defaults to true", required = false) @QueryParam("activeOnly") @DefaultValue("true") boolean activeOnly) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            TaskNotificationList taskNotificationList = userTaskAdminServiceBase.getTaskNotifications(containerId, tInstanceId, activeOnly);

            return createCorrectVariant(taskNotificationList, headers, Response.Status.OK, conversationIdHeader);
        } catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format(TASK_INSTANCE_NOT_FOUND, tInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Acknowledges a specified task execution error.",
            response=Void.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found") })
    @PUT
    @Path(ACK_ERROR_PUT_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response acknowledgeError(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that error belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of the execution error to be acknowledged", required = true, example = "xxx-yyy-zzz") @PathParam("errorId") String errorId) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            userTaskAdminServiceBase.acknowledgeError(Arrays.asList(errorId));
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

    @ApiOperation(value="Acknowledges one or more task execution errors in a specified KIE container.",
            response=Void.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found") })
    @PUT
    @Path(ACK_ERRORS_PUT_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response acknowledgeErrors(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that errors belong to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "list of identifiers of execution errors to be acknowledged", required = true) @QueryParam("errorId") List<String> errorIds) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            userTaskAdminServiceBase.acknowledgeError(errorIds);
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

    @ApiOperation(value="Returns information about a specified task execution error.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task instance or Container Id not found"), 
            @ApiResponse(code = 200, response = ExecutionErrorInstance.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_EXEC_ERROR_RESPONSE_JSON)})) })
    @GET
    @Path(ERROR_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getExecutionErrorById(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that error belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of the execution error to load", required = true, example = "xxx-yyy-zzz") @PathParam("errorId") String errorId) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            ExecutionErrorInstance executionErrorInstance = userTaskAdminServiceBase.getError(errorId);

            return createCorrectVariant(executionErrorInstance, headers, Response.Status.OK, conversationIdHeader);
        } catch (ExecutionErrorNotFoundException e) {
            return notFound(e.getMessage(), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns task execution errors for a specified task instance.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Container Id not found"), 
            @ApiResponse(code = 200, response = ExecutionErrorInstanceList.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_EXEC_ERRORS_RESPONSE_JSON)})) })
    @GET
    @Path(ERRORS_BY_TASK_ID_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getExecutionErrorsByTask(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of the task instance that errors should be collected for", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long taskId, 
            @ApiParam(value = "optional flag that indicates if acknowledged errors should also be collected, defaults to false", required = false) @QueryParam("includeAck") @DefaultValue("false") boolean includeAcknowledged, 
            @ApiParam(value = "optional pagination - at which page to start, defaults to 0 (meaning first)", required = false) @QueryParam("page") @DefaultValue("0") Integer page, 
            @ApiParam(value = "optional pagination - size of the result, defaults to 10", required = false) @QueryParam("pageSize") @DefaultValue("10") Integer pageSize, 
            @ApiParam(value = "optional sort column, no default", required = false) @QueryParam("sort") String sort, 
            @ApiParam(value = "optional sort direction (asc, desc) - defaults to asc", required = false) @QueryParam("sortOrder") @DefaultValue("true") boolean sortOrder) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            ExecutionErrorInstanceList executionErrorInstanceList = userTaskAdminServiceBase.getExecutionErrorsByTaskId(containerId, taskId, includeAcknowledged, page, pageSize, sort, sortOrder);

            return createCorrectVariant(executionErrorInstanceList, headers, Response.Status.OK, conversationIdHeader);
        } catch (ExecutionErrorNotFoundException e) {
            return notFound(e.getMessage(), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns all task execution errors for a specified KIE container.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Container Id not found"), 
            @ApiResponse(code = 200, response = ExecutionErrorInstanceList.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_EXEC_ERRORS_RESPONSE_JSON)})) })
    @GET
    @Path(ERRORS_GET_URI)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getExecutionErrors(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "optional flag that indicates if acknowledged errors should also be collected, defaults to false", required = false) @QueryParam("includeAck") @DefaultValue("false") boolean includeAcknowledged, 
            @ApiParam(value = "optional name of the task to filter by", required = false) @QueryParam("name") String taskName, 
            @ApiParam(value = "optional process id that the task belongs to to filter by", required = false) @QueryParam("process") String processId, 
            @ApiParam(value = "optional pagination - at which page to start, defaults to 0 (meaning first)", required = false) @QueryParam("page") @DefaultValue("0") Integer page, 
            @ApiParam(value = "optional pagination - size of the result, defaults to 10", required = false) @QueryParam("pageSize") @DefaultValue("10") Integer pageSize, 
            @ApiParam(value = "optional sort column, no default", required = false) @QueryParam("sort") String sort, 
            @ApiParam(value = "optional sort direction (asc, desc) - defaults to asc", required = false) @QueryParam("sortOrder") @DefaultValue("true") boolean sortOrder) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            ExecutionErrorInstanceList executionErrorInstanceList = userTaskAdminServiceBase.getExecutionErrorsByTaskName(containerId, processId, taskName, includeAcknowledged, page, pageSize, sort, sortOrder);

            return createCorrectVariant(executionErrorInstanceList, headers, Response.Status.OK, conversationIdHeader);
        } catch (ExecutionErrorNotFoundException e) {
            return notFound(e.getMessage(), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    /*
     * internal methods
     */

    protected Response addToTask(HttpHeaders headers, String userId, String containerId, Long tInstanceId, boolean removeExisting, String payload, int operation) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            switch (operation) {
                case POT_OWNER:
                    userTaskAdminServiceBase.addPotentialOwners(userId, containerId, tInstanceId, removeExisting, payload, type);
                    break;

                case EXL_OWNER:
                    userTaskAdminServiceBase.addExcludedOwners(userId, containerId, tInstanceId, removeExisting, payload, type);
                    break;

                case ADMIN:
                    userTaskAdminServiceBase.addBusinessAdmins(userId, containerId, tInstanceId, removeExisting, payload, type);
                    break;
            }

            return createResponse("", v, Response.Status.CREATED, conversationIdHeader);
        } catch (TaskNotFoundException e) {
            return notFound(MessageFormat.format(TASK_INSTANCE_NOT_FOUND, tInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }

    protected Response removeFromTask(HttpHeaders headers, String userId, String containerId, Long tInstanceId, boolean isUser, String entities, int operation) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            String[] multipleEntities = entities.split(",");
            List<String> listOfEntities = Arrays.asList(multipleEntities);

            switch (operation) {
                case POT_OWNER:
                    userTaskAdminServiceBase.removePotentialOwners(userId, containerId, tInstanceId, listOfEntities, isUser);
                    break;

                case EXL_OWNER:
                    userTaskAdminServiceBase.removeExcludedOwners(userId, containerId, tInstanceId, listOfEntities, isUser);
                    break;

                case ADMIN:
                    userTaskAdminServiceBase.removeBusinessAdmins(userId, containerId, tInstanceId, listOfEntities, isUser);
                    break;
            }

            return noContent(v, conversationIdHeader);
        } catch (TaskNotFoundException e) {
            return notFound(MessageFormat.format(TASK_INSTANCE_NOT_FOUND, tInstanceId), v, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }
}
