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
package org.outerj.daisy.docdiff;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.RepositorySchema;

import java.util.Locale;

/**
 * A context/helper object for implementations of {@link DocDiffOutput}.
 */
public class DocDiffOutputHelper {
    private Repository repository;
    private RepositorySchema repositorySchema;
    private Document document1;
    private Document document2;
    private Version version1;
    private Version version2;
    private Locale locale;

    public DocDiffOutputHelper(Document document1, Document document2, Version version1, Version version2,
            Repository repository, Locale locale) {
        this.repository = repository;
        this.repositorySchema = repository.getRepositorySchema();
        this.locale = locale;
        this.document1 = document1;
        this.document2 = document2;
        this.version1 = version1;
        this.version2 = version2;
    }

    public String getPartLabel(long typeId) throws Exception {
        PartType partType = repositorySchema.getPartTypeById(typeId, false);
        return partType.getLabel(locale);
    }

    public PartType getPartType(long typeId) throws Exception {
        return repositorySchema.getPartTypeById(typeId, false);
    }

    public String getFieldLabel(long typeId) throws Exception {
        FieldType fieldType = repositorySchema.getFieldTypeById(typeId, false);
        return fieldType.getLabel(locale);
    }

    public FieldType getFieldType(long typeId) throws Exception {
        return repositorySchema.getFieldTypeById(typeId, false);
    }

    public Version getVersion1() {
        return version1;
    }

    public Version getVersion2() {
        return version2;
    }

    public Document getDocument1() {
        return document1;
    }

    public Document getDocument2() {
        return document2;
    }

    public Locale getLocale() {
        return locale;
    }

    public Repository getRepository() {
        return repository;
    }
}
