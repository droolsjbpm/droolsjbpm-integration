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

package org.kie.server.client;

import java.util.List;

import org.kie.server.api.model.definition.ProcessInstanceQueryFilterSpec;
import org.kie.server.api.model.definition.TaskQueryFilterSpec;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.TaskInstance;

/**
 * KIE-Server Client API for the advanced searches provided by the <code>kie-server-services-jbpm-queries</code> extension.
 */	
public interface SearchServicesClient {

	List<ProcessInstance> findProcessInstancesWithFilters(ProcessInstanceQueryFilterSpec filterSpec, Integer page, Integer pageSize);
	
	List<TaskInstance> findHumanTasksWithFilters(TaskQueryFilterSpec filterSpec, Integer page, Integer pageSize);
	
}