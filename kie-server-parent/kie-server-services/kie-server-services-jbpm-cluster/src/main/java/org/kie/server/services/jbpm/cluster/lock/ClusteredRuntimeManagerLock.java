/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.services.jbpm.cluster.lock;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.infinispan.lock.api.ClusteredLockManager;
import org.jbpm.runtime.manager.spi.RuntimeManagerLock;
import org.jgroups.util.UUID;


public class ClusteredRuntimeManagerLock implements RuntimeManagerLock {

    private String clusteredLockId;

    private ClusteredLockManager clusteredLockManager;

    public ClusteredRuntimeManagerLock(ClusteredLockManager clusteredLockManager) {
        this.clusteredLockManager = clusteredLockManager;
        clusteredLockId = UUID.randomUUID().toString();
        this.clusteredLockManager.defineLock(clusteredLockId);
    }

    @Override
    public void lock() {
        clusteredLockManager.get(clusteredLockId).lock();
    }

    @Override
    public boolean tryLock(long units, TimeUnit milliseconds) throws InterruptedException {
        try {
            return clusteredLockManager.get(clusteredLockId).tryLock(units, milliseconds).get();
        } catch(ExecutionException e) {
            if(e.getCause() instanceof InterruptedException) {
                throw (InterruptedException) e.getCause();
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void lockInterruptible() throws InterruptedException {
        throw new UnsupportedOperationException("this lock does not support interruptible lock");
    }

    @Override
    public void forceUnlock() {
        try {
            clusteredLockManager.forceRelease(clusteredLockId).get();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unlock() {
        try {
            clusteredLockManager.get(clusteredLockId).unlock().get();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasQueuedThreads() {
        try {
            return clusteredLockManager.get(clusteredLockId).isLocked().get();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isHeldByCurrentThread() {
        try {
            return clusteredLockManager.get(clusteredLockId).isLockedByMe().get();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getQueueLength() {
        try {
            return clusteredLockManager.get(clusteredLockId).isLocked().get() ? 0 : 1;
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

}
