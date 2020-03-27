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
package org.outerj.daisy.tools.importexport.import_.schema;

import org.outerj.daisy.repository.ValueType;

public interface SchemaLoadListener {
    void conflictingFieldType(String fieldTypeName, ValueType requiredType, ValueType foundType) throws Exception;

    /**
     * Reports that a conflict is found between the multivalue setting of the
     * currently existing field type and the field type in the import definition
     * with the same name.
     *
     * <p>The implementation may choose to simply log this error and continue, or
     * throw an exception to interrupt the schema import process.
     */
    void conflictingMultiValue(String fieldTypeName, boolean needMultivalue, boolean foundMultivalue) throws Exception;

    void conflictingHierarchical(String fieldTypeName, boolean needHierarchical, boolean foundHierarchical) throws Exception;

    void fieldTypeLoaded(String fieldTypeName, SchemaLoadResult result);

    void partTypeLoaded(String partTypeName, SchemaLoadResult result);

    void documentTypeLoaded(String documentTypeName, SchemaLoadResult result);

    void done();

    /**
     * If this method returns true, the import will be interrupted by throwing an exception.
     */
    boolean isInterrupted();
}
