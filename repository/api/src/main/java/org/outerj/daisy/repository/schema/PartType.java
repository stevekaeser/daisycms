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
package org.outerj.daisy.repository.schema;

import org.outerj.daisy.repository.RepositoryException;
import org.outerx.daisy.x10.PartTypeDocument;

import java.util.Date;

/**
 * Describes a type of part in the repository.
 *
 * <p>Instances are retrieved from {@link RepositorySchema}.
 *
 * <p>The equals method for PartType is supported, two part types are
 * equal if all their defining data is equal, with exception of the ID.
 */
public interface PartType extends DescriptionEnabled, LabelEnabled {
    long getId();

    String getName();

    void setName(String name);

    /**
     * Returns a string describing limitations on the allowed mime-types.
     * An empty string means no restrictions. Otherwise, the string contains
     * a comma-separated list of allowed mime-types.
     */
    String getMimeTypes();

    /**
     * Specify the type of data that is allowed in parts of this type based
     * on the mime-type.
     *
     * @param mimeTypes A comma-separated list of mime-types. If empty, all mime-types are allowed.
     */
    void setMimeTypes(String mimeTypes);

    boolean mimeTypeAllowed(String mimeType);

    /**
     * Indicates if the content of the part is Daisy HTML (well-formed
     * XML using HTML elements).
     */
    boolean isDaisyHtml();

    /**
     * Sets the Daisy-HTML flag.
     *
     * <p>Note: when the argument daisyHTML is true, the link extractor will be forced
     * to "daisy-html", for reasons of back-compatibility (in the past, link extractors
     * were not explicitely configurable, and link extraction always happened for
     * daisy-html parts). It is possible to override this again by calling
     * setLinkExtractor after setDaisyHtml.
     */
    void setDaisyHtml(boolean daisyHtml);

    /**
     *
     * @param name allowed to be null
     */
    void setLinkExtractor(String name);

    /**
     * Returns the name of the link extractor to use for parts of this type, can be null.
     */
    String getLinkExtractor();

    boolean isDeprecated();

    void setDeprecated(boolean deprecated);

    /**
     * When was this PartType last changed (persistently). Returns null on newly
     * created PartTypes.
     */
    Date getLastModified();

    /**
     * Who (which user) last changed this PartType (persistently). Returns -1 on
     * newly created PartTypes.
     */
    long getLastModifier();

    PartTypeDocument getXml();

    /**
     * Changes the state of this object to match the given XML. Note that this will
     * only change properties otherwise changeable through methods of this interface,
     * and not internal properties like id or lastModified.
     */
    void setAllFromXml(PartTypeDocument.PartType partTypeXml);

    void save() throws RepositoryException;

    long getUpdateCount();
}
