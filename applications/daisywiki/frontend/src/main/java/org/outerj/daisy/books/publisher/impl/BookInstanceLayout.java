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
package org.outerj.daisy.books.publisher.impl;

import org.outerj.daisy.books.store.BookInstance;

/**
 * Helper class to determine the location of resources within a book instance.
 */
public class BookInstanceLayout {
    public static String getDocumentStorePath(BookInstance bookInstance, String documentId, long branchId, long languageId) {
        String storeBasePath = "/data/documents/" + documentId + "_" + branchId + "_" + languageId;
        String storePath = storeBasePath + ".xml";
        int counter = 0;
        while (bookInstance.exists(storePath)) {
            counter++;
            storePath = storeBasePath + "_" + counter + ".xml";
        }
        return storePath;
    }

    public static String getImageStorePath(String documentId, long branchId, long languageId, long versionId) {
        return "/data/resources/" + documentId + "_" + branchId + "_" + languageId + "_" + versionId;
    }

    public static String getResourceStorePath(String documentId, long branchId, long languageId, long versionId, long partTypeId) {
        return "/data/resources/" + documentId + "_" + branchId + "_" + languageId + "_" + versionId + "_" + partTypeId;
    }

    public static String geResourceStorePath() {
        return "/data/resources/";
    }

    public static String getDocumentInPublicationStorePath(String documentStorePath, String publicationOutputName) {
        return "/publications/" + publicationOutputName + "/documents/" + getFileName(documentStorePath);
    }

    public static String getPublicationOutputPath(String publicationOutputName) {
        return "/publications/" + publicationOutputName + "/";
    }

    public static String getDependenciesPath() {
        return "/data/dependencies.xml";
    }

    public static String getPublicationSpecsPath() {
        return "/publications/pubspecs.xml";
    }

    public static String getProcessedBookDefinitionPath() {
        return "/data/book_definition_processed.xml";
    }

    public static String getPublicationLogPath() {
        return "/publications/log.txt";
    }

    public static String getLinkLogPath() {
        return "/publications/link_errors.txt";
    }

    private static String getFileName(String path) {
        int slashPos = path.lastIndexOf("/");
        if (slashPos == -1)
            return path;
        return path.substring(slashPos + 1);
    }

}
