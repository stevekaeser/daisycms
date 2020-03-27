/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.books.publisher.impl.publicationprocess;

import org.outerx.daisy.x10Bookpubtype.*;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.QNameSet;
import org.outerj.daisy.books.publisher.impl.util.CustomImplementationHelper;

import java.util.List;
import java.util.ArrayList;

public class PublicationProcessBuilder {
    public static PublicationProcess build(PublicationProcessDocument.PublicationProcess publicationProcessXml) throws Exception {
        List tasks = new ArrayList();
        XmlObject[] tasksXml = publicationProcessXml.selectChildren(QNameSet.ALL);
        for (int i = 0; i < tasksXml.length; i++) {
            if (tasksXml[i] instanceof ApplyDocumentTypeStylingDocument.ApplyDocumentTypeStyling) {
                ApplyDocumentTypeStylingTask task = new ApplyDocumentTypeStylingTask();
                tasks.add(task);
            } else if (tasksXml[i] instanceof ShiftHeadersDocument.ShiftHeaders) {
                ShiftHeadersTask task = new ShiftHeadersTask();
                tasks.add(task);
            } else if (tasksXml[i] instanceof AssembleBookDocument.AssembleBook) {
                AssembleBookDocument.AssembleBook taskXml = (AssembleBookDocument.AssembleBook)tasksXml[i];
                AssembleBookTask task = new AssembleBookTask(taskXml.getOutput());
                tasks.add(task);
            } else if (tasksXml[i] instanceof VerifyIdsAndLinksDocument.VerifyIdsAndLinks) {
                VerifyIdsAndLinksDocument.VerifyIdsAndLinks taskXml = (VerifyIdsAndLinksDocument.VerifyIdsAndLinks)tasksXml[i];
                VerifyIdsAndLinksTask task = new VerifyIdsAndLinksTask(taskXml.getInput(), taskXml.getOutput());
                tasks.add(task);
            } else if (tasksXml[i] instanceof ApplyPipelineDocument.ApplyPipeline) {
                ApplyPipelineDocument.ApplyPipeline taskXml = (ApplyPipelineDocument.ApplyPipeline)tasksXml[i];
                ApplyPipelineTask task = new ApplyPipelineTask(taskXml.getInput(), taskXml.getOutput(), taskXml.getPipe());
                tasks.add(task);
            } else if (tasksXml[i] instanceof AddTocAndListsDocument.AddTocAndLists) {
                AddTocAndListsDocument.AddTocAndLists taskXml = (AddTocAndListsDocument.AddTocAndLists)tasksXml[i];
                AddTocAndListsTask task = new AddTocAndListsTask(taskXml.getInput(), taskXml.getOutput());
                tasks.add(task);
            } else if (tasksXml[i] instanceof SplitInChunksDocument.SplitInChunks) {
                SplitInChunksDocument.SplitInChunks taskXml = (SplitInChunksDocument.SplitInChunks)tasksXml[i];
                String publishExtension = taskXml.isSetPublishExtension() ? taskXml.getPublishExtension() : ".html";
                String chunkNamePrefix = taskXml.isSetChunkNamePrefix() ? taskXml.getChunkNamePrefix() : "chunk";
                SplitInChunksTask task = new SplitInChunksTask(taskXml.getInput(), taskXml.getOutput(),
                        chunkNamePrefix, taskXml.getFirstChunkName(), publishExtension);
                tasks.add(task);
            } else if (tasksXml[i] instanceof WriteChunksDocument.WriteChunks) {
                WriteChunksDocument.WriteChunks taskXml = (WriteChunksDocument.WriteChunks)tasksXml[i];
                WriteChunksTask task = new WriteChunksTask(taskXml.getInput(), taskXml.getOutputPrefix(),
                        taskXml.getChunkFileExtension(), taskXml.getApplyPipeline(), taskXml.getPipelineOutputPrefix(),
                        taskXml.getChunkAfterPipelineFileExtension());
                tasks.add(task);
            } else if (tasksXml[i] instanceof AddNumberingDocument.AddNumbering) {
                AddNumberingDocument.AddNumbering taskXml = (AddNumberingDocument.AddNumbering)tasksXml[i];
                NumberingTask task = new NumberingTask(taskXml.getInput(), taskXml.getOutput());
                tasks.add(task);
            } else if (tasksXml[i] instanceof AddSectionTypesDocument.AddSectionTypes) {
                AddSectionTypesTask task = new AddSectionTypesTask();
                tasks.add(task);
            } else if (tasksXml[i] instanceof AddIndexDocument.AddIndex) {
                AddIndexDocument.AddIndex taskXml = (AddIndexDocument.AddIndex)tasksXml[i];
                AddIndexTask task = new AddIndexTask(taskXml.getInput(), taskXml.getOutput());
                tasks.add(task);
            } else if (tasksXml[i] instanceof CopyResourceDocument.CopyResource) {
                CopyResourceDocument.CopyResource taskXml = (CopyResourceDocument.CopyResource)tasksXml[i];
                CopyResourceTask task = new CopyResourceTask(taskXml.getFrom(), taskXml.getTo());
                tasks.add(task);
            } else if (tasksXml[i] instanceof MakePDFDocument.MakePDF) {
                MakePDFDocument.MakePDF taskXml = (MakePDFDocument.MakePDF)tasksXml[i];
                MakePdfTask task = new MakePdfTask(taskXml.getInput(), taskXml.getOutput(), taskXml.getConfigPath());
                tasks.add(task);
            } else if (tasksXml[i] instanceof GetDocumentPartDocument.GetDocumentPart) {
                GetDocumentPartDocument.GetDocumentPart taskXml = (GetDocumentPartDocument.GetDocumentPart)tasksXml[i];
                GetDocumentPartTask task = new GetDocumentPartTask(taskXml.getPropertyName(), taskXml.getPropertyOrigin(), taskXml.getPartName(), taskXml.getSaveAs(), taskXml.getSetProperty());
                tasks.add(task);
            } else if (tasksXml[i] instanceof CopyBookInstanceImagesDocument.CopyBookInstanceImages) {
                // copyBookInstanceImages task is deprecated in favour of copyBookInstanceResources, but still
                // here for backwards compatibility
                CopyBookInstanceImagesDocument.CopyBookInstanceImages taskXml = (CopyBookInstanceImagesDocument.CopyBookInstanceImages)tasksXml[i];
                CopyBookInstanceImagesTask task = new CopyBookInstanceImagesTask(taskXml.getInput(), taskXml.getOutput(), taskXml.getTo());
                tasks.add(task);
            } else if (tasksXml[i] instanceof CopyBookInstanceResourcesDocument.CopyBookInstanceResources) {
                CopyBookInstanceResourcesDocument.CopyBookInstanceResources taskXml = (CopyBookInstanceResourcesDocument.CopyBookInstanceResources)tasksXml[i];
                CopyBookInstanceResourcesTask task = new CopyBookInstanceResourcesTask(taskXml.getInput(), taskXml.getOutput(), taskXml.getTo());
                tasks.add(task);
            } else if (tasksXml[i] instanceof ZipDocument.Zip) {
                ZipTask task = new ZipTask();
                tasks.add(task);
            } else if (tasksXml[i] instanceof CustomDocument.Custom) {
                CustomDocument.Custom customXml = (CustomDocument.Custom)tasksXml[i];
                String className = customXml.getClass1();
                Class clazz;
                try {
                    clazz = PublicationProcessBuilder.class.getClassLoader().loadClass(className);
                } catch (ClassNotFoundException e) {
                    throw new Exception("Non-existing class specified in custom publication process task: " + className, e);
                }
                if (!PublicationProcessTask.class.isAssignableFrom(clazz)) {
                    throw new Exception("The class specified in the custom publication process task does not implement PublicationProcessTask: " + className);
                }

                PublicationProcessTask customTask = (PublicationProcessTask)CustomImplementationHelper.instantiateComponent(clazz, customXml);
                tasks.add(customTask);
            } else {
                XmlOptions xmlOptions = new XmlOptions();
                xmlOptions.setSaveOuter();
                throw new Exception("Unsupport publication process task element encountered: " + tasksXml[i].xmlText(xmlOptions));
            }
        }
        PublicationProcess publicationProcess = new PublicationProcess((PublicationProcessTask[])tasks.toArray(new PublicationProcessTask[tasks.size()]));
        return publicationProcess;
    }
}
