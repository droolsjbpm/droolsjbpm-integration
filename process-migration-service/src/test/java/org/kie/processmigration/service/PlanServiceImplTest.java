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

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jboss.weld.junit4.WeldInitiator;
import org.junit.Rule;
import org.junit.Test;
import org.kie.processmigration.model.Plan;
import org.kie.processmigration.model.ProcessRef;
import org.kie.processmigration.service.impl.PlanServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PlanServiceImplTest extends AbstractPersistenceTest {

    @Rule
    public WeldInitiator weld = WeldInitiator
        .from(EntityManagerProducer.class, PlanServiceImpl.class)
        .inject(this)
        .build();

    @Inject
    private EntityManager entityManager;
    @Inject
    private PlanService planService;

    @Test
    public void testSaveAndFindAll() {
        // Given
        assertNotNull(planService);

        Plan plan = new Plan();
        plan.setName("name");
        plan.setSource(new ProcessRef().setContainerId("containerId").setProcessId("sourceProcessId"));
        plan.setTarget(new ProcessRef().setContainerId("targetContainerId").setProcessId("targetProcessId"));
        plan.setDescription("description");

        // When
        entityManager.getTransaction().begin();
        planService.create(plan);
        entityManager.getTransaction().commit();

        // Then
        List<Plan> plans = planService.findAll();

        assertNotNull(plans);
        assertEquals(1, plans.size());
    }
}
