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
package org.outerj.daisy.frontend.admin;

import org.apache.cocoon.forms.binding.AbstractCustomBinding;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.Repeater;
import org.apache.cocoon.forms.formmodel.ContainerWidget;
import org.apache.commons.jxpath.JXPathContext;
import org.outerj.daisy.repository.acl.*;
import static org.outerj.daisy.repository.acl.AclActionType.*;
import static org.outerj.daisy.repository.acl.AclDetailPermission.*;
import static org.outerj.daisy.repository.acl.AclPermission.*;

import java.util.*;

/**
 * The binding for loading an ACL object into the form for editing the binding,
 * and vice versa.
 */
public class AclBinding extends AbstractCustomBinding {
    protected void doLoad(Widget widget, JXPathContext jxPathContext) throws Exception {
        Acl acl = (Acl) jxPathContext.getValue(".");
        Repeater repeater = (Repeater) widget;

        for (int i = 0; i < acl.size(); i++) {
            Repeater.RepeaterRow objectRow = repeater.addRow();
            AclObject aclObject = acl.get(i);
            objectRow.getChild("expr").setValue(aclObject.getObjectExpr());

            for (int j = 0; j < aclObject.size(); j++) {
                Repeater entryRepeater = (Repeater) objectRow.getChild("entries");
                Repeater.RepeaterRow entryRow = entryRepeater.addRow();
                AclEntry aclEntry = aclObject.get(j);
                entryRow.getChild("subjectType").setValue(aclEntry.getSubjectType());
                entryRow.getChild("subjectValue").setValue(new Long(aclEntry.getSubjectValue()));

                entryRow.getChild("readPerm").setValue(aclEntry.get(READ));

                AccessDetails details = aclEntry.getDetails(READ);
                entryRow.getChild("radNonLive").setValue(details == null ? GRANT : details.get(NON_LIVE));
                entryRow.getChild("radLiveHistory").setValue(details == null ? GRANT : details.get(LIVE_HISTORY));
                entryRow.getChild("radAllFields").setValue(details == null ? GRANT : details.get(ALL_FIELDS));
                entryRow.getChild("radFields").setValue(details == null ? "" : createCSVList(details.getAccessibleFields()));
                entryRow.getChild("radAllParts").setValue(details == null ? GRANT : details.get(ALL_PARTS));
                entryRow.getChild("radParts").setValue(details == null ? "" : createCSVList(details.getAccessibleParts()));
                entryRow.getChild("radFullText").setValue(details == null ? GRANT : details.get(FULLTEXT_INDEX));
                entryRow.getChild("radFTFragments").setValue(details == null ? GRANT : details.get(FULLTEXT_FRAGMENTS));
                entryRow.getChild("radSummary").setValue(details == null ? GRANT : details.get(SUMMARY));

                entryRow.getChild("writePerm").setValue(aclEntry.get(WRITE));

                details = aclEntry.getDetails(WRITE);
                entryRow.getChild("wadDocumentName").setValue(details == null ? GRANT : details.get(DOCUMENT_NAME));
                entryRow.getChild("wadLinks").setValue(details == null ? GRANT : details.get(LINKS));
                entryRow.getChild("wadCustomFields").setValue(details == null ? GRANT : details.get(CUSTOM_FIELDS));
                entryRow.getChild("wadCollections").setValue(details == null ? GRANT : details.get(COLLECTIONS));
                entryRow.getChild("wadDocumentType").setValue(details == null ? GRANT : details.get(DOCUMENT_TYPE));
                entryRow.getChild("wadRetired").setValue(details == null ? GRANT : details.get(RETIRED));
                entryRow.getChild("wadPrivate").setValue(details == null ? GRANT : details.get(PRIVATE));
                entryRow.getChild("wadReferenceLanguage").setValue(details == null ? GRANT : details.get(REFERENCE_LANGUAGE));
                entryRow.getChild("wadAllFields").setValue(details == null ? GRANT : details.get(ALL_FIELDS));
                entryRow.getChild("wadFields").setValue(details == null ? "" : createCSVList(details.getAccessibleFields()));
                entryRow.getChild("wadAllParts").setValue(details == null ? GRANT : details.get(ALL_PARTS));
                entryRow.getChild("wadParts").setValue(details == null ? "" : createCSVList(details.getAccessibleParts()));
                entryRow.getChild("wadChangeComment").setValue(details == null ? GRANT : details.get(CHANGE_COMMENT));
                entryRow.getChild("wadChangeType").setValue(details == null ? GRANT : details.get(CHANGE_TYPE));
                entryRow.getChild("wadSyncedWith").setValue(details == null ? GRANT : details.get(SYNCED_WITH));
                entryRow.getChild("wadVersionMeta").setValue(details == null ? GRANT : details.get(VERSION_META));

                entryRow.getChild("publishPerm").setValue(aclEntry.get(PUBLISH));
                
                details = aclEntry.getDetails(PUBLISH);
                entryRow.getChild("padLiveHistory").setValue(details == null ? GRANT : details.get(LIVE_HISTORY));
                
                entryRow.getChild("deletePerm").setValue(aclEntry.get(DELETE));
            }
        }
    }

    private String createCSVList(Set<String> values) {
        if (values == null)
            return "";

        StringBuilder builder = new StringBuilder();
        List<String> list = new ArrayList<String>(values);
        Collections.sort(list);
        for (String value : list) {
            if (builder.length() > 0)
                builder.append(",");
            builder.append(value);
        }
        return builder.toString();
    }

    protected void doSave(Widget widget, JXPathContext jxPathContext) throws Exception {
        Repeater repeater = (Repeater) widget;
        Acl acl = (Acl) jxPathContext.getValue(".");
        acl.clear();
        for (int i = 0; i < repeater.getSize(); i++) {
            AclObject aclObject = acl.createNewObject((String) repeater.getRow(i).getChild("expr").getValue());

            Repeater entryRepeater = (Repeater) repeater.getRow(i).getChild("entries");
            for (int j = 0; j < entryRepeater.getSize(); j++) {
                Repeater.RepeaterRow row = entryRepeater.getRow(j);
                Long subjectValue = (Long) row.getChild("subjectValue").getValue();
                if (subjectValue == null)
                    subjectValue = new Long(-1);
                AclEntry aclEntry = aclObject.createNewEntry((AclSubjectType) row.getChild("subjectType").getValue(), subjectValue.longValue());

                AclActionType readPerm = (AclActionType) row.getChild("readPerm").getValue();

                AccessDetails readDetails = null;
                if (readPerm == GRANT) {
                    Map<AclDetailPermission, String> mapping = new HashMap<AclDetailPermission, String>();
                    mapping.put(NON_LIVE, "radNonLive");
                    mapping.put(LIVE_HISTORY, "radLiveHistory");
                    mapping.put(ALL_PARTS, "radAllParts");
                    mapping.put(ALL_FIELDS, "radAllFields");
                    mapping.put(FULLTEXT_INDEX, "radFullText");
                    mapping.put(FULLTEXT_FRAGMENTS, "radFTFragments");
                    mapping.put(SUMMARY, "radSummary");

                    readDetails = aclEntry.createNewDetails(READ);
                    if (!updateDetails(readDetails, mapping, row, "radFields", "radParts"))
                        readDetails = null;
                }

                if (readPerm != AclActionType.DO_NOTHING)
                    aclEntry.set(AclPermission.READ, readPerm, readDetails);

                AclActionType writePerm = (AclActionType) row.getChild("writePerm").getValue();
                AccessDetails writeDetails = null;
                if (writePerm == GRANT) {
                    Map<AclDetailPermission, String> mapping = new HashMap<AclDetailPermission, String>();
                    mapping.put(DOCUMENT_NAME, "wadDocumentName");
                    mapping.put(LINKS, "wadLinks");
                    mapping.put(CUSTOM_FIELDS, "wadCustomFields");
                    mapping.put(COLLECTIONS, "wadCollections");
                    mapping.put(DOCUMENT_TYPE, "wadDocumentType");
                    mapping.put(RETIRED, "wadRetired");
                    mapping.put(PRIVATE, "wadPrivate");
                    mapping.put(REFERENCE_LANGUAGE, "wadReferenceLanguage");
                    mapping.put(ALL_FIELDS, "wadAllFields");
                    mapping.put(ALL_PARTS, "wadAllParts");
                    mapping.put(CHANGE_COMMENT, "wadChangeComment");
                    mapping.put(CHANGE_TYPE, "wadChangeType");
                    mapping.put(SYNCED_WITH, "wadSyncedWith");
                    mapping.put(VERSION_META, "wadVersionMeta");

                    writeDetails = aclEntry.createNewDetails(WRITE);
                    if (!updateDetails(writeDetails, mapping, row, "wadFields", "wadParts"))
                        writeDetails = null;
                }
                
                if (writePerm != AclActionType.DO_NOTHING)
                    aclEntry.set(AclPermission.WRITE, writePerm, writeDetails);

                AclActionType publishPerm = (AclActionType) row.getChild("publishPerm").getValue();
                AccessDetails publishDetails = null;
                if (publishPerm == GRANT) {
                    Map<AclDetailPermission, String> mapping = new HashMap<AclDetailPermission, String>();
                    mapping.put(LIVE_HISTORY, "padLiveHistory");
                    
                    publishDetails = aclEntry.createNewDetails(PUBLISH);
                    if (!updateDetails(publishDetails, mapping, row, null, null))
                        publishDetails = null;
                }

                if (publishPerm != AclActionType.DO_NOTHING)
                    aclEntry.set(AclPermission.PUBLISH, publishPerm, publishDetails);

                AclActionType deletePerm = (AclActionType) row.getChild("deletePerm").getValue();
                if (deletePerm != AclActionType.DO_NOTHING)
                    aclEntry.set(AclPermission.DELETE, deletePerm);

                aclObject.add(aclEntry);
            }

            acl.add(aclObject);
        }
    }

    private boolean updateDetails(AccessDetails details, Map<AclDetailPermission, String> detailToWidgetIdMapping,
            ContainerWidget parentWidget, String fieldsId, String partsId) {
        boolean needDetails = false;
        for (Map.Entry<AclDetailPermission, String> entry : detailToWidgetIdMapping.entrySet()) {
            if (parentWidget.getChild(entry.getValue()).getValue() != GRANT) {
                needDetails = true;
                break;
            }
        }

        if (!needDetails)
            return false;

        for (Map.Entry<AclDetailPermission, String> entry : detailToWidgetIdMapping.entrySet()) {
            AclActionType action = (AclActionType)parentWidget.getChild(entry.getValue()).getValue();
            details.set(entry.getKey(), action);

            if (fieldsId != null) {
                if (entry.getKey() == AclDetailPermission.ALL_FIELDS && action == DENY) {
                    Set<String> fields = parse((String)parentWidget.getChild(fieldsId).getValue());
                    for (String field : fields) {
                        details.addAccessibleField(field);
                    }
                }
            }
            
            if (partsId != null) {
                if (entry.getKey() == AclDetailPermission.ALL_PARTS && action == DENY) {
                    Set<String> parts = parse((String)parentWidget.getChild(partsId).getValue());
                    for (String part : parts) {
                        details.addAccessiblePart(part);
                    }
                }
            }
        }

        return true;
    }

    private Set<String> parse(String text) {
        if (text == null)
            return Collections.emptySet();

        String[] values = text.split(",");

        if (values.length == 0)
            return Collections.emptySet();

        Set<String> result = new HashSet<String>();
        for (String value : values) {
            value = value.trim();
            if (value.length() > 0)
                result.add(value);
        }

        return result;
    }
}
