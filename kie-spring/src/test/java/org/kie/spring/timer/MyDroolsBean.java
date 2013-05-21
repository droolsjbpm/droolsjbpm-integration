/*
 * Copyright 2013 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.spring.timer;

import org.drools.core.base.MapGlobalResolver;
import org.kie.api.KieBase;
import org.kie.api.persistence.jpa.KieStoreServices;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieSession;
import org.kie.internal.KnowledgeBaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.JpaTransactionManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

public class MyDroolsBean {

    private static int timerTriggerCount;
    private static int sessionId;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private EntityManagerFactory emf;
    private KieBase kbase;
    private KieStoreServices kstore;
    private JpaTransactionManager txm;

    private TestWorkItemHandler workItemHandler = new TestWorkItemHandler();

    public void initStartDisposeAndLoadSession() {
        try {
            EntityManager em = txm.getEntityManagerFactory().createEntityManager();
            // create new ksession with kstore
            KieSession ksession = kstore.newKieSession(kbase,
                    null,
                    getEnvironment());
            sessionId = ksession.getId();

            logger.info("\n\tSession id: " + sessionId + "\n");

            ksession.getWorkItemManager().registerWorkItemHandler("testWorkItemHandler",
                    workItemHandler);

            ksession.startProcess("timer-flow",
                    null);
            Thread.sleep(4000);
            ksession.dispose();
        } catch (Exception ex) {
            throw new IllegalStateException("The endTheProcess method has been interrupted", ex);
        }
    }

    /**
     * Thread safe increment.
     */
    public static synchronized void incrementTimerTriggerCount() {
        timerTriggerCount++;
    }

    /**
     * Thread safe getter.
     * Note that if this method is not synchronized, there is no visibility guarantee,
     * so the returned value might be a stale cache.
     *
     * @return >= 0
     */
    public static synchronized int getTimerTriggerCount() {
        return timerTriggerCount;
    }

    public void endTheProcess() {
        try {
            KieSession ksession = kstore.loadKieSession(sessionId,
                    kbase,
                    null,
                    getEnvironment());

            //Sleep to check if the timer continues executing.
            logger.info("\n\nSleeping to check that the timer is still running");
            Thread.sleep(5000);

            ksession.getWorkItemManager().completeWorkItem(TestWorkItemHandler.getWorkItem().getId(),
                    null);

            logger.info("\n\nSleeping to check that the timer is no longer running");
            Thread.sleep(3000);
            logger.info("Ok");

            ksession.dispose();

        } catch (InterruptedException ex) {
            throw new IllegalStateException("The endTheProcess method has been interrupted", ex);
        }
    }

    private Environment getEnvironment() {
        Environment environment = KnowledgeBaseFactory.newEnvironment();
        environment.set(EnvironmentName.ENTITY_MANAGER_FACTORY,
                emf);
        environment.set(EnvironmentName.TRANSACTION_MANAGER,
                txm);
        environment.set(EnvironmentName.GLOBALS,
                new MapGlobalResolver());

        return environment;
    }

    public void setEmf(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public void setKbase(KieBase kbase) {
        this.kbase = kbase;
    }

    public void setKstore(KieStoreServices kstore) {
        this.kstore = kstore;
    }

    public void setTxm(JpaTransactionManager txm) {
        this.txm = txm;
    }
}
