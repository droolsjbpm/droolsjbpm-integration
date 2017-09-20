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

package org.kie.server.services.jbpm.search;

import org.jbpm.kie.services.impl.query.SqlQueryDefinition;
import org.jbpm.services.api.query.QueryService;
import org.jbpm.services.api.query.model.QueryDefinition;
import org.jbpm.services.api.query.model.QueryDefinition.Target;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.model.definition.BaseQueryFilterSpec;
import org.kie.server.api.model.definition.TaskQueryFilterSpec;
import org.kie.server.api.model.instance.TaskInstanceList;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.impl.marshal.MarshallerHelper;
import org.kie.server.services.jbpm.search.util.QueryStrategy;
import org.kie.server.services.jbpm.search.util.TaskQueryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskSearchServiceBase extends AbstractSearchServiceBase {

	private static final Logger logger = LoggerFactory.getLogger(TaskSearchServiceBase.class);

	private static final String MAPPER_NAME = "UserTasks";

	private static final String TASK_QUERY_NAME = "getTasksWithFilters";

	private MarshallerHelper marshallerHelper;
	private QueryServiceTemplate queryServiceTemplate;
	private QueryCallback queryCallback;

	public TaskSearchServiceBase(QueryService queryService, KieServerRegistry context) {
		this.queryServiceTemplate = new QueryServiceTemplate(queryService);
	
		this.marshallerHelper = new MarshallerHelper(context);
		
		// Register (or replace) query.
		String taskQuerySource = context.getConfig().getConfigItemValue(KieServerConstants.CFG_PERSISTANCE_DS,
				"java:jboss/datasources/ExampleDS");
		
		QueryStrategy queryStrategy = new TaskQueryStrategy();
		
		this.queryCallback = new QueryCallback() {
			
			@Override
			public QueryStrategy getQueryStrategy() {
				return queryStrategy;
			}
			
			@Override
			public String getQueryName() {
				return TASK_QUERY_NAME;
			}
			
			@Override
			public String getMapperName() {
				return MAPPER_NAME;
			}
		};
		
		QueryDefinition queryDefinition = new SqlQueryDefinition(TASK_QUERY_NAME, taskQuerySource, Target.CUSTOM);
		queryDefinition.setExpression(queryStrategy.getQueryExpression());
		queryService.replaceQuery(queryDefinition);
	}
	
	public TaskInstanceList getHumanTasksWithFilters(Integer page, Integer pageSize, String payload, String marshallingType) {
		
		RequestCallback reqCallback = new RequestCallback() {
			
			@Override
			public BaseQueryFilterSpec getQueryFilterSpec() {
				return marshallerHelper.unmarshal(payload, marshallingType, TaskQueryFilterSpec.class);
			}
			
		};
		
		return queryServiceTemplate.getWithFilters(page, pageSize, queryCallback, reqCallback);
		
	}

	// TODO: Should we also implement a method that supports QueryBuilders???
	

}