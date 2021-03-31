/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.server.remote.rest.common.resource;

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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceFilter;
import org.kie.server.api.model.KieContainerStatusFilter;
import org.kie.server.api.model.KieScannerResource;
import org.kie.server.api.model.Message;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ReleaseIdFilter;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.Severity;
import org.kie.server.remote.rest.common.Header;
import org.kie.server.services.impl.KieServerImpl;
import org.kie.server.services.impl.KieServerLocator;
import org.kie.server.services.impl.marshal.MarshallerHelper;

import static org.kie.server.api.rest.RestURI.*;
import static org.kie.server.remote.rest.common.docs.ParameterSamples.CREATE_CONTAINER_JSON;
import static org.kie.server.remote.rest.common.docs.ParameterSamples.CREATE_CONTAINER_XML;
import static org.kie.server.remote.rest.common.docs.ParameterSamples.JSON;
import static org.kie.server.remote.rest.common.docs.ParameterSamples.UPDATE_RELEASE_ID_JSON;
import static org.kie.server.remote.rest.common.docs.ParameterSamples.UPDATE_RELEASE_ID_XML;
import static org.kie.server.remote.rest.common.docs.ParameterSamples.UPDATE_SCANNER_JSON;
import static org.kie.server.remote.rest.common.docs.ParameterSamples.UPDATE_SCANNER_XML;
import static org.kie.server.remote.rest.common.docs.ParameterSamples.XML;
import static org.kie.server.remote.rest.common.docs.ParameterSamples.*;
import static org.kie.server.remote.rest.common.util.RestUtils.buildConversationIdHeader;
import static org.kie.server.remote.rest.common.util.RestUtils.createCorrectVariant;
import static org.kie.server.remote.rest.common.util.RestUtils.getContentType;
import static org.kie.server.remote.rest.common.util.RestUtils.serviceUnavailable;

@Api(value="KIE Server and KIE containers")
@Path("server")
public class KieServerRestImpl {

    private KieServerImpl server;
    private MarshallerHelper marshallerHelper;

    public KieServerRestImpl() {
        // for now, if no server impl is passed as parameter, create one
        this.server = KieServerLocator.getInstance();
        this.marshallerHelper = new MarshallerHelper(this.server.getServerRegistry());
    }

    public KieServerRestImpl(KieServerImpl server) {
        this.server = server;
        this.marshallerHelper = new MarshallerHelper(server.getServerRegistry());
    }

    public KieServerImpl getServer() {
        return server;
    }

    public void setServer(KieServerImpl server) {
        this.server = server;
    }


    @ApiOperation(value="Returns information about the KIE Server. The endpoint for this request is the base URL for the REST API.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"), 
            @ApiResponse(code = 200, response=ServiceResponse.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_SERVER_INF_RESPONSE_JSON)})) })
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getInfo(@Context HttpHeaders headers) { 
        return createCorrectVariant(server.getInfo(), headers);
    }

    @ApiOperation(value="Returns a list of KIE containers on the KIE Server.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"), 
            @ApiResponse(code = 200, response=ServiceResponse.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_CONTAINERS_RESPONSE_JSON)})) })
    @GET
    @Path("containers")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response listContainers(@Context HttpHeaders headers,
            @ApiParam(value = "optional groupId to filter containers by", required = false) @QueryParam("groupId") String groupId,
            @ApiParam(value = "optional artifactId to filter containers by", required = false) @QueryParam("artifactId") String artifactId,
            @ApiParam(value = "optional version to filter containers by", required = false) @QueryParam("version") String version,
            @ApiParam(value = "optional status to filter containers by", required = false) @QueryParam("status") String status) {
        ReleaseIdFilter releaseIdFilter = new ReleaseIdFilter.Builder()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .build();

        KieContainerStatusFilter statusFilter = KieContainerStatusFilter.parseFromNullableString(status);
        KieContainerResourceFilter containerFilter = new KieContainerResourceFilter(releaseIdFilter, statusFilter);
        return createCorrectVariant(server.listContainers(containerFilter), headers);
    }

    @ApiOperation(value="Creates a new KIE container in the KIE Server with a specified KIE container ID",
            response=ServiceResponse.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"), @ApiResponse(code = 400, message = "container could not be created"), 
            @ApiResponse(code = 201, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=CREATE_CONTAINER_RESPONSE_JSON)})) })
    @PUT
    @Path("containers/{" + CONTAINER_ID + "}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response createContainer( @Context HttpHeaders headers, 
            @ApiParam(value = "Container id to be assigned to deployed KIE Container", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String id, 
            @ApiParam(value = "KIE Container resource to be deployed as KieContainerResource", required = true, examples=@Example(value= {
                                      @ExampleProperty(mediaType=JSON, value=CREATE_CONTAINER_JSON),
                                      @ExampleProperty(mediaType=XML, value=CREATE_CONTAINER_XML)})) String containerPayload ) {
        
        ServiceResponse<?> forbidden = this.server.checkAccessability();
        if (forbidden != null) {                       
            return createCorrectVariant( forbidden, headers, Status.BAD_REQUEST );
        }

        String contentType = getContentType(headers);
        KieContainerResource container = marshallerHelper.unmarshal( containerPayload, contentType, KieContainerResource.class );

        ServiceResponse<KieContainerResource> response = server.createContainer( id, container );

        Header conversationIdHeader = buildConversationIdHeader(id, server.getServerRegistry(), headers);
        if ( response.getType() == ServiceResponse.ResponseType.SUCCESS ) {
            return createCorrectVariant( response, headers, Status.CREATED, conversationIdHeader );
        }
        return createCorrectVariant( response, headers, Status.BAD_REQUEST );
    }
    
    @ApiOperation(value="Activates (previously deactivated) KIE container on this server",
            response=ServiceResponse.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"), @ApiResponse(code = 400, message = "container could not be activated"), 
            @ApiResponse(code = 201, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=ACTIVATE_CONTAINER_RESPONSE_JSON)})) })
    @PUT
    @Path("containers/{" + CONTAINER_ID + "}/status/activated")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response activateContainer( @Context HttpHeaders headers, 
            @ApiParam(value = "Container id of deployed KIE Container", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String id) {        
        
        ServiceResponse<?> forbidden = this.server.checkAccessability();
        if (forbidden != null) {                       
            return createCorrectVariant( forbidden, headers, Status.BAD_REQUEST );
        }

        ServiceResponse<KieContainerResource> response = server.activateContainer( id );

        Header conversationIdHeader = buildConversationIdHeader(id, server.getServerRegistry(), headers);
        if ( response.getType() == ServiceResponse.ResponseType.SUCCESS ) {
            return createCorrectVariant( response, headers, Status.CREATED, conversationIdHeader );
        }
        return createCorrectVariant( response, headers, Status.BAD_REQUEST );
    }
    
    @ApiOperation(value="Deactivates (previously started) KIE container on this server",
            response=ServiceResponse.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"), @ApiResponse(code = 400, message = "container could not be deactivated"), 
            @ApiResponse(code = 201, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=DEACTIVATE_CONTAINER_RESPONSE_JSON)})) })
    @PUT
    @Path("containers/{" + CONTAINER_ID + "}/status/deactivated")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response deactivateContainer( @Context HttpHeaders headers, 
            @ApiParam(value = "Container id of deployed KIE Container", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String id) {        
        
        ServiceResponse<?> forbidden = this.server.checkAccessability();
        if (forbidden != null) {                       
            return createCorrectVariant( forbidden, headers, Status.BAD_REQUEST );
        }

        ServiceResponse<KieContainerResource> response = server.deactivateContainer( id );

        Header conversationIdHeader = buildConversationIdHeader(id, server.getServerRegistry(), headers);
        if ( response.getType() == ServiceResponse.ResponseType.SUCCESS ) {
            return createCorrectVariant( response, headers, Status.CREATED, conversationIdHeader );
        }
        return createCorrectVariant( response, headers, Status.BAD_REQUEST );
    }

    @ApiOperation(value="Returns information about a specified KIE container.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"), 
            @ApiResponse(code = 200, response=ServiceResponse.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_CONTAINER_RESPONSE_JSON)})) })
    @GET
    @Path("containers/{" + CONTAINER_ID + "}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getContainerInfo( @Context HttpHeaders headers, 
            @ApiParam(value = "Container id to be retrieved", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String id ) {
        Header conversationIdHeader = buildConversationIdHeader(id, server.getServerRegistry(), headers);
        return createCorrectVariant(server.getContainerInfo(id), headers, conversationIdHeader);
    }

    @ApiOperation(value="Disposes a specified KIE container.",
            response=ServiceResponse.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"), 
            @ApiResponse(code = 200, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=DISPOSE_CONTAINER_RESPONSE_JSON)})) })
    @DELETE
    @Path("containers/{" + CONTAINER_ID + "}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response disposeContainer( @Context HttpHeaders headers, 
            @ApiParam(value = "Container id to be disposed (undeployed)", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String id ) {
        ServiceResponse<?> forbidden = this.server.checkAccessability();
        if (forbidden != null) {                       
            return createCorrectVariant( forbidden, headers, Status.BAD_REQUEST );
        }
        
        Header conversationIdHeader = buildConversationIdHeader(id, server.getServerRegistry(), headers);
        return createCorrectVariant(server.disposeContainer(id), headers, conversationIdHeader);
    }

    @ApiOperation(value="Returns information about the KIE scanner used for automatic updates in a specified KIE container, if applicable.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"), 
            @ApiResponse(code = 200, response=ServiceResponse.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_SCANNER_RESPONSE_JSON)})) })
    @GET
    @Path("containers/{" + CONTAINER_ID + "}/scanner")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getScannerInfo( @Context HttpHeaders headers, 
            @ApiParam(value = "Container id for scanner to be loaded", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String id ) {
        Header conversationIdHeader = buildConversationIdHeader(id, server.getServerRegistry(), headers);
        return createCorrectVariant(server.getScannerInfo(id), headers, conversationIdHeader);
    }
    
    @ApiOperation(value="Starts or stops a KIE scanner that controls polling for updated KIE container deployments, if applicable.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"), 
            @ApiResponse(code = 200, response=ServiceResponse.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=UPDATE_SCANNER_RESPONSE_JSON)})) })
    @POST
    @Path("containers/{" + CONTAINER_ID + "}/scanner")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response updateScanner( @Context HttpHeaders headers, 
            @ApiParam(value = "Container id for scanner to be updated", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String id, 
            @ApiParam(value = "Scanner information given as KieScannerResource type", required = true, examples=@Example(value= {
                                     @ExampleProperty(mediaType=JSON, value=UPDATE_SCANNER_JSON),
                                     @ExampleProperty(mediaType=XML, value=UPDATE_SCANNER_XML)})) String resourcePayload ) {
        
        ServiceResponse<?> forbidden = this.server.checkAccessability();
        if (forbidden != null) {                       
            return createCorrectVariant( forbidden, headers, Status.BAD_REQUEST );
        }
        
        String contentType = getContentType(headers);
        
        KieScannerResource resource = marshallerHelper.unmarshal(resourcePayload, contentType, KieScannerResource.class);
        Header conversationIdHeader = buildConversationIdHeader(id, server.getServerRegistry(), headers);
        return createCorrectVariant(server.updateScanner(id, resource), headers, conversationIdHeader);
    };

    @ApiOperation(value="Returns release ID information (group ID, artifact ID, version) for a specified KIE container.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"), 
            @ApiResponse(code = 200, response=ServiceResponse.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_RELEASE_ID_RESPONSE_JSON)})) })
    @GET
    @Path("containers/{" + CONTAINER_ID + "}/release-id")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getReleaseId( @Context HttpHeaders headers, 
            @ApiParam(value = "Container id that release id should be loaded from", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String id) {
        Header conversationIdHeader = buildConversationIdHeader(id, server.getServerRegistry(), headers);
        return createCorrectVariant(server.getContainerReleaseId(id), headers, conversationIdHeader);
    }

    @ApiOperation(value="Updates release ID information (group ID, artifact ID, version) for a specified KIE container.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"), 
            @ApiResponse(code = 200, response=ServiceResponse.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=UPDATE_RELEASE_RESPONSE_JSON)})) })
    @POST
    @Path("containers/{" + CONTAINER_ID + "}/release-id")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response updateReleaseId( @Context HttpHeaders headers, 
            @ApiParam(value = "Container id that release id should be upgraded", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String id, 
            @ApiParam(value = "Release Id to be upgraded to as ReleaseId type", required = true, examples=@Example(value= {
                                   @ExampleProperty(mediaType=JSON, value=UPDATE_RELEASE_ID_JSON),
                                   @ExampleProperty(mediaType=XML, value=UPDATE_RELEASE_ID_XML)})) String releaseIdPayload,
            @ApiParam(value = "Determines whether active processes are aborted (reset) before updating when the server runs in development mode") @QueryParam("resetBeforeUpdate")  @DefaultValue("false") boolean resetBeforeUpdate) {
        
        ServiceResponse<?> forbidden = this.server.checkAccessability();
        if (forbidden != null) {                       
            return createCorrectVariant( forbidden, headers, Status.BAD_REQUEST );
        }
        
        String contentType = getContentType(headers);

        ReleaseId releaseId = marshallerHelper.unmarshal(releaseIdPayload, contentType, ReleaseId.class);
        Header conversationIdHeader = buildConversationIdHeader(id, server.getServerRegistry(), headers);

        return createCorrectVariant(server.updateContainerReleaseId(id, releaseId, resetBeforeUpdate), headers, conversationIdHeader);
    }

    @ApiOperation(value="Returns information about the current state and configurations of the KIE Server.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"), 
            @ApiResponse(code = 200, response=ServiceResponse.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=GET_SERVER_STATE_RESPONSE_JSON)})) })
    @GET
    @Path("state")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getServerState(@Context HttpHeaders headers) {
        return createCorrectVariant(server.getServerState(), headers);
    }
    
    
    
    @ApiOperation(value="Readiness check for KIE Server that indicates that server is fully booted and ready to accept requests",
            response=Void.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 503, message = "Service not yet available") })
    @GET
    @Path("readycheck")
    @Produces({MediaType.TEXT_PLAIN})
    public Response readycheck(@Context HttpHeaders headers) { 
        if (server.isKieServerReady()) {
            return Response.status(Response.Status.OK).build();
        }
        return serviceUnavailable();
    }
    
    @ApiOperation(value="Liveness check for KIE Server that validates both kie server and all extensions, optionally produces report",
            response=Message.class, code=200, responseContainer="List")
    @ApiResponses(value = { @ApiResponse(code = 503, message = "If any of the checks failed") })
    @GET
    @Path("healthcheck")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response healthcheck(@Context HttpHeaders headers,
                             @ApiParam(value = "optional report flag to return detailed report of the check, defaults to false", required = false) @QueryParam("report")  @DefaultValue("false") boolean report) { 
        List<Message> healthMessages = server.healthCheck(report);
        
        boolean anyfailures = healthMessages.stream().anyMatch(msg -> msg.getSeverity().equals(Severity.ERROR));
        if (anyfailures) {
            if (report) {
                return createCorrectVariant(healthMessages, headers, Response.Status.SERVICE_UNAVAILABLE);
            }
            
            return serviceUnavailable();
        }
        if (report) {
            return createCorrectVariant(healthMessages, headers, Response.Status.OK);
        }
        return Response.status(Response.Status.OK).build();
    }

}
