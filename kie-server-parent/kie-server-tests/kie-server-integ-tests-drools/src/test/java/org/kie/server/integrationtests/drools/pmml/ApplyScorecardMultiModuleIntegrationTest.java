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

public class ApplyScorecardMultiModuleIntegrationTest extends PMMLApplyModelBaseTest {

    private static Logger logger = LoggerFactory.getLogger(ApplyScorecardMultiModuleIntegrationTest.class);

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "scorecard_mm", "1.0.0.Final");

    private static final String CONTAINER_ID = "scorecard_mm";
    private static final String KJAR_RESOURCE_PATH = "/kjars-sources/scorecard_mm";

    private static final long EXTENDED_TIMEOUT = 900000L;
    private static ClassLoader classLoader;

    @BeforeClass
    public static void buildAndDeployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProjectFromResource(KJAR_RESOURCE_PATH);
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
    public void buildAndDeployTest() {
        Thread.currentThread().setContextClassLoader(classLoader);
        PMMLRequestData request = new PMMLRequestData("123", "SimpleScorecard");
        request.setSource("test_scorecard_simple.pmml");
        request.addRequestParam("param1", 10.0);
        request.addRequestParam("param2", 15.0);

        ApplyPmmlModelCommand command = (ApplyPmmlModelCommand) ((CommandFactoryServiceImpl) commandsFactory).newApplyPmmlModel(request);
        command.setPackageName("org.kie.scorecard");
        ServiceResponse<ExecutionResults> results = ruleClient.executeCommandsWithResults(CONTAINER_ID, command);
        assertNotNull(results);

        PMML4Result resultHolder = (PMML4Result) results.getResult().getValue("results");
        assertNotNull(resultHolder);
        assertEquals("OK", resultHolder.getResultCode());
        System.out.println(resultHolder.toString());
        logger.info(resultHolder.toString());
    }
}
