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
package org.outerj.daisy.workflow.serverimpl;

import org.jbpm.context.exe.Converter;
import org.outerj.daisy.workflow.WfVersionKey;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Converter for JBPM to map WfVersionKeys to strings and vice versa
 * for storage resp. retrieval to/from the database.
 *
 * <p>See also jbpm.varmapping.xml and jbpm.converter.properties.
 */
public class WfVersionKeyToStringConverter implements Converter {
    private static final Pattern DOC_ID_PATTERN = Pattern.compile("^([^@]*)@([^:]*):([^:]*)(?::([^:]*))?$");

    public boolean supports(Object value) {
        if (value == null)
            return true;
        return value.getClass() == WfVersionKey.class;
    }

    public Object convert(Object o) {
        WfVersionKey versionKey = (WfVersionKey)o;
        String documentId = versionKey.getDocumentId();

        // just to be sure...
        if (documentId.length() == 0)
            throw new RuntimeException("Invalid document ID in WfVersionKey object: it has a zero length.");
        if (documentId.indexOf('@') != -1)
            throw new RuntimeException("Invalid document ID in WfVersionKey object: it contains a '@' character.");

        // TODO we could consider checking the presence of a namespace in the document ID
        //      or even normalize the document ID ourselves (which would require access
        //      to the Repository object, which might be done through a thread local variable)
        //      Or we could the responsibility for giving a normalized WfVersionKey to the person
        //      supplying the variable value.

        return versionKey.getDocumentId() + "@" + versionKey.getBranchId() + ":" + versionKey.getLanguageId() + ":"
                + (versionKey.getVersion() != null ? versionKey.getVersion() : "");
    }

    public Object revert(Object o) {
        Matcher matcher = DOC_ID_PATTERN.matcher((String)o);
        if (matcher.matches()) {
            String docId = matcher.group(1);
            long branchId = Long.parseLong(matcher.group(2));
            long languageId = Long.parseLong(matcher.group(3));
            String version = matcher.group(4);
            if (version != null && version.length() == 0)
                version = null;

            return new WfVersionKey(docId, branchId, languageId, version);
        } else {
            // should normally not occur, unless someone's been hacking the database
            throw new RuntimeException("Invalid version key in string representation from database: " + o);
        }
    }
}
