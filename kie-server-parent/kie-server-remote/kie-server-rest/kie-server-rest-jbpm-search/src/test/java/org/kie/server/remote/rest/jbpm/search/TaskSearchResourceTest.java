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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.junit.Test;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.model.definition.TaskQueryFilterSpec;
import org.kie.server.api.model.instance.TaskInstanceList;
import org.kie.server.remote.rest.jbpm.search.TaskSearchResource;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.jbpm.search.TaskSearchServiceBase;
import org.skyscreamer.jsonassert.JSONAssert;
import org.xmlunit.matchers.CompareMatcher;

public class TaskSearchResourceTest {

	@Test
	public void testGetHumanTasksWithFiltersXML() throws Exception {
		
		TaskSearchServiceBase tqsbMock = mock(TaskSearchServiceBase.class);
		KieServerRegistry contextMock = mock(KieServerRegistry.class);
		
		//Registry mock needs to return extra classes registered by the extension.
		Set<Class<?>> extraClasses = new HashSet<>();
		extraClasses.add(TaskQueryFilterSpec.class);
		when(contextMock.getExtraClasses()).thenReturn(extraClasses);
		
		when(tqsbMock.getHumanTasksWithFilters(any(), any(), any(), eq("JAXB"))).thenReturn(getTaskInstanceList());
		
		TaskSearchResource tqr = new TaskSearchResource(tqsbMock, contextMock);
		
		MultivaluedMap<String, String> headers = new MultivaluedMapImpl<>();
		headers.put(KieServerConstants.KIE_CONTENT_TYPE_HEADER, Collections.singletonList("JAXB"));
		headers.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(MediaType.APPLICATION_XML));
		headers.put(HttpHeaders.ACCEPT, Collections.singletonList(MediaType.APPLICATION_JSON));
		
		HttpHeaders httpHeaders = new ResteasyHttpHeaders(headers);
		
		Response response = tqr.getHumanTasksWithFilters(httpHeaders, 0, 10, getPayload());
		String responseEntity = (String) response.getEntity();
		
		String expectedResponseEntity = "<task-instance-list/>";
		
		assertThat(responseEntity, CompareMatcher.isIdenticalTo(expectedResponseEntity).ignoreWhitespace());
	}
	
	@Test
	public void testGetHumanTasksWithFilterJSON() throws Exception {
		
		TaskSearchServiceBase tqsbMock = mock(TaskSearchServiceBase.class);
		KieServerRegistry contextMock = mock(KieServerRegistry.class);
		
		//Registry mock needs to return extra classes registered by the extension.
		Set<Class<?>> extraClasses = new HashSet<>();
		extraClasses.add(TaskQueryFilterSpec.class);
		when(contextMock.getExtraClasses()).thenReturn(extraClasses);
		
		when(tqsbMock.getHumanTasksWithFilters(any(), any(), any(), eq("JSON"))).thenReturn(getTaskInstanceList());
		
		TaskSearchResource tqr = new TaskSearchResource(tqsbMock, contextMock);
		
		MultivaluedMap<String, String> headers = new MultivaluedMapImpl<>();
		headers.put(KieServerConstants.KIE_CONTENT_TYPE_HEADER, Collections.singletonList("JSON"));
		headers.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.put(HttpHeaders.ACCEPT, Collections.singletonList(MediaType.APPLICATION_JSON));
		
		HttpHeaders httpHeaders = new ResteasyHttpHeaders(headers);
		
		Response response = tqr.getHumanTasksWithFilters(httpHeaders, 0, 10, getPayload());
		String responseEntity = (String) response.getEntity();
		
		String expectedResponseEntity = new StringBuilder().append("{").append("\"task-instance\" : null").append("}").toString();
		
		JSONAssert.assertEquals(expectedResponseEntity, responseEntity, false);
	}
	
	private static String getPayload() {
		StringBuilder payloadBuilder = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"); 
		payloadBuilder.append("<task-query-filter-spec>");
		payloadBuilder.append("<order-asc>false</order-asc>");
		payloadBuilder.append("<query-params>");
		payloadBuilder.append("<cond-column>DEPLOYMENTID</cond-column>");
		payloadBuilder.append("<cond-operator>EQUALS_TO</cond-operator>");
		payloadBuilder.append("<cond-values xsi:type=\"xs:string\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">simple-project</cond-values>");
		payloadBuilder.append("</query-params>");
		payloadBuilder.append("</task-query-filter-spec>");
		return payloadBuilder.toString();
	}
	
	private static TaskInstanceList getTaskInstanceList() {
		return new TaskInstanceList();
	}
}
