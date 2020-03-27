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
package org.outerj.daisy.navigation.impl;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;

/**
 * A context object used during navigation tree building.
 * Obviously, no references to this object should be kept after building (i.e., in the final tree)
 */
public class BuildContext {
    private Stack<EnumMap<CounterType, Counter>> counterStack = new Stack<EnumMap<CounterType, Counter>>();
    private Stack<VariantKey> importStack = new Stack<VariantKey>();
    private RootNode rootNode;
    private CommonNavigationManager.Context context;
    private VersionMode versionMode;
    private Stack<ContextVariant> variantStack = new Stack<ContextVariant>();
    private List<Map<String, ContextValue>> contextValuesStack = new ArrayList<Map<String, ContextValue>>();
    public enum CounterType { GROUP_COUNTER, LINK_COUNTER }


    public BuildContext(RootNode rootNode, CommonNavigationManager.Context context, VersionMode versionMode) {
        pushCounters(getNewCounters());
        this.rootNode = rootNode;
        this.context = context;
        this.versionMode = versionMode;
    }

    public int getNextValue(CounterType counterType) {
        return counterStack.peek().get(counterType).augment();
    }

    public static EnumMap<CounterType, Counter> getNewCounters() {
        EnumMap<CounterType, Counter> countersMap = new EnumMap<CounterType, Counter>(CounterType.class);
        for (CounterType counterType : CounterType.values()) {
            countersMap.put(counterType, new Counter());
        }
        return countersMap;
    }

    public void pushCounters(EnumMap<CounterType, Counter> counters) {
        this.counterStack.push(counters);
    }

    public EnumMap<CounterType, Counter> popCounters() {
        return this.counterStack.pop();
    }

    public EnumMap<CounterType, Counter> peekCounters() {
        return this.counterStack.peek();
    }

    public boolean containsImport(VariantKey variantKey) {
        return importStack.contains(variantKey);
    }

    public void pushImport(VariantKey variantKey) {
        importStack.push(variantKey);
    }

    public void popImport() {
        importStack.pop();
    }

    public VariantKey peekImport() {
        return importStack.peek();
    }

    public RootNode getRootNode() {
        return rootNode;
    }

    public CommonNavigationManager.Context getNavContext() {
        return context;
    }

    public VersionMode getVersionMode() {
        return versionMode;
    }

    /**
     * The context variant are the branch and language to use as default
     * when they are needed but none is available. Normally these are the
     * branch and language of the navigation document.
     */
    public void pushContextVariant(long branchId, long languageId) {
        variantStack.push(new ContextVariant(branchId, languageId));
    }

    public void popContextVariant() {
        variantStack.pop();
    }

    public long getBranchId() {
        return variantStack.peek().branchId;
    }

    public long getLanguageId() {
        return variantStack.peek().languageId;
    }
    
    public long getRootBranchId() {
    	return variantStack.firstElement().branchId;
    }
    
    public long getRootLanguageId() {
    	return variantStack.firstElement().languageId;
    }

    public void pushContextValues(Map<String, ContextValue> contextValues) {
        contextValuesStack.add(contextValues);
    }

    public void popContextValues() {
        contextValuesStack.remove(contextValuesStack.size() - 1);
    }

    public List<Map<String, ContextValue>> getContextValuesStack() {
        return contextValuesStack;
    }

    private static class ContextVariant {
        public long branchId;
        public long languageId;

        public ContextVariant(long branchId, long languageId) {
            this.branchId = branchId;
            this.languageId = languageId;
        }
    }
}
