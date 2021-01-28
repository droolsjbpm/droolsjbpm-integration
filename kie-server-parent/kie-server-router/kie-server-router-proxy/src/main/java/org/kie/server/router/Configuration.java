/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jboss.logging.Logger;
import org.kie.server.router.spi.ConfigRepository;
import org.kie.server.router.utils.FailedHostInfo;

public class Configuration {

    private static final Logger log = Logger.getLogger(Configuration.class);

    private Map<String, List<String>> hostsPerServer = new ConcurrentHashMap<>();
    private Map<String, List<String>> hostsPerContainer = new ConcurrentHashMap<>();
    private Map<String, List<ContainerInfo>> containerInfosPerContainer = new ConcurrentHashMap<>();

    private Set<ConfigurationListener> listeners = new CopyOnWriteArraySet<>();
    
    public Map<String, List<String>> getHostsPerServer() {
        return hostsPerServer;
    }
    
    public Map<String, List<String>> getHostsPerContainer() {
        return hostsPerContainer;
    }

    public Map<String, List<ContainerInfo>> getContainerInfosPerContainer() {
        return containerInfosPerContainer;
    }

    public void addContainerHost(String containerId, String serverUrl) {
        List<String> hosts = hostsPerContainer.get(containerId);
        if (hosts == null) {
            hosts = new ArrayList<>();
            hostsPerContainer.put(containerId, hosts);
        }
        if(!hosts.contains(serverUrl)) {
            hosts.add(serverUrl);
        }

        this.listeners.forEach(l -> l.onContainerAdded(containerId, serverUrl));
    }
    
    public void addServerHost(String serverId, String serverUrl) {
        List<String> hosts = hostsPerServer.get(serverId);
        if (hosts == null) {
            hosts = new ArrayList<>();
            hostsPerServer.put(serverId, hosts);
        }
        if(!hosts.contains(serverUrl)) {
            hosts.add(serverUrl);
        }

        this.listeners.forEach(l -> l.onServerAdded(serverId, serverUrl));
    }

    public void addContainerInfo(ContainerInfo containerInfo) {
        List<ContainerInfo> containersByAlias = containerInfosPerContainer.get(containerInfo.getAlias());
        if (containersByAlias == null) {
            containersByAlias = new ArrayList<>();
            containerInfosPerContainer.put(containerInfo.getAlias(), containersByAlias);
        }
        if(!containersByAlias.contains(containerInfo)) {
            containersByAlias.add(containerInfo);
        }

        List<ContainerInfo> containersById = containerInfosPerContainer.get(containerInfo.getContainerId());
        if (containersById == null) {
            containersById = new ArrayList<>();
            containerInfosPerContainer.put(containerInfo.getContainerId(), containersById);
        }
        if(!containersById.contains(containerInfo)) {
            containersById.add(containerInfo);
        }
    }
    
    public void removeContainerHost(String containerId, String serverUrl) {
        List<String> hosts = hostsPerContainer.get(containerId);
        if (hosts != null) {
            hosts.remove(serverUrl);
            if(hosts.isEmpty()) {
                hostsPerContainer.remove(containerId);
            }
        }

        this.listeners.forEach(l -> l.onContainerRemoved(containerId, serverUrl));
    }
    
    public void removeServerHost(String serverId, String serverUrl) {
        List<String> hosts = hostsPerServer.get(serverId);
        if (hosts != null) {
            hosts.remove(serverUrl);
            if(hosts.isEmpty()) {
                hostsPerServer.remove(serverId);
            }
        }

        this.listeners.forEach(l -> l.onServerRemoved(serverId, serverUrl));
    }

    public void removeContainerInfo(ContainerInfo containerInfo) {
        removeContainerBy(containerInfo, containerInfo.getContainerId());
        removeContainerBy(containerInfo, containerInfo.getAlias());
    }

    public void removeContainerBy(ContainerInfo containerInfo, String containerId) {
        List<ContainerInfo> containersById = containerInfosPerContainer.get(containerId);

        if (containersById == null) {
            log.warnv("Container info with id or alias '{}' is not found, nothing is removed.", containerId);
            return;
        }

        List<String> hosts = hostsPerContainer.getOrDefault(containerId, Collections.emptyList());
        if (!hosts.isEmpty()) {
            return;
        }

        containersById.remove(containerInfo);
        if(containersById.isEmpty()) {
            containerInfosPerContainer.remove(containerId);
        }
    }

    public synchronized FailedHostInfo removeUnavailableServer(String requestURL) {
        String serverUrl = null;
        String serverId = null;

        // locate server id for request url
        for (Map.Entry<String, List<String>> entry : hostsPerServer.entrySet()) {
            List<String> hosts = entry.getValue();

            for (String host : hosts) {
                if (requestURL.startsWith(host)) {
                    serverUrl = host;
                    serverId = entry.getKey();

                    break;
                }
            }
        }

        if (serverId != null && serverUrl != null) {
            removeServerHost(serverId, serverUrl);
        }

        // locate containers for request url
        Set<String> containers = new HashSet<>();

        for (Map.Entry<String, List<String>> entry : hostsPerContainer.entrySet()) {
            List<String> hosts = entry.getValue();

            for (String host : hosts) {
                if (requestURL.startsWith(host)) {
                    serverUrl = host;
                    containers.add(entry.getKey());
                }
            }
        }

        if (!containers.isEmpty()) {
            final String actualServerUrl = serverUrl;
            containers.forEach( container ->
                    removeContainerHost(container, actualServerUrl)
            );
        }
        FailedHostInfo failedHost = new FailedHostInfo(serverId, serverUrl, containers);
        return failedHost;
    }

    public void addListener(ConfigurationListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(ConfigurationListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public String toString() {
        return "{hostsPerServer=" + hostsPerServer + ", hostsPerContainer=" + hostsPerContainer + "}";
    }

    public void addEmptyContainerHost(String containerId) {
        List<String> hosts = hostsPerContainer.get(containerId);
        if (hosts == null) {
            hosts = new ArrayList<>();
            hostsPerContainer.put(containerId, hosts);
        }
    }

    public void addEmptyServerHost(String serverId) {
        List<String> hosts = hostsPerServer.get(serverId);
        if (hosts == null) {
            hosts = new ArrayList<>();
            hostsPerServer.put(serverId, hosts);
        }
    }
    
    public synchronized void reloadFrom(Configuration updated) {
        this.containerInfosPerContainer = updated.getContainerInfosPerContainer();
        
        // remove items if they are not existing in updated configuration        
        this.hostsPerServer.keySet().forEach(server -> {
            if (!updated.hostsPerServer.containsKey(server)) {
                
                List<String> serverUrls = new ArrayList<>(this.hostsPerServer.remove(server));
                
                serverUrls.forEach(url -> removeServerHost(server, url));
            }
        });
        
        this.hostsPerContainer.keySet().forEach(container -> {
            if (!updated.hostsPerContainer.containsKey(container)) {
                
                List<String> serverUrls = new ArrayList<>(this.hostsPerContainer.remove(container));
                
                serverUrls.forEach(url -> removeContainerHost(container, url));
            }
        });
        
        // update remaining items hosts per server
        this.hostsPerServer.keySet().forEach(server -> {
            
            List<String> serverUrls = new ArrayList<>(this.hostsPerServer.get(server));
            List<String> updatedServerUrls = updated.hostsPerServer.remove(server);

            for (String serverUrl : serverUrls) {
                if (updatedServerUrls.contains(serverUrl)) {
                    updatedServerUrls.remove(serverUrl);
                } else {
                    removeServerHost(server, serverUrl);
                }
            }

            // all remaining from updated list add to this configuration
            updatedServerUrls.forEach(url -> addServerHost(server, url));
        });
        // update remaining items hosts per container
        this.hostsPerContainer.keySet().forEach(container -> {
            
            List<String> serverUrls = new ArrayList<>(this.hostsPerContainer.get(container));
            List<String> updatedServerUrls = updated.hostsPerContainer.remove(container);

            for (String serverUrl : serverUrls) {
                if (updatedServerUrls.contains(serverUrl)) {
                    updatedServerUrls.remove(serverUrl);
                } else {
                    removeContainerHost(container, serverUrl);
                }
            }
            
            // all remaining from updated list add to this configuration
            updatedServerUrls.forEach(url -> addContainerHost(container, url));
        });
        
        // last add all left items
        updated.hostsPerServer.forEach((server, urls) -> {
            
            urls.forEach(url -> addServerHost(server, url));
        });
        
        updated.hostsPerContainer.forEach((container, urls) -> {
            
            urls.forEach(url -> addContainerHost(container, url));
        });
        
        this.listeners.forEach(l -> l.onConfigurationReloaded());
    }
    
    public synchronized void reloadFromRepository(ConfigRepository repository) {
        
        Configuration loaded = repository.load();
        
        this.containerInfosPerContainer = loaded.getContainerInfosPerContainer();
        this.hostsPerContainer = loaded.getHostsPerContainer();
        this.hostsPerServer = loaded.getHostsPerServer();
    }
}
