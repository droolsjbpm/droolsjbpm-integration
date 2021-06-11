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

package org.kie.server.services.jbpm.cluster;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jgroups.Address;
import org.kie.api.cluster.ClusterAwareService;
import org.kie.api.cluster.ClusterNode;
import org.kie.api.internal.utils.ServiceRegistry;

@Singleton
@Startup
public class EJBCacheBean {

    public static final String CACHE_NAME_LOOKUP = "java:jboss/infinispan/container/jbpm";
    
    public static final String CACHE_NODES_NAME_LOOKUP = "java:jboss/infinispan/cache/jbpm/nodes";
    
    public static final String CACHE_JOBS_NAME_LOOKUP = "java:jboss/infinispan/cache/jbpm/jobs";
    
    // this enforce the cache initializer
    public static final String CACHE_LOCKS_NAME_LOOKUP = "java:jboss/infinispan/cache/jbpm";

    @Resource(lookup = CACHE_LOCKS_NAME_LOOKUP)
    private EmbeddedCacheManager cacheContainer;

    @Resource(lookup = CACHE_NAME_LOOKUP)
    private EmbeddedCacheManager cacheManager;

    @Resource(lookup = CACHE_NODES_NAME_LOOKUP)
    private Cache<Address, ClusterNode> nodesCache;

    @Resource(lookup = CACHE_JOBS_NAME_LOOKUP)
    private Cache<String, List<Long>> jobsCache;

    @PostConstruct
    public void init() {
        ClusterAwareService clusterService = ServiceRegistry.getService(ClusterAwareService.class);
        ((InfinispanClusterAwareService) clusterService).init(cacheManager);
    }

    
    public EmbeddedCacheManager getCacheContainer() {
        return cacheContainer;
    }

}
