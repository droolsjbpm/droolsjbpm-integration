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

package org.kie.server.integrationtests.drools.pmml;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.drools.core.command.impl.CommandFactoryServiceImpl;
import org.drools.core.command.runtime.pmml.ApplyPmmlModelCommand;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.pmml.PMML4Result;
import org.kie.api.pmml.PMMLRequestData;
import org.kie.api.runtime.ExecutionResults;
import org.kie.scanner.KieMavenRepository;
import org.kie.scanner.KieURLClassLoader;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test used for applying a PMML model to input data.
 */
public class ApplyDecisionTreeModelIntegrationTest extends PMMLApplyModelBaseTest {

    private static Logger logger = LoggerFactory.getLogger(ApplyDecisionTreeModelIntegrationTest.class);

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "decision-tree", "1.0.0.Final");

    private static final String CONTAINER_ID = "decision-tree";

    private static final long EXTENDED_TIMEOUT = 900000L;
    private static ClassLoader classLoader;

    @BeforeClass
    public static void buildAndDeployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProjectFromResource("/kjars-sources/decision-tree");
        KieMavenRepository kmp = KieMavenRepository.getKieMavenRepository();
        File artifact = kmp.resolveArtifact(releaseId).getFile();
        if (artifact != null) {
            URL urls[] = new URL[1];
            try {
                urls[0] = artifact.toURI().toURL();
                classLoader = new KieURLClassLoader(urls, PMML4Result.class.getClassLoader());
            } catch (MalformedURLException e) {
                logger.error("FAILED TO GET CLASSLOADER !!!");
            }
        } else {
            logger.error("Could not find artifact file and no class loader created");
        }

        // Having timeout issues due to pmml -> raised timeout.
        KieServicesClient client = createDefaultStaticClient(EXTENDED_TIMEOUT);
        ServiceResponse<KieContainerResource> reply = client.createContainer(CONTAINER_ID, new KieContainerResource(CONTAINER_ID, releaseId));
        KieServerAssert.assertSuccess(reply);
    }

    @Test
    public void testApplyPmmlDecisionTree() {

        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        PMMLRequestData request = new PMMLRequestData("123", "TreeTest");
        request.setSource("test_tree_simple.pmml");
        request.addRequestParam("fld1", 30.0);
        request.addRequestParam("fld2", 60.0);
        request.addRequestParam("fld3", "false");
        request.addRequestParam("fld4", "optA");

        ApplyPmmlModelCommand command = (ApplyPmmlModelCommand) ((CommandFactoryServiceImpl) commandsFactory).newApplyPmmlModel(request);

        ServiceResponse<ExecutionResults> results = ruleClient.executeCommandsWithResults(CONTAINER_ID, command);

        PMML4Result resultHolder = (PMML4Result) results.getResult().getValue("results");
        assertNotNull(resultHolder);
        assertEquals("OK", resultHolder.getResultCode());
        Object obj = resultHolder.getResultValue("Fld5", null);
        assertNotNull(obj);

        String targetValue = resultHolder.getResultValue("Fld5", "value", String.class).orElse(null);
        assertEquals("tgtY", targetValue);
        logger.info("ApplyDecisionTreeModelIntegrationTest#testApplyPmmlDecisionTree completed successfully");
    }
}
