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
package org.outerj.daisy.tools.importexport.model.document;

import org.outerj.daisy.tools.importexport.model.ImpExpVariantKey;
import org.outerj.daisy.repository.VersionState;

import java.util.*;

public class ImpExpDocument {
    private String id;
    private String branch;
    private String language;
    private ImpExpVariantKey variantKey;
    private String owner;
    private String type;
    private String name;
    private VersionState versionState;
    private String referenceLanguage;
    private Set<String> collections = new HashSet<String>();
    private Map<String, ImpExpField> fields = new HashMap<String, ImpExpField>();
    private Map<String, ImpExpPart> parts = new HashMap<String, ImpExpPart>();
    private Map<String, ImpExpCustomField> customFields = new HashMap<String, ImpExpCustomField>();
    private List<ImpExpLink> links = new ArrayList<ImpExpLink>();

    /**
     * Creates an ImpExpDocument with the minimal required information.
     */
    public ImpExpDocument(String id, String branch, String language, String type, String name) {
        if (id == null)
            throw new IllegalArgumentException("Null argument: id");
        if (branch == null)
            throw new IllegalArgumentException("Null argument: branch");
        if (language == null)
            throw new IllegalArgumentException("Null argument: language");
        if (type == null)
            throw new IllegalArgumentException("Null argument: type");
        if (name == null)
            throw new IllegalArgumentException("Null argument: name");

        this.id = id;
        this.branch = branch;
        this.language = language;
        this.variantKey = new ImpExpVariantKey(id, branch, language);
        this.type = type;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getBranch() {
        return branch;
    }

    public String getLanguage() {
        return language;
    }

    public ImpExpVariantKey getVariantKey() {
        return variantKey;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addCollection(String name) {
        collections.add(name);
    }

    public void removeCollection(String name) {
        collections.remove(name);
    }

    public String[] getCollections() {
        return collections.toArray(new String[0]);
    }

    public void clearCollections() {
        collections.clear();
    }

    public void addField(ImpExpField field) {
        fields.put(field.getType().getName(), field);
    }

    public ImpExpField getField(String typeName) {
        return fields.get(typeName);
    }

    public boolean hasField(String typeName) {
        return fields.containsKey(typeName);
    }

    public void removeField(String typeName) {
        fields.remove(typeName);
    }

    public ImpExpField[] getFields() {
        return fields.values().toArray(new ImpExpField[0]);
    }

    public void clearFields() {
        fields.clear();
    }

    public void addPart(ImpExpPart part) {
        parts.put(part.getType().getName(), part);
    }

    public ImpExpPart getPart(String typeName) {
        return parts.get(typeName);
    }

    public boolean hasPart(String typeName) {
        return parts.containsKey(typeName);
    }

    public void removePart(String typeName) {
        parts.remove(typeName);
    }

    public ImpExpPart[] getParts() {
        return parts.values().toArray(new ImpExpPart[0]);
    }

    public void clearParts() {
        parts.clear();
    }

    public void addCustomField(ImpExpCustomField field) {
        customFields.put(field.getName(), field);
    }

    public ImpExpCustomField getCustomField(String name) {
        return customFields.get(name);
    }

    public boolean hasCustomField(String name) {
        return customFields.containsKey(name);
    }

    public void removeCustomField(String name) {
        customFields.remove(name);
    }

    public ImpExpCustomField[] getCustomFields() {
        return customFields.values().toArray(new ImpExpCustomField[0]);
    }

    public void clearCustomFields() {
        customFields.clear();
    }

    public void addLink(ImpExpLink link) {
        links.add(link);
    }

    public ImpExpLink[] getLinks() {
        return links.toArray(new ImpExpLink[0]);
    }

    public void clearLinks() {
        links.clear();
    }

    public VersionState getVersionState() {
        return versionState;
    }

    public void setVersionState(VersionState versionState) {
        this.versionState = versionState;
    }

    public String getReferenceLanguage() {
        return referenceLanguage;
    }

    public void setReferenceLanguage(String referenceLanguage) {
        this.referenceLanguage = referenceLanguage;
    }
}
