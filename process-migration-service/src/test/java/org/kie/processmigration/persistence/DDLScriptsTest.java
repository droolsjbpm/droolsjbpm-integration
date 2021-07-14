/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.processmigration.persistence;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jboss.weld.junit4.WeldInitiator;
import org.jbpm.test.persistence.scripts.TestPersistenceContextBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kie.processmigration.model.Execution;
import org.kie.processmigration.model.Migration;
import org.kie.processmigration.model.MigrationDefinition;
import org.kie.processmigration.model.MigrationReport;
import org.kie.processmigration.model.Plan;
import org.kie.processmigration.model.ProcessRef;
import org.kie.processmigration.model.exceptions.InvalidMigrationException;
import org.kie.processmigration.model.exceptions.MigrationNotFoundException;
import org.kie.processmigration.service.EntityManagerProducer;
import org.kie.processmigration.service.KieService;
import org.kie.processmigration.service.MigrationService;
import org.kie.processmigration.service.PlanService;
import org.kie.processmigration.service.SchedulerService;
import org.kie.processmigration.service.TransactionHelper;
import org.kie.processmigration.service.impl.MigrationServiceImpl;
import org.kie.processmigration.service.impl.PlanServiceImpl;
import org.kie.server.api.model.admin.MigrationReportInstance;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.admin.ProcessAdminServicesClient;
import org.mockito.Mockito;

import static org.jbpm.test.persistence.scripts.PersistenceUnit.DB_TESTING_VALIDATE;
import static org.jbpm.test.persistence.scripts.TestPersistenceContextBase.createAndInitContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Contains tests that test DDL scripts.
 */
public class DDLScriptsTest extends ScriptPersistenceBaseTest {

    private static final Execution SYNC = new Execution().setType(Execution.ExecutionType.SYNC);

    @Rule
    public WeldInitiator weld = WeldInitiator
            .from(EntityManagerProducer.class, PlanServiceImpl.class, MigrationServiceImpl.class, TransactionHelper.class)
            .addBeans(createMockBean(KieService.class), createMockBean(SchedulerService.class))
            .inject(this)
            .build();

    @Inject
    private EntityManager entityManager;

    @Inject
    private MigrationService migrationService;

    @Inject
    private PlanService planService;

    @Inject
    private KieService kieServiceMock;

    @Inject
    private SchedulerService schedulerServiceMock;

    @Before
    public void resetMocks() {
        System.setProperty("hibernate.hbm2ddl.auto", "none");
        Mockito.reset(kieServiceMock, schedulerServiceMock);
    }

    @After
    public void reset() {
        System.clearProperty("hibernate.hbm2ddl.auto");
    }

    /**
     * Tests that DB schema is created properly using DDL scripts.
     */
    @Test
    public void validateAndMigrateProcessUsingDDLs() throws InvalidMigrationException, MigrationNotFoundException {
        final TestPersistenceContextBase dbTestingContext = createAndInitContext(DB_TESTING_VALIDATE);
        try {
            assertMigrateProcess();
        } finally {
            dbTestingContext.clean();
        }
    }

    protected void assertMigrateProcess() throws InvalidMigrationException, MigrationNotFoundException {
        Map<String, Object> persistencePropertiesMap = entityManager.getEntityManagerFactory().getProperties();
        assertEquals("none", persistencePropertiesMap.get("hibernate.hbm2ddl.auto"));

        // Given
        Plan plan = createPlan();
        MigrationDefinition def = createMigrationDefinition(plan, SYNC);

        // Setup mock
        ProcessAdminServicesClient mockProcessAdminServicesClient = Mockito.mock(ProcessAdminServicesClient.class);
        QueryServicesClient mockQueryServicesClient = Mockito.mock(QueryServicesClient.class);
        Mockito.when(kieServiceMock.existsProcessDefinition(anyString(), any(ProcessRef.class))).thenReturn(Boolean.TRUE);
        for (long i = 1; i <= 3; i++) {
            Mockito.when(mockQueryServicesClient.findProcessInstanceById(eq(i))).thenReturn(buildProcessInstance(i, plan.getSource().getContainerId()));
            Mockito.when(mockProcessAdminServicesClient.migrateProcessInstance(anyString(),
                                                                               eq(i),
                                                                               anyString(),
                                                                               anyString(),
                                                                               anyMap()))
                    .thenReturn(createReport(i));
        }

        Mockito.when(kieServiceMock.getQueryServicesClient(anyString())).thenReturn(mockQueryServicesClient);
        Mockito.when(kieServiceMock.getProcessAdminServicesClient(anyString())).thenReturn(mockProcessAdminServicesClient);
        addMockConfigs(kieServiceMock);

        // When
        entityManager.getTransaction().begin();
        migrationService.submit(def);
        entityManager.getTransaction().commit();

        // Then
        for (long i = 1; i <= 3; i++) {
            verify(mockProcessAdminServicesClient).migrateProcessInstance(plan.getSource().getContainerId(),
                                                                          i,
                                                                          plan.getTarget().getContainerId(),
                                                                          plan.getTarget().getProcessId(),
                                                                          plan.getMappings());
        }
        List<Migration> migrations = migrationService.findAll();

        assertNotNull(migrations);
        assertEquals(1, migrations.size());
        Migration m = migrations.get(0);
        assertEquals(Execution.ExecutionStatus.COMPLETED, m.getStatus());
        assertNotNull(m.getCreatedAt());
        assertNotNull(m.getStartedAt());
        assertNotNull(m.getFinishedAt());
        List<MigrationReport> results = migrationService.getResults(m.getId());
        assertEquals(3, results.size());
        results.forEach(r -> assertTrue(r.getSuccessful()));
    }

    private MigrationDefinition createMigrationDefinition(Plan plan, Execution execution) {
        MigrationDefinition def = new MigrationDefinition();
        def.setPlanId(plan.getId());
        def.setKieServerId(MOCK_KIESERVER_ID);
        def.setExecution(execution);
        def.setProcessInstanceIds(Arrays.asList(1L, 2L, 3L));
        return def;
    }

    private Plan createPlan() {
        Plan plan = new Plan();
        plan.setName("name");
        plan.setSource(new ProcessRef().setContainerId("sourceContainerId").setProcessId("sourceProcessId"));
        plan.setTarget(new ProcessRef().setContainerId("targetContainerId").setProcessId("targetProcessId"));
        plan.setDescription("description");
        Map<String, String> mappings = new HashMap<>();
        mappings.put("source1", "target1");
        mappings.put("source2", "target2");
        plan.setMappings(mappings);
        entityManager.getTransaction().begin();
        planService.create(plan);
        entityManager.getTransaction().commit();
        return plan;
    }

    private MigrationReportInstance createReport(Long id) {
        MigrationReportInstance report = new MigrationReportInstance();
        report.setSuccessful(Boolean.TRUE);
        report.setProcessInstanceId(id);
        return report;
    }

    private ProcessInstance buildProcessInstance(long id, String containerId) {
        ProcessInstance instance = new ProcessInstance();
        instance.setId(id);
        instance.setContainerId(containerId);
        return instance;
    }
}
