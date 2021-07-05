/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.integrationtests.shared.basetests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.naming.InitialContext;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.command.KieCommands;
import org.kie.api.runtime.KieContainer;
import org.kie.server.api.exception.KieServicesException;
import org.kie.server.api.exception.KieServicesHttpException;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.admin.ExecutionErrorInstance;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.integrationtests.config.TestConfig;
import org.kie.server.integrationtests.shared.KieServerReflections;

@RunWith(Parameterized.class)
public abstract class RestJmsSharedBaseIntegrationTest extends KieServerBaseIntegrationTest {

    @Parameterized.Parameters(name = "{index}: {0} {1}")
    public static Collection<Object[]> data() {
        KieServicesConfiguration restConfiguration = createKieServicesRestConfiguration();

        Collection<Object[]> parameterData = new ArrayList<Object[]>(Arrays.asList(new Object[][]
                        {
                                {MarshallingFormat.JAXB, restConfiguration},
                                {MarshallingFormat.JSON, restConfiguration},
                                {MarshallingFormat.XSTREAM, restConfiguration}
                        }
        ));


        if (TestConfig.getRemotingUrl() != null && !TestConfig.skipJMS()) {
            KieServicesConfiguration jmsConfiguration = createKieServicesJmsConfiguration();
            parameterData.addAll(Arrays.asList(new Object[][]
                            {
                                    {MarshallingFormat.JAXB, jmsConfiguration},
                                    {MarshallingFormat.JSON, jmsConfiguration},
                                    {MarshallingFormat.XSTREAM, jmsConfiguration}
                            })
            );
        }

        return parameterData;
    }

    @Parameterized.Parameter(0)
    public MarshallingFormat marshallingFormat;

    @Parameterized.Parameter(1)
    public KieServicesConfiguration configuration;

    protected static KieCommands commandsFactory;
    protected static KieContainer kieContainer;

    protected static final String PERSON_CLASS_NAME = "org.jbpm.data.Person";
    protected static final String CUSTOM_PARAM_CLASS_NAME = "org.jbpm.data.CustomParameter";

    @Override
    protected KieServicesClient createDefaultClient() throws Exception {
        addExtraCustomClasses(extraClasses);
        if (TestConfig.isLocalServer()) {
            configuration = KieServicesFactory.newRestConfiguration(TestConfig.getKieServerHttpUrl(), null, null);
        }
        return createDefaultClient(configuration, marshallingFormat);
    }


    /**
     * Instantiate custom object.
     *
     * @param objectClassIdentifier Object class identifier - usually class name.
     * @param constructorParameters Object's constructor parameters.
     * @return Instantiated object.
     */
    protected Object createInstance(String objectClassIdentifier, Object... constructorParameters) {
        return KieServerReflections.createInstance(objectClassIdentifier, extraClasses.get(objectClassIdentifier).getClassLoader(), constructorParameters);
    }

    protected Object createPersonInstance(String name) {
        return createInstance(PERSON_CLASS_NAME, name);
    }

    protected Object createCustomParameterInstance(String name, long value) {
        return createInstance(CUSTOM_PARAM_CLASS_NAME, name, value);
    }

    /**
     * Change user used by client.
     *
     * @param username Name of user, default user taken from TestConfig in case of null parameter.
     */
    protected void changeUser(String username) throws Exception {
        if(username == null) {
            username = TestConfig.getUsername();
        }
        configuration.setUserName(username);
        client = createDefaultClient();
    }

    protected static KieServicesConfiguration createKieServicesJmsConfiguration() {
        try {
            InitialContext context = TestConfig.getInitialRemoteContext();

            Queue requestQueue = (Queue) context.lookup(TestConfig.getRequestQueueJndi());
            Queue responseQueue = (Queue) context.lookup(TestConfig.getResponseQueueJndi());
            ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup(TestConfig.getConnectionFactory());

            KieServicesConfiguration jmsConfiguration = KieServicesFactory.newJMSConfiguration(
                    connectionFactory, requestQueue, responseQueue, TestConfig.getUsername(),
                    TestConfig.getPassword());

            return jmsConfiguration;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create JMS client configuration!", e);
        }
    }

    protected static KieServicesConfiguration createKieServicesRestConfiguration() {
        return KieServicesFactory.newRestConfiguration(TestConfig.getKieServerHttpUrl(), TestConfig.getUsername(), TestConfig.getPassword());
    }

    protected void assertClientException(ThrowableAssert.ThrowingCallable callable, int expectedHttpCode, String message) {
        assertClientException(callable, expectedHttpCode, message, message);
    }

    protected void assertClientException(ThrowableAssert.ThrowingCallable callable, int expectedHttpCode, String restMessage, String jmsMessage) {
        if(configuration.isRest()) {
            Assertions.assertThatThrownBy(callable)
                    .isInstanceOf(KieServicesHttpException.class)
                    .hasFieldOrPropertyWithValue("httpCode", expectedHttpCode)
                    .hasMessageContaining(restMessage);
        } else {
            Assertions.assertThatThrownBy(callable)
                    .isInstanceOf(KieServicesException.class)
                    .hasMessageContaining(jmsMessage);
        }
    }

    protected List<ExecutionErrorInstance> filterErrorsByProcessId(Collection<ExecutionErrorInstance> errors, String processId) {
        return errors.stream().filter(error -> processId.equals(error.getProcessId())).collect(Collectors.toList());
    }

    protected List<ExecutionErrorInstance> filterErrorsByProcessInstanceId(Collection<ExecutionErrorInstance> errors, Long processInstanceId) {
        return errors.stream().filter(error -> processInstanceId.equals(error.getProcessInstanceId())).collect(Collectors.toList());
    }
}
