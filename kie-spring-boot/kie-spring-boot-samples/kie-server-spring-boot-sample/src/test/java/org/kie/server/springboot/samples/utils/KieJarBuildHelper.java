/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.server.springboot.samples.utils;

import static org.kie.scanner.KieMavenRepository.getKieMavenRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.appformer.maven.integration.MavenRepository;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.Message;
import org.kie.scanner.KieMavenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KieJarBuildHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(KieJarBuildHelper.class);
    
    public static void createKieJar(String resource) {
        KieServices ks = KieServices.get();
        KieBuilder kieBuilder = ks.newKieBuilder(new File(resource));
        KieBuilder build = kieBuilder.buildAll();
        InternalKieModule kjar = (InternalKieModule) build.getKieModule();

        List<Message> messages = kieBuilder.buildAll().getResults().getMessages();
        if (!messages.isEmpty()) {
            for (Message message : messages) {
                logger.error("Error Message: ({}) {}", message.getPath(), message.getText());
            }
            throw new RuntimeException("There are errors building the package, please check your knowledge assets!");
        }
        
        String pomFileName = MavenRepository.toFileName(kjar.getReleaseId(), null) + ".pom";
        File pomFile = new File(System.getProperty("java.io.tmpdir"), pomFileName);
        try (FileOutputStream fos = new FileOutputStream(pomFile)) {
            fos.write(Files.readAllBytes((new File(resource + "/pom.xml")).toPath()));
            fos.flush();
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to write pom.xml to temporary file : " + ioe.getMessage(), ioe);
        }
    
        KieMavenRepository repository = getKieMavenRepository();
        repository.installArtifact(kjar.getReleaseId(), kjar, pomFile);
    }
    
    public static void replaceInFile(String targetFile, String replacedFile, Map<String,String> map) {
        Set<String> keys = map.keySet();
        String[] patterns = keys.toArray(new String[keys.size()]);
        Collection<String> values = map.values();
        String[] replacements = values.toArray(new String[0]);
        
        try (Stream<String> lines = Files.lines(Paths.get(targetFile))) {
            List<String> replaced = lines.map(line-> StringUtils.replaceEach(line, patterns, replacements))
                                     .collect(Collectors.toList());
            Files.write(Paths.get(replacedFile), replaced);
            Paths.get(replacedFile).toFile().deleteOnExit();
         } catch (IOException ioe) {
             throw new RuntimeException("Unable to replace "+map+
                     " at file "+targetFile+" : " + ioe.getMessage(), ioe);
        }
    }
}
