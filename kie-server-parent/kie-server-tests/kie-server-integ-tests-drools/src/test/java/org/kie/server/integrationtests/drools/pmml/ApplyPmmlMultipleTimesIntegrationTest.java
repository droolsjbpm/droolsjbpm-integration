/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.integrationtests.drools.pmml;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.drools.core.command.impl.CommandFactoryServiceImpl;
import org.drools.core.command.runtime.pmml.ApplyPmmlModelCommand;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.pmml.PMML4Result;
import org.kie.api.pmml.PMMLRequestData;
import org.kie.api.runtime.ExecutionResults;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;

public class ApplyPmmlMultipleTimesIntegrationTest extends PMMLApplyModelBaseTest {

    private static final ReleaseId releaseId =
            new ReleaseId("org.kie.server.testing", "pmml-regression", "1.0.0.Final");

    private static final String CONTAINER_ID = "regression";

    private static final long EXTENDED_TIMEOUT = 900000L;

    @BeforeClass
    public static void buildAndDeployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProjectFromResource("/kjars-sources/pmml-regression");

        // Having timeout issues due to pmml -> raised timeout.
        KieServicesClient client = createDefaultStaticClient(EXTENDED_TIMEOUT);
        ServiceResponse<KieContainerResource> reply = client.createContainer(CONTAINER_ID, new KieContainerResource(CONTAINER_ID, releaseId));
        KieServerAssert.assertSuccess(reply);
    }

    @Test
    public void testMultipleRegressionEvaluation() {
        double fld1 = 12.0;
        double fld2 = 25.0;
        String fld3 = "x";
        PMMLRequestData request = new PMMLRequestData("123", "LinReg");
        request.setSource("test_regression.pmml");
        request.addRequestParam("fld1", fld1);
        request.addRequestParam("fld2", fld2);
        request.addRequestParam("fld3", fld3);

        ApplyPmmlModelCommand command = (ApplyPmmlModelCommand) ((CommandFactoryServiceImpl) commandsFactory)
                .newApplyPmmlModel(request);
        ServiceResponse<ExecutionResults> results = ruleClient.executeCommandsWithResults(CONTAINER_ID, command);

        PMML4Result resultHolder = (PMML4Result) results.getResult().getValue("results");
        Assertions.assertThat(resultHolder).isNotNull();
        Assertions.assertThat(resultHolder.getResultCode()).isEqualTo("OK");
        Object obj = resultHolder.getResultValue("Fld4", null);
        Assertions.assertThat(obj).isNotNull();

        Double targetValue = resultHolder.getResultValue("Fld4", "value", Double.class)
                .orElse(null);
        Assertions.assertThat(targetValue).isNotNull();
        Assertions.assertThat(targetValue).isEqualTo(simpleRegressionResult(fld1, fld2, fld3));

        fld1 = 5;
        fld2 = 8;
        fld3 = "y";
        request = new PMMLRequestData("123", "LinReg");
        request.addRequestParam("fld1", fld1);
        request.addRequestParam("fld2", fld2);
        request.addRequestParam("fld3", fld3);

        command = (ApplyPmmlModelCommand) ((CommandFactoryServiceImpl) commandsFactory)
                .newApplyPmmlModel(request);
        results = ruleClient.executeCommandsWithResults(CONTAINER_ID, command);

        resultHolder = (PMML4Result) results.getResult().getValue("results");
        Assertions.assertThat(resultHolder).isNotNull();
        Assertions.assertThat(resultHolder.getResultCode()).isEqualTo("OK");
        obj = resultHolder.getResultValue("Fld4", null);
        Assertions.assertThat(obj).isNotNull();

        targetValue = resultHolder.getResultValue("Fld4", "value", Double.class)
                .orElse(null);
        Assertions.assertThat(targetValue).isNotNull();
        Assertions.assertThat(targetValue).isEqualTo(simpleRegressionResult(fld1, fld2, fld3));
    }

    private static double simpleRegressionResult(double fld1, double fld2, String fld3) {
        double result = 0.5 + 5 * fld1 * fld1 + 2 * fld2 + fld3Coefficient(fld3) + 0.4 * fld1 * fld2;
        result = 1.0 / (1.0 + Math.exp(-result));

        return result;
    }

    private static double fld3Coefficient(String fld3) {
        final Map<String, Double> fld3ValueMap = new HashMap<>();
        fld3ValueMap.put("x", -3.0);
        fld3ValueMap.put("y", 3.0);

        return fld3ValueMap.getOrDefault(fld3, 0.0);
    }
}
