/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.kie.karaf.itest.blueprint;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.KieSession;
import org.kie.karaf.itest.AbstractKarafIntegrationTest;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Constants;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class KieBlueprintDependencyKarafIntegrationTest extends AbstractKarafIntegrationTest {

    private static final String BLUEPRINT_XML_LOCATION = "/org/kie/karaf/itest/blueprint/kie-beans-blueprint.xml";
    private static final String DRL_LOCATION = "/drl_kiesample_dependency/Hal1.drl";

    @Inject
    private KieSession kieSession;

    @Test
    public void testKieBase() throws Exception {
        // just to test a PR
        assertNotNull(kieSession);
        assertTrue("KieBase contains no packages?", kieSession.getKieBase().getKiePackages().size() > 0);
    }

    @Configuration
    public static Option[] configure() {
        return new Option[]{
                // Install Karaf Container
                getKarafDistributionOption(),

                // Don't bother with local console output as it just ends up cluttering the logs
                configureConsole().ignoreLocalConsole(),
                // Force the log level to INFO so we have more details during the test.  It defaults to WARN.
                logLevel(LogLevelOption.LogLevel.WARN),

                // Option to be used to do remote debugging
                // debugConfiguration("5005", true),

                // Load Kie-Aries-Blueprint
                loadKieFeatures("kie-aries-blueprint"),

                // wrap and install junit bundle - the DRL imports a class from it
                // (simulates for instance a bundle with domain classes used in rules)
                wrappedBundle(mavenBundle().groupId("junit").artifactId("junit").versionAsInProject()),

                // Create a bundle with META-INF/spring/kie-beans.xml - this should be processed automatically by Spring
                streamBundle(bundle()
                        .set(Constants.BUNDLE_MANIFESTVERSION, "2")
                        .add("OSGI-INF/blueprint/kie-beans-blueprint.xml",
                                KieBlueprintDependencyKarafIntegrationTest.class.getResource(BLUEPRINT_XML_LOCATION))
                        .add("drl_kiesample_dependency/Hal1.drl",
                                KieBlueprintDependencyKarafIntegrationTest.class.getResource(DRL_LOCATION))
                        .set(Constants.IMPORT_PACKAGE, "org.kie.aries.blueprint," +
                                                       "org.kie.aries.blueprint.factorybeans," +
                                                       "org.kie.aries.blueprint.helpers," +
                                                       "org.kie.api," +
                                                       "org.kie.api.runtime," +
                                                       // junit is acting as a dependency for the rule
                                                       "org.junit," +
                                                       "*")
                        .set(Constants.BUNDLE_SYMBOLICNAME, "Test-Blueprint-Bundle")
                        // alternative for enumerating org.kie.aries.blueprint packages in Import-Package:
                        //.set(Constants.DYNAMICIMPORT_PACKAGE, "*")
                        .build()).start()

        };
    }
}
