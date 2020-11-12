/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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

package org.kie.camel.container.integration.tests.remote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.drools.core.command.runtime.BatchExecutionCommandImpl;
import org.drools.core.command.runtime.rule.FireAllRulesCommand;
import org.drools.core.command.runtime.rule.InsertObjectCommand;
import org.junit.Test;
import org.kie.api.command.ExecutableCommand;
import org.kie.camel.container.api.ExecutionServerCommand;
import org.kie.camel.container.api.model.Person;

public class RuleClientIntegrationTest extends AbstractRemoteIntegrationTest {

    @Test
    public void testExecuteCommand() {
        final Person person = new Person();
        person.setName("John");
        person.setAge(25);
        final InsertObjectCommand insertObjectCommand = new InsertObjectCommand();
        insertObjectCommand.setOutIdentifier("person");
        insertObjectCommand.setObject(person);
        final FireAllRulesCommand fireAllRulesCommand = new FireAllRulesCommand();
        final List<ExecutableCommand<?>> commands = new ArrayList<ExecutableCommand<?>>();
        final BatchExecutionCommandImpl executionCommand = new BatchExecutionCommandImpl(commands);
        executionCommand.setLookup("defaultKieSession");
        executionCommand.addCommand(insertObjectCommand);
        executionCommand.addCommand(fireAllRulesCommand);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", CONTAINER_ID);
        final ExecutionServerCommand executionServerCommand = new ExecutionServerCommand();
        executionServerCommand.setClient("rule");
        executionServerCommand.setOperation("executeCommands");
        executionServerCommand.setParameters(parameters);
        executionServerCommand.setBodyParam("cmd");
        executionServerCommand.setBody(executionCommand);

        final Object response = runOnExecutionServer(executionServerCommand);
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response).isInstanceOf(String.class);

        final String responseString = (String) response;
        Assertions.assertThat(responseString).contains("results");
    }
}
