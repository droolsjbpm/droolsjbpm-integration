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

import java.util.EventListener;
import java.util.Map;

import org.drools.SessionConfiguration;
import org.drools.agent.KnowledgeAgent;
import org.drools.event.process.ProcessEventListener;
import org.drools.event.rule.AgendaEventListener;
import org.drools.event.rule.WorkingMemoryEventListener;
import org.drools.runtime.CommandExecutor;
import org.drools.runtime.StatelessKnowledgeSession;
import org.drools.runtime.process.WorkItemHandler;

public class StatelessKnowledgeSessionBeanFactory extends AbstractKnowledgeSessionBeanFactory {
    private StatelessKnowledgeSession ksession;
    private KnowledgeAgent            kagent;

    public void setKnowledgeAgent(KnowledgeAgent kagent) {
        this.kagent = kagent;
    }

    public KnowledgeAgent getKnowledgeAgent() {
        return this.kagent;
    }

    public Class<StatelessKnowledgeSession> getObjectType() {
        return StatelessKnowledgeSession.class;
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

        if ( this.kagent != null ) {
            ksession = this.kagent.newStatelessKnowledgeSession( getConf() );
        } else {
            ksession = getKbase().newStatelessKnowledgeSession( getConf() );
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
    }
}
