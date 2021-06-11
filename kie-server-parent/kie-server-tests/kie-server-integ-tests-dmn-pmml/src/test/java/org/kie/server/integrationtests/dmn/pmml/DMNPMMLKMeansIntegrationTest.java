/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.integrationtests.dmn.pmml;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNResult;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieServiceResponse;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;

public class DMNPMMLKMeansIntegrationTest extends DMNPMMLKieServerBaseIntegrationTest {

    private static final ReleaseId kjar1 = new ReleaseId(
            "org.kie.server.testing", "dmn-pmml-kmeans-kjar",
            "1.0.0.Final" );

    private static final String CONTAINER_1_ID  = "dmn-pml-kmeans";

    private static final String KMEANS_MODEL_NAMESPACE
            = "https://kiegroup.org/dmn/_51A1FD67-8A67-4332-9889-B718BE8B7456";
    private static final String KMEANS_MODEL_NAME = "KMeansDMN";
    private static final String KMEANS_DECISION_NAME = "Decision1";

    private static final long EXTENDED_TIMEOUT = 900000L;

    @BeforeClass
    public static void deployArtifacts() {
        if (DMNPMMLTestUtils.extendedPMMLTestsEnabled() == true) {
            KieServerDeployer.buildAndDeployCommonMavenParent();
            KieServerDeployer.buildAndDeployMavenProjectFromResource("/kjars-sources/dmn-pmml-kmeans");

            kieContainer = KieServices.Factory.get().newKieContainer(kjar1);

            // Having timeout issues due to pmml -> raised timeout.
            KieServicesClient client = createDefaultStaticClient(EXTENDED_TIMEOUT);
            ServiceResponse<KieContainerResource> reply = client.createContainer(CONTAINER_1_ID, new KieContainerResource(CONTAINER_1_ID, kjar1));
            KieServerAssert.assertSuccess(reply);
        }
    }

    /*
     * This it.test is reportedly not working in the "embedded" EE container,
     * working correctly instead with the proper EE container activated with mvn profiles ("wildfly", etc.)
     */
    @Test
    public void testDMNWithPMMLKmeans() {
        Assume.assumeTrue(DMNPMMLTestUtils.extendedPMMLTestsEnabled());

        final DMNContext dmnContext = dmnClient.newContext();
        dmnContext.set("x", 5);
        dmnContext.set("y", 5);

        final ServiceResponse<DMNResult> serviceResponse = dmnClient.evaluateDecisionByName(
                CONTAINER_1_ID,
                KMEANS_MODEL_NAMESPACE,
                KMEANS_MODEL_NAME,
                KMEANS_DECISION_NAME,
                dmnContext);

        Assertions.assertThat(serviceResponse.getType()).isEqualTo(KieServiceResponse.ResponseType.SUCCESS);
        Assertions.assertThat(serviceResponse.getResult().getDecisionResultByName("Decision1").getResult()).isNotNull();
        final Map<String, Object> decisionResult = (Map<String, Object>) serviceResponse.getResult().getDecisionResultByName("Decision1").getResult();
        Assertions.assertThat((String) decisionResult.get("predictedValue")).isEqualTo("4");
    }
}
