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

package org.kie.server.integrationtests.jbpm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.kie.api.KieServices;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieServerConfigItem;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class ProcessServiceIntegrationTest extends JbpmKieServerBaseIntegrationTest {

    private static final KieServerConfigItem PPI_RUNTIME_STRATEGY = new KieServerConfigItem(KieServerConstants.PCFG_RUNTIME_STRATEGY, RuntimeStrategy.PER_PROCESS_INSTANCE.name(), String.class.getName());

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");

    protected static final String SORT_BY_PROCESS_ID = "ProcessId";


    @Parameterized.Parameters(name = "{index}: {0} {1}")
    public static Collection<Object[]> data() {
        KieServicesConfiguration restConfiguration = createKieServicesRestConfiguration();

        Collection<Object[]> parameterData = new ArrayList<Object[]>(Arrays.asList(new Object[][]
                        {
                                {MarshallingFormat.JAXB, restConfiguration},
                        }
        ));
        return parameterData;
    }

    @BeforeClass
    public static void buildAndDeployArtifacts() {

        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProjectFromResource("/kjars-sources/definition-project");

        kieContainer = KieServices.Factory.get().newKieContainer(releaseId);

        createContainer(CONTAINER_ID, releaseId, PPI_RUNTIME_STRATEGY);
    }

    @Override
    protected void addExtraCustomClasses(Map<String, Class<?>> extraClasses) throws Exception {
        extraClasses.put(PERSON_CLASS_NAME, Class.forName(PERSON_CLASS_NAME, true, kieContainer.getClassLoader()));
    }

    @Test
    public void testStartProcessOnDeactivatedContainer() throws Exception {
        
        Object person = createPersonInstance(USER_JOHN);

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("test", USER_MARY);
        parameters.put("number", new Integer(12345));

        List<Object> list = new ArrayList<Object>();
        list.add("item");

        parameters.put("list", list);
        parameters.put("person", person);
        Long processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);

        assertNotNull(processInstanceId);
        assertTrue(processInstanceId.longValue() > 0);

        ServiceResponse<KieContainerResource> reply = client.deactivateContainer(CONTAINER_ID);            
        KieServerAssert.assertSuccess(reply);
        
//        assertClientException(
//                () -> processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters),
//                400,
//                "Deployment " + CONTAINER_ID + " is not active");
                 
        // abort is allowed on deactivated container
        processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
        
        reply = client.activateContainer(CONTAINER_ID);            
        KieServerAssert.assertSuccess(reply);
        
        // since we activate it again new instance can be started
        processInstanceId = processClient.startProcess(CONTAINER_ID, PROCESS_ID_EVALUATION, parameters);

        processClient.abortProcessInstance(CONTAINER_ID, processInstanceId);
    }
}
