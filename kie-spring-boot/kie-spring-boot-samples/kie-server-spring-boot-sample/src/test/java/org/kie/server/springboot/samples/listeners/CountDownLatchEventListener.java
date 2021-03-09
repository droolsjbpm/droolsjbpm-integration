/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.springboot.samples.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.kie.api.event.process.DefaultProcessEventListener;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessNodeLeftEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.springframework.stereotype.Component;

@Component
public class CountDownLatchEventListener extends DefaultProcessEventListener {

    private List<Long> ids = new ArrayList<>();
    private String expectedProcessId;
    private CountDownLatch latch;
    private String expectedNodeName;
    
    public void configure(String processId, int threads) {
        this.expectedProcessId = processId;
        this.latch = new CountDownLatch(threads);
    }
    
    public void configureNode(String processId, String nodeName, int threads) {
        configure(processId, threads);
        this.expectedNodeName = nodeName;
    }
    
    public CountDownLatch getCountDown() {
        return this.latch;
    }
    
    public List<Long> getIds() {
        return this.ids;
    }
    
    @Override
    public void afterProcessStarted(ProcessStartedEvent event) {
        if (this.latch != null) {
            this.ids.add(event.getProcessInstance().getId());
            this.latch.countDown();
        }
    }
    
    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {
        if (this.latch != null && event.getProcessInstance().getProcessId().equals(expectedProcessId)) {
            this.ids = new ArrayList<>();
            this.latch.countDown();
        }
    }
    
    @Override
    public void afterNodeLeft(ProcessNodeLeftEvent processNodeLeftEvent) {
        if (this.latch != null && expectedNodeName !=null && 
            expectedNodeName.equals(processNodeLeftEvent.getNodeInstance().getNodeName())) {
            this.latch.countDown();
        }
    }

}
