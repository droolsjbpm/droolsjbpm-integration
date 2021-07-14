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

package org.kie.processmigration.service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Startup
@ApplicationScoped
public class EntityManagerProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityManagerProducer.class);
    private EntityManager em;
    private static final String PERSISTENT_UNIT = "migration-unit";

    @Produces
    @ApplicationScoped
    public EntityManager getEntityManager() {
        return em;
    }

    public void close(@Disposes EntityManager entityManager) {
        entityManager.close();
    }

    @PostConstruct
    public void onStartup() {
        em = Persistence
                .createEntityManagerFactory(PERSISTENT_UNIT, getPersistenceProperties())
                .createEntityManager();
    }

    private Map<String, String> getPersistenceProperties() {
        Map<String, String> persistenceProperties = new HashMap<>();

        System.getProperties().stringPropertyNames()
                .stream()
                .filter(EntityManagerProducer::isValidPersistenceKey)
                .forEach(name -> persistenceProperties.put(name, System.getProperty(name)));
        LOGGER.info("persistence properties fetched {}", persistenceProperties);
        return persistenceProperties;
    }

    private static Boolean isValidPersistenceKey(String propertyName) {
        return propertyName.startsWith("hibernate.") || propertyName.startsWith("javax.persistence");
    }
}
