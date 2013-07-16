/*
 * Copyright 2013 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.spring.tests;

import org.drools.compiler.kproject.ReleaseIdImpl;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieSession;
import org.kie.spring.InternalKieSpringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;

import static org.junit.Assert.assertNotNull;

/**
 * bz761427 reproducer
 * One way of listeners configuration require specific order of elements, another one does not.
 * Probably because of <xsd:all> vs. <xsd:sequence> in drools-spring.xsd
 *
 * @author rsynek
 */
public class KieSpringListenersOrderTest {
    private static ApplicationContext ctx;

    @BeforeClass
    public static void runBeforeClass() {
        ReleaseId releaseId = new ReleaseIdImpl("listeners-order-spring","test-spring","0001");
        ctx = InternalKieSpringUtils.getSpringContext(releaseId,
                KieSpringListenersTest.class.getResource("/org/kie/spring/listenersOrderTest.xml"),
                new File(KieSpringListenersTest.class.getResource("/").getFile()));
    }

    private KieSession getSession() {
        return (KieSession) ctx.getBean("ksession2");
    }

    /**
     * reproducer for https://bugzilla.redhat.com/show_bug.cgi?id=761435
     * <p/>
     * See org/drools/container/spring/listenersOrderTest.xml for further details.
     */
    @Test
    public void testListeners() {
        KieSession session = getSession();
        assertNotNull(session);
    }
}
