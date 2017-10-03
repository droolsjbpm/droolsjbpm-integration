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

package org.kie.server.remote.rest.common.resource;

import static org.kie.server.remote.rest.common.util.RestUtils.buildConversationIdHeader;
import static org.kie.server.remote.rest.common.util.RestUtils.createCorrectVariant;
import static org.kie.server.remote.rest.common.util.RestUtils.getContentType;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceFilter;
import org.kie.server.api.model.KieContainerStatusFilter;
import org.kie.server.api.model.KieScannerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ReleaseIdFilter;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.remote.rest.common.Header;
import org.kie.server.services.impl.KieServerImpl;
import org.kie.server.services.impl.KieServerLocator;
import org.kie.server.services.impl.marshal.MarshallerHelper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(value="KIE Server :: Core")
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


    @ApiOperation(value="Retrieves KIE Server information - id, name, location, capabilities, messages",
            response=ServiceResponse.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getInfo(@Context HttpHeaders headers) { 
        return createCorrectVariant(server.getInfo(), headers);
    }

    @ApiOperation(value="Retrieves containers deployed to this server, optionally filtered by group, artifact, version or status",
            response=ServiceResponse.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @GET
    @Path("containers")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
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

    @ApiOperation(value="Creates (deploys) new KIE container to this server",
            response=ServiceResponse.class, code=201)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"), @ApiResponse(code = 400, message = "container could not be created") })
    @PUT
    @Path("containers/{id}")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response createContainer( @Context HttpHeaders headers, 
            @ApiParam(value = "Container id to be assigned to deployed KIE Container", required = true) @PathParam("id") String id, 
            @ApiParam(value = "KIE Container resource to be deployed as KieContainerResource", required = true) String containerPayload ) {
        String contentType = getContentType( headers );

        KieContainerResource container = marshallerHelper.unmarshal( containerPayload, contentType, KieContainerResource.class );

        ServiceResponse<KieContainerResource> response = server.createContainer( id, container );

        Header conversationIdHeader = buildConversationIdHeader(id, server.getServerRegistry(), headers);
        if ( response.getType() == ServiceResponse.ResponseType.SUCCESS ) {
            return createCorrectVariant( response, headers, Status.CREATED, conversationIdHeader );
        }
        return createCorrectVariant( response, headers, Status.BAD_REQUEST );
    }

    @ApiOperation(value="Retrieves container with given id",
            response=ServiceResponse.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @GET
    @Path("containers/{id}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getContainerInfo( @Context HttpHeaders headers, 
            @ApiParam(value = "Container id to be retrieved", required = true) @PathParam("id") String id ) {
        Header conversationIdHeader = buildConversationIdHeader(id, server.getServerRegistry(), headers);
        return createCorrectVariant(server.getContainerInfo(id), headers, conversationIdHeader);
    }

    @ApiOperation(value="Disposes (undeploys) container with given id",
            response=ServiceResponse.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @DELETE
    @Path("containers/{id}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response disposeContainer( @Context HttpHeaders headers, 
            @ApiParam(value = "Container id to be disposed (undeployed)", required = true) @PathParam("id") String id ) {
        Header conversationIdHeader = buildConversationIdHeader(id, server.getServerRegistry(), headers);
        return createCorrectVariant(server.disposeContainer(id), headers, conversationIdHeader);
    }

    @ApiOperation(value="Retrieves stanner information for given container",
            response=ServiceResponse.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @GET
    @Path("containers/{id}/scanner")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getScannerInfo( @Context HttpHeaders headers, 
            @ApiParam(value = "Container id for scanner to be loaded", required = true) @PathParam("id") String id ) {
        Header conversationIdHeader = buildConversationIdHeader(id, server.getServerRegistry(), headers);
        return createCorrectVariant(server.getScannerInfo(id), headers, conversationIdHeader);
    }
    
    @ApiOperation(value="Updates scanner for given container",
            response=ServiceResponse.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @POST
    @Path("containers/{id}/scanner")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response updateScanner( @Context HttpHeaders headers, 
            @ApiParam(value = "Container id for scanner to be updated", required = true) @PathParam("id") String id, 
            @ApiParam(value = "Scanner information given as KieScannerResource type", required = true) String resourcePayload ) {
        String contentType = getContentType(headers);

        KieScannerResource resource = marshallerHelper.unmarshal(resourcePayload, contentType, KieScannerResource.class);
        Header conversationIdHeader = buildConversationIdHeader(id, server.getServerRegistry(), headers);
        return createCorrectVariant(server.updateScanner(id, resource), headers, conversationIdHeader);
    };

    @ApiOperation(value="Retrieves release id of the KIE container with given id",
            response=ServiceResponse.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @GET
    @Path("containers/{id}/release-id")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getReleaseId( @Context HttpHeaders headers, 
            @ApiParam(value = "Container id that release id should be loaded from", required = true) @PathParam("id") String id) {
        Header conversationIdHeader = buildConversationIdHeader(id, server.getServerRegistry(), headers);
        return createCorrectVariant(server.getContainerReleaseId(id), headers, conversationIdHeader);
    }

    @ApiOperation(value="Updates release id of the KIE container with given id to provided release id",
            response=ServiceResponse.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @POST
    @Path("containers/{id}/release-id")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response updateReleaseId( @Context HttpHeaders headers, 
            @ApiParam(value = "Container id that release id should be upgraded", required = true) @PathParam("id") String id, 
            @ApiParam(value = "Release Id to be upgraded to as ReleaseId type", required = true) String releaseIdPayload) {

        String contentType = getContentType(headers);

        ReleaseId releaseId = marshallerHelper.unmarshal(releaseIdPayload, contentType, ReleaseId.class);
        Header conversationIdHeader = buildConversationIdHeader(id, server.getServerRegistry(), headers);
        return createCorrectVariant(server.updateContainerReleaseId(id, releaseId), headers, conversationIdHeader);
    }

    @ApiOperation(value="Retrieves server state - configuration that the server is currently running with",
            response=ServiceResponse.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error") })
    @GET
    @Path("state")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getServerState(@Context HttpHeaders headers) {
        return createCorrectVariant(server.getServerState(), headers);
    }

}
