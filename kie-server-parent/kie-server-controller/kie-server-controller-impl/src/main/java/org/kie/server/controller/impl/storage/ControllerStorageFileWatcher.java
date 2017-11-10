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

package org.kie.server.controller.impl.storage;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ControllerStorageFileWatcher implements Runnable {
    
    private static final Logger log = LoggerFactory.getLogger(ControllerStorageFileWatcher.class);

    private WatchService watcher;
    private Path toWatch;
    private AtomicBoolean active = new AtomicBoolean(true);
    private String templateFileName;
    
    private FileBasedKieServerTemplateStorage storage;
   
    public ControllerStorageFileWatcher(String configFilePath, FileBasedKieServerTemplateStorage storage) {
        this.toWatch = Paths.get(configFilePath);
        this.templateFileName = toWatch.toFile().getName();
        this.storage = storage;
        if (!Files.isDirectory(this.toWatch)) {
            this.toWatch = Paths.get(configFilePath).getParent();
        }
        
        try {
            this.watcher = toWatch.getFileSystem().newWatchService();
            log.debug("About to start watching " + toWatch.toString());
            toWatch.register(watcher, ENTRY_MODIFY);
        } catch (Exception e) {
            log.error("Error when setting up config file watcher :: " + e.getMessage(), e);
            this.active.set(false);
        }
    }

    public void stop() {
        this.active.set(false);
    }
    
    @Override
    public void run() {
        try{
            while(active.get()) {
                WatchKey key = watcher.poll(5, TimeUnit.SECONDS);
                if (key != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path updatedFile = (Path) event.context();
                        File modifiedFile = updatedFile.toFile();
                        if (modifiedFile.getName().equals(templateFileName)) {
                            log.debug("Received template file update event, reloading...");
                            try (FileReader reader = new FileReader(new File(toWatch.toFile(), templateFileName))){                                                                
                                this.storage.loadTemplateMapsFromFile();
                                log.info("Successfully reloaded server templates from file");
                            } catch (Exception e) {
                                log.error("Unexpected exception while reading updated template file :: " + e.getMessage(), e);
                            }
                        }
                    }
                    key.reset();
                }
            }
        } catch (InterruptedException e) {
            log.debug("Interrupted exception received...");
        }
    }
}
