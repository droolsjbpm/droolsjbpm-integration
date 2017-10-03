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

import static org.kie.server.api.rest.RestURI.CANCEL_JOB_DEL_URI;
import static org.kie.server.api.rest.RestURI.JOB_INSTANCES_BY_CMD_GET_URI;
import static org.kie.server.api.rest.RestURI.JOB_INSTANCES_BY_CONTAINER_GET_URI;
import static org.kie.server.api.rest.RestURI.JOB_INSTANCES_BY_KEY_GET_URI;
import static org.kie.server.api.rest.RestURI.JOB_INSTANCES_BY_PROCESS_INSTANCE_GET_URI;
import static org.kie.server.api.rest.RestURI.JOB_INSTANCE_GET_URI;
import static org.kie.server.api.rest.RestURI.JOB_URI;
import static org.kie.server.api.rest.RestURI.REQUEUE_JOB_PUT_URI;
import static org.kie.server.api.rest.RestURI.UPDATE_JOB_DATA_POST_URI;
import static org.kie.server.remote.rest.common.util.RestUtils.badRequest;
import static org.kie.server.remote.rest.common.util.RestUtils.buildConversationIdHeader;
import static org.kie.server.remote.rest.common.util.RestUtils.createCorrectVariant;
import static org.kie.server.remote.rest.common.util.RestUtils.createResponse;
import static org.kie.server.remote.rest.common.util.RestUtils.getContentType;
import static org.kie.server.remote.rest.common.util.RestUtils.getVariant;
import static org.kie.server.remote.rest.common.util.RestUtils.internalServerError;
import static org.kie.server.remote.rest.common.util.RestUtils.noContent;
import static org.kie.server.remote.rest.jbpm.resources.Messages.UNEXPECTED_ERROR;

import java.text.MessageFormat;
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

import org.kie.server.api.model.instance.RequestInfoInstance;
import org.kie.server.api.model.instance.RequestInfoInstanceList;
import org.kie.server.remote.rest.common.Header;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.jbpm.ExecutorServiceBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(value="Asynchronous jobs :: BPM")
@Path("server/" + JOB_URI)
public class ExecutorResource {

    public static final Logger logger = LoggerFactory.getLogger(ExecutorResource.class);

    private ExecutorServiceBase executorServiceBase;
    private KieServerRegistry context;

    public ExecutorResource() {

    }

    public ExecutorResource(ExecutorServiceBase executorServiceBase, KieServerRegistry context) {
        this.executorServiceBase = executorServiceBase;
        this.context = context;
    }

    // operations
    
    
    @ApiOperation(value="Schedules new asynchronous job based on given body",
            response=Long.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @POST
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response scheduleRequest(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "optional container id that the job should be associated with", required = false) @QueryParam("containerId") String containerId, 
            @ApiParam(value = "asynchronous job definition represented as JobRequestInstance", required = true) String payload) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        // no container id available so only used to transfer conversation id if given by client
        Header conversationIdHeader = buildConversationIdHeader("", context, headers);

        try {

            String response = executorServiceBase.scheduleRequest(containerId, payload, type);

            logger.debug("Returning CREATED response with content '{}'", response);
            return createResponse(response, v, Response.Status.CREATED, conversationIdHeader);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid Command type ", e.getMessage(), e);
            return internalServerError( e.getMessage(), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(MessageFormat.format(UNEXPECTED_ERROR, e.getMessage()), v, conversationIdHeader);
        }


    }

    @ApiOperation(value="Cancels active asynchronous job identified by given jobId",
            response=Void.class, code=204)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @DELETE
    @Path(CANCEL_JOB_DEL_URI)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response cancelRequest(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "identifier of the asynchronous job to be canceled", required = true) @PathParam("jobId") long requestId) {
        Variant v = getVariant(headers);
        // no container id available so only used to transfer conversation id if given by client
        Header conversationIdHeader = buildConversationIdHeader("", context, headers);
        try {
            executorServiceBase.cancelRequest(requestId);
            // produce 204 NO_CONTENT response code
            return noContent(v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(MessageFormat.format(UNEXPECTED_ERROR, e.getMessage()), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Requeues failed asynchronous job identified by given jobId",
            response=Void.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @PUT
    @Path(REQUEUE_JOB_PUT_URI)
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response requeueRequest(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "identifier of the asynchronous job to be requeued", required = true) @PathParam("jobId") long requestId){
        Variant v = getVariant(headers);
        // no container id available so only used to transfer conversation id if given by client
        Header conversationIdHeader = buildConversationIdHeader("", context, headers);
        try {
            executorServiceBase.requeueRequest(requestId);

            return createResponse("", v, Response.Status.CREATED, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(MessageFormat.format(UNEXPECTED_ERROR, e.getMessage()), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Updates active asynchronous job's data (identified by given jobId)",
            response=Void.class, code=204)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @POST
    @Path(UPDATE_JOB_DATA_POST_URI)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response updateRequestData(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "identifier of the asynchronous job to be updated", required = true) @PathParam("jobId") long requestId,
            @ApiParam(value = "optional container id that the job should be associated with", required = false) @QueryParam("containerId") String containerId, 
            @ApiParam(value = "data to be updated on the asynchronous job represented as Map", required = true) String payload) {
        Variant v = getVariant(headers);
        String type = getContentType(headers);
        // no container id available so only used to transfer conversation id if given by client
        Header conversationIdHeader = buildConversationIdHeader("", context, headers);
        try {
            executorServiceBase.updateRequestData(requestId, containerId, payload, type);
            return createResponse("", v, Response.Status.CREATED, conversationIdHeader);
        } catch (IllegalStateException e){
          return badRequest(e.getMessage(), v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(MessageFormat.format(UNEXPECTED_ERROR, e.getMessage()), v, conversationIdHeader);
        }
    }

    // queries
    
    @ApiOperation(value="Retrieves asynchronous jobs filtered by status",
            response=RequestInfoInstanceList.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getRequestsByStatus(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "optional job status (QUEUED, DONE, CANCELLED, ERROR, RETRYING, RUNNING)", required = true, allowableValues="QUEUED,DONE,CANCELLED,ERROR,RETRYING,RUNNING") @QueryParam("status") List<String> statuses,
            @ApiParam(value = "optional pagination - at which page to start, defaults to 0 (meaning first)", required = false) @QueryParam("page") @DefaultValue("0") Integer page, 
            @ApiParam(value = "optional pagination - size of the result, defaults to 10", required = false) @QueryParam("pageSize") @DefaultValue("10") Integer pageSize) {
        Variant v = getVariant(headers);
        // no container id available so only used to transfer conversation id if given by client
        Header conversationIdHeader = buildConversationIdHeader("", context, headers);
        try {
            RequestInfoInstanceList result = executorServiceBase.getRequestsByStatus(statuses, page, pageSize);

            return createCorrectVariant(result, headers, Response.Status.OK, conversationIdHeader);
        }  catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(MessageFormat.format(UNEXPECTED_ERROR, e.getMessage()), v, conversationIdHeader);
        }

    }

    @ApiOperation(value="Retrieves asynchronous jobs by business key",
            response=RequestInfoInstanceList.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @GET
    @Path(JOB_INSTANCES_BY_KEY_GET_URI)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getRequestsByBusinessKey(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "identifier of the business key that asynchornous jobs should be found for", required = true) @PathParam("key") String businessKey,
            @ApiParam(value = "optional job status (QUEUED, DONE, CANCELLED, ERROR, RETRYING, RUNNING)", required = false, allowableValues="QUEUED,DONE,CANCELLED,ERROR,RETRYING,RUNNING") @QueryParam("status") List<String> statuses,
            @ApiParam(value = "optional pagination - at which page to start, defaults to 0 (meaning first)", required = false) @QueryParam("page") @DefaultValue("0") Integer page, 
            @ApiParam(value = "optional pagination - size of the result, defaults to 10", required = false) @QueryParam("pageSize") @DefaultValue("10") Integer pageSize) {

        Variant v = getVariant(headers);
        // no container id available so only used to transfer conversation id if given by client
        Header conversationIdHeader = buildConversationIdHeader("", context, headers);
        try {
            RequestInfoInstanceList result = null;
            if (statuses == null || statuses.isEmpty()) {
                result = executorServiceBase.getRequestsByBusinessKey(businessKey, page, pageSize);
            } else {
                result = executorServiceBase.getRequestsByBusinessKey(businessKey, statuses, page, pageSize);
            }

            return createCorrectVariant(result, headers, Response.Status.OK, conversationIdHeader);
        }  catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(MessageFormat.format(UNEXPECTED_ERROR, e.getMessage()), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Retrieves asynchronous jobs by command",
            response=RequestInfoInstanceList.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @GET
    @Path(JOB_INSTANCES_BY_CMD_GET_URI)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getRequestsByCommand(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "name of the command that asynchornous jobs should be found for", required = true) @PathParam("cmd") String command,
            @ApiParam(value = "optional job status (QUEUED, DONE, CANCELLED, ERROR, RETRYING, RUNNING)", required = false, allowableValues="QUEUED,DONE,CANCELLED,ERROR,RETRYING,RUNNING") @QueryParam("status") List<String> statuses,
            @ApiParam(value = "optional pagination - at which page to start, defaults to 0 (meaning first)", required = false) @QueryParam("page") @DefaultValue("0") Integer page, 
            @ApiParam(value = "optional pagination - size of the result, defaults to 10", required = false) @QueryParam("pageSize") @DefaultValue("10") Integer pageSize) {
        Variant v = getVariant(headers);
        // no container id available so only used to transfer conversation id if given by client
        Header conversationIdHeader = buildConversationIdHeader("", context, headers);
        try {

            RequestInfoInstanceList result = null;
            if (statuses == null || statuses.isEmpty()) {
                result = executorServiceBase.getRequestsByCommand(command, page, pageSize);
            } else {
                result = executorServiceBase.getRequestsByCommand(command, statuses, page, pageSize);
            }

            return createCorrectVariant(result, headers, Response.Status.OK, conversationIdHeader);
        }  catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(MessageFormat.format(UNEXPECTED_ERROR, e.getMessage()), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Retrieves asynchronous jobs by container",
            response=RequestInfoInstanceList.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @GET
    @Path(JOB_INSTANCES_BY_CONTAINER_GET_URI)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getRequestsByContainer(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "identifier of the container that asynchornous jobs should be found for", required = true) @PathParam("id") String containerId,
            @ApiParam(value = "optional job status (QUEUED, DONE, CANCELLED, ERROR, RETRYING, RUNNING)", required = false, allowableValues="QUEUED,DONE,CANCELLED,ERROR,RETRYING,RUNNING") @QueryParam("status") List<String> statuses,
            @ApiParam(value = "optional pagination - at which page to start, defaults to 0 (meaning first)", required = false) @QueryParam("page") @DefaultValue("0") Integer page, 
            @ApiParam(value = "optional pagination - size of the result, defaults to 10", required = false) @QueryParam("pageSize") @DefaultValue("10") Integer pageSize) {

        Variant v = getVariant(headers);
        // no container id available so only used to transfer conversation id if given by client
        Header conversationIdHeader = buildConversationIdHeader("", context, headers);
        try {
            RequestInfoInstanceList result = executorServiceBase.getRequestsByContainer(containerId, statuses, page, pageSize);

            return createCorrectVariant(result, headers, Response.Status.OK, conversationIdHeader);
        }  catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(MessageFormat.format(UNEXPECTED_ERROR, e.getMessage()), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Retrieves asynchronous jobs by process instance id",
            response=RequestInfoInstanceList.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @GET
    @Path(JOB_INSTANCES_BY_PROCESS_INSTANCE_GET_URI)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getRequestsByProcessInstance(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "identifier of the process instance that asynchornous jobs should be found for", required = true) @PathParam("pInstanceId") Long processInstanceId,
            @ApiParam(value = "optional job status (QUEUED, DONE, CANCELLED, ERROR, RETRYING, RUNNING)", required = false, allowableValues="QUEUED,DONE,CANCELLED,ERROR,RETRYING,RUNNING") @QueryParam("status") List<String> statuses,
            @ApiParam(value = "optional pagination - at which page to start, defaults to 0 (meaning first)", required = false) @QueryParam("page") @DefaultValue("0") Integer page, 
            @ApiParam(value = "optional pagination - size of the result, defaults to 10", required = false) @QueryParam("pageSize") @DefaultValue("10") Integer pageSize) {
        Variant v = getVariant(headers);
        // no container id available so only used to transfer conversation id if given by client
        Header conversationIdHeader = buildConversationIdHeader("", context, headers);
        try {

            RequestInfoInstanceList result = executorServiceBase.getRequestsByProcessInstance(processInstanceId, statuses, page, pageSize);

            return createCorrectVariant(result, headers, Response.Status.OK, conversationIdHeader);
        }  catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(MessageFormat.format(UNEXPECTED_ERROR, e.getMessage()), v, conversationIdHeader);
        }
    }

    // instance details
    @ApiOperation(value="Retrieves asynchronous job by given jobId",
            response=RequestInfoInstance.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @GET
    @Path(JOB_INSTANCE_GET_URI)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getRequestById(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "identifier of the asynchronous job to be retrieved", required = true) @PathParam("jobId") Long requestId,
            @ApiParam(value = "optional flag that indicats if errors should be loaded as well", required = false) @QueryParam("withErrors") boolean withErrors, 
            @ApiParam(value = "optional flag that indicats if input/output data should be loaded as well", required = false) @QueryParam("withData") boolean withData) {

        Variant v = getVariant(headers);
        String type = getContentType(headers);
        // no container id available so only used to transfer conversation id if given by client
        Header conversationIdHeader = buildConversationIdHeader("", context, headers);
        try {

            String response = executorServiceBase.getRequestById(requestId, withErrors, withData, type);

            return createResponse(response, v, Response.Status.OK, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(MessageFormat.format(UNEXPECTED_ERROR, e.getMessage()), v, conversationIdHeader);
        }
    }
}
