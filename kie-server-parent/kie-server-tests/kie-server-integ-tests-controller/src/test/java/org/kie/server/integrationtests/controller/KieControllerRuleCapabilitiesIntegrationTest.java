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
package org.kie.server.integrationtests.controller;

import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.controller.api.model.spec.ServerTemplate;
import org.kie.server.controller.client.exception.KieServerControllerClientException;
import org.kie.server.controller.impl.storage.InMemoryKieServerTemplateStorage;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.kie.server.integrationtests.shared.KieServerSynchronization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public abstract class KieControllerRuleCapabilitiesIntegrationTest<T extends KieServerControllerClientException> extends KieControllerManagementBaseTest {

    private KieServerInfo kieServerInfo;

    @BeforeClass
    public static void initialize() throws Exception {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/stateless-session-kjar").getFile());
    }

    protected abstract void assertNotFoundException(T exception);

    protected abstract void assertBadRequestException(T exception);

    @Before
    public void getKieServerInfo() {
        InMemoryKieServerTemplateStorage.getInstance().clear();
        // Getting info from currently started kie server.
        ServiceResponse<KieServerInfo> reply = client.getServerInfo();
        KieServerAssert.assertSuccess(reply);
        kieServerInfo = reply.getResult();
    }

    @After
    public void removeNewContainer() {
        KieServerDeployer.removeLocalArtifact(RELEASE_ID_101);
    }

    @Test //RHPAM-479
    public void testScanNow() throws Exception {
        ServerTemplate serverTemplate = createServerTemplate();

        ContainerSpec container = createContainerSpec(serverTemplate,
                                                      RELEASE_ID_LATEST,
                                                      KieContainerStatus.STARTED);
        KieServerSynchronization.waitForContainerWithReleaseId(client,
                                                               RELEASE_ID);

        checkKieContainerResource(RELEASE_ID_LATEST,
                                  RELEASE_ID);

        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/stateless-session-kjar101").getFile());
        controllerClient.scanNow(container);

        KieServerSynchronization.waitForContainerWithReleaseId(client,
                                                               RELEASE_ID_101);
        checkKieContainerResource(RELEASE_ID_101,
                                  RELEASE_ID_101);
    }

    @Test
    public void testScanNowNotExistingContainer() {
        ServerTemplate serverTemplate = createServerTemplate();
        ContainerSpec container = new ContainerSpec("not-existing",
                                                    "not-existing",
                                                    serverTemplate,
                                                    RELEASE_ID,
                                                    KieContainerStatus.STARTED,
                                                    null);
        try {
            controllerClient.scanNow(container);
            fail("Should throw exception about container not found.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T) e);
            assertThat(e.getMessage()).endsWith("No container spec found for id not-existing");
        }
    }

    @Test
    public void testStartAndStopScanner() throws Exception {
        ServerTemplate serverTemplate = createServerTemplate();
        ContainerSpec container = startContainerWithVersion(RELEASE_ID,
                                                            serverTemplate);

        checkKieContainerResource(RELEASE_ID,
                                  RELEASE_ID);

        controllerClient.startScanner(container,
                                      5L);
        controllerClient.stopScanner(container);

        checkKieContainerResource(RELEASE_ID,
                                  RELEASE_ID);
    }

    @Test
    public void testStartScannerNotExistingContainer() {
        ServerTemplate serverTemplate = createServerTemplate();
        ContainerSpec container = new ContainerSpec("not-existing",
                                                    "not-existing",
                                                    serverTemplate,
                                                    RELEASE_ID,
                                                    KieContainerStatus.STARTED,
                                                    null);

        try {
            controllerClient.startScanner(container,
                                          1000L);
            fail("Should throw exception about container not found.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T) e);
            assertThat(e.getMessage()).endsWith("No container spec found for id not-existing");
        }
    }

    @Test
    public void testStopScannerNotExistingContainer() {
        ServerTemplate serverTemplate = createServerTemplate();
        ContainerSpec container = new ContainerSpec("not-existing",
                                                    "not-existing",
                                                    serverTemplate,
                                                    RELEASE_ID,
                                                    KieContainerStatus.STARTED,
                                                    null);
        try {
            controllerClient.stopScanner(container);
            fail("Should throw exception about container not found.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T) e);
            assertThat(e.getMessage()).endsWith("No container spec found for id not-existing");
        }
    }

    @Test
    public void testStopNotRunningScanner() throws Exception {
        ServerTemplate serverTemplate = createServerTemplate();
        ContainerSpec container = startContainerWithVersion(RELEASE_ID,
                                                            serverTemplate);

        checkKieContainerResource(RELEASE_ID,
                                  RELEASE_ID);

        controllerClient.stopContainer(container);
        controllerClient.stopScanner(container);
    }

    @Test
    public void testUpgradeNotExistingContainer() {
        ServerTemplate serverTemplate = createServerTemplate();
        ContainerSpec container = new ContainerSpec("not-existing",
                                                    "not-existing",
                                                    serverTemplate,
                                                    RELEASE_ID,
                                                    KieContainerStatus.STARTED,
                                                    null);
        try {
            controllerClient.upgradeContainer(container,
                                              RELEASE_ID_101);
            fail("Should throw exception about container not found.");
        } catch (KieServerControllerClientException e) {
            assertNotFoundException((T) e);
            assertThat(e.getMessage()).endsWith("No container spec found for id not-existing");
        }
    }

    private ContainerSpec startContainerWithVersion(ReleaseId releaseId,
                                                    ServerTemplate serverTemplate) throws Exception {
        ContainerSpec container = createContainerSpec(serverTemplate,
                                                      releaseId,
                                                      KieContainerStatus.STARTED);
        KieServerSynchronization.waitForContainerWithReleaseId(client,
                                                               releaseId);
        return container;
    }

    protected ServerTemplate createServerTemplate() {
        return createServerTemplate(kieServerInfo.getServerId(),
                                    kieServerInfo.getName(),
                                    kieServerInfo.getLocation());
    }

    protected void checkKieContainerResource(ReleaseId expectedReleaseId,
                                             ReleaseId expectedResolvedReleaseId) {
        ServiceResponse<KieContainerResource> containerInfo = client.getContainerInfo(CONTAINER_ID);
        assertThat(containerInfo.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        KieContainerResource container = containerInfo.getResult();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(container.getContainerId()).isEqualTo(CONTAINER_ID);
            softly.assertThat(container.getStatus()).isEqualTo(KieContainerStatus.STARTED);
            softly.assertThat(container.getReleaseId()).isEqualTo(expectedReleaseId);
            softly.assertThat(container.getResolvedReleaseId()).isEqualTo(expectedResolvedReleaseId);
        });
    }
}