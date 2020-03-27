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
package org.outerj.daisy.workflow.serverimpl.query;

import org.outerj.daisy.workflow.WorkflowException;
import org.outerj.daisy.workflow.QueryOrderByItem;
import org.outerj.daisy.workflow.serverimpl.IntWfContext;
import org.outerj.daisy.workflow.serverimpl.query.ValueGetterProvider.ValueGetterProviderBuilder;
import org.outerj.daisy.repository.query.SortOrder;
import org.outerj.daisy.i18n.I18nMessage;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.Timer;

import java.util.*;

public class ResultSorter {
    public static List<TaskInstance> sortTasks(List<TaskInstance> tasks, List<QueryOrderByItem> orderByItems,
            IntWfContext context) throws WorkflowException {

        ResultObjectGetter<TaskInstance> resultObjectGetter = new ResultObjectGetter<TaskInstance>() {
            public TaskInstance get(ValueGetter.Provider provider) throws WorkflowException {
                return provider.getTaskInstance();
            }
        };
        ValueGetterProviderBuilder provider = ValueGetterProvider.getTaskValueProviderBuilder(context);
        List<TaskInstance> result = sort(tasks, resultObjectGetter, orderByItems, context, provider);

        return result;
    }

    public static List<ProcessInstance> sortProcesses(List<ProcessInstance> processes, List<QueryOrderByItem> orderByItems,
            IntWfContext context) throws WorkflowException {

        ResultObjectGetter<ProcessInstance> resultObjectGetter = new ResultObjectGetter<ProcessInstance>() {
            public ProcessInstance get(ValueGetter.Provider provider) {
                return provider.getProcessInstance();
            }
        };
        ValueGetterProviderBuilder provider = ValueGetterProvider.getProcessValueProviderBuilder(context);
        List<ProcessInstance> result = sort(processes, resultObjectGetter, orderByItems, context, provider);

        return result;
    }

    public static List<Timer> sortTimers(List<Timer> timers, List<QueryOrderByItem> orderByItems,
            IntWfContext context) throws WorkflowException {

        ResultObjectGetter<Timer> resultObjectGetter = new ResultObjectGetter<Timer>() {
            public Timer get(ValueGetter.Provider provider) {
                return provider.getTimer();
            }
        };
        ValueGetterProviderBuilder provider = ValueGetterProvider.getTimerValueProviderBuilder(context);
        List<Timer> result = sort(timers, resultObjectGetter, orderByItems, context, provider);

        return result;
    }

    private static <T> List<T> sort(List<T> things, ResultObjectGetter<T> resultObjectGetter, List<QueryOrderByItem> orderByItems,
            IntWfContext context, ValueGetterProviderBuilder valueProviderBuilder) throws WorkflowException {
        // first check if we have anything to do at all
        if (orderByItems.size() == 0)
            return things;

        QueryMetadataRegistry registry = context.getQueryMetadataRegistry();

        List<ValueGetter.Provider> valueProviders = new ArrayList<ValueGetter.Provider>(things.size());
        for (Object thing : things) {
            valueProviders.add(valueProviderBuilder.getProvider(thing));
        }

        List<CompareInfo> propertyGetters = new ArrayList<CompareInfo>(orderByItems.size());
        for (QueryOrderByItem item : orderByItems) {
            ValueGetter getter;
            switch (item.getType()) {
                case PROPERTY:
                    getter = registry.getProperty(item.getName()).getPropertyGetter();
                    break;
                case TASK_VARIABLE:
                    getter = new TaskVariableGetter(item.getName());
                    break;
                case PROCESS_VARIABLE:
                    getter = new ProcessVariableGetter(item.getName());
                    break;
                default:
                    throw new RuntimeException("Unexpected situation, unhandled order by type: " + item.getType());
            }
            propertyGetters.add(new CompareInfo(getter, item.getSortOrder()));
        }

        ProviderComparator comparator = new ProviderComparator(propertyGetters, context.getLocale());
        Collections.sort(valueProviders, comparator);

        List<T> result = new ArrayList<T>(things.size());
        for (ValueGetter.Provider provider : valueProviders) {
            result.add(resultObjectGetter.get(provider));
        }
        return result;
    }

    private static class ProviderComparator implements Comparator {
        private final List<CompareInfo> propertyGetters;
        private final Locale locale;

        public ProviderComparator(List<CompareInfo> propertyGetters, Locale locale) {
            this.propertyGetters = propertyGetters;
            this.locale = locale;
        }

        public int compare(Object o1, Object o2) {
            ValueGetter.Provider p1 = (ValueGetter.Provider)o1;
            ValueGetter.Provider p2 = (ValueGetter.Provider)o2;

            for (CompareInfo compareInfo : propertyGetters) {
                Object value1;
                Object value2;
                try {
                    value1 = getSortValue(compareInfo.getter, p1);
                    value2 = getSortValue(compareInfo.getter, p2);
                } catch (WorkflowException e) {
                    throw new RuntimeException(e);
                }
                int result = valueCompare(value1, value2);
                if (result != 0) {
                    if (compareInfo.sortOrder == SortOrder.DESCENDING)
                        result = result * -1;
                    return result;
                }
            }

            return 0;
        }

        private Object getSortValue(ValueGetter getter, ValueGetter.Provider provider) throws WorkflowException {
            Object value = getter.getValue(provider);
            // if a label is available, we sort on the label, since that is what is displayed to the user
            Object label = getter.getLabel(provider, value, locale);
            if (label == null)
                label = GenericValueFormatter.getLabelForSorting(value, provider.getContext());
            if (label != null)
                return label;
            return value;
        }

        private int valueCompare(Object value1, Object value2) {
            if (value1 == null && value2 == null) {
                return 0;
            } else if (value1 == null) {
                return 1;
            } else if (value2 == null) {
                return -1;
            } else if (value1.getClass() != value2.getClass()) {
                // group objects of different type together by sorting on their name
                return value1.getClass().getName().compareTo(value2.getClass().getName());
            } else if (value1 instanceof I18nMessage) {
                return ((I18nMessage)value1).getText().compareTo(((I18nMessage)value2).getText());
            } else {
                // Let's assume they all implement comparable
                return ((Comparable)value1).compareTo(value2);
            }
        }
    }

    private static class CompareInfo {
        ValueGetter getter;
        SortOrder sortOrder;

        public CompareInfo(ValueGetter getter, SortOrder sortOrder) {
            this.getter = getter;
            this.sortOrder = sortOrder;
        }
    }

    private static interface ResultObjectGetter<T> {
        public T get(ValueGetter.Provider provider) throws WorkflowException;
    }
}