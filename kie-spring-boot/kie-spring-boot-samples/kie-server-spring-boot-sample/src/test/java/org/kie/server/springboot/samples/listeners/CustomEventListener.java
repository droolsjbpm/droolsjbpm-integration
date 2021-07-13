/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.server.springboot.samples.listeners;

import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.instance.context.variable.VariableScopeInstance;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.kie.api.event.process.DefaultProcessEventListener;
import org.kie.api.event.process.ProcessStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CustomEventListener extends DefaultProcessEventListener {
    private static final Logger logger = LoggerFactory.getLogger(CustomEventListener.class);
    
    @Override
    public void afterProcessStarted(ProcessStartedEvent event) {
        logger.info("afterProcessStarted, pid:"+event.getProcessInstance().getId());
        
        VariableScopeInstance kcontext = 
                (VariableScopeInstance) ((WorkflowProcessInstance)event.getProcessInstance()).getContextInstance(VariableScope.VARIABLE_SCOPE);

        kcontext.setVariable("testListenerStarted", true);
    }
}
