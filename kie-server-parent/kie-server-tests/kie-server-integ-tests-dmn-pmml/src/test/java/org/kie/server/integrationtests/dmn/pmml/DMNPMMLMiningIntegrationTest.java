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
public class DMNPMMLMiningIntegrationTest extends DMNPMMLKieServerBaseIntegrationTest {

    private static final ReleaseId kjar1 = new ReleaseId(
            "org.kie.server.testing", "dmn-pmml-mining-kjar",
            "1.0.0.Final" );

    private static final String CONTAINER_1_ID  = "dmn-pmml-mining";

    private static final String MINING_MODEL_NAMESPACE
            = "https://kiegroup.org/dmn/_0E8EC382-BB89-4877-8D37-A59B64285F05";
    private static final String MINING_MODEL_NAME = "MiningModelDMN";
    private static final String MINING_DECISION_NAME = "Decision";

    private static final long EXTENDED_TIMEOUT = 900000L;

    @BeforeClass
    public static void deployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProjectFromResource("/kjars-sources/dmn-pmml-mining");

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
    public void testDMNWithPMMLMining() {
        final DMNContext dmnContext = dmnClient.newContext();
        dmnContext.set("input1", 200);
        dmnContext.set("input2", -1);
        dmnContext.set("input3", 2);
        final ServiceResponse<DMNResult> serviceResponse = dmnClient.evaluateDecisionByName(
                CONTAINER_1_ID,
                MINING_MODEL_NAMESPACE,
                MINING_MODEL_NAME,
                MINING_DECISION_NAME,
                dmnContext);

        Assertions.assertThat(serviceResponse.getType()).isEqualTo(KieServiceResponse.ResponseType.SUCCESS);

        final DMNResult dmnResult = serviceResponse.getResult();
        Assertions.assertThat(dmnResult).isNotNull();
        Assertions.assertThat(dmnResult.hasErrors()).isFalse();
        final BigDecimal result = (BigDecimal) dmnResult.getDecisionResultByName(MINING_DECISION_NAME).getResult();
        Assertions.assertThat(result).isEqualTo(new BigDecimal("-299"));
    }
}
