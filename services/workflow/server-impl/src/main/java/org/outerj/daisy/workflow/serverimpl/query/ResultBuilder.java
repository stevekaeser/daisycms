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

import org.outerj.daisy.workflow.*;
import org.outerj.daisy.workflow.commonimpl.WfXmlHelper;
import org.outerj.daisy.workflow.serverimpl.IntWfContext;
import org.outerj.daisy.workflow.serverimpl.WfActorKeyToStringConverter;
import org.outerj.daisy.workflow.serverimpl.WfVersionKeyToStringConverter;
import org.outerj.daisy.workflow.serverimpl.query.ValueGetterProvider.ValueGetterProviderBuilder;
import org.outerj.daisy.i18n.I18nMessage;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.Timer;
import org.outerx.daisy.x10Workflow.SearchResultDocument;
import org.outerx.daisy.x10Workflow.RepeatedSearchResultValues;
import org.apache.xmlbeans.XmlDateTime;
import org.apache.xmlbeans.XmlObject;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;

/**
 * Builds an XML-resultset based for processes or tasks based on the selected values.
 */
public class ResultBuilder {

    public static SearchResultDocument buildTaskResult(List<TaskInstance> tasks, List<QuerySelectItem> selectItems,
            ResultChunker.ChunkInfo chunkInfo, IntWfContext context) throws WorkflowException {

        ValueGetterProvider.ValueGetterProviderBuilder provider = ValueGetterProvider.getTaskValueProviderBuilder(context);
        return buildResult(tasks, selectItems, chunkInfo, context, provider);
    }

    public static SearchResultDocument buildProcessResult(List<ProcessInstance> processes, List<QuerySelectItem> selectItems,
            ResultChunker.ChunkInfo chunkInfo, IntWfContext context) throws WorkflowException {

        ValueGetterProvider.ValueGetterProviderBuilder provider = ValueGetterProvider.getProcessValueProviderBuilder(context);
        return buildResult(processes, selectItems, chunkInfo, context, provider);
    }

    public static SearchResultDocument buildTimerResult(List<Timer> timers, List<QuerySelectItem> selectItems,
            ResultChunker.ChunkInfo chunkInfo, IntWfContext context) throws WorkflowException {

        ValueGetterProvider.ValueGetterProviderBuilder provider = ValueGetterProvider.getTimerValueProviderBuilder(context);
        return buildResult(timers, selectItems, chunkInfo, context, provider);
    }

    public static SearchResultDocument buildResult(List things, List<QuerySelectItem> selectItems,
            ResultChunker.ChunkInfo chunkInfo, IntWfContext context,
            ValueGetterProviderBuilder valueProviderBuilder) throws WorkflowException {

        Locale locale = context.getLocale();
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
        QueryMetadataRegistry registry = context.getQueryMetadataRegistry();

        SearchResultDocument searchResultDocument = SearchResultDocument.Factory.newInstance();
        SearchResultDocument.SearchResult searchResult = searchResultDocument.addNewSearchResult();
        SearchResultDocument.SearchResult.Rows rows = searchResult.addNewRows();
        SearchResultDocument.SearchResult.Titles titles = searchResult.addNewTitles();

        List<ValueGetter> valueGetters = new ArrayList<ValueGetter>(selectItems.size());
        for (QuerySelectItem item : selectItems) {
            ValueGetter getter;
            SearchResultDocument.SearchResult.Titles.Title title = titles.addNewTitle();
            switch (item.getType()) {
                case PROPERTY:
                    IntProperty prop = registry.getProperty(item.getName());
                    getter = prop.getPropertyGetter();
                    title.set(WfXmlHelper.i18nMessageToXml(prop.getLabel(locale)));
                    break;
                case TASK_VARIABLE:
                    getter = new TaskVariableGetter(item.getName());
                    title.set(WfXmlHelper.stringToXml(item.getName()));
                    break;
                case PROCESS_VARIABLE:
                    getter = new ProcessVariableGetter(item.getName());
                    title.set(WfXmlHelper.stringToXml(item.getName()));
                    break;
                default:
                    throw new RuntimeException("Unexpected situation, unhandled order by type: " + item.getType());
            }
            title.setName(item.getName());
            title.setSource(item.getType().toString());
            valueGetters.add(getter);
        }

        for (Object thing : things) {
            RepeatedSearchResultValues row = rows.addNewRow();
            ValueGetter.Provider provider = valueProviderBuilder.getProvider(thing);
            for (ValueGetter getter : valueGetters) {
                Object value = getter.getValue(provider);
                RepeatedSearchResultValues.Value valueXml = row.addNewValue();
                if (value != null) {
                    valueXml.addNewRaw().set(getXmlObject(value));
                    Object label = getter.getLabel(provider, value, locale);
                    if (label != null) {
                        valueXml.addNewLabel().set(getXmlObject(label));
                    } else {
                        String defaultLabel = GenericValueFormatter.getLabel(value, dateFormat, context);
                        if (defaultLabel != null)
                            valueXml.addNewLabel().set(WfXmlHelper.stringToXml(defaultLabel));
                    }
                }
            }
        }

        SearchResultDocument.SearchResult.ResultInfo resultInfo = searchResult.addNewResultInfo();
        resultInfo.setChunkOffset(chunkInfo.chunkOffset);
        resultInfo.setChunkLength(chunkInfo.chunkLength);
        resultInfo.setRequestedChunkOffset(chunkInfo.requestChunkOffset);
        resultInfo.setRequestedChunkLength(chunkInfo.requestChunkLength);
        resultInfo.setSize(chunkInfo.size);

        return searchResultDocument;
    }

    private static final WfActorKeyToStringConverter actorKeyToStringConverter = new WfActorKeyToStringConverter();
    private static final WfVersionKeyToStringConverter versionKeyToStringConverter = new WfVersionKeyToStringConverter();

    private static XmlObject getXmlObject(Object value) {
        if (value instanceof I18nMessage) {
            return WfXmlHelper.i18nMessageToXml((I18nMessage)value);
        } else if (value instanceof Date) {
            XmlDateTime date = XmlDateTime.Factory.newInstance();
            date.setDateValue((Date)value);
            return date;
        } else if (value instanceof WfActorKey) {
            return WfXmlHelper.stringToXml((String)actorKeyToStringConverter.convert(value));
        } else if (value instanceof WfUserKey) {
            return WfXmlHelper.stringToXml(String.valueOf(((WfUserKey)value).getId()));
        } else if (value instanceof WfVersionKey) {
            return WfXmlHelper.stringToXml((String)versionKeyToStringConverter.convert(value));
        } else {
            return WfXmlHelper.stringToXml(value.toString());
        }
    }
}
