/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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

package org.kie.maven.plugin;

import java.io.File;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class BuildPMMLTrustyTest extends KieMavenPluginBaseIntegrationTest {

    private static final String PROJECT_NAME = "kjar-12-with-pmml-trusty";

    private static final String GAV_ARTIFACT_ID = "kie-maven-plugin-test-kjar-12";
    private static final String GAV_VERSION = "1.0.0.Final";


    private static final String PMML_FILE_NAME = "logisticregressionirisdata/logisticRegressionIrisData.pmml";
    private static final String EXAMPLE_PMML_CLASS = "compoundnestedpredicatescorecard/PMMLRuleMappersImpl.class";

    public BuildPMMLTrustyTest(MavenRuntime.MavenRuntimeBuilder builder) {
        super(builder);
    }

    @Test
    public void testContentKjarWithPMML() throws Exception {
        final MavenExecutionResult result = buildKJarProject(PROJECT_NAME,
                                                             new String[]{"-Dorg.kie.version=" + TestUtil.getProjectVersion()},
                                                             "clean", "install");

        final File basedir = result.getBasedir();
        final File kjarFile = new File(basedir, "target/" + GAV_ARTIFACT_ID + "-" + GAV_VERSION + ".jar");
        Assertions.assertThat(kjarFile).exists();

        final JarFile jarFile = new JarFile(kjarFile);
        final Set<String> jarContent = new HashSet<>();
        final Enumeration<JarEntry> kjarEntries = jarFile.entries();
        while (kjarEntries.hasMoreElements()) {
            final String entryName = kjarEntries.nextElement().getName();
            jarContent.add(entryName);
        }

        Assertions.assertThat(jarContent).isNotEmpty();
        Assertions.assertThat(jarContent).contains(PMML_FILE_NAME);
        Assertions.assertThat(jarContent).contains(EXAMPLE_PMML_CLASS);
    }
}