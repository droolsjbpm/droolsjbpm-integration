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

package org.kie.server.remote.rest.jbpm.search;

import static org.kie.server.api.rest.RestURI.PROCESS_INSTANCES_GET_FILTERED_URI;
import static org.kie.server.remote.rest.common.util.RestUtils.buildConversationIdHeader;
import static org.kie.server.remote.rest.common.util.RestUtils.createCorrectVariant;
import static org.kie.server.remote.rest.common.util.RestUtils.getContentType;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.kie.server.api.model.instance.ProcessInstanceList;
import org.kie.server.remote.rest.common.Header;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.jbpm.search.ProcessInstanceSearchServiceBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("server/" + PROCESS_INSTANCES_GET_FILTERED_URI)
public class ProcessInstanceSearchResource {

private static final Logger logger = LoggerFactory.getLogger(TaskSearchResource.class);

	private ProcessInstanceSearchServiceBase processInstanceQueryServiceBase;
	private KieServerRegistry context;

	public ProcessInstanceSearchResource() {

	}

	public ProcessInstanceSearchResource(ProcessInstanceSearchServiceBase processInstanceQueryServiceBase, KieServerRegistry context) {
		this.processInstanceQueryServiceBase = processInstanceQueryServiceBase;
		this.context = context;
	}


	//TODO: We now only support standard QueryFilterSpec configurations ... Do we also need to support builders???
	@POST
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response getProcessInstancesWithFilters(@Context HttpHeaders headers,
			@QueryParam("page") @DefaultValue("0") Integer page, @QueryParam("pageSize") @DefaultValue("10") Integer pageSize,
			String payload) {

		String type = getContentType(headers);
		// no container id available so only  used to transfer conversation id if given by client.
		Header conversationIdHeader = buildConversationIdHeader("", context, headers);

		ProcessInstanceList result = processInstanceQueryServiceBase.getProcessInstancesWithFilters(page, pageSize, payload, type);

		logger.debug("Returning result of process instance search: {}", result);

		return createCorrectVariant(result, headers, Response.Status.OK, conversationIdHeader);
	}


}
