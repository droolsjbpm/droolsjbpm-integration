/**
 * Copyright (C) 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.server.services.openshift.impl.storage.cloud;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.thoughtworks.xstream.XStream;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.DoneableConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.services.impl.StartupStrategyProvider;
import org.kie.server.services.impl.storage.KieServerState;
import org.kie.server.services.impl.storage.KieServerStateRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kie.server.services.openshift.impl.storage.cloud.KieServerStateCloudRepository.initializeXStream;

public class KieServerStateOpenShiftRepositoryTest {

    private static final String KIE_SERVER_STARTUP_IN_PROGRESS_KEY_PREFIX = "org.kie.server.services/";
    private static final String KIE_SERVER_STARTUP_IN_PROGRESS_VALUE = "kie.server.startup_in_progress";

    // Must match the the kie server id specified at test file
    private static final String TEST_KIE_SERVER_ID = "myapp2-kieserver";
    private static XStream xs = initializeXStream();
    private static Supplier<OpenShiftClient> clouldClientHelper = () -> (new CloudClientFactory() {
    }).createOpenShiftClient();

    /**
     *  Must match current project name associated with OpenShift login
     *  if test against real OCP/K8S cluster
     */
    private String testNamespace = "myproject";
    private OpenShiftClient client;
    private KieServerStateOpenShiftRepository repo;

    @Rule
    public OpenShiftServer server = new OpenShiftServer(false, true);

    @Before
    public void setup() {

        /**
         *  Get fabric8 client and connect to real OpenShift/Kubernetes server
         *  Require to set the following environment properties to run:
         *  	KUBERNETES_MASTER
         *  	KUBERNETES_AUTH_TOKEN
         *  	KIE_SERVER_ID (Value doesn't matter)
         */
        if (System.getenv("KIE_SERVER_ID") != null) {
            System.setProperty(KieServerConstants.KIE_SERVER_STARTUP_STRATEGY, "OpenShiftStartupStrategy");
            // If KIE_SERVER_ID is set, connect to real OCP/K8S server
            client = clouldClientHelper.get();
        } else {
            // Get client from MockKubernetes Server
            client = server.getOpenshiftClient();

            // The default namespace for MockKubernetes Server is 'test'
            testNamespace = "test";
        }

        // Load testing KieServerState ConfigMap data into mock server from file
        ConfigMap cfm = client.configMaps()
                              .load(KieServerStateOpenShiftRepositoryTest.class
                              .getResourceAsStream("/test-kieserver-state-config-map.yml")).get();

        client.configMaps().inNamespace(testNamespace).createOrReplace(cfm);

        // Create cloud repository instance with mock K8S server test client
        repo = new KieServerStateOpenShiftRepository() {

            @Override
            public OpenShiftClient createOpenShiftClient() {
                return client;
            }

            @Override
            public KubernetesClient createKubernetesClient() {
                return client;
            }

            @Override
            public boolean isKieServerReady() {
                return true;
            }
        };

        repo.load(TEST_KIE_SERVER_ID);
    }

    @Test
    public void testLiteralConfigMap() throws InterruptedException {
        HashMap<String, String> data = new HashMap<>();
        data.put("foo", "bar");
        data.put("cheese", "gouda");

        Map<String, String> ant = new ConcurrentHashMap<>();
        ant.put("services.server.kie.org/kie-server-state.changeTimestamp",
                ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT));

        Map<String, String> lab = new ConcurrentHashMap<>();
        lab.put("startup_in_progress", "kieserverId");

        client.configMaps().inNamespace(testNamespace)
              .createOrReplace(new ConfigMapBuilder()
                                                     .withNewMetadata()
                                                     .withName("cfg1")
                                                     .endMetadata()
                                                     .addToData(data).build());

        try {
            client.configMaps().inNamespace(testNamespace)
                  .create(new ConfigMapBuilder()
                                                .withNewMetadata()
                                                .withName("cfg1")
                                                .withLabels(lab)
                                                .withAnnotations(ant)
                                                .endMetadata()
                                                .addToData(data).build());
        } catch (Exception e) {
            // If test against real cluster, second create will fail
        }

        // If test against real cluster, uncomment out the following
        //        assertTrue(client.configMaps().inNamespace(testNamespace)
        //                   .withLabel("startup_in_progress", "kieserverId")
        //                   .list().getItems().isEmpty());

        client.configMaps().inNamespace(testNamespace)
              .createOrReplace(new ConfigMapBuilder()
                                                     .withNewMetadata()
                                                     .withName("cfg2")
                                                     .withLabels(lab)
                                                     .withAnnotations(ant)
                                                     .endMetadata()
                                                     .addToData(data).build());

        ConfigMapList cfgList = client.configMaps().inNamespace(testNamespace)
                                      .withLabel("startup_in_progress", "kieserverid")
                                      .list();

        Map<String, String> keys = client.configMaps()
                                         .inNamespace(testNamespace)
                                         .withName("cfg1").get().getData();

        assertEquals("gouda", keys.get("cheese"));
        assertEquals("bar", keys.get("foo"));

        client.configMaps().inNamespace(testNamespace).delete(cfgList.getItems());

        assertTrue(client.configMaps().inNamespace(testNamespace)
                         .withLabel("startup_in_progress", "kieserverid")
                         .list().getItems().isEmpty());
    }

    @Test
    public void testKieServerStateConfigMap() throws InterruptedException {
        Resource<ConfigMap, DoneableConfigMap> configMapResource = client.configMaps().inNamespace(testNamespace)
                                                                         .withName(TEST_KIE_SERVER_ID);

        ConfigMap configMap = configMapResource.get();
        assertEquals(TEST_KIE_SERVER_ID, configMap.getMetadata().getName());

        Map<String, String> data = configMap.getData();

        // Avoid attribute name having '.' as it confuses jsonpath
        String srvStateInXML = data.get(KieServerStateCloudRepository.CFG_MAP_DATA_KEY);
        KieServerState kieServerState = (KieServerState) xs.fromXML(srvStateInXML);

        assertNotNull(kieServerState);
        assertEquals(TEST_KIE_SERVER_ID,
                     kieServerState.getConfiguration().getConfigItem(KieServerConstants.KIE_SERVER_ID).getValue());

        // Since KubenetesClient is AutoCloseable, try-with-resource can be used
        assertTrue(client instanceof AutoCloseable);
    }

    @Test
    public void testStoreAndLoad() throws InterruptedException {
        // Retrieve the seeded KSSConfigMap and Store it under new name
        String srvStateInXML = client.configMaps().inNamespace(testNamespace)
                                     .withName(TEST_KIE_SERVER_ID).get().getData().get(KieServerStateCloudRepository.CFG_MAP_DATA_KEY);
        KieServerState kieServerState = (KieServerState) xs.fromXML(srvStateInXML);

        assertNotNull(kieServerState);
        
        /**
         * At normal situation, DC will not be null otherwise there will no KieServer Pod running
         */
        createDummyDC();
        repo.store(TEST_KIE_SERVER_ID, kieServerState);
        assertNotNull(client.configMaps().inNamespace(testNamespace)
                            .withName(TEST_KIE_SERVER_ID).get());

        KieServerState kssLoaded = repo.load(TEST_KIE_SERVER_ID);
        assertNotNull(kssLoaded);
        assertEquals(TEST_KIE_SERVER_ID,
                     kssLoaded.getConfiguration().getConfigItem(KieServerConstants.KIE_SERVER_ID).getValue());

        KieContainerResource[] kcr = kssLoaded.getContainers()
                                              .<KieContainerResource> toArray(new KieContainerResource[]{});

        assertEquals(2, kcr.length);
        assertEquals("mortgages_1.0.0-SNAPSHOT", kcr[0].getContainerId());
        assertEquals("mortgage-process_1.0.0-SNAPSHOT", kcr[1].getContainerId());
    }

    @Test
    public void testStoreAndLoadWithRolloutTrigger() throws InterruptedException {
        // Retrieve the seeded KSSConfigMap and Store it under new name
        String srvStateInXML = client.configMaps().inNamespace(testNamespace)
                                     .withName(TEST_KIE_SERVER_ID).get().getData().get(KieServerStateCloudRepository.CFG_MAP_DATA_KEY);
        KieServerState kieServerState = (KieServerState) xs.fromXML(srvStateInXML);

        repo.store(TEST_KIE_SERVER_ID, kieServerState);
        assertNotNull(client.configMaps().inNamespace(testNamespace)
                            .withName(TEST_KIE_SERVER_ID)
                            .get()
                            .getMetadata()
                            .getAnnotations()
                            .containsKey(KieServerStateCloudRepository.ROLLOUT_REQUIRED));

    }

    @Test(expected = Exception.class)
    public void testLoadWithNullServerId() {
        repo.load(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testLoadWithInvalidServerId() {
        repo.load("dummy");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStoreWithEmptyKieServerState() {
        repo.store("dummy", new KieServerState());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStoreWithMissMatchingServerId() {
        KieServerState kss = repo.load(TEST_KIE_SERVER_ID);
        repo.store("dummy", kss);
    }

    @Test(expected = IllegalStateException.class)
    public void testStoreWithoutPreSeededConfigMap() {
        KieServerState kss = repo.load(TEST_KIE_SERVER_ID);

        // Remove the configmap created at Setup to set a no pre-seeded scenario
        client.configMaps().inNamespace(testNamespace).withName(TEST_KIE_SERVER_ID).delete();

        repo.store(TEST_KIE_SERVER_ID, kss);
    }

    @Test
    public void testAnnotation() {

        String kieServer1 = KIE_SERVER_STARTUP_IN_PROGRESS_KEY_PREFIX + UUID.randomUUID().toString();
        String kieServer2 = KIE_SERVER_STARTUP_IN_PROGRESS_KEY_PREFIX + UUID.randomUUID().toString();

        ConfigMap cm = client.configMaps().withName(TEST_KIE_SERVER_ID).get();
        ObjectMeta md = cm.getMetadata();
        Map<String, String> ann = md.getAnnotations() == null ? new HashMap<>() : md.getAnnotations();
        md.setAnnotations(ann);
        ann.put(kieServer1, KIE_SERVER_STARTUP_IN_PROGRESS_VALUE);
        ann.put(kieServer2, KIE_SERVER_STARTUP_IN_PROGRESS_VALUE);
        client.configMaps().createOrReplace(cm);

        assertNotNull(client.configMaps().withName(TEST_KIE_SERVER_ID).get().getMetadata().getAnnotations());
        assertTrue(client.configMaps().withName(TEST_KIE_SERVER_ID).get().getMetadata()
                         .getAnnotations().containsKey(kieServer1));
        assertTrue(client.configMaps().withName(TEST_KIE_SERVER_ID).get().getMetadata()
                         .getAnnotations().containsKey(kieServer2));
        assertTrue(
                   client.configMaps().withName(TEST_KIE_SERVER_ID).get().getMetadata()
                         .getAnnotations().containsValue(KIE_SERVER_STARTUP_IN_PROGRESS_VALUE));

        ann.remove(kieServer1);
        client.configMaps().createOrReplace(cm);

        assertTrue(
                   client.configMaps().withName(TEST_KIE_SERVER_ID).get().getMetadata()
                         .getAnnotations().containsValue(KIE_SERVER_STARTUP_IN_PROGRESS_VALUE));

        ann.remove(kieServer2);
        client.configMaps().createOrReplace(cm);

        assertFalse(
                    client.configMaps().withName(TEST_KIE_SERVER_ID).get().getMetadata()
                          .getAnnotations().containsValue(KIE_SERVER_STARTUP_IN_PROGRESS_VALUE));

    }

    @Test
    public void testNPEWhenNoServiceProviderConfig() {
        ServiceLoader<KieServerStateRepository> serverStateRepos = ServiceLoader.load(KieServerStateRepository.class);
        assertNotNull(serverStateRepos);

        String repoType = StartupStrategyProvider.get().getStrategy().getRepositoryType();
        for (KieServerStateRepository repo : serverStateRepos) {
            assertNotNull(repo);
            assertNotNull(repo.getClass().getSimpleName());
            if (repo.getClass().getSimpleName().equals(repoType)) {
                fail("Unexpected repo type: " + repoType);
            }
        }
    }

    @Test
    public void testCreateAndLoad() {
        // Retrieve the seeded KSSConfigMap and Store it under new name
        String srvStateInXML = client.configMaps().inNamespace(testNamespace)
                                     .withName(TEST_KIE_SERVER_ID).get().getData().get(KieServerStateCloudRepository.CFG_MAP_DATA_KEY);
        KieServerState kieServerState = (KieServerState) xs.fromXML(srvStateInXML);

        assertNotNull(kieServerState);

        kieServerState.getConfiguration()
                      .getConfigItem(KieServerConstants.KIE_SERVER_ID).setValue(TEST_KIE_SERVER_ID + "_NEW");

        repo.create(kieServerState);
        assertNotNull(client.configMaps().inNamespace(testNamespace)
                            .withName(TEST_KIE_SERVER_ID + "_NEW").get());

        assertNotNull(repo.load(TEST_KIE_SERVER_ID + "_NEW"));
    }

    @Test
    public void testDeleteAndExists() {
        assertTrue(repo.exists(TEST_KIE_SERVER_ID));
        repo.delete(TEST_KIE_SERVER_ID);
        assertTrue(!repo.exists(TEST_KIE_SERVER_ID));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDeleteAttachedKieServerStateIsNotAllowed() {
        assertTrue(repo.exists(TEST_KIE_SERVER_ID));
        // Create a dummy DeploymentConfig to simulate attached KieServeState scenario
        createDummyDC();
        repo.delete(TEST_KIE_SERVER_ID);
    }

 
    @Test
    public void testRetrieveAllKieServerIdsAndStates() {
        KieServerState state = repo.load(TEST_KIE_SERVER_ID);
        state.getConfiguration()
             .getConfigItem(KieServerConstants.KIE_SERVER_ID).setValue(TEST_KIE_SERVER_ID + "_1");
        // Create new KieServer    
        repo.create(state);

        state.getConfiguration()
             .getConfigItem(KieServerConstants.KIE_SERVER_ID).setValue(TEST_KIE_SERVER_ID + "_2");
        // Create new KieServer    
        repo.create(state);

        List<String> kIds = repo.retrieveAllKieServerIds();
        assertEquals(3, kIds.size());
        assertTrue(kIds.contains(TEST_KIE_SERVER_ID));
        assertTrue(kIds.contains(TEST_KIE_SERVER_ID + "_1"));
        assertTrue(kIds.contains(TEST_KIE_SERVER_ID + "_2"));

        List<KieServerState> kStates = repo.retrieveAllKieServerStates();
        assertEquals(3, kStates.size());

        repo.delete(TEST_KIE_SERVER_ID);
        repo.delete(TEST_KIE_SERVER_ID + "_1");
        repo.delete(TEST_KIE_SERVER_ID + "_2");

        assertEquals(0, repo.retrieveAllKieServerIds().size());
        assertEquals(0, repo.retrieveAllKieServerStates().size());
    }

    @Test
    public void testRetrieveAllKieServerIdsAndStatesWithContaminatedCF() {
        // Adding a contaminated configmap which does not include required label
        ConfigMap cfm = client.configMaps()
                .load(KieServerStateOpenShiftRepositoryTest.class
                .getResourceAsStream("/test-kieserver-state-config-map-without-label.yml")).get();

        client.configMaps().inNamespace(testNamespace).createOrReplace(cfm);
        
        // Now there are two configmaps in the test namespace
        assertEquals(2, client.configMaps().list().getItems().size());
        
        // But still have only 1 valid KieServerState
        List<String> kIds = repo.retrieveAllKieServerIds();
        assertEquals(1, kIds.size());

        List<KieServerState> kStates = repo.retrieveAllKieServerStates();
        assertEquals(1, kStates.size());
    }

    @After
    public void tearDown() {
        client.configMaps().inNamespace(testNamespace).delete();
        client.close();
    }

    protected void createDummyDC() {
        client.deploymentConfigs().inNamespace(testNamespace).createOrReplaceWithNew()
              .withNewMetadata()
                .withName(TEST_KIE_SERVER_ID)
              .endMetadata()
              .withNewSpec()
                .withReplicas(0)
                  .addNewTrigger()
                    .withType("ConfigChange")
                  .endTrigger()
                .withNewTemplate()
                  .withNewMetadata()
                    .addToLabels("app", "kieserver")
                  .endMetadata()
                  .withNewSpec()
                    .addNewContainer()
                      .withName("kieserver")
                      .withImage("kiserver")
                      .addNewPort()
                        .withContainerPort(80)
                      .endPort()
                    .endContainer()
                  .endSpec()
                .endTemplate()
              .endSpec()
              .done();
    }
}
