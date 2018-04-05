/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.kie.spring.persistence;

import org.drools.persistence.api.TransactionManager;
import org.drools.persistence.api.TransactionManagerFactory;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

public class KieSpringTransactionManagerFactory extends TransactionManagerFactory {

    private AbstractPlatformTransactionManager globalTransactionManager;
    
    @Override
    public TransactionManager newTransactionManager() {
        if (globalTransactionManager == null) {
            throw new RuntimeException("No transaction manager set nor environment provided to look it up");
        }
        return new KieSpringTransactionManager(globalTransactionManager);
    }

    @Override
    public TransactionManager newTransactionManager(Environment environment) {

        Object tm = environment.get( EnvironmentName.TRANSACTION_MANAGER );
        if (tm == null) {
            throw new IllegalArgumentException("Transaction manager not found in environment");
        }

        if (tm instanceof KieSpringTransactionManager) {
            return (KieSpringTransactionManager) tm;
        }

        KieSpringTransactionManager springTransactionManager = new KieSpringTransactionManager((AbstractPlatformTransactionManager) tm);
        environment.set(EnvironmentName.TRANSACTION_MANAGER, springTransactionManager);
        return springTransactionManager;
    }
    
    public void setGlobalTransactionManager(AbstractPlatformTransactionManager txm) {
        this.globalTransactionManager = txm;       
    }
}
