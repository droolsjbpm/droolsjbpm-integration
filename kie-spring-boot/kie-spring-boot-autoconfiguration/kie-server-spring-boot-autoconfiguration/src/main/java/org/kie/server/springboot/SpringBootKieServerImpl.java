/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.springboot;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.drools.core.impl.InternalKieContainer;
import org.kie.api.builder.ReleaseId;
import org.kie.internal.identity.IdentityProvider;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.Message;
import org.kie.server.services.api.KieServerExtension;
import org.kie.server.services.impl.KieServerImpl;
import org.kie.server.services.impl.storage.KieServerState;


public class SpringBootKieServerImpl extends KieServerImpl{

    private IdentityProvider identityProvider;
    private List<KieServerExtension> extensions;
    private boolean useClasspathContainers;
    
    private List<KieContainerResource> containers;
    
    public SpringBootKieServerImpl(List<KieServerExtension> extensions, IdentityProvider identityProvider, boolean useClasspathContainers, List<KieContainerResource> containers) {
        this.extensions = extensions;
        this.identityProvider = identityProvider;
        this.useClasspathContainers = useClasspathContainers;
        
        this.containers = containers;
    }
    
    @Override
    protected List<KieServerExtension> sortKnownExtensions() {
        getServerRegistry().registerIdentityProvider(identityProvider);
        Collections.sort(extensions, new Comparator<KieServerExtension>() {
            @Override
            public int compare(KieServerExtension e1, KieServerExtension e2) {
                return e1.getStartOrder().compareTo(e2.getStartOrder());
            }
        });
        return extensions;
    }

    @Override
    public void init() {        
        super.init();
        
        if (containers != null) {
            for (KieContainerResource container : containers) {
                createContainer(container.getContainerId(), container);
            }
        }
    }

    @Override
    protected InternalKieContainer createKieContainer(KieContainerResource containerInfo) {
        if (useClasspathContainers) {
            return (InternalKieContainer) ks.newKieClasspathContainer(containerInfo.getContainerId());
        } else {
            return super.createKieContainer(containerInfo);
        }
    }

    @Override
    protected void storeServerState(Consumer<KieServerState> kieServerStateConsumer) {
        if (!useClasspathContainers) {
            super.storeServerState(kieServerStateConsumer);
        }
    }

    @Override
    protected Map<String, Object> getContainerParameters(ReleaseId releaseId, List<Message> messages) {
        if (!useClasspathContainers) {
            return super.getContainerParameters(releaseId, messages);
        }
        
        Map<String, Object> parameters = new HashMap<String, Object>();        
        parameters.put(KieServerConstants.KIE_SERVER_PARAM_MESSAGES, messages);
        return parameters;
    }
    
    
}
