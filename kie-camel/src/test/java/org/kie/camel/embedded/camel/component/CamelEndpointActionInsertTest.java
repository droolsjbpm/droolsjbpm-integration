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

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * under the License.
 */

package org.kie.camel.embedded.camel.component;

import javax.naming.Context;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieSession;
import org.kie.pipeline.camel.Person;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class CamelEndpointActionInsertTest extends KieCamelTestSupport {
    private KieSession ksession;
    private AgendaEventListener ael;
    private RuleRuntimeEventListener wmel;

    @Test
    public void testSessionInsert() throws Exception {
        Person person = new Person();
        person.setName("Bob");

        template.sendBody("direct:test-no-ep", person);

        ArgumentCaptor<ObjectInsertedEvent> oie = ArgumentCaptor.forClass(ObjectInsertedEvent.class);

        verify(wmel).objectInserted(oie.capture());
        assertThat((Person)oie.getValue().getObject(), is(person));
    }

    @Test
    public void testSessionInsertEntryPoint() throws Exception {
        Person person = new Person();
        person.setName("Bob");

        template.sendBody("direct:test-with-ep", person);

        ArgumentCaptor<ObjectInsertedEvent> oie = ArgumentCaptor.forClass(ObjectInsertedEvent.class);

        verify(wmel).objectInserted(oie.capture());
        assertThat((Person)oie.getValue().getObject(), is(person));
    }

    @Test
    public void testSessionInsertMessage() throws Exception {
        Person person = new Person();
        person.setName("Bob");

        template.sendBody("direct:test-message", person);

        ArgumentCaptor<ObjectInsertedEvent> oie = ArgumentCaptor.forClass(ObjectInsertedEvent.class);

        verify(wmel).objectInserted(oie.capture());
        assertThat((Person)((Message)oie.getValue().getObject()).getBody(), is(person));
    }

    @Test
    public void testSessionInsertExchange() throws Exception {
        Person person = new Person();
        person.setName("Bob");

        template.sendBody("direct:test-exchange", person);

        ArgumentCaptor<ObjectInsertedEvent> oie = ArgumentCaptor.forClass(ObjectInsertedEvent.class);

        verify(wmel).objectInserted(oie.capture());
        assertThat((Person)((Exchange)oie.getValue().getObject()).getIn().getBody(), is(person));
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws Exception {
                from("direct:test-no-ep").to("kie-local://ksession1?action=insertBody");
                from("direct:test-with-ep").to("kie-local://ksession1?action=insertBody&entryPoint=ep1");
                from("direct:test-message").to("kie-local://ksession1?action=insertMessage");
                from("direct:test-exchange").to("kie-local://ksession1?action=insertExchange");
            }
        };
    }

    @Override
    protected void configureDroolsContext(Context jndiContext) {
        String rule = "";
        rule += "import " + Person.class.getCanonicalName() + ";\n";
        rule += "import org.apache.camel.Exchange \n";
        rule += "import org.apache.camel.Message \n";
        rule += "rule rule1 \n";
        rule += "  when \n";
        rule += "    $p : Person() \n";
        rule += "  then \n";
        rule += "    // no-op \n";
        rule += "end\n";
        rule += "rule rule2 \n";
        rule += "  when \n";
        rule += "    $p : Person() from entry-point ep1 \n";
        rule += "  then \n";
        rule += "    // no-op \n";
        rule += "end\n";
        rule += "rule rule3 \n";
        rule += "  when \n";
        rule += "    $m : Message() \n";
        rule += "  then \n";
        rule += "    // no-op \n";
        rule += "end\n";
        rule += "rule rule4 \n";
        rule += "  when \n";
        rule += "    $e : Exchange() \n";
        rule += "  then \n";
        rule += "    // no-op \n";
        rule += "end\n";

        ksession = registerKnowledgeRuntime("ksession1", rule);
        ael = mock(AgendaEventListener.class);
        wmel = mock(RuleRuntimeEventListener.class);
        ksession.addEventListener(ael);
        ksession.addEventListener(wmel);
    }
}
