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

package org.kie.server.router.proxy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kie.server.router.ContainerInfo;
import org.kie.server.router.spi.ContainerResolver;

import io.undertow.server.HttpServerExchange;

public class DefaultContainerResolver implements ContainerResolver {

    private Pattern p = Pattern.compile(".*/containers/([^/]+).*");
    private Pattern p2 = Pattern.compile(".*/containers/instances/([^/]+).*");

    @Override
    public String resolveContainerId(HttpServerExchange exchange, Map<String, List<ContainerInfo>> containerInfoPerContainer) {
        String relativePath = exchange.getRelativePath();
        Matcher matcher = p.matcher(relativePath);

        Set<String> knownContainers = containerInfoPerContainer.keySet();

        if (matcher.find()) {
            String containerId = matcher.group(1);
            if (knownContainers.contains(containerId)) {
                return containerId;
            }
        }
        matcher = p2.matcher(relativePath);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return NOT_FOUND;
    }

    @Override
    public String toString() {
        return "Default container resolver";
    }
}
