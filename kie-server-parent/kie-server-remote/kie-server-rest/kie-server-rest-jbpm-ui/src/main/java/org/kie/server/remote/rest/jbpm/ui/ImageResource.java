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

package org.kie.server.remote.rest.jbpm.ui;

import static org.kie.server.api.rest.RestURI.*;
import static org.kie.server.remote.rest.common.util.RestUtils.*;

import java.text.MessageFormat;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.jbpm.services.api.ProcessInstanceNotFoundException;
import org.kie.server.remote.rest.common.Header;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.jbpm.ui.ImageServiceBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(value="Process definition and instance images :: BPM")
@Path("server/" + IMAGE_URI)
public class ImageResource {

    private static final Logger logger = LoggerFactory.getLogger(ImageResource.class);
    private ImageServiceBase imageServiceBase;
    private KieServerRegistry context;

    public ImageResource() {

    }

    public ImageResource(ImageServiceBase imageServiceBase, KieServerRegistry context) {
        this.imageServiceBase = imageServiceBase;
        this.context = context;
    }

    @ApiOperation(value="Retrieves process definition image",
            response=String.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process definition, image or Container Id not found") })
    @GET
    @Path(PROCESS_IMG_GET_URI)
    @Produces({MediaType.APPLICATION_SVG_XML})
    public Response getProcessImage(@javax.ws.rs.core.Context HttpHeaders headers, 
            @ApiParam(value = "container id that process definition belongs to", required = true) @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of the process definition that image should be loaded for", required = true) @PathParam(PROCESS_ID) String processId) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            String svgString = imageServiceBase.getProcessImage(containerId, processId);

            logger.debug("Returning OK response with content '{}'", svgString);
            return createResponse(svgString, v, Response.Status.OK, conversationIdHeader);

        } catch (IllegalArgumentException e) {
            return notFound("Image for process id " + processId + " not found", v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(MessageFormat.format("Unexpected error encountered", e.getMessage()), v, conversationIdHeader);
        }
    }

    @ApiOperation(value="Retrieves process instance image",
            response=String.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process instance, image or Container Id not found") })
    @GET
    @Path(PROCESS_INST_IMG_GET_URI)
    @Produces({MediaType.APPLICATION_SVG_XML})
    public Response getProcessInstanceImage(@javax.ws.rs.core.Context HttpHeaders headers,
            @ApiParam(value = "container id that process instance belongs to", required = true) @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of the process instance that image should be loaded for", required = true) @PathParam(PROCESS_INST_ID) Long procInstId) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            String svgString = imageServiceBase.getActiveProcessImage(containerId, procInstId);

            logger.debug("Returning OK response with content '{}'", svgString);
            return createResponse(svgString, v, Response.Status.OK, conversationIdHeader);

        } catch (ProcessInstanceNotFoundException e) {
            return notFound(MessageFormat.format("Not found", e.getMessage()), v, conversationIdHeader);
        } catch (IllegalArgumentException e) {
            return notFound("Image for process instance id " + procInstId + " not found", v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(MessageFormat.format("Unexpected error encountered", e.getMessage()), v, conversationIdHeader);
        }
    }
}
