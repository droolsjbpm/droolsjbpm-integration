/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.services.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.scanner.KieMavenRepository;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.KieServerEnvironment;
import org.kie.server.api.commands.CommandScript;
import org.kie.server.api.commands.CreateContainerCommand;
import org.kie.server.api.commands.DisposeContainerCommand;
import org.kie.server.api.commands.UpdateReleaseIdCommand;
import org.kie.server.api.commands.UpdateScannerCommand;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceFilter;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.KieScannerResource;
import org.kie.server.api.model.KieScannerStatus;
import org.kie.server.api.model.KieServerCommand;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.KieServiceResponse.ResponseType;
import org.kie.server.api.model.Message;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.ServiceResponsesList;
import org.kie.server.api.model.Severity;
import org.kie.server.controller.api.KieServerController;
import org.kie.server.controller.api.model.KieServerSetup;
import org.kie.server.services.api.KieContainerInstance;
import org.kie.server.services.api.KieControllerNotConnectedException;
import org.kie.server.services.api.KieServerExtension;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.api.SupportedTransports;
import org.kie.server.services.impl.controller.DefaultRestControllerImpl;
import org.kie.server.services.impl.storage.KieServerState;
import org.kie.server.services.impl.storage.KieServerStateRepository;
import org.kie.server.services.impl.storage.file.KieServerStateFileRepository;

public class KieServerImplTest {

    private static final File REPOSITORY_DIR = new File("target/repository-dir");
    private static final String KIE_SERVER_ID = "kie-server-impl-test";
    private static final String GROUP_ID = "org.kie.server.test";
    private static final String DEFAULT_VERSION = "1.0.0.Final";

    private KieServerImpl kieServer;
    private org.kie.api.builder.ReleaseId releaseId;
    private String origServerId = null;

    @Before
    public void setupKieServerImpl() throws Exception {
        origServerId = KieServerEnvironment.getServerId();
        System.setProperty("org.kie.server.id", KIE_SERVER_ID);
        KieServerEnvironment.setServerId(KIE_SERVER_ID);

        FileUtils.deleteDirectory(REPOSITORY_DIR);
        FileUtils.forceMkdir(REPOSITORY_DIR);
        kieServer = new KieServerImpl(new KieServerStateFileRepository(REPOSITORY_DIR));
        kieServer.init();
    }

    @After
    public void cleanUp() {
        if (kieServer != null) {
            kieServer.destroy();
        }
        KieServerEnvironment.setServerId(origServerId);
    }
    
    @Test
    public void testReadinessCheck() {
        
        assertTrue(kieServer.isKieServerReady());
    }
    
    @Test(timeout=10000)
    public void testReadinessCheckDelayedStart() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch startedlatch = new CountDownLatch(1);
        kieServer.destroy();
        kieServer = delayedKieServer(latch, startedlatch);
        
        assertFalse(kieServer.isKieServerReady());
        latch.countDown();
        
        startedlatch.await();        
        assertTrue(kieServer.isKieServerReady());
    }
    
    @Test
    public void testHealthCheck() {
        
        List<Message> healthMessages = kieServer.healthCheck(false);
        
        assertEquals(healthMessages.size(), 0);
    }
    
    @Test
    public void testHealthCheckWithReport() {
        
        List<Message> healthMessages = kieServer.healthCheck(true);
        
        assertEquals(healthMessages.size(), 2);
        Message header = healthMessages.get(0);
        assertEquals(Severity.INFO, header.getSeverity());
        assertEquals(2, header.getMessages().size());
        
        Message footer = healthMessages.get(1);
        assertEquals(Severity.INFO, footer.getSeverity());
        assertEquals(1, footer.getMessages().size());
    }
    
    @Test(timeout=10000)
    public void testHealthCheckDelayedStart() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch startedlatch = new CountDownLatch(1);
        kieServer.destroy();
        kieServer = delayedKieServer(latch, startedlatch);
        
        assertFalse(kieServer.isKieServerReady());        
        
        List<Message> healthMessages = kieServer.healthCheck(false);
        assertEquals(healthMessages.size(), 1);
        
        Message notReady = healthMessages.get(0);
        assertEquals(Severity.ERROR, notReady.getSeverity());
        assertEquals(1, notReady.getMessages().size());
        
        latch.countDown();
        startedlatch.await();        
        assertTrue(kieServer.isKieServerReady());
        
        healthMessages = kieServer.healthCheck(false);
        
        assertEquals(healthMessages.size(), 0);
    }
    
    @Test
    public void testHealthCheckFailedContainer() {
        kieServer.destroy();
        kieServer = new KieServerImpl(new KieServerStateFileRepository(REPOSITORY_DIR)) {

            @Override
            protected List<KieContainerInstanceImpl> getContainers() {
                List<KieContainerInstanceImpl> containers = new ArrayList<>();
                KieContainerInstanceImpl container = new KieContainerInstanceImpl("test", KieContainerStatus.FAILED);
                containers.add(container);
                return containers;
            }
            
        };
        kieServer.init();
        List<Message> healthMessages = kieServer.healthCheck(false);
        
        assertEquals(healthMessages.size(), 1);
        Message failedContainer = healthMessages.get(0);
        assertEquals(Severity.ERROR, failedContainer.getSeverity());
        assertEquals(1, failedContainer.getMessages().size());
        assertEquals("KIE Container 'test' is in FAILED state", failedContainer.getMessages().iterator().next());
    }
    
    @Test
    public void testHealthCheckFailedExtension() {
        kieServer.destroy();
        kieServer = new KieServerImpl(new KieServerStateFileRepository(REPOSITORY_DIR)) {

            @Override
            public List<KieServerExtension> getServerExtensions() {
                List<KieServerExtension> extensions = new ArrayList<>();
                extensions.add(new KieServerExtension() {
                    
                    @Override
                    public List<Message> healthCheck(boolean report) {
                        List<Message> messages = KieServerExtension.super.healthCheck(report);
                        messages.add(new Message(Severity.ERROR, "TEST extension is unhealthy"));
                        return messages;
                    }

                    @Override
                    public void updateContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {                        
                    }
                    
                    @Override
                    public boolean isUpdateContainerAllowed(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
                        return false;
                    }
                    
                    @Override
                    public boolean isInitialized() {
                        return true;
                    }
                    
                    @Override
                    public boolean isActive() {
                        return true;
                    }
                    
                    @Override
                    public void init(KieServerImpl kieServer, KieServerRegistry registry) {                        
                    }
                    
                    @Override
                    public Integer getStartOrder() {
                        return 10;
                    }
                    
                    @Override
                    public List<Object> getServices() {
                        return null;
                    }
                    
                    @Override
                    public String getImplementedCapability() {
                        return "TEST";
                    }
                    
                    @Override
                    public String getExtensionName() {
                        return "TEST";
                    }
                    
                    @Override
                    public <T> T getAppComponents(Class<T> serviceType) {
                        return null;
                    }
                    
                    @Override
                    public List<Object> getAppComponents(SupportedTransports type) {
                        return null;
                    }
                    
                    @Override
                    public void disposeContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
                    }
                    
                    @Override
                    public void destroy(KieServerImpl kieServer, KieServerRegistry registry) {
                    }
                    
                    @Override
                    public void createContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
                    }
                });
                return extensions;
            }
            
        };
        kieServer.init();
        List<Message> healthMessages = kieServer.healthCheck(false);
        
        assertEquals(healthMessages.size(), 1);
        Message failedContainer = healthMessages.get(0);
        assertEquals(Severity.ERROR, failedContainer.getSeverity());
        assertEquals(1, failedContainer.getMessages().size());
        assertEquals("TEST extension is unhealthy", failedContainer.getMessages().iterator().next());
    }
    
    @Test
    public void testManagementDisabledDefault() {
        
        assertNull(kieServer.checkAccessability());
    }
    
    @Test
    public void testManagementDisabledConfigured() {
        System.setProperty(KieServerConstants.KIE_SERVER_MGMT_API_DISABLED, "true");
        try {
            kieServer.destroy();
            kieServer = new KieServerImpl(new KieServerStateFileRepository(REPOSITORY_DIR));
            kieServer.init();
            ServiceResponse<?> forbidden = kieServer.checkAccessability();
            assertForbiddenResponse(forbidden);
        } finally {
            System.clearProperty(KieServerConstants.KIE_SERVER_MGMT_API_DISABLED);
        }
    }
    
    @Test
    public void testManagementDisabledConfiguredViaCommandService() {
        System.setProperty(KieServerConstants.KIE_SERVER_MGMT_API_DISABLED, "true");
        try {
            kieServer.destroy();
            kieServer = new KieServerImpl(new KieServerStateFileRepository(REPOSITORY_DIR));
            kieServer.init();
            
            KieContainerCommandServiceImpl commandService = new KieContainerCommandServiceImpl(kieServer, kieServer.getServerRegistry());
            List<KieServerCommand> commands = new ArrayList<>();
            
            commands.add(new CreateContainerCommand());
            commands.add(new DisposeContainerCommand());
            commands.add(new UpdateScannerCommand());
            commands.add(new UpdateReleaseIdCommand());
            
            CommandScript commandScript = new CommandScript(commands);
            ServiceResponsesList responseList = commandService.executeScript(commandScript, MarshallingFormat.JAXB, null);
            assertNotNull(responseList);
            
            List<ServiceResponse<?>> responses = responseList.getResponses();
            assertEquals(4, responses.size());
            
            for (ServiceResponse<?> forbidden : responses) {
            
                assertForbiddenResponse(forbidden);
            }
            
        } finally {
            System.clearProperty(KieServerConstants.KIE_SERVER_MGMT_API_DISABLED);
        }
    }

    @Test
    // https://issues.jboss.org/browse/RHBPMS-4087
    public void testPersistScannerState() {
        String containerId = "persist-scanner-state";
        createEmptyKjar(containerId);
        // create the container and update the scanner
        KieContainerResource kieContainerResource = new KieContainerResource(containerId, new ReleaseId(releaseId));
        kieServer.createContainer(containerId, kieContainerResource);
        KieScannerResource kieScannerResource = new KieScannerResource(KieScannerStatus.STARTED, 20000L);
        kieServer.updateScanner(containerId, kieScannerResource);

        KieServerStateRepository stateRepository = new KieServerStateFileRepository(REPOSITORY_DIR);
        KieServerState state = stateRepository.load(KIE_SERVER_ID);
        Set<KieContainerResource> containers = state.getContainers();
        Assertions.assertThat(containers).hasSize(1);
        KieContainerResource container = containers.iterator().next();
        Assertions.assertThat(container.getScanner()).isEqualTo(kieScannerResource);

        KieScannerResource updatedKieScannerResource = new KieScannerResource(KieScannerStatus.DISPOSED);
        kieServer.updateScanner(containerId, updatedKieScannerResource);

        // create new state repository instance to avoid caching via 'knownStates'
        // this simulates the server restart (since the status is loaded from filesystem after restart)
        stateRepository = new KieServerStateFileRepository(REPOSITORY_DIR);
        state = stateRepository.load(KIE_SERVER_ID);
        containers = state.getContainers();
        Assertions.assertThat(containers).hasSize(1);
        container = containers.iterator().next();
        Assertions.assertThat(container.getScanner()).isEqualTo(updatedKieScannerResource);
    }

    @Test
    // https://issues.jboss.org/browse/JBPM-5288
    public void testCreateScannerWhenCreatingContainer() {
        String containerId = "scanner-state-when-creating-container";
        createEmptyKjar(containerId);

        // create the container (provide scanner info as well)
        KieContainerResource kieContainerResource = new KieContainerResource(containerId, new ReleaseId(releaseId));
        KieScannerResource kieScannerResource = new KieScannerResource(KieScannerStatus.STARTED, 20000L);
        kieContainerResource.setScanner(kieScannerResource);
        ServiceResponse<KieContainerResource> createResponse = kieServer.createContainer(containerId, kieContainerResource);
        Assertions.assertThat(createResponse.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        Assertions.assertThat(createResponse.getResult().getScanner()).isEqualTo(kieScannerResource);

        ServiceResponse<KieContainerResource> getResponse = kieServer.getContainerInfo(containerId);
        Assertions.assertThat(getResponse.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
        Assertions.assertThat(getResponse.getResult().getScanner()).isEqualTo(kieScannerResource);
    }

    @Test
    public void testExecutorPropertiesInStateRepository() {
        KieServerStateFileRepository stateRepository = new KieServerStateFileRepository(REPOSITORY_DIR);
        KieServerState state = stateRepository.load(KIE_SERVER_ID);

        String executorInterval = state.getConfiguration().getConfigItemValue(KieServerConstants.CFG_EXECUTOR_INTERVAL);
        String executorRetries = state.getConfiguration().getConfigItemValue(KieServerConstants.CFG_EXECUTOR_RETRIES);
        String executorPool = state.getConfiguration().getConfigItemValue(KieServerConstants.CFG_EXECUTOR_POOL);
        String executorTimeUnit = state.getConfiguration().getConfigItemValue(KieServerConstants.CFG_EXECUTOR_TIME_UNIT);
        String executorJMSQueue = state.getConfiguration().getConfigItemValue(KieServerConstants.CFG_EXECUTOR_JMS_QUEUE);
        String executorDisabled = state.getConfiguration().getConfigItemValue(KieServerConstants.CFG_EXECUTOR_DISABLED);

        assertNull(executorInterval);
        assertNull(executorRetries);
        assertNull(executorPool);
        assertNull(executorTimeUnit);
        assertNull(executorJMSQueue);
        assertNull(executorDisabled);
        try {
            System.setProperty(KieServerConstants.CFG_EXECUTOR_INTERVAL, "4");
            System.setProperty(KieServerConstants.CFG_EXECUTOR_RETRIES, "7");
            System.setProperty(KieServerConstants.CFG_EXECUTOR_POOL, "11");
            System.setProperty(KieServerConstants.CFG_EXECUTOR_TIME_UNIT, "HOURS");
            System.setProperty(KieServerConstants.CFG_EXECUTOR_JMS_QUEUE, "queue/MY.OWN.QUEUE");
            System.setProperty(KieServerConstants.CFG_EXECUTOR_DISABLED, "true");

            stateRepository.clearCache();

            state = stateRepository.load(KIE_SERVER_ID);

            executorInterval = state.getConfiguration().getConfigItemValue(KieServerConstants.CFG_EXECUTOR_INTERVAL);
            executorRetries = state.getConfiguration().getConfigItemValue(KieServerConstants.CFG_EXECUTOR_RETRIES);
            executorPool = state.getConfiguration().getConfigItemValue(KieServerConstants.CFG_EXECUTOR_POOL);
            executorTimeUnit = state.getConfiguration().getConfigItemValue(KieServerConstants.CFG_EXECUTOR_TIME_UNIT);
            executorJMSQueue = state.getConfiguration().getConfigItemValue(KieServerConstants.CFG_EXECUTOR_JMS_QUEUE);
            executorDisabled = state.getConfiguration().getConfigItemValue(KieServerConstants.CFG_EXECUTOR_DISABLED);

            assertNotNull(executorInterval);
            assertNotNull(executorRetries);
            assertNotNull(executorPool);
            assertNotNull(executorTimeUnit);
            assertNotNull(executorJMSQueue);
            assertNotNull(executorDisabled);

            assertEquals("4", executorInterval);
            assertEquals("7", executorRetries);
            assertEquals("11", executorPool);
            assertEquals("HOURS", executorTimeUnit);
            assertEquals("queue/MY.OWN.QUEUE", executorJMSQueue);
            assertEquals("true", executorDisabled);
        } finally {
            System.clearProperty(KieServerConstants.CFG_EXECUTOR_INTERVAL);
            System.clearProperty(KieServerConstants.CFG_EXECUTOR_RETRIES);
            System.clearProperty(KieServerConstants.CFG_EXECUTOR_POOL);
            System.clearProperty(KieServerConstants.CFG_EXECUTOR_TIME_UNIT);
            System.clearProperty(KieServerConstants.CFG_EXECUTOR_JMS_QUEUE);
            System.clearProperty(KieServerConstants.CFG_EXECUTOR_DISABLED);
        }
    }

    private void assertReleaseIds(String containerId, ReleaseId configuredReleaseId, ReleaseId resolvedReleaseId, long timeoutMillis) throws InterruptedException {
        long timeSpentWaiting = 0;
        while (timeSpentWaiting < timeoutMillis) {
            ServiceResponse<KieContainerResourceList> listResponse = kieServer.listContainers(KieContainerResourceFilter.ACCEPT_ALL);
            Assertions.assertThat(listResponse.getType()).isEqualTo(ServiceResponse.ResponseType.SUCCESS);
            List<KieContainerResource> containers = listResponse.getResult().getContainers();
            for (KieContainerResource container : containers) {
                if (configuredReleaseId.equals(container.getReleaseId())
                        && resolvedReleaseId.equals(container.getResolvedReleaseId())) {
                    return;
                }
            }
            Thread.sleep(200);
            timeSpentWaiting += 200L;
        }
        Assertions.fail("Waiting too long for container " + containerId + " to have expected releaseIds updated! " +
                "expected: releaseId=" + configuredReleaseId + ", resolvedReleaseId=" + resolvedReleaseId);
    }


    private void createEmptyKjar(String artifactId) {
        createEmptyKjar(artifactId, DEFAULT_VERSION);
    }
    private void createEmptyKjar(String artifactId, String version) {
        // create empty kjar; content does not matter
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kfs = kieServices.newKieFileSystem();
        releaseId = kieServices.newReleaseId(GROUP_ID, artifactId, version);
        KieModule kieModule = kieServices.newKieBuilder( kfs ).buildAll().getKieModule();
        KieMavenRepository.getKieMavenRepository().installArtifact( releaseId, (InternalKieModule)kieModule, createPomFile( artifactId, version ) );
        kieServices.getRepository().addKieModule(kieModule);
    }

    private File createPomFile(String artifactId, String version) {
        String pomContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "\n" +
                "  <groupId>org.kie.server.test</groupId>\n" +
                "  <artifactId>" + artifactId + "</artifactId>\n" +
                "  <version>" + version + "</version>\n" +
                "  <packaging>pom</packaging>\n" +
                "</project>";
        try {
            File file = new File("target/" + artifactId + "-1.0.0.Final.pom");
            FileUtils.write(file, pomContent);
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private KieServerImpl delayedKieServer(CountDownLatch latch, CountDownLatch startedlatch) {
        KieServerImpl server = new KieServerImpl(new KieServerStateFileRepository(REPOSITORY_DIR)) {

            @Override
            public void markAsReady() {
                super.markAsReady();
                startedlatch.countDown();
            }

            @Override
            protected KieServerController getController() {
                return new DefaultRestControllerImpl(getServerRegistry()) {                    
                    @Override
                    public KieServerSetup connect(KieServerInfo serverInfo) {
                        try {
                            if (latch.await(10, TimeUnit.MILLISECONDS)) {
                                return new KieServerSetup();
                            }
                            throw new KieControllerNotConnectedException("Unable to connect to any controller");
                        } catch (InterruptedException e) {
                            throw new KieControllerNotConnectedException("Unable to connect to any controller");
                        }
                    }
                    
                };
            }
            
        };
        server.init();
        return server;
    }
    
    private void assertForbiddenResponse(ServiceResponse<?> forbidden) {        
        assertNotNull(forbidden);
        
        assertEquals(ResponseType.FAILURE, forbidden.getType());
        assertEquals("KIE Server management api is disabled", forbidden.getMsg());
    }

}
