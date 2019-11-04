/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.router.client;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.server.api.KieServerConstants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class KieServerRouterEventListenerTest {

    @After
    public void cleanup() {
        System.clearProperty(KieServerConstants.KIE_SERVER_ROUTER);
        System.clearProperty(KieServerConstants.KIE_SERVER_ID);
    }

    @Before
    public void setup() {
        System.setProperty(KieServerConstants.KIE_SERVER_ID, "sample-id");
    }

    @Test
    public void testRouterUrlsWithSlashAtTheEnd() {

        System.setProperty(KieServerConstants.KIE_SERVER_ROUTER, "http://localhost:9000/,http://localhost:9001/, http://localhost:9002/");

        KieServerRouterEventListener client = new KieServerRouterEventListener();
        List<String> routerUrls = client.routers();

        assertNotNull(routerUrls);
        assertEquals(3, routerUrls.size());

        assertTrue(routerUrls.contains("http://localhost:9000"));
        assertTrue(routerUrls.contains("http://localhost:9001"));
        assertTrue(routerUrls.contains("http://localhost:9002"));
    }

    @Test
    public void testRouterUrlsWithoutSlashAtTheEnd() {

        System.setProperty(KieServerConstants.KIE_SERVER_ROUTER, "http://localhost:9000,http://localhost:9001, http://localhost:9002");

        KieServerRouterEventListener client = new KieServerRouterEventListener();
        List<String> routerUrls = client.routers();

        assertNotNull(routerUrls);
        assertEquals(3, routerUrls.size());

        assertTrue(routerUrls.contains("http://localhost:9000"));
        assertTrue(routerUrls.contains("http://localhost:9001"));
        assertTrue(routerUrls.contains("http://localhost:9002"));
    }

    @Test
    public void testRouterUrlsWithAndWithoutSlashAtTheEnd() {

        System.setProperty(KieServerConstants.KIE_SERVER_ROUTER, "http://localhost:9000,http://localhost:9001/, http://localhost:9002/");

        KieServerRouterEventListener client = new KieServerRouterEventListener();
        List<String> routerUrls = client.routers();

        assertNotNull(routerUrls);
        assertEquals(3, routerUrls.size());

        assertTrue(routerUrls.contains("http://localhost:9000"));
        assertTrue(routerUrls.contains("http://localhost:9001"));
        assertTrue(routerUrls.contains("http://localhost:9002"));
    }

    @Test
    public void testListenerUsername() {
        KieServerRouterEventListener client = new KieServerRouterEventListener();
        Assert.assertEquals("sample-id", client.getUsername());
    }

    @Test
    public void testListenerOverwriteUsername() {
        System.setProperty(KieServerConstants.KIE_ROUTER_MANAGEMENT_USERNAME, "sample-id-overwrite");

        KieServerRouterEventListener client = new KieServerRouterEventListener();
        Assert.assertEquals("sample-id-overwrite", client.getUsername());

        System.clearProperty(KieServerConstants.KIE_ROUTER_MANAGEMENT_USERNAME);
    }
}
