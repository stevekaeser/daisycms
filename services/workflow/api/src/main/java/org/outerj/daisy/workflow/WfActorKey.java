/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.workflow;

import java.util.List;
import java.util.Collections;

/**
 * Represents an actor in workflow process, which can be either a user or a set of pools.
 *
 * <p>This object is immutable.
 */
public class WfActorKey {
    private long id;
    private List<Long> poolIds;
    private boolean isPool;

    public WfActorKey(long id) {
        this.id = id;
        this.isPool = false;
    }

    public WfActorKey(List<Long> poolIds) {
        if (poolIds == null)
            throw new IllegalArgumentException("Null argument: poolIds");
        if (poolIds.size() == 0)
            throw new IllegalArgumentException("List of pool IDs should contain at least one entry.");

        this.poolIds = Collections.unmodifiableList(poolIds);
        this.isPool = true;
    }

    public boolean isPool() {
        return isPool;
    }

    public boolean isUser() {
        return !isPool;
    }

    public long getUserId() {
        if (isPool)
            throw new IllegalStateException("getUserId should not be called on a WfActorKey object that represents a pool(s).");

        return id;
    }

    public List<Long> getPoolIds() {
        if (!isPool)
            throw new IllegalStateException("getPoolIds should not be called on a WfActorKey object that represents a user.");

        return poolIds;
    }

    public boolean equals(Object obj) {
        if (obj instanceof WfActorKey) {
            WfActorKey key = (WfActorKey)obj;
            if (isPool != key.isPool)
                return false;
            if (isPool) {
                return poolIds.equals(key.poolIds);
            } else {
                return id == key.id;
            }
        } else {
            return false;
        }
    }

    public int hashCode() {
        // The calculation technique for this hashcode is taken from the HashCodeBuilder
        // of Jakarta Commons Lang, which in itself is based on techniques from the
        // "Effective Java" book by Joshua Bloch.
        final int iConstant = 159;
        int iTotal = 615;

        iTotal = appendHash(isPool ? 1 : 2, iTotal, iConstant);
        if (isPool) {
            for (long p : poolIds)
                iTotal = appendHash(p, iTotal, iConstant);
        } else {
            iTotal = appendHash(id, iTotal, iConstant);
        }

        return iTotal;
    }

    private int appendHash(long value, int iTotal, int iConstant) {
        return iTotal * iConstant + ((int) (value ^ (value >> 32)));
    }
}
