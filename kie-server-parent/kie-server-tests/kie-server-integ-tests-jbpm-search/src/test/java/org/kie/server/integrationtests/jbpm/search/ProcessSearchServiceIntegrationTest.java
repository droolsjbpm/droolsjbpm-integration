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

package org.kie.server.integrationtests.jbpm.search;

import org.assertj.core.api.Assertions;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.kie.server.api.model.definition.ProcessInstanceField;
import org.kie.server.api.model.definition.ProcessInstanceQueryFilterSpec;
import org.kie.server.api.util.ProcessInstanceQueryFilterSpecBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ProcessSearchServiceIntegrationTest extends JbpmQueriesKieServerBaseIntegrationTest{

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project", "1.0.0.Final");

    @BeforeClass
    public static void buildAndDeployArtifacts() {

        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/definition-project").getFile());

        kieContainer = KieServices.Factory.get().newKieContainer(releaseId);

        createContainer(CONTAINER_ID, releaseId);
    }

    @Before
    public void cleanup()  {
        super.cleanup();
    }

    @Override
    protected void addExtraCustomClasses(Map<String, Class<?>> extraClasses) throws Exception {
        extraClasses.put(PERSON_CLASS_NAME, Class.forName(PERSON_CLASS_NAME, true, kieContainer.getClassLoader()));
    }

    @Test
    public void testFindProcessWithIncompatibleTypeFilter() throws Exception {
        assertClientException(
                () -> searchServicesClient.findProcessInstancesWithFilters(createQueryFilterEqualsTo(ProcessInstanceField.PROCESSID, 1), 0, 100),
                500,
                "Can't lookup on specified data set: getProcessInstancesWithFilters");
    }

    @Test
    public void testFindProcessInstanceWithProcessNameEqualsToFilter() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        Assertions.assertThat(processInstanceId).isNotNull();
        testFindProcessInstanceWithQueryFilter(createQueryFilterEqualsTo(ProcessInstanceField.PROCESSNAME, "evaluation"), processInstanceId);
    }

    @Test
    public void testFindProcessInstanceWithProcessInstanceIdGreaterThanFilter() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        Assertions.assertThat(processInstanceId).isNotNull();
        testFindProcessInstanceWithQueryFilter(createQueryFilterGreaterThan(ProcessInstanceField.PROCESSINSTANCEID, 0), processInstanceId);
    }

    @Test
    public void testFindProcessInstanceWithStartDateGreaterThanFilter() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        Assertions.assertThat(processInstanceId).isNotNull();
        testFindProcessInstanceWithQueryFilter(createQueryFilterGreaterThan(ProcessInstanceField.START_DATE, Date.from(Instant.EPOCH)), processInstanceId);
    }

    @Test
    public void testFindProcessInstanceWithCorrelationKeyGreaterThanFilter() throws Exception {
       Map<String, Object> parameters = new HashMap<>();
       Long processInstanceId  = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        Assertions.assertThat(processInstanceId).isNotNull();
        testFindProcessInstanceWithQueryFilter(createQueryFilterEqualsTo(ProcessInstanceField.CORRELATIONKEY, processInstanceId), processInstanceId);
    }

    @Test
    public void testFindProcessInstanceWithExternalIdEqualsToFilter() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        Assertions.assertThat(processInstanceId).isNotNull();
        testFindProcessInstanceWithQueryFilter(createQueryFilterEqualsTo(ProcessInstanceField.EXTERNALID, "definition-project"), processInstanceId);
    }

    @Test
    public void testFindProcessInstanceWithUserIdEqualsToFilter() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        Assertions.assertThat(processInstanceId).isNotNull();
        testFindProcessInstanceWithQueryFilter(createQueryFilterEqualsTo(ProcessInstanceField.USER_IDENTITY, USER_YODA), processInstanceId);
    }

    @Test
    public void testFindProcessInstanceWithParentIdEqualsToFilter() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        Long processInstanceId  = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        Assertions.assertThat(processInstanceId).isNotNull();
        testFindProcessInstanceWithQueryFilter(createQueryFilterEqualsTo(ProcessInstanceField.PARENTPROCESSINSTANCEID, -1), processInstanceId);
    }

    public void testFindProcessInstanceWithStatusEqualsToFilter() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);
        Assertions.assertThat(processInstanceId).isNotNull();
        testFindProcessInstanceWithQueryFilter(createQueryFilterEqualsTo(ProcessInstanceField.STATUS, 1), processInstanceId);
    }

    @Test
    public void testFindTaskWithAndEqualsToFilter() throws Exception {
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION);
        Assertions.assertThat(processInstanceId).isNotNull();
        ProcessInstance process = processClient.getProcessInstance(CONTAINER_ID, processInstanceId);
        Assertions.assertThat(process).isNotNull();

        HashMap<ProcessInstanceField, Comparable<?>> compareList = new HashMap<>();
        compareList.put(ProcessInstanceField.PROCESSID, process.getProcessId());
        compareList.put(ProcessInstanceField.EXTERNALID, CONTAINER_ID);
        compareList.put(ProcessInstanceField.PROCESSINSTANCEID, processInstanceId);
        compareList.put(ProcessInstanceField.PROCESSINSTANCEDESCRIPTION, process.getProcessInstanceDescription());
        compareList.put(ProcessInstanceField.CORRELATIONKEY, process.getCorrelationKey());
        compareList.put(ProcessInstanceField.USER_IDENTITY, USER_YODA);
        compareList.put(ProcessInstanceField.PARENTPROCESSINSTANCEID, process.getParentId());
        compareList.put(ProcessInstanceField.STATUS, process.getState());
        compareList.put(ProcessInstanceField.PROCESSVERSION, process.getProcessVersion());
        compareList.put(ProcessInstanceField.PROCESSNAME, process.getProcessName());

        List<Long> resultsIds = new ArrayList<>();
        List<ProcessInstance> results = null;

        results = searchServicesClient.
                findProcessInstancesWithFilters(createQueryFilterAndEqualsTo(compareList), 0, 100);

        resultsIds = new ArrayList<>();
        for (ProcessInstance res : results) {
            resultsIds.add(res.getId());
        }

        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.size()).isGreaterThanOrEqualTo(0);
        Assertions.assertThat(resultsIds).contains(process.getId());

        final ProcessInstance[] instance = new ProcessInstance[1];
        results.forEach((p) -> {
            if (p.getId().equals(process.getId())){
                instance[0] = p;
            }
        });

        Assertions.assertThat(instance[0].getContainerId()).isEqualTo(CONTAINER_ID);
        Assertions.assertThat(instance[0].getId()).isEqualTo(processInstanceId);
        Assertions.assertThat(instance[0].getProcessName()).isEqualTo(process.getProcessName());
        Assertions.assertThat(instance[0].getCorrelationKey()).isEqualTo(process.getCorrelationKey());
        Assertions.assertThat(instance[0].getInitiator()).isEqualTo(USER_YODA);
        Assertions.assertThat(instance[0].getProcessInstanceDescription()).isEqualTo(process.getProcessInstanceDescription());
        Assertions.assertThat(instance[0].getParentId()).isEqualTo(process.getParentId());
        Assertions.assertThat(instance[0].getState()).isEqualTo(process.getState());
        Assertions.assertThat(instance[0].getProcessVersion()).isEqualTo(process.getProcessVersion());
        Assertions.assertThat(instance[0].getProcessId()).isEqualTo(process.getProcessId());
    }

    private void testFindProcessInstanceWithQueryFilter(ProcessInstanceQueryFilterSpec filter, Long processInstanceId) {
        List<Long> resultsIds = new ArrayList<>();
        List<ProcessInstance> results = null;

        results = searchServicesClient.
                findProcessInstancesWithFilters(filter, 0, 100);

        resultsIds = new ArrayList<>();
        for (ProcessInstance res : results) {
            resultsIds.add(res.getId());
        }

        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.size()).isGreaterThanOrEqualTo(1);
        Assertions.assertThat(resultsIds).contains(processInstanceId);
    }

    private ProcessInstanceQueryFilterSpec createQueryFilterEqualsTo(ProcessInstanceField processInstanceField, Comparable<?> equalsTo) {
        return  new ProcessInstanceQueryFilterSpecBuilder().equalsTo(processInstanceField, equalsTo).get();
    }

    private ProcessInstanceQueryFilterSpec createQueryFilterGreaterThan(ProcessInstanceField processInstanceField, Comparable<?> greaterThan) {
        return  new ProcessInstanceQueryFilterSpecBuilder().greaterThan(processInstanceField, greaterThan).get();
    }

    private ProcessInstanceQueryFilterSpec createQueryFilterAndEqualsTo(Map<ProcessInstanceField, Comparable<?>> filterProperties) {
        ProcessInstanceQueryFilterSpecBuilder result = new ProcessInstanceQueryFilterSpecBuilder();
        filterProperties.forEach(result::equalsTo);
        return  result.get();
    }
}
