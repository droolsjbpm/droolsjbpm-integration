/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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
package org.kie.server.api.model.instance;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.kie.server.api.model.ItemList;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "process-instance-custom-list")
public class ProcessInstanceCustomVarsList implements ItemList<ProcessInstanceCustomVars> {

    @XmlElement(name = "process-instance-vars")
    private ProcessInstanceCustomVars[] processInstances;

    public ProcessInstanceCustomVarsList() {
    }

    public ProcessInstanceCustomVarsList(ProcessInstanceCustomVars[] processInstances) {
        this.processInstances = processInstances;
    }

    public ProcessInstanceCustomVarsList(List<ProcessInstanceCustomVars> processInstances) {
        this.processInstances = processInstances.toArray(new ProcessInstanceCustomVars[processInstances.size()]);
    }

    public ProcessInstanceCustomVars[] getProcessInstances() {
        return processInstances;
    }

    public void setProcessInstances(ProcessInstanceCustomVars[] processInstances) {
        this.processInstances = processInstances;
    }

    @Override
    public List<ProcessInstanceCustomVars> getItems() {
        if (processInstances == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(processInstances);
    }

    @Override
    public String toString() {
        return "ProcessInstanceCustomVarsList [processInstances=" + Arrays.toString(processInstances) + "]";
    }

}
