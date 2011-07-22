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

import java.util.*;

import org.drools.KnowledgeBase;
import org.drools.command.Command;
import org.drools.container.spring.namespace.EventListenersUtil;
import org.drools.event.process.ProcessEventListener;
import org.drools.event.rule.AgendaEventListener;
import org.drools.event.rule.WorkingMemoryEventListener;
import org.drools.grid.GridNode;
import org.drools.runtime.CommandExecutor;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.process.WorkItemHandler;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NamedBean;

public abstract class AbstractKnowledgeSessionBeanFactory
        implements
        FactoryBean,
        InitializingBean,
        BeanNameAware,
        NamedBean {

    private GridNode node;
    private Map<String, WorkItemHandler> workItems;
    private KnowledgeSessionConfiguration conf;
    private KnowledgeBase kbase;
    private String beanName;
    private String name;

    private List<Command<?>> batch;

    // Additions for JIRA JBRULES-3076
    protected List<AgendaEventListener> agendaEventListeners;
    protected List<ProcessEventListener> processEventListeners;
    protected List<WorkingMemoryEventListener> workingMemoryEventListeners;
    protected List<Object> groupedListeners = new ArrayList<Object>();
    // End of additions for JIRA JBRULES-3076

    public AbstractKnowledgeSessionBeanFactory() {
        super();
        // Additions for JIRA JBRULES-3076
        agendaEventListeners = new ArrayList<AgendaEventListener>();
        processEventListeners = new ArrayList<ProcessEventListener>();
        workingMemoryEventListeners = new ArrayList<WorkingMemoryEventListener>();
        // End of additions for JIRA JBRULES-3076
    }

    public Object getObject() throws Exception {
        return getCommandExecutor();
    }

    public Map<String, WorkItemHandler> getWorkItems() {
        return workItems;
    }

    public void setWorkItems(Map<String, WorkItemHandler> workItems) {
        this.workItems = workItems;
    }

    public KnowledgeSessionConfiguration getConf() {
        return conf;
    }

    public void setConf(KnowledgeSessionConfiguration conf) {
        this.conf = conf;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public KnowledgeBase getKbase() {
        return kbase;
    }

    public void setKbase(KnowledgeBase kbase) {
        this.kbase = kbase;
    }

    public boolean isSingleton() {
        return true;
    }

    public List<Command<?>> getBatch() {
        return batch;
    }

    public void setBatch(List<Command<?>> commands) {
        this.batch = commands;
    }

    public final void afterPropertiesSet() throws Exception {
        if (kbase == null) {
            throw new IllegalArgumentException("kbase property is mandatory");
        }
        if (name == null) {
            name = beanName;
        }
        internalAfterPropertiesSet();
    }

    protected abstract CommandExecutor getCommandExecutor();

    protected abstract void internalAfterPropertiesSet();

    public GridNode getNode() {
        return node;
    }

    public void setNode(GridNode node) {
        this.node = node;
    }

    public void setBeanName(String name) {
        this.beanName = name;

    }

    public String getBeanName() {
        return beanName;
    }

    // Additions for JIRA JBRULES-3076
    public void setEventListenersFromGroup(List<Object> eventListenerList) {
        for (Object eventListener : eventListenerList) {
            if (eventListener instanceof AgendaEventListener) {
                agendaEventListeners.add((AgendaEventListener) eventListener);
            }
            if (eventListener instanceof WorkingMemoryEventListener) {
                workingMemoryEventListeners.add((WorkingMemoryEventListener) eventListener);
            }
            if (eventListener instanceof ProcessEventListener) {
                processEventListeners.add((ProcessEventListener) eventListener);
            }
        }
        groupedListeners.addAll(eventListenerList);
        // System.out.println("adding listener-group elements " + groupedListeners.size());
    }

    public void setEventListeners(Map<String, List> eventListenerMap) {
        for (String key : eventListenerMap.keySet()) {
            List<Object> eventListenerList = eventListenerMap.get(key);
            if (EventListenersUtil.TYPE_AGENDA_EVENT_LISTENER.equalsIgnoreCase(key)) {
                for (Object eventListener : eventListenerList) {
                    if (eventListener instanceof AgendaEventListener) {
                        agendaEventListeners.add((AgendaEventListener) eventListener);
                    }
                }
            } else if (EventListenersUtil.TYPE_WORKING_MEMORY_EVENT_LISTENER.equalsIgnoreCase(key)) {
                for (Object eventListener : eventListenerList) {
                    if (eventListener instanceof WorkingMemoryEventListener) {
                        workingMemoryEventListeners.add((WorkingMemoryEventListener) eventListener);
                    }
                }
            } else if (EventListenersUtil.TYPE_PROCESS_EVENT_LISTENER.equalsIgnoreCase(key)) {
                for (Object eventListener : eventListenerList) {
                    if (eventListener instanceof ProcessEventListener) {
                        processEventListeners.add((ProcessEventListener) eventListener);
                    }
                }
            }
        }
    }

    public List<AgendaEventListener> getAgendaEventListeners() {
        return agendaEventListeners;
    }

    public void setAgendaEventListeners(List<AgendaEventListener> agendaEventListeners) {
        this.agendaEventListeners = agendaEventListeners;
    }

    public List<ProcessEventListener> getProcessEventListeners() {
        return processEventListeners;
    }

    public void setProcessEventListeners(List<ProcessEventListener> processEventListeners) {
        this.processEventListeners = processEventListeners;
    }

    public List<WorkingMemoryEventListener> getWorkingMemoryEventListeners() {
        return workingMemoryEventListeners;
    }

    public void setWorkingMemoryEventListeners(List<WorkingMemoryEventListener> workingMemoryEventListeners) {
        this.workingMemoryEventListeners = workingMemoryEventListeners;
    }

    // End of Changes for JIRA JBRULES-3076
}
