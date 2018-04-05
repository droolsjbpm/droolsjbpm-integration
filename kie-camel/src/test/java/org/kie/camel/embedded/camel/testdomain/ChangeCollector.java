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

package org.kie.camel.embedded.camel.testdomain;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.drools.core.xml.jaxb.util.JaxbListAdapter;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class ChangeCollector implements RuleRuntimeEventListener {

    @XmlElement
    @XmlJavaTypeAdapter(JaxbListAdapter.class)
    private List<String> retracted;

    @XmlElement
    @XmlJavaTypeAdapter(JaxbListAdapter.class)
    private List changes;

    public List<String> getRetracted() {
        return retracted;
    }

    public List getChanges() {
        return changes;
    }

    public void objectInserted(ObjectInsertedEvent event) {

    }

    public void objectUpdated(ObjectUpdatedEvent event) {
        if (changes == null)
            changes = new ArrayList();
        if (event.getObject() instanceof Cheese) {
            Cheese c = (Cheese)event.getObject();
            changes.add(c);
        }
    }

    public void objectDeleted(ObjectDeletedEvent event) {
        if (retracted == null)
            retracted = new ArrayList<String>();
        if (event.getOldObject() instanceof Cheese) {
            Cheese c = (Cheese)event.getOldObject();
            retracted.add(c.getType());
        }
    }
}
