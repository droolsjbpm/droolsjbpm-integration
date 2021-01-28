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

package org.kie.server.integrationtests.router.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.server.common.rest.Authenticator;
import org.kie.server.integrationtests.config.TestConfig;
import org.kie.server.integrationtests.router.client.KieServerRouterClient;
import org.kie.server.router.Configuration;
import org.kie.server.router.ConfigurationListener;
import org.kie.server.router.ContainerInfo;
import org.kie.server.router.KieServerRouter;
import org.kie.server.router.KieServerRouterConstants;
import org.kie.server.router.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.WireMockServer;

public class KieServerRouterUnavailabilityRecoveryTest {

    private static final Logger logger = LoggerFactory.getLogger(KieServerRouterUnavailabilityRecoveryTest.class);

    private static KieServerRouter router;
    private static File repository;

    private static String serverUrl;
    
    private WireMockServer wireMockServer;
    private int mockKieServerPort;
    
    private TestConfigurationListener listener = new TestConfigurationListener();

    @Before
    public void startStandaloneRouter(){
        System.setProperty(KieServerRouterConstants.KIE_SERVER_CONTROLLER_ATTEMPT_INTERVAL, "1");
        mockKieServerPort = allocatePort();
        configureMockServer();
                
        // setup repository for config of router
        repository = new File("target/unavailability-router-repo");
        repository.mkdirs();
        System.setProperty(KieServerRouterConstants.ROUTER_REPOSITORY_DIR, repository.getAbsolutePath());

        Configuration config = new Configuration();
        config.addContainerHost("container1", "http://localhost:" + mockKieServerPort);       
        config.addServerHost("server1", "http://localhost:" + mockKieServerPort);
        ContainerInfo containerInfo = new ContainerInfo("container1", "test", "org.kie:test:1.0");
        config.addContainerInfo(containerInfo);
        
        FileRepository repo = new FileRepository(repository);
        repo.persist(config);

        // setup and start router
        Integer port = allocatePort();
        router = new KieServerRouter();
        router.start("localhost", port, listener);

        serverUrl = "http://localhost:" + port;
    }

    @After
    public void stopStandaloneRouter(){
        // stop router and remove its config
        System.clearProperty(KieServerRouterConstants.ROUTER_REPOSITORY_DIR);
        System.clearProperty(KieServerRouterConstants.KIE_SERVER_CONTROLLER_ATTEMPT_INTERVAL);
        router.stop(true);
        router = null;

        Stream.of(repository.listFiles()).forEach(f-> f.delete());
        repository.delete();
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @Test
    public void testReactToUnavailableServersInRouterContainerBasedOperations() throws Exception {

        Response response = null;
        try (KieServerRouterClient routerClient = new KieServerRouterClient(serverUrl)) {
            Configuration initialConfig = routerClient.getRouterConfig();
            assertEquals(1, initialConfig.getHostsPerContainer().size());
            assertEquals(1, initialConfig.getHostsPerServer().size());

            assertEquals(1, initialConfig.getHostsPerContainer().get("container1").size());
            assertEquals(1, initialConfig.getHostsPerServer().get("server1").size());
            
            WebTarget clientRequest = newRequest(serverUrl + "/containers");
            logger.debug("[GET] " + clientRequest.getUri());

            response = clientRequest.request(MediaType.APPLICATION_XML_TYPE).get();
            Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            response.close();

            Configuration config = routerClient.getRouterConfig();

            assertEquals(1, config.getHostsPerContainer().size());
            assertEquals(1, config.getHostsPerServer().size());

            assertEquals(1, config.getHostsPerContainer().get("container1").size());
            assertEquals(1, config.getHostsPerServer().get("server1").size());
            
            wireMockServer.stop();
            
            clientRequest = newRequest(serverUrl + "/containers");
            logger.debug("[GET] " + clientRequest.getUri());

            response = clientRequest.request(MediaType.APPLICATION_XML_TYPE).get();
            Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            response.close();
            
            config = routerClient.getRouterConfig();

            assertEquals(0, config.getHostsPerContainer().size());
            assertEquals(0, config.getHostsPerServer().size());

            assertNull(config.getHostsPerContainer().get("container1"));
            assertNull(config.getHostsPerServer().get("server1"));
            
            CountDownLatch latch = listener.activate();
            wireMockServer.start();
            
            latch.await(5, TimeUnit.SECONDS);
            
            clientRequest = newRequest(serverUrl + "/containers");
            logger.debug("[GET] " + clientRequest.getUri());

            response = clientRequest.request(MediaType.APPLICATION_XML_TYPE).get();
            Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            response.close();

            config = routerClient.getRouterConfig();

            assertEquals(1, config.getHostsPerContainer().size());
            assertEquals(1, config.getHostsPerServer().size());

            assertEquals(1, config.getHostsPerContainer().get("container1").size());
            assertEquals(1, config.getHostsPerServer().get("server1").size());
        } finally {
            if(response != null) {
                response.close();
            }
        }
    }


    private static int allocatePort() {
        try {
            ServerSocket server = new ServerSocket(0);
            int port = server.getLocalPort();
            server.close();
            return port;
        } catch (IOException e) {
            // failed to dynamically allocate port, try to use hard coded one
            return 9783;
        }
    }
    private static Client httpClient;

    protected WebTarget newRequest(String uriString) {

        if(httpClient == null) {
            httpClient = new ResteasyClientBuilder()
                    .establishConnectionTimeout(10, TimeUnit.SECONDS)
                    .socketTimeout(10, TimeUnit.SECONDS)
                    .register(new Authenticator(TestConfig.getUsername(), TestConfig.getPassword()))
                    .build();
        }
        return httpClient.target(uriString);
    }
    
    private void configureMockServer() {
        wireMockServer = new WireMockServer(mockKieServerPort);
        wireMockServer.stubFor(get(urlEqualTo("/containers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<response type=\"SUCCESS\" msg=\"Kie Server info\">\n" +
                                "  <kie-server-info>\n" +
                                "    <version>1.0.0</version>\n" +
                                "  </kie-server-info>\n" +
                                "</response>")));
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("<response type=\"SUCCESS\" msg=\"Kie Server info\">\n" +
                                "  <kie-server-info>\n" +
                                "    <version>1.0.0</version>\n" +
                                "  </kie-server-info>\n" +
                                "</response>")));
        wireMockServer.start();
    }
    
    private class TestConfigurationListener implements ConfigurationListener {
        private CountDownLatch latch;
        @Override
        public void onServerAdded(String serverId, String serverUrl) {
            if (latch != null) {
                latch.countDown();
            }
        }
        
        
        public CountDownLatch activate() {
            latch = new CountDownLatch(1);
            
            return latch;
        }
    }
}
