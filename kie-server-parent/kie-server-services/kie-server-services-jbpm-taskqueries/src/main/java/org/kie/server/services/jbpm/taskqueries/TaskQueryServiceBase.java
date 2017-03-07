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

package org.kie.server.services.jbpm.taskqueries;

import static org.kie.server.services.jbpm.ConvertUtils.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpm.kie.services.impl.query.SqlQueryDefinition;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.jbpm.services.api.model.ProcessInstanceWithVarsDesc;
import org.jbpm.services.api.model.UserTaskInstanceDesc;
import org.jbpm.services.api.model.UserTaskInstanceWithVarsDesc;
import org.jbpm.services.api.query.QueryMapperRegistry;
import org.jbpm.services.api.query.QueryNotFoundException;
import org.jbpm.services.api.query.QueryResultMapper;
import org.jbpm.services.api.query.QueryService;
import org.jbpm.services.api.query.model.QueryDefinition;
import org.jbpm.services.api.query.model.QueryDefinition.Target;
import org.jbpm.services.api.query.model.QueryParam;
import org.kie.api.runtime.query.QueryContext;
import org.kie.api.task.model.TaskSummary;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.marshalling.Marshaller;
import org.kie.server.api.marshalling.MarshallerFactory;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieServerConfig;
import org.kie.server.api.model.instance.TaskInstanceList;
import org.kie.server.jbpm.taskqueries.api.model.definition.TaskQueryFilterSpec;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.impl.marshal.MarshallerHelper;
import org.kie.server.services.jbpm.taskqueries.util.TaskQueriesStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskQueryServiceBase {

	private static final Logger logger = LoggerFactory.getLogger(TaskQueryServiceBase.class);

	private static final String MAPPER_NAME = "UserTasksWithCustomVariables";

	private static final String TASK_QUERY_NAME = "getTasksWithFilters";

	private QueryService queryService;
	private MarshallerHelper marshallerHelper;
	private Marshaller jaxbMarshaller;
	private KieServerRegistry context;
	private TaskQueriesStrategy taskQueriesStrategy;

	public TaskQueryServiceBase(QueryService queryService, KieServerRegistry context, TaskQueriesStrategy taskQueriesStrategy) {
		this.queryService = queryService;
		this.context = context;
		
		//We need to add additional JAXB classes (TaskQueryFilterSpec in particular) to be able to unmarshall our response.
		//TODO: Looks like it's impossible to do this with the existing MarshallerHelper, so we need to build our own Marshaller for now.
		this.marshallerHelper = new MarshallerHelper(context);
		
		MarshallerFactory marshallerFactory  = new MarshallerFactory();
		
		//TODO: this is a bit hard-coded. We only support JAXB atm. It would be nicer if we could do this through MarshallerHelper ....
		//We can also simply register marshallers for all 3 formats in a static way .... until MarshallerHelper supports adding custom classes (if it ever will).
		Set<Class<?>> extraClasses = new HashSet<>();
		extraClasses.add(TaskQueryFilterSpec.class);
		jaxbMarshaller = marshallerFactory.getMarshaller(extraClasses, MarshallingFormat.JAXB, this.getClass().getClassLoader());
		
		this.taskQueriesStrategy = taskQueriesStrategy;

		KieServerConfig config = context.getConfig();

		// Register query.
		// TODO: Do we need to do this? Only if multiple threads can register the same query. So, if this class is loaded twice ....
		// I think this class is loaded twice, once for the RESTful endpoint by 'JbpmTaskQueriesRestApplicationComponentsService' and once by the component that loads the JMS stuff.
		// Note that we have not implemented the JMS stuff yet, so atm, this class is loaded only once.
		synchronized (this.getClass()) {
			try {
				// Throws QueryNotFoundException when the query is not found. So in the exception handling logic we register our query.
				queryService.getQuery(TASK_QUERY_NAME);
			} catch (QueryNotFoundException qnfe) {
				String taskQuerySource = config.getConfigItemValue(KieServerConstants.CFG_PERSISTANCE_DS,
						"java:jboss/datasources/ExampleDS");

				QueryDefinition queryDefinition = new SqlQueryDefinition(TASK_QUERY_NAME, taskQuerySource, Target.CUSTOM);
				queryDefinition.setExpression(taskQueriesStrategy.getTaskQueryExpression());
				queryService.registerQuery(queryDefinition);
			}
		}
	}

	public TaskInstanceList getHumanTasksWithFilters(Integer page, Integer pageSize, String payload, String marshallingType) {
		QueryParam[] params = new QueryParam[0];
		Map<String, String> columnMapping = null;
		QueryContext queryContext = buildQueryContext(page, pageSize);
		Map<String, Object> queryParameters = new HashMap<String, Object>();

		if (payload != null & !payload.isEmpty()) {
			logger.debug("About to unmarshall query params from payload: '{}'", payload);
			
			//TaskQueryFilterSpec filterSpec = marshallerHelper.unmarshal(payload, marshallingType, TaskQueryFilterSpec.class);
			TaskQueryFilterSpec filterSpec = jaxbMarshaller.unmarshall(payload, TaskQueryFilterSpec.class);
			
			
			//queryParameters = marshallerHelper.unmarshal(payload, marshallingType, Map.class);

			queryContext.setOrderBy(filterSpec.getOrderBy());
			queryContext.setAscending(filterSpec.isAscending());

			// build parameters for filtering the query
			if (filterSpec.getParameters() != null) {
				params = new QueryParam[filterSpec.getParameters().length];
				int index = 0;
				for (org.kie.server.api.model.definition.QueryParam param : filterSpec.getParameters()) {
					params[index] = new QueryParam(param.getColumn(), param.getOperator(), param.getValue());
					index++;
				}
			}
			
			//TODO: Define the column-mapping based on the db-type and the passed in filters/params.
			columnMapping = taskQueriesStrategy.getColumnMapping(params);
		}
		
		QueryResultMapper<?> resultMapper = QueryMapperRegistry.get().mapperFor(MAPPER_NAME, columnMapping);
		
		logger.debug("About to perform query '{}' with page {} and page size {}", TASK_QUERY_NAME, page, pageSize);

		//Object result = queryService.query(QUERY_NAME, resultMapper, queryContext, params);
		Object result = queryService.query(TASK_QUERY_NAME, resultMapper, queryContext, params);
		
		logger.debug("Result returned from the query {} mapped with {}", result, resultMapper);
		
		// The result should be a TaskInstanceList
		Object actualResult = null;
		if (result instanceof Collection) {
			if (UserTaskInstanceWithVarsDesc.class.isAssignableFrom(resultMapper.getType())) {
				logger.debug("Converting collection of UserTaskInstanceWithVarsDesc to TaskInstanceList");
				actualResult = convertToTaskInstanceWithVarsList((Collection<UserTaskInstanceWithVarsDesc>) result);
			} else if (UserTaskInstanceDesc.class.isAssignableFrom(resultMapper.getType())) {
				logger.debug("Converting collection of UserTaskInstanceDesc to TaskInstanceList");
				actualResult = convertToTaskInstanceList((Collection<UserTaskInstanceDesc>) result);
			} else {
				String message = "The result should be convertible to TaskInstanceList. Current result is of type: '" + result.getClass().getCanonicalName() + "' and the result-mapper is of type: '" + resultMapper.getType() + "'";
				logger.error(message);
				throw new IllegalArgumentException(message);
			}
		} else if (result == null) {
			//Nothing to do here, result is simply empty.
		} else  {
			String message = "The result should be a collection. Current result is of type: '" + result.getClass().getCanonicalName() + "'";
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
		return (TaskInstanceList) transform(result, resultMapper);
	}

	// Get the column-mapping for the given parameters.
	private Map<String, String> getColumnMapping(org.kie.server.api.model.definition.QueryParam[] parameters) {
		Map<String, String> columnMapping = new HashMap<>();

		return columnMapping;
	}

	// TODO: Should we also implement a method that supports QueryBuilders???

	/*
	 * helper methods
	 */

	protected Object transform(Object result, QueryResultMapper resultMapper) {
		Object actualResult = null;
		if (result instanceof Collection) {

			if (ProcessInstanceWithVarsDesc.class.isAssignableFrom(resultMapper.getType())) {

				logger.debug("Converting collection of ProcessInstanceWithVarsDesc to ProcessInstanceList");
				actualResult = convertToProcessInstanceWithVarsList((Collection<ProcessInstanceWithVarsDesc>) result);
			} else if (ProcessInstanceDesc.class.isAssignableFrom(resultMapper.getType())) {

				logger.debug("Converting collection of ProcessInstanceDesc to ProcessInstanceList");
				actualResult = convertToProcessInstanceList((Collection<ProcessInstanceDesc>) result);
			} else if (UserTaskInstanceWithVarsDesc.class.isAssignableFrom(resultMapper.getType())) {

				logger.debug("Converting collection of UserTaskInstanceWithVarsDesc to TaskInstanceList");
				actualResult = convertToTaskInstanceWithVarsList((Collection<UserTaskInstanceWithVarsDesc>) result);
			} else if (UserTaskInstanceDesc.class.isAssignableFrom(resultMapper.getType())) {

				logger.debug("Converting collection of UserTaskInstanceDesc to TaskInstanceList");
				actualResult = convertToTaskInstanceList((Collection<UserTaskInstanceDesc>) result);
			} else if (TaskSummary.class.isAssignableFrom(resultMapper.getType())) {

				logger.debug("Converting collection of TaskSummary to TaskSummaryList");
				actualResult = convertToTaskSummaryList((Collection<TaskSummary>) result);
			} else if (List.class.isAssignableFrom(resultMapper.getType())) {

				logger.debug("Converting collection of List to ArrayList");
				actualResult = new ArrayList((Collection) result);
			} else {

				logger.debug("Convert not supported for custom type {}", resultMapper.getType());
				actualResult = result;
			}

			logger.debug("Actual result after converting is {}", actualResult);
		} else {
			logger.debug("Result is not a collection - {}, skipping any conversion", result);
			actualResult = result;
		}
		return actualResult;
	}

}

/*
 * 
 * public Object queryFilteredWithBuilder(String queryName, String mapper, String builder, Integer page, Integer pageSize, String payload,
 * String marshallingType) { Map<String, String> columnMapping = null; QueryContext queryContext = buildQueryContext(page, pageSize);
 * 
 * 
 * Map<String, Object> queryParameters = new HashMap<String, Object>(); if (payload != null && !payload.isEmpty()) {
 * logger.debug("About to unmarshal query params from payload: '{}'", payload); queryParameters = marshallerHelper.unmarshal(payload,
 * marshallingType, Map.class);
 * 
 * String orderBy = (String) queryParameters.remove(KieServerConstants.QUERY_ORDER_BY); Boolean ascending = (Boolean)
 * queryParameters.remove(KieServerConstants.QUERY_ASCENDING); columnMapping = (Map<String, String>
 * )queryParameters.remove(KieServerConstants.QUERY_COLUMN_MAPPING);
 * 
 * if (orderBy != null) { queryContext.setOrderBy(orderBy); } if (ascending != null) { queryContext.setAscending(ascending); } }
 * QueryResultMapper<?> resultMapper = QueryMapperRegistry.get().mapperFor(mapper, columnMapping); QueryParamBuilderFactory
 * paramBuilderFactory = QueryParamBuilderManager.get().find(builder);
 * 
 * if (paramBuilderFactory == null) { new RuntimeException("No query param builder found for " + builder); }
 * 
 * 
 * logger.debug("About to perform query '{}' with page {} and page size {}", queryName, page, pageSize);
 * 
 * Object result = queryService.query(queryName, resultMapper, queryContext, paramBuilderFactory.newInstance(queryParameters));
 * logger.debug("Result returned from the query {} mapped with {}", result, resultMapper);
 * 
 * return transform(result, resultMapper); }
 */