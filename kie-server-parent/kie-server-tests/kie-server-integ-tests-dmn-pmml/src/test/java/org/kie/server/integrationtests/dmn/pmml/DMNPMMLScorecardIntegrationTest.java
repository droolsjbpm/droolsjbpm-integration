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


package org.kie.server.integrationtests.dmn.pmml;

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
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

/*
 * This it.test is reportedly not working in the "embedded" EE container,
 * working correctly instead with the proper EE container activated with mvn profiles ("wildfly", etc.)
 */
public class DMNPMMLScorecardIntegrationTest extends DMNPMMLKieServerBaseIntegrationTest {

    private static final ReleaseId kjar1 = new ReleaseId(
            "org.kie.server.testing", "dmn-pmml-scorecard-kjar",
            "1.0.0.Final" );

    private static final String CONTAINER_1_ID  = "dmn-pmml-scorecard";

    private static final String SCORECARD_MODEL_NAMESPACE
            = "http://www.trisotech.com/definitions/_ca466dbe-20b4-4e88-a43f-4ce3aff26e4f";
    private static final String SCORECARD_MODEL_NAME = "KiePMMLScoreCard";
    private static final String SCORECARD_DECISION_NAME = "my decision";

    private static final long EXTENDED_TIMEOUT = 900000L;

    @BeforeClass
    public static void deployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProjectFromResource("/kjars-sources/dmn-pmml-scorecard");

        kieContainer = KieServices.Factory.get().newKieContainer(kjar1);

        // Having timeout issues due to pmml -> raised timeout.
        KieServicesClient client = createDefaultStaticClient(EXTENDED_TIMEOUT);
        ServiceResponse<KieContainerResource> reply = client.createContainer(CONTAINER_1_ID, new KieContainerResource(CONTAINER_1_ID, kjar1));
        KieServerAssert.assertSuccess(reply);
    }

    /*
     * This it.test is reportedly not working in the "embedded" EE container,
     * working correctly instead with the proper EE container activated with mvn profiles ("wildfly", etc.)
     */
    @Test
    public void testDMNwithPMMLScorecard() {
        final DMNContext dmnContext = dmnClient.newContext();
        final ServiceResponse<DMNResult> serviceResponse = dmnClient.evaluateDecisionByName(
                CONTAINER_1_ID,
                SCORECARD_MODEL_NAMESPACE,
                SCORECARD_MODEL_NAME,
                SCORECARD_DECISION_NAME,
                dmnContext);

        Assertions.assertThat(serviceResponse.getType()).isEqualTo(KieServiceResponse.ResponseType.SUCCESS);

        final DMNResult dmnResult = serviceResponse.getResult();
        Assertions.assertThat(dmnResult).isNotNull();
        Assertions.assertThat(dmnResult.hasErrors()).isFalse();
        final BigDecimal result = (BigDecimal) dmnResult.getDecisionResultByName(SCORECARD_DECISION_NAME).getResult();
        Assertions.assertThat(result).isEqualTo(new BigDecimal("41.345"));
    }
}
