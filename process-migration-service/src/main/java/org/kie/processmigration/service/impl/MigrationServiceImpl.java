/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.processmigration.service.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.kie.processmigration.model.Execution.ExecutionStatus;
import org.kie.processmigration.model.Execution.ExecutionType;
import org.kie.processmigration.model.Migration;
import org.kie.processmigration.model.MigrationDefinition;
import org.kie.processmigration.model.MigrationReport;
import org.kie.processmigration.model.Plan;
import org.kie.processmigration.model.exceptions.InvalidKieServerException;
import org.kie.processmigration.model.exceptions.InvalidMigrationException;
import org.kie.processmigration.model.exceptions.MigrationNotFoundException;
import org.kie.processmigration.model.exceptions.PlanNotFoundException;
import org.kie.processmigration.model.exceptions.ProcessNotFoundException;
import org.kie.processmigration.model.exceptions.ReScheduleException;
import org.kie.processmigration.service.KieService;
import org.kie.processmigration.service.MigrationService;
import org.kie.processmigration.service.PlanService;
import org.kie.processmigration.service.SchedulerService;
import org.kie.processmigration.service.TransactionHelper;
import org.kie.server.api.model.admin.MigrationReportInstance;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.admin.ProcessAdminServicesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MigrationServiceImpl implements MigrationService {

    private static final Logger logger = LoggerFactory.getLogger(MigrationServiceImpl.class);

    private static final List<Integer> QUERY_PROCESS_INSTANCE_STATUSES = Collections.singletonList(org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE);
    public static final Integer QUERY_PAGE_SIZE = 100;

    @Inject
    private EntityManager em;

    @Inject
    private PlanService planService;

    @Inject
    private KieService kieService;

    @Inject
    private SchedulerService schedulerService;

    @Inject
    private TransactionHelper txHelper;

    @Override
    public Migration get(Long id) throws MigrationNotFoundException {
        TypedQuery<Migration> query = em.createNamedQuery("Migration.findById", Migration.class);
        query.setParameter("id", id);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            throw new MigrationNotFoundException(id);
        }
    }

    @Override
    public List<MigrationReport> getResults(Long id) throws MigrationNotFoundException {
        Migration m = get(id);
        TypedQuery<MigrationReport> query = em.createNamedQuery("MigrationReport.findByMigrationId", MigrationReport.class);
        query.setParameter("id", m.getId());
        return query.getResultList();
    }

    @Override
    public List<Migration> findAll() {
        return em.createNamedQuery("Migration.findAll", Migration.class).getResultList();
    }

    @Override
    public Migration submit(MigrationDefinition definition) throws InvalidMigrationException {
        validateDefinition(definition);
        Migration migration = txHelper.withTransaction(() -> {
            Migration m = new Migration(definition);
            em.persist(m);
            return m;
        });
        if (ExecutionType.SYNC.equals(definition.getExecution().getType())) {
            migrate(migration);
        } else {
            validatePlanExecution(definition);
            schedulerService.scheduleMigration(migration);
        }
        return migration;
    }

    @Override
    @Transactional
    public Migration delete(Long id) throws MigrationNotFoundException {
        Migration migration = get(id);
        em.remove(migration);
        return migration;
    }

    @Override
    public Migration update(Long id, MigrationDefinition definition) throws MigrationNotFoundException, ReScheduleException, InvalidMigrationException {
        validateDefinition(definition);
        Migration migration = get(id);
        if (!ExecutionStatus.SCHEDULED.equals(migration.getStatus())) {
            throw new ReScheduleException("The migration is not scheduled and cannot be re-scheduled");
        }
        if (ExecutionType.SYNC.equals(definition.getExecution().getType())) {
            throw new ReScheduleException("The migration execution type MUST be ASYNC");
        }
        migration.setDefinition(definition);
        txHelper.withTransaction(() -> em.merge(migration));
        schedulerService.reScheduleMigration(migration);
        return migration;
    }

    @Override
    public Migration migrate(Migration migration) throws InvalidMigrationException {
        try {
            Plan plan = planService.get(migration.getDefinition().getPlanId());
            validatePlanExecution(migration.getDefinition(), plan);
            if (ExecutionStatus.CREATED.equals(migration.getStatus()) || ExecutionStatus.SCHEDULED.equals(migration.getStatus())) {
                migration.start();
            }
            AtomicBoolean hasErrors = new AtomicBoolean(false);
            List<Long> instanceIds = getInstancesToMigrate(migration);
            ProcessAdminServicesClient adminService = kieService.getProcessAdminServicesClient(migration.getDefinition().getKieServerId());
            QueryServicesClient queryService = kieService.getQueryServicesClient(migration.getDefinition().getKieServerId());
            for (Long instanceId : instanceIds) {
                boolean successful = migrateInstance(instanceId, migration, plan, adminService, queryService);
                if (!hasErrors.get() && !Boolean.TRUE.equals(successful)) {
                    hasErrors.set(Boolean.TRUE);
                }
            }
            migration.complete(hasErrors.get());
        } catch (ProcessNotFoundException e) {
            migration.fail(e);
            throw e;
        } catch (PlanNotFoundException e) {
            migration.fail(e);
            throw new InvalidMigrationException("The provided plan id does not exist: " + migration.getDefinition().getPlanId());
        } catch (Exception e) {
            logger.warn("Migration failed", e);
            migration.fail(e);
        } finally {
            txHelper.withTransaction(() -> em.merge(migration));
            if (ExecutionType.ASYNC.equals(migration.getDefinition().getExecution().getType()) &&
                migration.getDefinition().getExecution().getCallbackUrl() != null) {
                doCallback(migration);
            }
        }
        return migration;
    }

    private boolean migrateInstance(Long instanceId, Migration migration, Plan plan, ProcessAdminServicesClient adminService, QueryServicesClient queryService) {
        MigrationReportInstance reportInstance = null;
        try {
            ProcessInstance pi = queryService.findProcessInstanceById(instanceId);
            if (pi != null && pi.getContainerId().equals(plan.getSource().getContainerId())) {
                reportInstance = adminService.migrateProcessInstance(
                    plan.getSource().getContainerId(),
                    instanceId,
                    plan.getTarget().getContainerId(),
                    plan.getTarget().getProcessId(),
                    plan.getMappings());
            } else {
                reportInstance = buildReport(instanceId);
                reportInstance.setLogs(Arrays.asList("Instance did not exist in source container. Migration skipped"));
                logger.debug("Process Instance {} did not exist in source container with id {}", instanceId, plan.getSource().getContainerId());
            }
        } catch (Exception e) {
            logger.warn("Unable to migrate instanceID: " + instanceId, e);
            reportInstance = buildReportFromError(instanceId, e);
        }
        final MigrationReport report = new MigrationReport(migration.getId(), reportInstance);
        return txHelper.withTransaction(() -> {
            em.persist(report);
            return report;
        }).getSuccessful();
    }

    private void doCallback(Migration migration) {
        URI callbackURI = null;
        try {
            callbackURI = migration.getDefinition().getExecution().getCallbackUrl();
            Response response = ClientBuilder.newClient()
                .target(callbackURI)
                .request(MediaType.APPLICATION_JSON)
                .buildPost(Entity.json(migration))
                .invoke();
            if (Status.OK.getStatusCode() == response.getStatus()) {
                logger.debug("Migration [{}] - Callback to {} replied successfully", migration.getId(), callbackURI);
            } else {
                logger.warn("Migration [{}] - Callback to {} replied with {}", migration.getId(), callbackURI, response.getStatus());
            }
        } catch (Exception e) {
            logger.error("Migration [{}] - Callback to {} failed.", migration.getId(), callbackURI, e);
        }
    }

    public void validateDefinition(MigrationDefinition definition) throws InvalidMigrationException {
        if (definition == null) {
            throw new InvalidMigrationException("The Migration Definition must not be null");
        }
        if (definition.getPlanId() == null) {
            throw new InvalidMigrationException("The Plan ID is mandatory");
        }
        if (StringUtils.isBlank(definition.getKieServerId())) {
            throw new InvalidMigrationException("The KIE Server ID is mandatory");
        }
        if (!kieService.hasKieServer(definition.getKieServerId())) {
            throw new InvalidKieServerException(definition.getKieServerId());
        }
    }

    private void validatePlanExecution(MigrationDefinition definition) throws InvalidMigrationException {
        try {
            validatePlanExecution(definition, planService.get(definition.getPlanId()));
        } catch (PlanNotFoundException e) {
            throw new InvalidMigrationException("Plan not found with ID: " + definition.getPlanId());
        }
    }

    private void validatePlanExecution(MigrationDefinition definition, Plan plan) throws InvalidMigrationException {
        if (!kieService.existsProcessDefinition(definition.getKieServerId(), plan.getSource())) {
            throw new ProcessNotFoundException(plan.getSource().getContainerId());
        }
        if (!kieService.existsProcessDefinition(definition.getKieServerId(), plan.getTarget())) {
            throw new ProcessNotFoundException(plan.getTarget().getContainerId());
        }
    }

    private List<Long> getInstancesToMigrate(Migration migration) throws InvalidKieServerException, PlanNotFoundException {
        List<Long> instanceIds = migration.getDefinition().getProcessInstanceIds();
        List<Long> migratedInstances = new ArrayList<>();
        if (migration.getReports() != null && !migration.getReports().isEmpty()) {
            migration.getReports().stream().map(r -> r.getProcessInstanceId()).forEach(id -> migratedInstances.add(id));
        }
        Plan plan = planService.get(migration.getDefinition().getPlanId());
        if (instanceIds == null || instanceIds.isEmpty()) {
            boolean allFetched = false;
            int page = 0;
            while (!allFetched) {
                List<ProcessInstance> instances = kieService.getQueryServicesClient(migration.getDefinition().getKieServerId())
                    .findProcessInstancesByContainerId(plan.getSource().getContainerId(), QUERY_PROCESS_INSTANCE_STATUSES, page++, QUERY_PAGE_SIZE);

                instances.stream().forEach(p -> instanceIds.add(p.getId()));
                if (instances.size() < QUERY_PAGE_SIZE) {
                    allFetched = true;
                }
            }
        }
        return instanceIds.stream().filter(id -> !migratedInstances.contains(id)).collect(Collectors.toList());
    }

    private MigrationReportInstance buildReport(Long instanceId) {
        MigrationReportInstance reportInstance = new MigrationReportInstance();
        reportInstance.setSuccessful(true);
        reportInstance.setProcessInstanceId(instanceId);
        reportInstance.setStartDate(new Date());
        reportInstance.setEndDate(new Date());
        return reportInstance;
    }

    private MigrationReportInstance buildReportFromError(Long instanceId, Exception e) {
        MigrationReportInstance reportInstance = buildReport(instanceId);
        reportInstance.setSuccessful(false);
        reportInstance.setLogs(Arrays.asList(e.getMessage()));
        return reportInstance;
    }
}
