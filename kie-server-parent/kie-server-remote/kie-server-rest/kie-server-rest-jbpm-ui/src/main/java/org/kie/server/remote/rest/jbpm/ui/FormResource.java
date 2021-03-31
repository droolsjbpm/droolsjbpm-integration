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


import static org.kie.server.api.rest.RestURI.CASE_FORM_CONTENT_GET_URI;
import static org.kie.server.api.rest.RestURI.CONTAINER_ID;
import static org.kie.server.api.rest.RestURI.FORM_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_FORM_CONTENT_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_FORM_GET_URI;
import static org.kie.server.api.rest.RestURI.PROCESS_ID;
import static org.kie.server.api.rest.RestURI.TASK_FORM_CONTENT_GET_URI;
import static org.kie.server.api.rest.RestURI.TASK_FORM_GET_URI;
import static org.kie.server.api.rest.RestURI.TASK_INSTANCE_ID;
import static org.kie.server.remote.rest.common.util.RestUtils.buildConversationIdHeader;
import static org.kie.server.remote.rest.common.util.RestUtils.createResponse;
import static org.kie.server.remote.rest.common.util.RestUtils.getVariant;
import static org.kie.server.remote.rest.common.util.RestUtils.internalServerError;
import static org.kie.server.remote.rest.common.util.RestUtils.notFound;
import static org.kie.server.remote.rest.common.util.RestUtils.permissionDenied;
import static org.kie.server.remote.rest.common.util.RestUtils.errorMessage;
import static org.kie.server.remote.rest.jbpm.ui.docs.ParameterSamples.JSON;
import static org.kie.server.remote.rest.jbpm.ui.docs.ParameterSamples.PROCESS_FORM_DEF_JSON;
import static org.kie.server.remote.rest.jbpm.ui.docs.ParameterSamples.TASK_FORM_DEF_JSON;

import java.text.MessageFormat;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Variant;

import org.apache.commons.lang3.StringUtils;
import org.jbpm.services.api.DeploymentNotFoundException;
import org.jbpm.services.api.ProcessDefinitionNotFoundException;
import org.jbpm.services.api.TaskNotFoundException;
import org.jbpm.services.task.exception.PermissionDeniedException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.kie.server.remote.rest.common.Header;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.jbpm.ui.FormRendererBase;
import org.kie.server.services.jbpm.ui.FormServiceBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;

@Api(value="Process and task forms")
@Path("server/" + FORM_URI)
public class FormResource {

    private static final Logger logger = LoggerFactory.getLogger(FormResource.class);
    private static int PRETTY_PRINT_INDENT_FACTOR = 4;
    public static final String CONTAINER_NOT_FOUND = "Could not find container \"{0}\"";
    public static final String PROCESS_DEFINITION_NOT_FOUND = "Could not find process definition \"{0}\" in container \"{1}\"";
    public static final String TASK_PERMISSION_ERROR = "User has no permission to see task instance with id \"{0}\"";
    public static final String TASK_INSTANCE_NOT_FOUND = "Could not find task instance with id \"{0}\"";

    private FormServiceBase formServiceBase;
    private FormRendererBase formRendererBase;
    private KieServerRegistry context;

    public FormResource() {

    }

    public FormResource(FormServiceBase formServiceBase, FormRendererBase formRendererBase, KieServerRegistry context) {
        this.formServiceBase = formServiceBase;
        this.formRendererBase = formRendererBase;
        this.context = context;
    }

    @ApiOperation(value="Returns the form information for a specified process definition.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process definition, form or Container Id not found"), 
            @ApiResponse(code = 200, response=String.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=PROCESS_FORM_DEF_JSON)})) })
    @GET
    @Path(PROCESS_FORM_GET_URI)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getProcessForm(@javax.ws.rs.core.Context HttpHeaders headers,
            @ApiParam(value = "container id that process definition belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of process definition that form should be fetched for", required = true, example = "evaluation") @PathParam(PROCESS_ID) String processId,
            @ApiParam(value = "optional language that the form should be found for", required = false) @QueryParam("lang") @DefaultValue("en") String language, 
            @ApiParam(value = "optional filter flag if form should be filtered or returned as is", required = false) @QueryParam("filter") boolean filter,
            @ApiParam(value = "optional type of the form, defaults to ANY so system will find the most current one", required = false) @QueryParam("type") @DefaultValue("ANY") String formType, 
            @ApiParam(value = "optional marshall content flag if the content should be transformed or not, defaults to true", required = false) @QueryParam("marshallContent") @DefaultValue("true") boolean marshallContent) {

        Variant variant = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {

            String response = formServiceBase.getFormDisplayProcess(containerId, processId, language, filter, formType);

            if ( marshallContent ) {
                response = marshallFormContent( response, formType, variant);
            }

            logger.debug("Returning OK response with content '{}'", response);
            return createResponse(response, variant, Response.Status.OK, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), variant, conversationIdHeader);
        } catch (ProcessDefinitionNotFoundException e) {
            return notFound( MessageFormat.format(PROCESS_DEFINITION_NOT_FOUND, processId, containerId), variant, conversationIdHeader);
        } catch (IllegalStateException e) {
            return notFound("Form for process id " + processId + " not found", variant, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), variant, conversationIdHeader);
        }
    }

    @ApiOperation(value="Returns the form information for a specified task instance.")
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task, form or Container Id not found"), 
            @ApiResponse(code = 200, response=String.class, message = "Successful response", examples=@Example(value= {
                    @ExampleProperty(mediaType=JSON, value=TASK_FORM_DEF_JSON)})) })
    @GET
    @Path(TASK_FORM_GET_URI)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getTaskForm(@javax.ws.rs.core.Context HttpHeaders headers,
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance that form should be fetched for", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long taskId,
            @ApiParam(value = "optional language that the form should be found for", required = false) @QueryParam("lang") @DefaultValue("en") String language, 
            @ApiParam(value = "optional filter flag if form should be filtered or returned as is", required = false) @QueryParam("filter") boolean filter,
            @ApiParam(value = "optional type of the form, defaults to ANY so system will find the most current one", required = false) @QueryParam("type") @DefaultValue("ANY") String formType, 
            @ApiParam(value = "optional marshall content flag if the content should be transformed or not, defaults to true", required = false) @QueryParam("marshallContent") @DefaultValue("true") boolean marshallContent,
            @ApiParam(value = "optional user id to be used instead of authenticated user - only when bypass authenticated user is enabled", required = false) @QueryParam("user") String user ) {

        Variant variant = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            String response = formServiceBase.getFormDisplayTask(containerId, taskId, user, language, filter, formType);
            if ( marshallContent ) {
                response = marshallFormContent( response, formType, variant);
            }

            logger.debug("Returning OK response with content '{}'", response);
            return createResponse(response, variant, Response.Status.OK, conversationIdHeader);

        } catch (PermissionDeniedException e) {
            return permissionDenied(MessageFormat.format(TASK_PERMISSION_ERROR, taskId), variant, conversationIdHeader);
        } catch (TaskNotFoundException e) {
            return notFound(MessageFormat.format(TASK_INSTANCE_NOT_FOUND, taskId), variant, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), variant, conversationIdHeader);
        } catch (IllegalStateException e) {
            return notFound("Form for task id " + taskId + " not found", variant, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), variant, conversationIdHeader);
        }
    }
    
    @ApiOperation(value="Returns the rendered form for a specified process definition",
            response=String.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process, form or Container Id not found") })
    @GET
    @Path(PROCESS_FORM_CONTENT_GET_URI)
    @Produces({MediaType.TEXT_HTML})
    public Response getProcessRenderedForm(@javax.ws.rs.core.Context HttpHeaders headers,
            @ApiParam(value = "container id that process definition belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of process definition that form should be fetched for", required = true, example = "evaluation") @PathParam(PROCESS_ID) String processId, 
            @ApiParam(value = "optional renderer name that the form should be rendered with", required = false) @QueryParam("renderer") @DefaultValue("patternfly") String renderer) {
        Variant variant = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
                
        try {
            String renderedForm = formRendererBase.getProcessRenderedForm(renderer, containerId, processId);
            if (renderedForm == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
         
            return Response.ok().entity(renderedForm).build();
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), variant, conversationIdHeader);
        } catch (ProcessDefinitionNotFoundException e) {
            return notFound( MessageFormat.format(PROCESS_DEFINITION_NOT_FOUND, processId, containerId), variant, conversationIdHeader);
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage(), variant, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), variant, conversationIdHeader);
        }
        
    }
    
    @ApiOperation(value="Returns the rendered form for a specified task instance.",
            response=String.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Task, form or Container Id not found") })
    @GET
    @Path(TASK_FORM_CONTENT_GET_URI)
    @Produces({MediaType.TEXT_HTML})
    public Response getTaskRenderedForm(@javax.ws.rs.core.Context HttpHeaders headers,
            @ApiParam(value = "container id that task instance belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of task instance that form should be fetched for", required = true, example = "123") @PathParam(TASK_INSTANCE_ID) Long taskId, 
            @ApiParam(value = "optional renderer name that the form should be rendered with", required = false) @QueryParam("renderer") @DefaultValue("patternfly") String renderer) {
        Variant variant = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
                
        try {
            String renderedForm = formRendererBase.getTaskRenderedForm(renderer, containerId, taskId);
            if (renderedForm == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
         
            return Response.ok().entity(renderedForm).build();
        } catch (PermissionDeniedException e) {
            return permissionDenied(MessageFormat.format(TASK_PERMISSION_ERROR, taskId), variant, conversationIdHeader);
        } catch (TaskNotFoundException e) {
            return notFound(MessageFormat.format(TASK_INSTANCE_NOT_FOUND, taskId), variant, conversationIdHeader);
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), variant, conversationIdHeader);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return notFound(e.getMessage(), variant, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), variant, conversationIdHeader);
        }
        
    }
    
    @ApiOperation(value="Returns the rendered form for a specified case definition.",
            response=String.class, code=200)
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Case, form or Container Id not found") })
    @GET
    @Path(CASE_FORM_CONTENT_GET_URI)
    @Produces({MediaType.TEXT_HTML})
    public Response getCaseRenderedForm(@javax.ws.rs.core.Context HttpHeaders headers,
            @ApiParam(value = "container id that case definition belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId, 
            @ApiParam(value = "identifier of case definition that form should be fetched for", required = true, example = "orderhardware") @PathParam("caseDefId") String caseDefId, 
            @ApiParam(value = "optional renderer name that the form should be rendered with", required = false) @QueryParam("renderer") @DefaultValue("patternfly") String renderer) {
        Variant variant = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
                
        try {
            String renderedForm = formRendererBase.getCaseRenderedForm(renderer, containerId, caseDefId);
            if (renderedForm == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
         
            return Response.ok().entity(renderedForm).build();
        } catch (DeploymentNotFoundException e) {
            return notFound(MessageFormat.format(CONTAINER_NOT_FOUND, containerId), variant, conversationIdHeader);
        } catch (ProcessDefinitionNotFoundException e) {
            return notFound( MessageFormat.format(PROCESS_DEFINITION_NOT_FOUND, caseDefId, containerId), variant, conversationIdHeader);
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage(), variant, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), variant, conversationIdHeader);
        }
        
    }

    protected String marshallFormContent( String formContent, String formType, Variant variant ) throws Exception {

        if ( StringUtils.isEmpty( formContent ) ) {
            return formContent;
        }

        FormServiceBase.FormType actualFormType = FormServiceBase.FormType.fromName(formType);

        String actualContentType = actualFormType.getContentType();

        if ( actualContentType == null ) {
            actualContentType = getMediaTypeForFormContent( formContent );
        }

        if (variant.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE) && !MediaType.APPLICATION_JSON_TYPE.getSubtype().equals( actualContentType )) {
            JSONObject json = XML.toJSONObject(formContent);
            formatJSONResponse(json);
            formContent = json.toString(PRETTY_PRINT_INDENT_FACTOR);
        } else if (variant.getMediaType().equals(MediaType.APPLICATION_XML_TYPE) && !MediaType.APPLICATION_XML_TYPE.getSubtype().equals( actualContentType )) {
            Object json = parseToJSON(formContent);
            formContent = XML.toString(json);
        }

        return formContent;
    }

    protected String getMediaTypeForFormContent( String contentType ) {
        if ( contentType != null ) {
            if ( contentType.startsWith( "{" ) || contentType.startsWith( "[" ) ) {
                return MediaType.APPLICATION_JSON_TYPE.getSubtype();
            }
            if ( contentType.startsWith( "<" ) ) {
                return MediaType.APPLICATION_XML_TYPE.getSubtype();
            }
        }
        return null;
    }

    private void formatJSONResponse(JSONObject json) {
        try {
            JSONObject form = json.getJSONObject("form");
            putPropertyArrayToObject(form);
            Object fields = form.get("field");
            if (fields instanceof JSONArray) {
                for (int i = 0; i < ((JSONArray)fields).length(); ++i) {
                    JSONObject field = ((JSONArray)fields).getJSONObject(i);
                    putPropertyArrayToObject(field);
                }
            } else {
                putPropertyArrayToObject((JSONObject)fields);
            }
        } catch (JSONException e) {
            logger.debug("exception while formatting :: {}", e.getMessage(), e);
        }
    }

    private void putPropertyArrayToObject(JSONObject obj) throws JSONException {
        JSONArray properties = obj.getJSONArray("property");
        for (int j = 0; j<properties.length(); ++j) {
            JSONObject property = properties.getJSONObject(j);
            obj.put(property.getString("name"), property.get("value"));
        }
        obj.remove("property");
    }

    private Object parseToJSON(String content) throws JSONException{
        try {
            return new JSONArray(content);
        } catch (JSONException e) {
            return new JSONObject(content);
        }
    }
}
