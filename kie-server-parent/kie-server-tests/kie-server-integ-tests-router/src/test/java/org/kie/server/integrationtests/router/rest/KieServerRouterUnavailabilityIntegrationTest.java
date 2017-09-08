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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.integrationtests.config.TestConfig;
import org.kie.server.integrationtests.router.DBExternalResource;
import org.kie.server.integrationtests.shared.basetests.RestOnlyBaseIntegrationTest;
import org.kie.server.router.Configuration;
import org.kie.server.router.ContainerInfo;
import org.kie.server.router.KieServerRouter;
import org.kie.server.router.KieServerRouterConstants;
import org.kie.server.router.repository.ConfigurationMarshaller;
import org.kie.server.router.repository.FileRepository;

import static org.junit.Assert.*;
import static org.kie.server.api.rest.RestURI.*;

public class KieServerRouterUnavailabilityIntegrationTest extends RestOnlyBaseIntegrationTest {

    @ClassRule
    public static ExternalResource StaticResource = new DBExternalResource();

    private static KieServerRouter router;
    private static File repository;

    private static String serverUrl;

    private ConfigurationMarshaller marshaller = new ConfigurationMarshaller();

    @Before
    public void startStandaloneRouter(){

        // setup repository for config of router
        repository = new File("target/unavailability-router-repo");
        repository.mkdirs();
        System.setProperty(KieServerRouterConstants.ROUTER_REPOSITORY_DIR, repository.getAbsolutePath());

        Configuration config = new Configuration();

        config.addContainerHost("container1", "http://localhost:56561");
        config.addContainerHost("container2", "http://localhost:56562");
        config.addContainerHost("container3", "http://invalid-host:8080");

        config.addServerHost("server1", "http://localhost:56561");
        config.addServerHost("server2", "http://localhost:56562");
        config.addServerHost("server3", "http://invalid-host:8080");

        ContainerInfo containerInfo = new ContainerInfo("container1", "test", "org.kie:test:1.0");
        config.addContainerInfo(containerInfo);

        ContainerInfo containerInfo2 = new ContainerInfo("container2", "test2", "org.kie:test:2.0");
        config.addContainerInfo(containerInfo2);

        ContainerInfo containerInfo3 = new ContainerInfo("container3", "test3", "org.kie:test:3.0");
        config.addContainerInfo(containerInfo3);

        FileRepository repo = new FileRepository(repository);
        repo.persist(config);

        // setup and start router
        Integer port = allocatePort();
        router = new KieServerRouter();
        router.start("localhost", port);

        serverUrl = "http://localhost:" + port;
    }

    @After
    public void stopStandaloneRouter(){
        // stop router and remove its config
        System.clearProperty(KieServerRouterConstants.ROUTER_REPOSITORY_DIR);
        router.stop(true);
        router = null;

        Stream.of(repository.listFiles()).forEach(f-> f.delete());
        repository.delete();
    }

    @Test
    public void testReactToUnavailableServersInRouterContainerBasedOperations() throws Exception {

        Response response = null;
        try {
            WebTarget clientRequest = newRequest(serverUrl + "/mgmt/list");
            logger.info( "[GET] " + clientRequest.getUri());

            response = clientRequest.request(getMediaType()).get();
            Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            String responseBody = response.readEntity(String.class);
            logger.debug(responseBody);
            response.close();

            Configuration initialConfig = marshaller.unmarshall(new StringReader(responseBody));
            assertEquals(3, initialConfig.getHostsPerContainer().size());
            assertEquals(3, initialConfig.getHostsPerServer().size());

            assertEquals(1, initialConfig.getHostsPerContainer().get("container1").size());
            assertEquals(1, initialConfig.getHostsPerContainer().get("container2").size());
            assertEquals(1, initialConfig.getHostsPerContainer().get("container3").size());
            assertEquals(1, initialConfig.getHostsPerServer().get("server1").size());
            assertEquals(1, initialConfig.getHostsPerServer().get("server2").size());
            assertEquals(1, initialConfig.getHostsPerServer().get("server3").size());

            clientRequest = newRequest(serverUrl + "/containers/container1/instances");
            logger.debug("[GET] " + clientRequest.getUri());

            response = clientRequest.request(getMediaType()).get();
            Assert.assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
            response.close();


            clientRequest = newRequest(serverUrl + "/mgmt/list");
            logger.debug("[GET] " + clientRequest.getUri());

            response = clientRequest.request(getMediaType()).get();
            Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            responseBody = response.readEntity(String.class);
            logger.debug(responseBody);
            response.close();

            Configuration config = marshaller.unmarshall(new StringReader(responseBody));

            assertEquals(3, config.getHostsPerContainer().size());
            assertEquals(3, config.getHostsPerServer().size());

            assertEquals(0, config.getHostsPerContainer().get("container1").size());
            assertEquals(1, config.getHostsPerContainer().get("container2").size());
            assertEquals(1, config.getHostsPerContainer().get("container3").size());
            assertEquals(0, config.getHostsPerServer().get("server1").size());
            assertEquals(1, config.getHostsPerServer().get("server2").size());
            assertEquals(1, config.getHostsPerServer().get("server3").size());
        } finally {
            if(response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testReactToUnavailableServersInRouterQueryBasedOperations() throws Exception {

        Response response = null;
        try {
            WebTarget clientRequest = newRequest(serverUrl + "/mgmt/list");
            logger.debug( "[GET] " + clientRequest.getUri());

            response = clientRequest.request(getMediaType()).get();
            Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            String responseBody = response.readEntity(String.class);
            logger.debug(responseBody);
            response.close();

            Configuration initialConfig = marshaller.unmarshall(new StringReader(responseBody));
            assertEquals(3, initialConfig.getHostsPerContainer().size());
            assertEquals(3, initialConfig.getHostsPerServer().size());

            assertEquals(1, initialConfig.getHostsPerContainer().get("container1").size());
            assertEquals(1, initialConfig.getHostsPerContainer().get("container2").size());
            assertEquals(1, initialConfig.getHostsPerContainer().get("container3").size());
            assertEquals(1, initialConfig.getHostsPerServer().get("server1").size());
            assertEquals(1, initialConfig.getHostsPerServer().get("server2").size());
            assertEquals(1, initialConfig.getHostsPerServer().get("server3").size());

            clientRequest = newRequest(serverUrl + "/containers");
            logger.debug( "[GET] " + clientRequest.getUri());

            response = clientRequest.request(getMediaType()).get();
            Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            response.close();

            clientRequest = newRequest(serverUrl + "/mgmt/list");
            logger.debug( "[GET] " + clientRequest.getUri());

            response = clientRequest.request(getMediaType()).get();
            Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            responseBody = response.readEntity(String.class);
            logger.debug(responseBody);
            response.close();

            // since there are no servers connected and query operations are broadcasts all should be cleared
            Configuration config = marshaller.unmarshall(new StringReader(responseBody));
            assertEquals(3, config.getHostsPerContainer().size());
            assertEquals(3, config.getHostsPerServer().size());

            assertEquals(0, config.getHostsPerContainer().get("container1").size());
            assertEquals(0, config.getHostsPerContainer().get("container2").size());
            assertEquals(0, config.getHostsPerContainer().get("container3").size());
            assertEquals(0, config.getHostsPerServer().get("server1").size());
            assertEquals(0, config.getHostsPerServer().get("server2").size());
            assertEquals(0, config.getHostsPerServer().get("server3").size());

        } finally {
            if(response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testReactToUnavailableServersInRouterContainerBasedOperationsInvalidHost() throws Exception {

        Response response = null;
        try {
            WebTarget clientRequest = newRequest(serverUrl + "/mgmt/list");
            logger.debug( "[GET] " + clientRequest.getUri());

            response = clientRequest.request(getMediaType()).get();
            Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            String responseBody = response.readEntity(String.class);
            logger.debug(responseBody);
            response.close();

            Configuration initialConfig = marshaller.unmarshall(new StringReader(responseBody));
            assertEquals(3, initialConfig.getHostsPerContainer().size());
            assertEquals(3, initialConfig.getHostsPerServer().size());

            assertEquals(1, initialConfig.getHostsPerContainer().get("container1").size());
            assertEquals(1, initialConfig.getHostsPerContainer().get("container2").size());
            assertEquals(1, initialConfig.getHostsPerContainer().get("container3").size());
            assertEquals(1, initialConfig.getHostsPerServer().get("server1").size());
            assertEquals(1, initialConfig.getHostsPerServer().get("server2").size());
            assertEquals(1, initialConfig.getHostsPerServer().get("server3").size());

            clientRequest = newRequest(serverUrl + "/containers/container3/instances");
            logger.debug("[GET] " + clientRequest.getUri());
            try {
                response = clientRequest.request(getMediaType()).get();
                fail("Should fail as this is an invalid host");
            } catch (Exception e) {
                // expected
            } finally {
                response.close();
            }

            clientRequest = newRequest(serverUrl + "/mgmt/list");
            logger.debug( "[GET] " + clientRequest.getUri());

            response = clientRequest.request(getMediaType()).get();
            Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            responseBody = response.readEntity(String.class);
            logger.debug(responseBody);
            response.close();

            Configuration config = marshaller.unmarshall(new StringReader(responseBody));

            assertEquals(3, config.getHostsPerContainer().size());
            assertEquals(3, config.getHostsPerServer().size());

            assertEquals(1, config.getHostsPerContainer().get("container1").size());
            assertEquals(1, config.getHostsPerContainer().get("container2").size());
            assertEquals(0, config.getHostsPerContainer().get("container3").size());
            assertEquals(1, config.getHostsPerServer().get("server1").size());
            assertEquals(1, config.getHostsPerServer().get("server2").size());
            assertEquals(0, config.getHostsPerServer().get("server3").size());
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
}
