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
package org.outerj.daisy.repository.commonimpl.namespace;

import org.outerj.daisy.repository.namespace.Namespace;
import org.outerx.daisy.x10.NamespaceDocument;

import java.util.Date;
import java.util.GregorianCalendar;

public class NamespaceImpl implements Namespace {
    private long id;
    private String name;
    private String fingerprint;
    private long registeredBy;
    private Date registeredOn;
    private boolean isManaged;
    private long documentCount;

    public NamespaceImpl(long id, String name, String fingerprint, long registeredBy, Date registeredOn, long documentCount, boolean isManaged) {
        this.id = id;
        this.name = name;
        this.fingerprint = fingerprint;
        this.registeredBy = registeredBy;
        this.registeredOn = registeredOn;
        this.isManaged = isManaged;
        this.documentCount = documentCount;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public long getRegisteredBy() {
        return registeredBy;
    }

    public Date getRegisteredOn() {
        return new Date(registeredOn.getTime());
    }

    public boolean isManaged() {
        return this.isManaged;
    }
    
    public void setManaged(boolean isManaged) {
        this.isManaged = isManaged;
    }

    public long getDocumentCount () {
        return this.documentCount;
    }

    public void setDocumentCount(long documentCount) {
        this.documentCount = documentCount;
    }

    public NamespaceDocument getXml() {
        NamespaceDocument namespaceDocument = NamespaceDocument.Factory.newInstance();
        NamespaceDocument.Namespace namespaceXml = namespaceDocument.addNewNamespace();
        namespaceXml.setId(id);
        namespaceXml.setName(name);
        namespaceXml.setFingerprint(fingerprint);
        namespaceXml.setRegisteredBy(registeredBy);
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(registeredOn);
        namespaceXml.setRegisteredOn(calendar);
        namespaceXml.setIsManaged(isManaged);
        namespaceXml.setDocumentCount(documentCount);

        return namespaceDocument;
    }
}
