/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.integrationtests.jbpm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.exception.KieServicesException;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.admin.ExecutionErrorInstance;
import org.kie.server.api.model.definition.ProcessDefinition;
import org.kie.server.api.model.definition.QueryDefinition;
import org.kie.server.api.model.definition.QueryFilterSpec;
import org.kie.server.api.model.definition.QueryParam;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.ProcessInstanceCustomVars;
import org.kie.server.api.model.instance.TaskInstance;
import org.kie.server.api.model.instance.TaskWithProcessDescription;
import org.kie.server.api.util.QueryFilterSpecBuilder;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.integrationtests.config.TestConfig;
import org.kie.server.integrationtests.shared.KieServerDeployer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.kie.server.api.util.QueryParamFactory.equalsTo;
import static org.kie.server.api.util.QueryParamFactory.or;

public class QueryDataServiceIntegrationTest extends JbpmKieServerBaseIntegrationTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "query-definition-project", "1.0.0.Final");

    private static final String CONTAINER_ID = "query-definition-project";

    private static final long EXTENDED_TIMEOUT = 300000;

    @BeforeClass
    public static void buildAndDeployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProjectFromResource("/kjars-sources/query-definition-project");

        kieContainer = KieServices.Factory.get().newKieContainer(releaseId);

        // Having timeout issues due to kjar dependencies -> raised timeout.
        KieServicesClient client = createDefaultStaticClient(EXTENDED_TIMEOUT);
        ServiceResponse<KieContainerResource> reply = client.createContainer(CONTAINER_ID, new KieContainerResource(CONTAINER_ID, releaseId));
        Assume.assumeTrue(reply.getType().equals(ServiceResponse.ResponseType.SUCCESS));
    }

    @Override
    protected void addExtraCustomClasses(Map<String, Class<?>> extraClasses) throws Exception {
        extraClasses.put(PERSON_CLASS_NAME, Class.forName(PERSON_CLASS_NAME, true, kieContainer.getClassLoader()));
        extraClasses.put(CUSTOM_PARAMETER_CLASS_NAME, Class.forName(CUSTOM_PARAMETER_CLASS_NAME, true, kieContainer.getClassLoader()));
    }

    @Test
    public void testQueryDefinitionsFromKjar() throws Exception {
        String expectedResolvedDS = System.getProperty("org.kie.server.persistence.ds", "jdbc/jbpm-ds");

        QueryDefinition registeredQuery = queryClient.getQuery("first-query");
        assertNotNull(registeredQuery);
        assertEquals("first-query", registeredQuery.getName());
        assertEquals(expectedResolvedDS, registeredQuery.getSource());
        assertEquals("select * from ProcessInstanceLog", registeredQuery.getExpression());
        assertEquals("PROCESS", registeredQuery.getTarget());

        registeredQuery = queryClient.getQuery("second-query");
        assertNotNull(registeredQuery);
        assertEquals("second-query", registeredQuery.getName());
        assertEquals(expectedResolvedDS, registeredQuery.getSource());
        assertEquals("select * from NodeInstanceLog", registeredQuery.getExpression());
        assertEquals("CUSTOM", registeredQuery.getTarget());
    }

    @Test
    public void testCRUDOnQueryDefinition() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        QueryDefinition query = createQueryDefinition("PROCESS");
        try {

            QueryDefinition registered = queryClient.registerQuery(query);
            assertNotNull(registered);
            assertEquals(query.getName(), registered.getName());
            assertEquals(query.getSource(), registered.getSource());
            assertEquals(query.getExpression(), registered.getExpression());
            assertEquals(query.getTarget(), registered.getTarget());
            assertNotNull(registered.getColumns());
            assertEquals(registered.getColumns().size(), 18);

            List<QueryDefinition> queries = queryClient.getQueries(0, 100);

            QueryDefinition registeredQuery = queries.stream().filter(q -> q.getName().equals("allProcessInstances")).findFirst().orElse(null);
            assertNotNull(registeredQuery);
            assertEquals(query.getName(), registeredQuery.getName());
            assertEquals(query.getSource(), registeredQuery.getSource());
            assertEquals(query.getExpression(), registeredQuery.getExpression());
            assertEquals(query.getTarget(), registeredQuery.getTarget());

            registeredQuery = queryClient.getQuery(query.getName());

            assertNotNull(registeredQuery);
            assertEquals(query.getName(), registeredQuery.getName());
            assertEquals(query.getSource(), registeredQuery.getSource());
            assertEquals(query.getExpression(), registeredQuery.getExpression());
            assertEquals(query.getTarget(), registeredQuery.getTarget());

        } finally {
            abortProcessInstances(processInstanceIds);
            queryClient.unregisterQuery(query.getName());
        }

    }

    @Test
    public void testGetProcessInstancesWithQueryDataService() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        QueryDefinition query = createQueryDefinition("CUSTOM");
        try {

            queryClient.registerQuery(query);

            List<ProcessInstance> instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI, 0, 10, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(5, instances.size());

            List<Long> found = collectInstances(instances);
            assertEquals(processInstanceIds, found);

            instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI, 0, 3, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(3, instances.size());

            instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI, "status", 1, 3, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(2, instances.size());

        } finally {
            abortProcessInstances(processInstanceIds);
            queryClient.unregisterQuery(query.getName());
        }

    }

    @Test
    public void testGetProcessInstancesWithVariablesQueryDataService() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));
        parameters.put("nullAccepted", true);

        List<Long> processInstanceIds = createProcessInstances(parameters);

        final QueryDefinition query = getProcessInstanceWithVariablesQueryDefinition();
        try {

            queryClient.registerQuery(query);

            final List<ProcessInstance> instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI_WITH_VARS, 0, 20, ProcessInstance.class);
            assertNotNull(instances);
            final List<Long> found = collectInstances(instances);
            assertEquals(processInstanceIds, found);

            for (ProcessInstance instance : instances) {
                final Map<String, Object> variables = instance.getVariables();
                assertNotNull(variables);
                assertEquals(4, variables.size());

                assertEquals(TestConfig.getUsername(), variables.get("initiator"));
                assertEquals("waiting for signal", variables.get("stringData"));
                assertEquals("Person{name='john'}", variables.get("personData"));
                assertEquals("true", variables.get("nullAccepted"));
            }

        } finally {
            abortProcessInstances(processInstanceIds);
            queryClient.unregisterQuery(query.getName());
        }

    }

    @Test
    public void testGetProcessInstancesFilteredWithVariablesQueryDataService() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        final QueryDefinition query = getProcessInstanceWithVariablesQueryDefinition();
        try {

            queryClient.registerQuery(query);

            QueryFilterSpec filterSpec = new QueryFilterSpecBuilder().greaterThan("processinstanceid", processInstanceIds.get(3)).get();

            List<ProcessInstance> instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI_WITH_VARS, filterSpec, 0, 10, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(1, instances.size());

            for (ProcessInstance instance : instances) {
                final Map<String, Object> variables = instance.getVariables();
                assertNotNull(variables);
                assertEquals(4, variables.size());

                assertEquals(TestConfig.getUsername(), variables.get("initiator"));
                assertEquals("waiting for signal", variables.get("stringData"));
                assertEquals("Person{name='john'}", variables.get("personData"));
                assertEquals("true", variables.get("nullAccepted"));
            }

        } finally {
            abortProcessInstances(processInstanceIds);
            queryClient.unregisterQuery(query.getName());
        }

    }

    @Test
    public void testGetProcessInstancesWithQueryDataServiceUsingCustomMapper() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        QueryDefinition query = createQueryDefinition("CUSTOM");
        try {

            queryClient.registerQuery(query);

            List<ProcessInstance> instances = queryClient.query(query.getName(), "CustomMapper", 0, 10, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(5, instances.size());

            List<Long> found = collectInstances(instances);
            assertEquals(processInstanceIds, found);

            instances = queryClient.query(query.getName(), "CustomMapper", 0, 3, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(3, instances.size());

            instances = queryClient.query(query.getName(), "CustomMapper", "status", 1, 3, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(2, instances.size());

        } finally {
            abortProcessInstances(processInstanceIds);
            queryClient.unregisterQuery(query.getName());
        }

    }

    @Test
    public void testGetProcessInstancesWithQueryDataServiceUsingCustomQueryBuilder() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        QueryDefinition query = createQueryDefinition("CUSTOM");
        try {

            queryClient.registerQuery(query);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("min", processInstanceIds.get(4));
            params.put("max", processInstanceIds.get(0));

            List<ProcessInstance> instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI, "test", params, 0, 10, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(2, instances.size());

        } finally {
            abortProcessInstances(processInstanceIds);
            queryClient.unregisterQuery(query.getName());
        }

    }

    @Test
    public void testGetProcessInstancesWithQueryDataServiceUsingCustomQueryBuilderAndCustomParameters() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        QueryDefinition query = createQueryDefinition("CUSTOM");
        try {

            queryClient.registerQuery(query);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("customparams",
                       Arrays.asList(createCustomParameterInstance("min", processInstanceIds.get(4)),
                                     createCustomParameterInstance("max", processInstanceIds.get(0))));

            List<ProcessInstance> instances = queryClient.query(CONTAINER_ID, query.getName(), QueryServicesClient.QUERY_MAP_PI, "test", params, 0, 10, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(2, instances.size());

        } finally {
            abortProcessInstances(processInstanceIds);
            queryClient.unregisterQuery(query.getName());
        }

    }

    @Test
    public void testGetProcessInstancesWithQueryDataServiceRawMapper() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        QueryDefinition query = createQueryDefinition("CUSTOM");
        try {

            queryClient.registerQuery(query);

            List<List> instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_RAW, 0, 10, List.class);
            assertNotNull(instances);
            assertEquals(5, instances.size());

            for (List row : instances) {
                assertEquals(18, row.size());
            }

            instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_RAW, 0, 3, List.class);
            assertNotNull(instances);
            assertEquals(3, instances.size());

            instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_RAW, "status", 1, 3, List.class);
            assertNotNull(instances);
            assertEquals(2, instances.size());

        } finally {
            abortProcessInstances(processInstanceIds);
            queryClient.unregisterQuery(query.getName());
        }

    }

    @Test
    public void testQueryDataServiceReplaceQuery() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(CONTAINER_ID));

        Long pid = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        QueryDefinition query = new QueryDefinition();
        query.setName("getTasksByState");
        query.setSource(System.getProperty("org.kie.server.persistence.ds", "jdbc/jbpm-ds"));
        query.setExpression("select * from AuditTaskImpl where status = 'Reserved'");
        query.setTarget("CUSTOM");
        try {

            queryClient.registerQuery(query);

            List<TaskInstance> tasks = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_TASK, 0, 10, TaskInstance.class);
            assertNotNull(tasks);
            assertEquals(1, tasks.size());
            Long taskId = tasks.get(0).getId();

            query.setExpression("select * from AuditTaskImpl where status = 'InProgress'");

            QueryDefinition replaced = queryClient.replaceQuery(query);
            
            assertNotNull(replaced);
            assertEquals(query.getName(), replaced.getName());
            assertEquals(query.getSource(), replaced.getSource());
            assertEquals(query.getExpression(), replaced.getExpression());
            assertEquals(query.getTarget(), replaced.getTarget());
            assertNotNull(replaced.getColumns());
            assertEquals(19, replaced.getColumns().size());

            tasks = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_TASK, 0, 10, TaskInstance.class);
            assertNotNull(tasks);
            assertEquals(0, tasks.size());

            taskClient.startTask(CONTAINER_ID, taskId, USER_YODA);

            tasks = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_TASK, 0, 10, TaskInstance.class);
            assertNotNull(tasks);
            assertEquals(1, tasks.size());
            assertEquals(taskId, tasks.get(0).getId());
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, pid);
            queryClient.unregisterQuery(query.getName());
        }
    }

    @Test
    public void testInvalidQuery() {
        QueryDefinition query = new QueryDefinition();
        query.setName("invalidQuery");
        query.setSource(System.getProperty("org.kie.server.persistence.ds", "jdbc/jbpm-ds"));
        query.setExpression("this is an invalid query");
        query.setTarget("CUSTOM");

        try {
            assertClientException(
                    () -> queryClient.registerQuery(query),
                    500,
                    "Can't get metadata on specified data set: invalidQuery");

            assertClientException(
                    () -> queryClient.getQuery(query.getName()),
                    404,
                    "Could not find query definition with name \"" + query.getName() + "\"",
                    "Query invalidQuery not found");

        } catch (Throwable e) {
            try {
                queryClient.unregisterQuery(query.getName());
            } catch (KieServicesException ex) {
                // expected in case the query wasn't registered
            }
            throw e;
        }
    }

    @Test
    public void testCRUDOnQueryDefinitionWithDSAsProperty() throws Exception {       
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance("john"));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        QueryDefinition query = new QueryDefinition();
        query.setName("allProcessInstances");
        query.setSource("${org.kie.server.persistence.ds}");
        query.setExpression("select * from ProcessInstanceLog where status = 1");
        query.setTarget("PROCESS");
        try {

            queryClient.registerQuery(query);

            List<QueryDefinition> queries = queryClient.getQueries(0, 100);

            QueryDefinition registeredQuery = queries.stream().filter(q -> q.getName().equals("allProcessInstances")).findFirst().orElse(null);
            assertNotNull(registeredQuery);
            assertEquals(query.getName(), registeredQuery.getName());
            assertEquals(query.getSource(), registeredQuery.getSource());
            assertEquals(query.getExpression(), registeredQuery.getExpression());
            assertEquals(query.getTarget(), registeredQuery.getTarget());

            registeredQuery = queryClient.getQuery(query.getName());

            assertNotNull(registeredQuery);
            assertEquals(query.getName(), registeredQuery.getName());
            assertEquals(query.getSource(), registeredQuery.getSource());
            assertEquals(query.getExpression(), registeredQuery.getExpression());
            assertEquals(query.getTarget(), registeredQuery.getTarget());
            
            List<ProcessInstance> instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI, 0, 10, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(5, instances.size());

        } finally {
            abortProcessInstances(processInstanceIds);
            queryClient.unregisterQuery(query.getName());
        }
    }

    @Test
    public void testGetFilteredProcessInstancesWithQueryDataService() throws Exception {
        // don't run the test on local server as it does not properly support authentication
        assumeFalse(TestConfig.isLocalServer());

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance("john"));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        QueryDefinition query = new QueryDefinition();
        query.setName("allProcessInstancesForUser");
        query.setSource(System.getProperty("org.kie.server.persistence.ds", "jdbc/jbpm-ds"));
        query.setExpression("select * from ProcessInstanceLog where status = 1");
        query.setTarget("FILTERED_PROCESS");
        try {

            queryClient.registerQuery(query);
            // default user (yoda) does not have engineering role so should not be able to find any instances
            List<ProcessInstance> instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI, 0, 10, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(0, instances.size());

            // switch to john user who has engineering role
            changeUser(USER_JOHN);

            instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI, 0, 10, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(5, instances.size());

        } finally {
            abortProcessInstances(processInstanceIds);
            queryClient.unregisterQuery(query.getName());
            changeUser(TestConfig.getUsername());
        }

    }

    @Test
    public void testGetProcessInstancesWithQueryDataServiceUsingCustomQueryBuilderAndFilterSpec() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        QueryDefinition query = createQueryDefinition("CUSTOM");
        try {

            queryClient.registerQuery(query);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("min", processInstanceIds.get(4));
            params.put("max", processInstanceIds.get(0));
            params.put(KieServerConstants.QUERY_ORDER_BY, "processInstanceId");
            params.put(KieServerConstants.QUERY_ASCENDING, false);

            List<ProcessInstance> instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI, "test", params, 0, 10, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(2, instances.size());

            long pi1 = instances.get(0).getId();
            long pi2 = instances.get(1).getId();
            // since sort order is descending first should be instance id which is bigger then second
            assertTrue(pi1 > pi2);

        } finally {
            abortProcessInstances(processInstanceIds);
            queryClient.unregisterQuery(query.getName());
        }

    }

    @Test
    public void testQueryDataServiceUsingCustomQueryBuilderFilterSpecWithOrderByClause() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        QueryDefinition query = createQueryDefinition("CUSTOM");
        try {

            queryClient.registerQuery(query);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("min", processInstanceIds.get(1));
            params.put("max", processInstanceIds.get(0));
            params.put(KieServerConstants.QUERY_ORDER_BY_CLAUSE, "processId asc , processInstanceId desc");

            List<ProcessInstance> instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI, "test", params, 0, 10, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(5, instances.size());

            long pi1 = instances.get(0).getId();
            long pi2 = instances.get(1).getId();
            // since sort order is descending first should be instance id which is bigger then second
            assertTrue(pi1 + " not greater than " + pi2, pi1 > pi2);

        } finally {
            abortProcessInstances(processInstanceIds);
            queryClient.unregisterQuery(query.getName());
        }

    }

    @Test
    public void testGetProcessInstancesWithQueryDataServiceUsingCustomQueryBuilderAndColumnMapping() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        QueryDefinition query = getProcessInstanceWithVariablesQueryDefinition();
        try {

            queryClient.registerQuery(query);

            Map<String, String> columnMapping = new HashMap<>();
            columnMapping.put("variableId", "String");
            columnMapping.put("value", "String");

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("min", processInstanceIds.get(4));
            params.put("max", processInstanceIds.get(0));
            params.put(KieServerConstants.QUERY_ORDER_BY, "processInstanceId");
            params.put(KieServerConstants.QUERY_ASCENDING, false);
            params.put(KieServerConstants.QUERY_COLUMN_MAPPING, columnMapping);

            List<ProcessInstance> instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI_WITH_CUSTOM_VARS, "test", params, 0, 10, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(2, instances.size());

            long pi1 = instances.get(0).getId();
            long pi2 = instances.get(1).getId();
            // since sort order is descending first should be instance id which is bigger then second
            assertTrue(pi1 > pi2);

            for (ProcessInstance instance : instances) {
                final Map<String, Object> variables = instance.getVariables();
                assertNotNull(variables);
                assertEquals(2, variables.size());

                assertTrue(variables.containsKey("variableId"));
                assertTrue(variables.containsKey("value"));
            }

        } finally {
            abortProcessInstances(processInstanceIds);
            queryClient.unregisterQuery(query.getName());
        }

    }

    /*
     * JBPM-5468 Test case for using ORDER BY clause with query builder and columm mapper.
     */
    @Test
    public void testQueryDataServiceUsingCustomQueryBuilderColumnMappingWithOrderByClause() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        QueryDefinition query = getProcessInstanceWithVariablesQueryDefinition();
        try {

            queryClient.registerQuery(query);

            Map<String, String> columnMapping = new HashMap<>();
            columnMapping.put("variableId", "String");
            columnMapping.put("value", "String");

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("min", processInstanceIds.get(4));
            params.put("max", processInstanceIds.get(0));
            params.put(KieServerConstants.QUERY_ORDER_BY_CLAUSE, "processInstanceId desc");
            params.put(KieServerConstants.QUERY_COLUMN_MAPPING, columnMapping);

            List<ProcessInstance> instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI_WITH_CUSTOM_VARS, "test", params, 0, 10, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(2, instances.size());

            long pi1 = instances.get(0).getId();
            long pi2 = instances.get(1).getId();
            // since sort order is descending first should be instance id which is bigger then second
            assertTrue(pi1 + " not greater than " + pi2, pi1 > pi2);

            for (ProcessInstance instance : instances) {
                final Map<String, Object> variables = instance.getVariables();
                assertNotNull(variables);
                assertEquals(2, variables.size());

                assertTrue(variables.containsKey("variableId"));
                assertTrue(variables.containsKey("value"));
            }

        } finally {
            abortProcessInstances(processInstanceIds);
            queryClient.unregisterQuery(query.getName());
        }

    }

    /*
     * JBPM-5468 Client requests with 'sortBy' request parameter can now use full ORDER BY clause syntax.
     */
    @Test
    public void testGetProcessInstancesWithOrderByClause() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        QueryDefinition query = createQueryDefinition("CUSTOM");
        try {

            queryClient.registerQuery(query);

            List<ProcessInstance> instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI, "processId asc, processInstanceId desc", 0, 10, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(5, instances.size());

            List<Long> found = collectInstances(instances);
            assertEquals(processInstanceIds, found);

            long pi1 = instances.get(0).getId();
            long pi2 = instances.get(1).getId();
            // since sort order is descending first should be instance id which is bigger then second
            assertTrue(pi1 + " not greater than " + pi2, pi1 > pi2);

        } finally {
            abortProcessInstances(processInstanceIds);
            queryClient.unregisterQuery(query.getName());
        }

    }

    @Test
    public void testErrorHandlingFailedToSignal() throws Exception {

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("nullAccepted", false);

        QueryDefinition query = createErrorsQueryDefinition();

        Long processInstanceId = null;
        try {
            queryClient.registerQuery(query);
            processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters);

            List<ExecutionErrorInstance> errors = processAdminClient.getErrorsByProcessInstance(CONTAINER_ID, processInstanceId , false, 0, 10);
            assertNotNull(errors);
            assertEquals(0, errors.size());

            try {
                processClient.signalProcessInstance(CONTAINER_ID, processInstanceId, "Signal1", null);
                fail("Process instance signal should fail as it provides null as event");
            } catch (KieServicesException e) {
                // expected
            }

            errors = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_ERROR, 0, 10, ExecutionErrorInstance.class);
            errors = filterErrorsByProcessInstanceId(errors, processInstanceId);
            assertNotNull(errors);
            assertEquals(1, errors.size());
            ExecutionErrorInstance errorInstance = errors.get(0);
            assertNotNull(errorInstance.getErrorId());
            assertNull(errorInstance.getError());
            assertNotNull(errorInstance.getProcessInstanceId());
            assertNotNull(errorInstance.getActivityId());
            assertNotNull(errorInstance.getErrorDate());

            assertEquals(CONTAINER_ID, errorInstance.getContainerId());
            assertEquals(PROCESS_ID_SIGNAL_PROCESS, errorInstance.getProcessId());
            assertEquals("Signal 1 data", errorInstance.getActivityName());

            assertFalse(errorInstance.isAcknowledged());
            assertNull(errorInstance.getAcknowledgedAt());
            assertNull(errorInstance.getAcknowledgedBy());

            errors = processAdminClient.getErrorsByProcessInstanceAndNode(CONTAINER_ID, processInstanceId, "Signal 1 data", false, 0, 10);
            assertNotNull(errors);
            assertEquals(1, errors.size());
            ExecutionErrorInstance errorInstance2 = errors.get(0);
            assertEquals(errorInstance.getErrorId(), errorInstance2.getErrorId());

            processAdminClient.acknowledgeError(CONTAINER_ID, errorInstance.getErrorId());

            errors = processAdminClient.getErrorsByProcessInstance(CONTAINER_ID, processInstanceId, false, 0, 10);
            assertNotNull(errors);
            assertEquals(0, errors.size());

            errorInstance = processAdminClient.getError(CONTAINER_ID, errorInstance.getErrorId());
            assertNotNull(errorInstance);
            assertNotNull(errorInstance.getErrorId());
            assertTrue(errorInstance.isAcknowledged());
            assertNotNull(errorInstance.getAcknowledgedAt());
            assertEquals(USER_YODA, errorInstance.getAcknowledgedBy());
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            fail(e.getMessage());
        } finally {
            queryClient.unregisterQuery(query.getName());
            if (processInstanceId != null) {
                processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
            }
        }
    }
    
    @Test
    public void testGetTaskInstancesUsingUserTaskPOMapper() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(CONTAINER_ID));

        Long pid = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        QueryDefinition query = new QueryDefinition();
        query.setName("jbpmHumanTasksPO");
        query.setSource(System.getProperty("org.kie.server.persistence.ds", "jdbc/jbpm-ds"));
        query.setExpression("select t.actualowner_id as actualowner, t.CREATEDBY_ID as createdby, t.CREATEDON as CREATEDON, t.EXPIRATIONTIME as expirationDate, " +
                "t.id as TASKID, t.name as NAME, t.priority as PRIORITY, t.PROCESSINSTANCEID as PROCESSINSTANCEID, t.PROCESSID as PROCESSID, t.STATUS as STATUS,  " +
                "po.entity_id as POTOWNER, t.FORMNAME AS FORMNAME, p.processinstancedescription as PROCESSINSTANCEDESCRIPTION, t.subject as SUBJECT, t.deploymentid as DEPLOYMENTID " +
                "from TASK t " +
                "inner join PEOPLEASSIGNMENTS_POTOWNERS po on t.id=po.task_id " +
                "inner join PROCESSINSTANCELOG p on t.processinstanceid = p.processinstanceid " +
                "WHERE t.processinstanceid = " + pid);
        query.setTarget("CUSTOM");
        
        try {

            queryClient.registerQuery(query);

            List<TaskWithProcessDescription> tasks = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_TASK_WITH_PO, 0, 10, TaskWithProcessDescription.class);
            assertNotNull(tasks);
            assertEquals(1, tasks.size());

            TaskWithProcessDescription task = tasks.get(0);
            assertEquals("First task", task.getName());
            assertEquals(1, task.getPotentialOwners().size());
            assertEquals("yoda", task.getPotentialOwners().get(0));
            assertEquals("Very nice process description", task.getProcessInstanceDescription());
            
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, pid);
            queryClient.unregisterQuery(query.getName());
        }

    }

    @Test
    public void testGetTaskInstancesUsingUserTaskWithModificationMapper() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(CONTAINER_ID));

        Long pid = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        QueryDefinition query = new QueryDefinition();
        query.setName("jbpmGetTaskWithPO");
        query.setSource(System.getProperty("org.kie.server.persistence.ds", "jdbc/jbpm-ds"));
        query.setExpression("select t.id as TASKID, t.name as NAME,  t.FORMNAME AS FORMNAME, t.subject as SUBJECT, " +
                "t.actualowner_id as ACTUALOWNER, po.entity_id as POTOWNER, p.processinstancedescription as PROCESSINSTANCEDESCRIPTION, t.CREATEDON as CREATEDON, " +
                "t.CREATEDBY_ID as CREATEDBY, t.EXPIRATIONTIME as EXPIRATIONTIME, " +
                "(select max(logtime) from taskevent where processinstanceid = t.processinstanceid and taskid = t.id) as lastmodificationdate, " +
                            "(select a.userid from taskevent a left join taskevent b on a.id < b.id WHERE b.id IS NULL and a.processinstanceid = t.processinstanceid and a.taskid = t.id) as lastmodificationuser, " +
                "t.priority as PRIORITY, t.STATUS as STATUS, t.PROCESSINSTANCEID as PROCESSINSTANCEID, t.PROCESSID as PROCESSID, " +
                "t.deploymentid as DEPLOYMENTID, d.name as TVNAME, d.type as TVTYPE, d.value as TVVALUE " +
                "from TASK t " +
                "inner join PEOPLEASSIGNMENTS_POTOWNERS po on t.id=po.task_id " +
                "inner join PROCESSINSTANCELOG p on t.processinstanceid = p.processinstanceid " +
                "inner join TASKVARIABLEIMPL d on t.id=d.taskid " +
                "WHERE t.processinstanceid = " + pid);
        query.setTarget("CUSTOM");
        
        try {

            queryClient.registerQuery(query);

            List<TaskWithProcessDescription> tasks = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_TASK_WITH_MODIF, 0, 10, TaskWithProcessDescription.class);
            assertNotNull(tasks);
            assertEquals(1, tasks.size());

            TaskWithProcessDescription task = tasks.get(0);
            assertEquals("First task", task.getName());
            assertEquals(1, task.getPotentialOwners().size());
            assertEquals("yoda", task.getPotentialOwners().get(0));
            assertEquals("Very nice process description", task.getProcessInstanceDescription());
            assertEquals("waiting for signal", task.getInputData().get("_string"));
            Object person = task.getInputData().get("_person");
            assertEquals(person, "Person{name='" + CONTAINER_ID + "'}");
            assertNotNull(task.getLastModificationDate());
            assertNotNull(task.getLastModificationUser());

        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, pid);
            queryClient.unregisterQuery(query.getName());
        }

    }

    @Test
    public void testGetTasksWithPotentialOwnerDoubleGroup() throws Exception {
        changeUser(USER_JOHN);
        Long pidOne = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK_DOUBLE_GROUP);
        Long pidTwo = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK_DOUBLE_GROUP);

        QueryDefinition query = getTasksWithPotentialOwnerQueryDefinition();

        try {
            queryClient.registerQuery(query);
            
            List<org.kie.server.api.model.instance.TaskSummary> test = taskClient.findTasksAssignedAsPotentialOwner(USER_JOHN, 0, 10);
            assertNotNull(test);
            assertEquals(2, test.size());
            
            QueryFilterSpec filterSpec = new QueryFilterSpecBuilder().in("processInstanceId", Arrays.asList(pidOne, pidTwo)).get();
            
            List<TaskInstance> tasks = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_TASK, filterSpec, 0, 10, TaskInstance.class);
            assertNotNull(tasks);
            assertEquals(2, tasks.size());
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, pidOne);
            processClient.abortProcessInstance(CONTAINER_ID, pidTwo);
            queryClient.unregisterQuery(query.getName());
            changeUser(USER_YODA);
        }
    }
    
    @Test
    public void testGetProcessInstanceCustomMapper() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(CONTAINER_ID));

        Long pid = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        QueryDefinition query = new QueryDefinition();
        query.setName("jbpmProcessWithVarsSearch");
        query.setSource(System.getProperty("org.kie.server.persistence.ds", "jdbc/jbpm-ds"));
        query.setExpression("select p. PROCESSINSTANCEID, p.PROCESSID,  p.PROCESSNAME, p.PROCESSVERSION, p.STATUS,  " +
                            "p.EXTERNALID, pr.STARTDATE as START_DATE,   p.USER_IDENTITY, p.PROCESSINSTANCEDESCRIPTION, " +
                            "p.CORRELATIONKEY, p.PARENTPROCESSINSTANCEID, pr.LASTMODIFICATIONDATE, var.variableId, var.value " +
                            "from PROCESSINSTANCELOG p inner join PROCESSINSTANCEINFO pr on p.PROCESSINSTANCEID = pr.INSTANCEID " +
                            "inner join (select v.processInstanceId, v.variableId, v.value from VariableInstanceLog v " +
                            "where v.id = (select MAX(vil.id) from VariableInstanceLog vil " +
                            "where v.variableId = vil.variableId and v.processInstanceId = vil.processInstanceId)) var " +
                            "on  p.PROCESSINSTANCEID = var.PROCESSINSTANCEID");

        query.setTarget("CUSTOM");
        
        try {

            queryClient.registerQuery(query);

            List<ProcessInstanceCustomVars> instance = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI_CUSTOM, 0, 10, ProcessInstanceCustomVars.class);
            assertNotNull(instance);
            assertEquals(1, instance.size());
            
            final Map<String, Object> variables = instance.get(0).getVariables();
            assertNotNull(variables);
            assertEquals(3, variables.size());

            assertEquals(TestConfig.getUsername(), variables.get("initiator"));
            assertEquals("waiting for signal", variables.get("stringData"));
            assertEquals(parameters.get("personData").toString(), variables.get("personData"));
            
        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, pid);
            queryClient.unregisterQuery(query.getName());
        }

    }
    
    @Test
    public void testGetProcessInstanceCustomWithoutVarsMapper() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(CONTAINER_ID));

        Long pid = processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters);

        QueryDefinition query = new QueryDefinition();
        query.setName("jbpmProcessSearch");
        query.setSource(System.getProperty("org.kie.server.persistence.ds", "jdbc/jbpm-ds"));
        query.setExpression("select p.PROCESSINSTANCEID, p.PROCESSID,  p.PROCESSNAME, p.PROCESSVERSION, p.STATUS,  p.EXTERNALID, " +
                            "pr.STARTDATE as START_DATE,   p.USER_IDENTITY, p.PROCESSINSTANCEDESCRIPTION, p.CORRELATIONKEY, " +
                            "p.PARENTPROCESSINSTANCEID, pr.LASTMODIFICATIONDATE from PROCESSINSTANCELOG p " +
                            "inner join PROCESSINSTANCEINFO pr on p.PROCESSINSTANCEID = pr.INSTANCEID");

        query.setTarget("CUSTOM");
        
        try {

            queryClient.registerQuery(query);

            List<ProcessInstanceCustomVars> instance = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI_CUSTOM, 0, 10, ProcessInstanceCustomVars.class);
            assertNotNull(instance);
            assertEquals(1, instance.size());
            
            assertNull(instance.get(0).getVariables());

        } finally {
            processClient.abortProcessInstance(CONTAINER_ID, pid);
            queryClient.unregisterQuery(query.getName());
        }

    }

    @Test
    public void testQueryDataServiceUsingComplexCustomQueryBuilderFilterSpecWithOrderByClause() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);
        final QueryDefinition query = getProcessInstanceWithVariablesQueryDefinition();
        try {

            queryClient.registerQuery(query);

            QueryParam expression = or(equalsTo("PROCESSINSTANCEID", processInstanceIds.get(0)), equalsTo("PROCESSINSTANCEID", processInstanceIds.get(1)));
            QueryFilterSpec filterSpec = QueryFilterSpec.builder()
                                                        .where(expression)
                                                        .get();

            List<ProcessInstance> instances = queryClient.query(query.getName(), QueryServicesClient.QUERY_MAP_PI_WITH_VARS, filterSpec, 0, 10, ProcessInstance.class);
            assertNotNull(instances);
            assertEquals(2, instances.size());


        } finally {
            abortProcessInstances(processInstanceIds);
            queryClient.unregisterQuery(query.getName());
        }


    }
    
    @Test
    public void testDefaultQueryJbpmProcessInstances() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("stringData", "waiting for signal");
        parameters.put("personData", createPersonInstance(USER_JOHN));

        List<Long> processInstanceIds = createProcessInstances(parameters);

        QueryDefinition registeredQuery = queryClient.getQuery("jbpmProcessInstances");
        
        try {
            List<ProcessInstance> instances = queryClient.query(registeredQuery.getName(), QueryServicesClient.QUERY_MAP_PI, 0, 10, ProcessInstance.class);
            assertNotNull(instances);
        } finally {
            abortProcessInstances(processInstanceIds);
        }
    }
    
    @Test
    public void testDefaultQueryJbpmHumanTasksWithAdmin() {
        QueryDefinition registeredQuery = queryClient.getQuery("jbpmHumanTasksWithAdmin");
        
        List<TaskInstance> tasks = queryClient.query(registeredQuery.getName(), QueryServicesClient.QUERY_MAP_TASK, 0, 10, TaskInstance.class);
        assertNotNull(tasks);
    }

    protected QueryDefinition getProcessInstanceWithVariablesQueryDefinition() {
        final QueryDefinition query = new QueryDefinition();
        query.setName("allProcessInstancesWithVars");
        query.setSource(System.getProperty("org.kie.server.persistence.ds", "jdbc/jbpm-ds"));
        query.setExpression("select pil.*, v.variableId, v.value " + "from ProcessInstanceLog pil " + "inner join (select vil.processInstanceId ,vil.variableId, max(vil.ID) maxvilid  from VariableInstanceLog vil " + "group by vil.processInstanceId, vil.variableId) x on (x.processInstanceId = pil.processInstanceId) " + "inner join VariableInstanceLog v " + "on (v.variableId = x.variableId  and v.id = x.maxvilid and v.processInstanceId = pil.processInstanceId) " + "where pil.status = 1");
        query.setTarget("CUSTOM");
        return query;
    }

    private QueryDefinition getTasksWithPotentialOwnerQueryDefinition() {
        final QueryDefinition query = new QueryDefinition();
        query.setName("getAllTasks");
        query.setSource(System.getProperty("org.kie.server.persistence.ds", "jdbc/jbpm-ds"));
        query.setExpression("select ti.taskId, ti.activationTime, ti.actualOwner, ti.createdBy, ti.createdOn, ti.deploymentId, ti.description, ti.dueDate, ti.name, ti.parentId, ti.priority, ti.processId, ti.processInstanceId, ti.processSessionId, ti.status, ti.workItemId, oe.id, eo.entity_id from AuditTaskImpl ti left join PeopleAssignments_PotOwners po on ti.taskId = po.task_id left join OrganizationalEntity oe on po.entity_id = oe.id left join PeopleAssignments_ExclOwners eo on ti.taskId = eo.task_id");
        query.setTarget("PO_TASK");
        return query;
    }

    private QueryDefinition createQueryDefinition(String target) {
        QueryDefinition query = new QueryDefinition();
        query.setName("allProcessInstances");
        query.setSource(System.getProperty("org.kie.server.persistence.ds", "jdbc/jbpm-ds"));
        query.setExpression("select * from ProcessInstanceLog where status = 1");
        query.setTarget(target);
        return query;
    }
    
    private QueryDefinition createErrorsQueryDefinition() {
        String queryExpression = "select * from ExecutionErrorInfo where ERROR_ACK = 0";

        QueryDefinition query = new QueryDefinition();
        query.setName("unAckErrors");
        query.setSource(System.getProperty("org.kie.server.persistence.ds", "jdbc/jbpm-ds"));
        query.setExpression(queryExpression);
        query.setTarget("CUSTOM");
        return query;
    }

    protected List<Long> createProcessInstances(Map<String, Object> parameters) {
        List<Long> processInstanceIds = new ArrayList<Long>();

        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters));
        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters));
        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters));
        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_USERTASK, parameters));
        processInstanceIds.add(processClient.startProcess(CONTAINER_ID, PROCESS_ID_SIGNAL_PROCESS, parameters));

        Collections.sort(processInstanceIds);
        return processInstanceIds;
    }

    protected void abortProcessInstances(List<Long> processInstanceIds) {
        for (Long piId : processInstanceIds) {
            processClient.abortProcessInstance(CONTAINER_ID, piId);
        }
    }

    protected List<String> collectDefinitions(List<ProcessDefinition> definitions) {
        List<String> ids = new ArrayList<String>();

        for (ProcessDefinition definition : definitions) {
            ids.add(definition.getId());
        }
        return ids;
    }

    protected List<Long> collectInstances(List<ProcessInstance> instances) {
        List<Long> ids = new ArrayList<Long>();

        for (ProcessInstance instance : instances) {
            ids.add(instance.getId());
        }
        Collections.sort(ids);
        return ids;
    }
}
