/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.controller.api.service;

import org.kie.server.controller.api.model.runtime.ServerInstanceKey;
import org.kie.server.controller.api.model.spec.*;

public interface SpecManagementService {

    void saveContainerSpec(String serverTemplateId, ContainerSpec containerSpec);

    void updateContainerSpec(String serverTemplateId, ContainerSpec containerSpec);

    void updateContainerSpec(String serverTemplateId, String containerId, ContainerSpec containerSpec);

    void updateContainerSpec(String serverTemplateId, String containerId, ContainerSpec containerSpec, Boolean resetBeforeUpdate);

    void saveServerTemplate(ServerTemplate serverTemplate);

    ServerTemplate getServerTemplate(String serverTemplateId);

    ServerTemplateKeyList listServerTemplateKeys();

    ServerTemplateList listServerTemplates();

    ContainerSpecList listContainerSpec(String serverTemplateId);

    ContainerSpec getContainerInfo(String serverTemplateId, String containerId);

    void deleteContainerSpec(String serverTemplateId, String containerSpecId);

    void deleteServerTemplate(String serverTemplateId);

    void copyServerTemplate(String serverTemplateId,  String newServerTemplateId, String newServerTemplateName);

    void updateContainerConfig(String serverTemplateId, String containerSpecId, Capability capability, ContainerConfig containerConfig);

    void updateServerTemplateConfig(String serverTemplateId, Capability capability, ServerConfig serverTemplateConfig);

    void startContainer(ContainerSpecKey containerSpecKey);

    void stopContainer(ContainerSpecKey containerSpecKey);

    void deleteServerInstance(ServerInstanceKey serverInstanceKey);

    void activateContainer(ContainerSpecKey containerSpecKey);
    
    void deactivateContainer(ContainerSpecKey containerSpecKey);

}
