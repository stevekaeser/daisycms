/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.repository.serverimpl.query;

public class Counter implements Comparable {
    private long count = 0;

    public void increment() {
        count++;
    }

    public int compareTo(Object o) {
        Counter otherCounter = (Counter)o;
        if (otherCounter.count == count)
            return 0;
        else if (count < otherCounter.count)
            return -1;
        else
            return 1;
    }

    public long getValue() {
        return count;
    }
}