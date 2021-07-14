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

package org.kie.processmigration.service;

import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.kie.processmigration.model.Execution.ExecutionStatus;
import org.kie.processmigration.model.Migration;
import org.kie.processmigration.model.exceptions.InvalidMigrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@Singleton
public class RecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(RecoveryService.class);
    private static final List<ExecutionStatus> PENDING_STATUSES = Arrays.asList(ExecutionStatus.STARTED,
                                                                                ExecutionStatus.CREATED);

    @Inject
    private EntityManager em;

    @Inject
    MigrationService migrationService;

    @PostConstruct
    public void resumeMigrations() {
        logger.info("Resuming ongoing migrations ...");
        TypedQuery<Migration> query = em.createNamedQuery("Migration.findByStatus", Migration.class);
        query.setParameter("statuses", PENDING_STATUSES);
        query.getResultList().forEach(m -> {
            try {
                migrationService.migrate(m);
            } catch (InvalidMigrationException e) {
                logger.warn("Unable to resume migration with id: " + m.getId(), e);
            }
        });
    }
}
