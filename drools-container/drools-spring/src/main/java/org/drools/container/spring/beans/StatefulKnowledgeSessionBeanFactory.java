/*
 * Copyright 2010 JBoss Inc
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

package org.drools.container.spring.beans;

import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.drools.KnowledgeBaseFactory;
import org.drools.SessionConfiguration;
import org.drools.command.Command;
import org.drools.event.process.ProcessEventListener;
import org.drools.event.rule.AgendaEventListener;
import org.drools.event.rule.WorkingMemoryEventListener;
import org.drools.marshalling.ObjectMarshallingStrategy;
import org.drools.marshalling.impl.ClassObjectMarshallingStrategyAcceptor;
import org.drools.marshalling.impl.SerializablePlaceholderResolverStrategy;
import org.drools.persistence.jpa.JPAKnowledgeService;
import org.drools.runtime.CommandExecutor;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.WorkItemHandler;
import org.springframework.transaction.PlatformTransactionManager;

public class StatefulKnowledgeSessionBeanFactory extends AbstractKnowledgeSessionBeanFactory {
    private StatefulKnowledgeSession ksession;

    private JpaConfiguration         jpaConfiguration;

    public Class<StatefulKnowledgeSession> getObjectType() {
        return StatefulKnowledgeSession.class;
    }

    public JpaConfiguration getJpaConfiguration() {
        return jpaConfiguration;
    }

    public void setJpaConfiguration(JpaConfiguration jpaConfiguration) {
        this.jpaConfiguration = jpaConfiguration;
    }

    @Override
    protected CommandExecutor getCommandExecutor() {
        return ksession;
    }

    @Override
    protected void internalAfterPropertiesSet() {
        if ( getConf() != null && getWorkItems() != null && !getWorkItems().isEmpty() ) {
            Map<String, WorkItemHandler> map = ((SessionConfiguration) getConf()).getWorkItemHandlers();
            map.putAll( getWorkItems() );
        }

        if ( jpaConfiguration != null ) {

            Environment env = KnowledgeBaseFactory.newEnvironment();
            env.set( EnvironmentName.ENTITY_MANAGER_FACTORY,
                     jpaConfiguration.getEntityManagerFactory() );
            env.set( EnvironmentName.TRANSACTION_MANAGER,
                     jpaConfiguration.getPlatformTransactionManager() );
            env.set( EnvironmentName.OBJECT_MARSHALLING_STRATEGIES,
                     new ObjectMarshallingStrategy[]{new SerializablePlaceholderResolverStrategy( ClassObjectMarshallingStrategyAcceptor.DEFAULT )} );

            if ( jpaConfiguration.getId() >= 0 ) {
                ksession = JPAKnowledgeService.loadStatefulKnowledgeSession( jpaConfiguration.getId(),
                                                                             getKbase(),
                                                                             getConf(),
                                                                             env );
            } else {
                ksession = JPAKnowledgeService.newStatefulKnowledgeSession( getKbase(),
                                                                            getConf(),
                                                                            env );
            }
        } else {
            ksession = getKbase().newStatefulKnowledgeSession( getConf(),
                                                               null );
        }

        if ( getBatch() != null && !getBatch().isEmpty() ) {
            for ( Command< ? > cmd : getBatch() ) {
                ksession.execute( cmd );
            }
        }

        if ( getNode() != null ) {
            getNode().set( getName(),
                           this.ksession );
        }
        // Additions for JIRA JBRULES-3076
        for (AgendaEventListener agendaEventListener :getAgendaEventListeners()) {
            ksession.addEventListener(agendaEventListener);
        }
        for (ProcessEventListener processEventListener :getProcessEventListeners()) {
            ksession.addEventListener(processEventListener);
        }
        for (WorkingMemoryEventListener workingMemoryEventListener :getWorkingMemoryEventListeners()) {
            ksession.addEventListener(workingMemoryEventListener);
        }
        // End of Additions for JIRA JBRULES-3076

        //start of changes for kloggers
        attachLoggers(ksession);
        //end of kloggers
    }

    public static class JpaConfiguration {
        private EntityManagerFactory       emf;

        private PlatformTransactionManager tm;

        private int                        id = -1;

        public EntityManagerFactory getEntityManagerFactory() {
            return this.emf;
        }

        public void setEntityManagerFactory(EntityManagerFactory emf) {
            this.emf = emf;
        }

        public PlatformTransactionManager getPlatformTransactionManager() {
            return this.tm;
        }

        public void setPlatformTransactionManager(PlatformTransactionManager tm) {
            this.tm = tm;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

    }
}
