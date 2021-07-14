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

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import org.kie.processmigration.model.Plan;
import org.kie.processmigration.model.exceptions.PlanNotFoundException;
import org.kie.processmigration.service.PlanService;

@ApplicationScoped
public class PlanServiceImpl implements PlanService {

    @Inject
    private EntityManager em;

    @Override
    public List<Plan> findAll() {
        return em.createNamedQuery("Plan.findAll", Plan.class).getResultList();
    }

    @Override
    public Plan get(Long id) throws PlanNotFoundException {
        TypedQuery<Plan> query = em.createNamedQuery("Plan.findById", Plan.class);
        query.setParameter("id", id);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            throw new PlanNotFoundException(id);
        }
    }

    @Override
    @Transactional
    public Plan delete(Long id) throws PlanNotFoundException {
        Plan plan = get(id);
        em.remove(plan);
        return plan;
    }

    @Override
    @Transactional
    public Plan create(Plan plan) {
        em.persist(plan);
        return plan;
    }

    @Override
    @Transactional
    public Plan update(Long id, Plan plan) throws PlanNotFoundException {
        return create(get(id).copy(plan));
    }
}
