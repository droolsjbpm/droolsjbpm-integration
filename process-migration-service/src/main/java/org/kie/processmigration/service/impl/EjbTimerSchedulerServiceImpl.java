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

import java.util.Date;
import java.util.Optional;

import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.inject.Inject;

import org.kie.processmigration.model.Credentials;
import org.kie.processmigration.model.Migration;
import org.kie.processmigration.service.CredentialsService;
import org.kie.processmigration.service.MigrationService;
import org.kie.processmigration.service.SchedulerService;

@Singleton
@Startup
public class EjbTimerSchedulerServiceImpl implements SchedulerService {

    @Resource
    private TimerService timerService;

    @Inject
    private MigrationService migrationService;

    @Inject
    private CredentialsService credentialsService;

    @Timeout
    public void doMigration(Timer timer) {
        Long migrationId = (Long) timer.getInfo();
        Migration migration = migrationService.get(migrationId);
        migrationService.migrate(migration);
    }

    @Override
    public void scheduleMigration(Migration migration, Credentials credentials) {
        Long migrationId = migration.getId();
        credentialsService.save(credentials.setMigrationId(migrationId));
        if (migration.getDefinition().getExecution().getScheduledStartTime() == null) {
            timerService.createTimer(new Date(), migrationId);
        } else {
            Date startTime = Date.from(migration
                                                .getDefinition()
                                                .getExecution()
                                                .getScheduledStartTime());
            timerService.createTimer(startTime, migrationId);
        }
    }

    @Override
    public void reScheduleMigration(Migration migration, Credentials credentials) {
        Optional<Timer> timer = timerService.getTimers()
                                            .stream()
                                            .filter(t -> t.getInfo().equals(migration.getId())).findFirst();
        if (timer.isPresent()) {
            timer.get().cancel();
            scheduleMigration(migration, credentials);
        }
    }

}
