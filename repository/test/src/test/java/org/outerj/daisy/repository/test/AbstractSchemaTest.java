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
package org.outerj.daisy.repository.test;

import org.outerj.daisy.repository.testsupport.AbstractDaisyTestCase;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.schema.*;

import java.util.Locale;
import java.util.List;

public abstract class AbstractSchemaTest extends AbstractDaisyTestCase {
    protected boolean resetDataStores() {
        return true;
    }

    public void testSchema() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();
        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);
        RepositorySchema schema = repository.getRepositorySchema();

        UserManager userManager = repository.getUserManager();
        long testUserId = userManager.getUser("testuser", false).getId();

        //
        // Test basic operations for PartType
        //

        // basic operation
        PartType partType = schema.createPartType("mypart", "");
        partType.save();

        // error in name
        try {
            partType.setName("part type name with spaces");
            fail("Could set an invalid part name");
        } catch (Exception e) {}

        try {
            partType.setName("0partNameStartingWithDigit");
            fail("Could set an invalid part name");
        } catch (Exception e) {}

        // retrieve the created PartType by id and by name
        partType = schema.getPartTypeById(partType.getId(), true);
        assertNotNull(partType);

        partType = schema.getPartTypeById(partType.getId(), false);
        assertNotNull(partType);

        partType = schema.getPartTypeByName("mypart", true);
        assertNotNull(partType);

        partType = schema.getPartTypeByName("mypart", false);
        assertNotNull(partType);

        try {
            schema.getPartTypeByName("nonExistingPartType", true);
            fail("Retrieving non existing part type should throw exception.");
        } catch (Exception e) {}

        try {
            schema.getPartTypeByName("nonExistingPartType", false);
            fail("Retrieving non existing part type should throw exception.");
        } catch (Exception e) {}

        // by default, deprecated is false
        assertFalse(partType.isDeprecated());

        // do some updates on the part type
        partType = schema.getPartTypeById(partType.getId(), true);
        partType.setMimeTypes("text/xml");
        partType.setLabel(new Locale("nl"), "mijn part");
        partType.setDescription(new Locale("nl"), "beschrijving van mijn part");
        partType.setDeprecated(true);
        partType.save();

        partType = schema.getPartTypeById(partType.getId(), true);
        assertEquals("text/xml", partType.getMimeTypes());
        assertEquals("mijn part", partType.getLabelExact(new Locale("nl")));
        assertEquals("beschrijving van mijn part", partType.getDescriptionExact(new Locale("nl")));
        assertEquals("mijn part", partType.getLabel(new Locale("nl","BE")));
        assertNull(partType.getLabelExact(new Locale("nl", "BE")));
        assertTrue(partType.isDeprecated());
        assertEquals(testUserId, partType.getLastModifier());

        // test concurrent modification
        PartType partTypeConcurrent = schema.getPartTypeById(partType.getId(), true);
        partType.save();
        try {
            partTypeConcurrent.save();
            fail("Expected a concurrent modification exception.");
        } catch (Exception e) {}

        PartType partType2 = schema.createPartType("mypart2", "");
        partType2.save();
        PartType partType3 = schema.createPartType("mypart3", "");
        partType3.save();

        assertEquals(3, schema.getAllPartTypes(true).getArray().length);
        assertEquals(3, schema.getAllPartTypes(false).getArray().length);

        // creating second with same name should fail
        PartType partTypeSameName = schema.createPartType("mypart", "");
        try {
            partTypeSameName.save();
            fail("Expected exception when creating part with same name as existing part.");
        } catch (Exception e) {}

        // test readonlyness
        partType = schema.getPartTypeByName("mypart", false);
        try {
            partType.save();
            fail("Saving read-only part should fail");
        } catch (Exception e) {}

        // test part type deletion
        PartType partTypeToBeDeleted = schema.createPartType("PartTypeToBeDeleted", "");
        partTypeToBeDeleted.save();
        schema.deletePartType(partTypeToBeDeleted.getId());
        try {
            schema.getPartTypeById(partTypeToBeDeleted.getId(), false);
            fail("Expected PartTypeNotFoundException.");
        } catch (PartTypeNotFoundException e) {
        }
        try {
            schema.getPartTypeById(partTypeToBeDeleted.getId(), true);
            fail("Expected PartTypeNotFoundException.");
        } catch (PartTypeNotFoundException e) {
        }
        try {
            partType.save();
            fail("Expected exception when saving deleted part.");
        } catch (Exception e) {
        }



        //
        // Test basic operations for FieldType
        //

        // basic operation
        FieldType fieldType = schema.createFieldType("myfield", ValueType.STRING);
        fieldType.save();

        // error in name
        try {
            fieldType.setName("field type name with spaces");
            fail("Could set an invalid field name");
        } catch (Exception e) {}

        // retrieve the created FieldType by id and by name
        fieldType = schema.getFieldTypeById(fieldType.getId(), true);
        assertNotNull(fieldType);

        fieldType = schema.getFieldTypeById(fieldType.getId(), false);
        assertNotNull(fieldType);

        fieldType = schema.getFieldTypeByName("myfield", true);
        assertNotNull(fieldType);

        fieldType = schema.getFieldTypeByName("myfield", false);
        assertNotNull(fieldType);

        try {
            schema.getFieldTypeByName("nonExistingFieldType", true);
            fail("Retrieving non existing field type should throw exception.");
        } catch (Exception e) {}

        try {
            schema.getFieldTypeByName("nonExistingFieldType", false);
            fail("Retrieving non existing field type should throw exception.");
        } catch (Exception e) {}

        // by default, deprecated is false
        assertFalse(fieldType.isDeprecated());
        assertFalse(fieldType.isAclAllowed());
        // by default, fieldtype size is zero
        assertEquals(fieldType.getSize(), 0);

        // do some updates on the field type
        fieldType = schema.getFieldTypeById(fieldType.getId(), true);
        fieldType.setLabel(new Locale("nl"), "mijn veld");
        fieldType.setDescription(new Locale("nl"), "beschrijving van mijn veld");
        fieldType.setDeprecated(true);
        fieldType.setAclAllowed(true);
        fieldType.setSize(10);

        // add selection list
        assertNull("no selection list set and yet not null returned", fieldType.getSelectionList());
        StaticSelectionList selList = fieldType.createStaticSelectionList();

        // first add two items which comply to the valuetype of the field type (and hence the selection list)
        StaticListItem statListItem1 = selList.createItem("polski");
        statListItem1.setLabel(Locale.US, "labeltest");
        StaticListItem statListItem2 = selList.createItem("bez");
        try {
            // now add an item which does not comply
            selList.createItem(new Integer(967));
            assertTrue("could add non-valuetype compliant item to selection list", false);
        } catch (Exception e1) {}
        // TODO catch the exception here
        // and a third one which complies again
        StaticListItem statListItem3 = selList.createItem("trudu");

        fieldType.save();

        fieldType = schema.getFieldTypeById(fieldType.getId(), true);
        assertEquals("mijn veld", fieldType.getLabelExact(new Locale("nl")));
        assertEquals("beschrijving van mijn veld", fieldType.getDescriptionExact(new Locale("nl")));
        assertEquals("mijn veld", fieldType.getLabel(new Locale("nl","BE")));
        assertNull(fieldType.getLabelExact(new Locale("nl", "BE")));
        assertTrue(fieldType.isDeprecated());
        assertTrue(fieldType.isAclAllowed());
        assertEquals(testUserId, fieldType.getLastModifier());
        assertEquals(fieldType.getSize(), 10);

        // test static selection list
        // we'll use the fieldType with valuetype STRING
        StaticSelectionList fetchedSelList = (StaticSelectionList)fieldType.getSelectionList();
        assertNotNull(fetchedSelList);
        List<? extends ListItem> selListItems = fetchedSelList.getItems();
        assertEquals(selListItems.size(), 3);
        // selection list should maintain order
        ListItem listItem1 = selListItems.get(0);
        ListItem listItem2 = selListItems.get(1);
        ListItem listItem3 = selListItems.get(2);

        assertEquals(statListItem1.getValue(), listItem1.getValue());
        assertEquals(statListItem2.getValue(), listItem2.getValue());
        assertEquals(statListItem3.getValue(), listItem3.getValue());

        assertEquals("labeltest", listItem1.getLabel(Locale.US));

        // add a new field to the list
        fetchedSelList.createItem("DumuziAbsu");
        fieldType.save();

        fieldType = schema.getFieldTypeById(fieldType.getId(), true);
        fetchedSelList = (StaticSelectionList)fieldType.getSelectionList();
        selListItems = fetchedSelList.getItems();
        assertEquals(selListItems.size(), 4);
        assertEquals(selListItems.get(3).getValue(), "DumuziAbsu");

        // clear the selection list
        fieldType.clearSelectionList();
        fieldType.save();
        fieldType = schema.getFieldTypeById(fieldType.getId(), true);
        assertNull(fieldType.getSelectionList());

        // test concurrent modification
        FieldType fieldTypeConcurrent = schema.getFieldTypeById(fieldType.getId(), true);
        fieldType.save();
        try {
            fieldTypeConcurrent.save();
            fail("Expected a concurrent modification exception.");
        } catch (Exception e) {}

        FieldType fieldType2 = schema.createFieldType("myfield2", ValueType.STRING);
        fieldType2.save();
        FieldType fieldType3 = schema.createFieldType("myfield3", ValueType.STRING);
        fieldType3.save();

        assertEquals(3, schema.getAllFieldTypes(true).getArray().length);
        assertEquals(3, schema.getAllFieldTypes(false).getArray().length);

        // creating second with same name should fail
        FieldType fieldTypeSameName = schema.createFieldType("myfield", ValueType.STRING);
        try {
            fieldTypeSameName.save();
            fail("Expected exception when creating field with same name as existing field.");
        } catch (Exception e) {}

        // test readonlyness
        fieldType = schema.getFieldTypeByName("myfield", false);
        try {
            fieldType.save();
            fail("Saving read-only field should fail");
        } catch (Exception e) {}

        // test that saving/loading of the different ValueTypes goes correctly

        FieldType otherFieldType = schema.createFieldType("datefield", ValueType.DATE);
        otherFieldType.save();
        otherFieldType = schema.getFieldTypeById(otherFieldType.getId(), true);
        assertEquals(ValueType.DATE, otherFieldType.getValueType());

        otherFieldType = schema.createFieldType("datetimefield", ValueType.DATETIME);
        otherFieldType.save();
        otherFieldType = schema.getFieldTypeById(otherFieldType.getId(), true);
        assertEquals(ValueType.DATETIME, otherFieldType.getValueType());

        otherFieldType = schema.createFieldType("longfield", ValueType.LONG);
        otherFieldType.save();
        otherFieldType = schema.getFieldTypeById(otherFieldType.getId(), true);
        assertEquals(ValueType.LONG, otherFieldType.getValueType());

        otherFieldType = schema.createFieldType("doublefield", ValueType.DOUBLE);
        otherFieldType.save();
        otherFieldType = schema.getFieldTypeById(otherFieldType.getId(), true);
        assertEquals(ValueType.DOUBLE, otherFieldType.getValueType());

        otherFieldType = schema.createFieldType("decimalfield", ValueType.DECIMAL);
        otherFieldType.save();
        otherFieldType = schema.getFieldTypeById(otherFieldType.getId(), true);
        assertEquals(ValueType.DECIMAL, otherFieldType.getValueType());

        otherFieldType = schema.createFieldType("booleanfield", ValueType.BOOLEAN);
        otherFieldType.save();
        otherFieldType = schema.getFieldTypeById(otherFieldType.getId(), true);
        assertEquals(ValueType.BOOLEAN, otherFieldType.getValueType());

        otherFieldType = schema.createFieldType("linkfield", ValueType.LINK);
        otherFieldType.save();
        otherFieldType = schema.getFieldTypeById(otherFieldType.getId(), true);
        assertEquals(ValueType.LINK, otherFieldType.getValueType());

        // check multivalue property
        FieldType multiValueFieldType = schema.createFieldType("string_non_multi_value", ValueType.STRING, false);
        assertEquals(false, multiValueFieldType.isMultiValue());
        multiValueFieldType.save();
        multiValueFieldType = schema.getFieldTypeById(multiValueFieldType.getId(), true);
        assertEquals(false, multiValueFieldType.isMultiValue());

        multiValueFieldType = schema.createFieldType("string_multi_value", ValueType.STRING, true);
        assertEquals(true, multiValueFieldType.isMultiValue());
        multiValueFieldType.save();
        multiValueFieldType = schema.getFieldTypeById(multiValueFieldType.getId(), true);
        assertEquals(true, multiValueFieldType.isMultiValue());

        // check hierarchical property
        FieldType hierarchicalFieldType = schema.createFieldType("string_non_hierarchical", ValueType.STRING);
        assertEquals(false, hierarchicalFieldType.isHierarchical());
        hierarchicalFieldType.save();
        hierarchicalFieldType = schema.getFieldTypeById(hierarchicalFieldType.getId(), true);
        assertEquals(false, hierarchicalFieldType.isHierarchical());

        hierarchicalFieldType = schema.createFieldType("string_hierarchical", ValueType.STRING, false, true);
        assertEquals(true, hierarchicalFieldType.isHierarchical());
        hierarchicalFieldType.save();
        hierarchicalFieldType = schema.getFieldTypeById(hierarchicalFieldType.getId(), true);
        assertEquals(true, hierarchicalFieldType.isHierarchical());

        // test field type deletion
        FieldType fieldTypeToBeDeleted = schema.createFieldType("FieldTypeToBeDeleted", ValueType.STRING);
        fieldTypeToBeDeleted.save();
        schema.deleteFieldType(fieldTypeToBeDeleted.getId());
        try {
            schema.getFieldTypeById(fieldTypeToBeDeleted.getId(), false);
            fail("Expected FieldTypeNotFoundException.");
        } catch (FieldTypeNotFoundException e) {
        }
        try {
            schema.getFieldTypeById(fieldTypeToBeDeleted.getId(), true);
            fail("Expected FieldTypeNotFoundException.");
        } catch (FieldTypeNotFoundException e) {
        }
        try {
            fieldType.save();
            fail("Expected exception when saving deleted field.");
        } catch (Exception e) {
        }

        //
        // Test basic operations for DocumentType
        //

        DocumentType documentType = schema.createDocumentType("mydoctype");
        documentType.save();

        documentType = schema.getDocumentTypeById(documentType.getId(), true);
        documentType.addFieldType(fieldType, true);
        documentType.addFieldType(fieldType2, false).setEditable(false);
        documentType.save();
        documentType.addPartType(partType, true);
        documentType.addPartType(partType2, false).setEditable(false);
        documentType.save();

        documentType = schema.getDocumentTypeById(documentType.getId(), true);
        assertTrue(documentType.getFieldTypeUse(fieldType.getId()).isRequired());
        assertTrue(documentType.getFieldTypeUse(fieldType.getId()).isEditable());
        assertFalse(documentType.getFieldTypeUse(fieldType2.getId()).isRequired());
        assertFalse(documentType.getFieldTypeUse(fieldType2.getId()).isEditable());

        assertTrue(documentType.getPartTypeUse(partType.getId()).isRequired());
        assertTrue(documentType.getPartTypeUse(partType.getId()).isEditable());
        assertFalse(documentType.getPartTypeUse(partType2.getId()).isRequired());
        assertFalse(documentType.getPartTypeUse(partType2.getId()).isEditable());

        assertFalse(documentType.isDeprecated());

        documentType.setDeprecated(true);
        documentType.setName("altereddoctype");
        documentType.save();

        documentType = schema.getDocumentTypeById(documentType.getId(), false);
        assertTrue(documentType.isDeprecated());
        assertEquals("altereddoctype", documentType.getName());

        DocumentType documentType2 = schema.createDocumentType("mydoctype2");
        documentType2.addFieldType(fieldType, false);
        documentType2.save();

        assertEquals(2, schema.getAllDocumentTypes(true).getArray().length);
        assertEquals(2, schema.getAllDocumentTypes(false).getArray().length);

        DocumentType documentTypeConcurrent = schema.getDocumentTypeById(documentType2.getId(), true);
        documentType2.save();
        try {
            documentTypeConcurrent.save();
            fail("Expected a concurrent modification exception.");
        } catch (Exception e) {}

        // creating second with same name should fail
        DocumentType documentTypeSameName = schema.createDocumentType("mydoctype2");
        try {
            documentTypeSameName.save();
            fail("Expected exception when creating document type with same name as existing one.");
        } catch (Exception e) {}

        // test readonlyness
        documentType2 = schema.getDocumentTypeByName("mydoctype2", false);
        try {
            documentType2.getFieldTypeUse(fieldType.getId()).setEditable(true);
            fail("Trying to modify a read-only document type should fail");
        } catch (RuntimeException e) {}

        try {
            documentType2.save();
            fail("Saving read-only document type should fail");
        } catch (Exception e) {}


        // test document type deletion
        DocumentType documentTypeToBeDeleted = schema.createDocumentType("DocumentTypeToBeDeleted");
        documentTypeToBeDeleted.addFieldType(fieldType, true);
        documentTypeToBeDeleted.addPartType(partType, true);
        documentTypeToBeDeleted.save();

        // verify that the field and part cannot be deleted now
        try {
            schema.deleteFieldType(fieldType.getId());
            fail("Deleting field type associated with document type should fail.");
        } catch (Exception e) {}
        try {
            schema.deletePartType(partType.getId());
            fail("Deleting part type associated with document type should fail.");
        } catch (Exception e) {}

        schema.deleteDocumentType(documentTypeToBeDeleted.getId());
        try {
            schema.getDocumentTypeById(documentTypeToBeDeleted.getId(), false);
            fail("Expected DocumentTypeNotFoundException.");
        } catch (DocumentTypeNotFoundException e) {
        }
        try {
            schema.getDocumentTypeById(documentTypeToBeDeleted.getId(), true);
            fail("Expected DocumentTypeNotFoundException.");
        } catch (DocumentTypeNotFoundException e) {
        }
        try {
            documentType.save();
            fail("Expected exception when saving deleted document.");
        } catch (Exception e) {
        }

        //
        // Test static hierarchical selection lists
        //

        // Lets create a field type with this list:
        //   A
        //     B
        //     C
        //       D
        //       E
        //     F
        //   G
        //   H
        FieldType hierFieldType = repository.getRepositorySchema().createFieldType("FieldTypeWithHierSelList", ValueType.STRING, false, true);
        StaticSelectionList hierList = hierFieldType.createStaticSelectionList();
        StaticListItem hierItem = hierList.createItem("A");
        hierItem.createItem("B");
        hierItem = hierItem.createItem("C");
        hierItem.createItem("D");
        hierItem.createItem("E");
        hierList.getItem("A").createItem("F");
        hierList.createItem("G");
        hierList.createItem("H");
        hierFieldType.save();
        hierFieldType = repository.getRepositorySchema().getFieldTypeByName("FieldTypeWithHierSelList", true);
        hierList = (StaticSelectionList)hierFieldType.getSelectionList();
        assertEquals(3, hierList.getItems().size());
        assertEquals(3, hierList.getItems().get(0).getItems().size());
        assertEquals("E", hierList.getItem("A").getItem("C").getItems().get(1).getValue());


        //
        // Test hierarchical query selection list
        //
        {
            FieldType linkFieldType = repository.getRepositorySchema().createFieldType("LinkField2", ValueType.LINK, true, false);
            linkFieldType.save();

            DocumentType linkDocumentType = repository.getRepositorySchema().createDocumentType("LinkDocType");
            linkDocumentType.addFieldType(linkFieldType, false);
            linkDocumentType.save();

            DocumentType linkDocumentType2 = repository.getRepositorySchema().createDocumentType("LinkDocType2");
            linkDocumentType2.addFieldType(linkFieldType, false);
            linkDocumentType2.save();

            Document docC = repository.createDocument("C", "LinkDocType2");
            docC.save();
            Document docB = repository.createDocument("B", "LinkDocType2");
            docB.save();
            Document docA = repository.createDocument("A", "LinkDocType");
            docA.setField("LinkField2", new Object[] {docB.getVariantKey(), docC.getVariantKey()});
            docA.save();

            FieldType hierLinkFieldType = repository.getRepositorySchema().createFieldType("FieldTypeWithHierQuerySelList", ValueType.LINK, false, true);
            hierLinkFieldType.createHierarchicalQuerySelectionList("documentType = 'LinkDocType'", new String[] {"LinkField2"}, false);
            hierLinkFieldType.save();
            hierLinkFieldType = repository.getRepositorySchema().getFieldTypeByName("FieldTypeWithHierQuerySelList", true);
            assertEquals(2, hierLinkFieldType.getSelectionList().getItems().get(0).getItems().size());
            //dumpList(hierLinkFieldType.getSelectionList());
        }

        //
        // Test parent-linked selection list
        //
        {
            FieldType parentLinkFieldType = repository.getRepositorySchema().createFieldType("ParentLinkField", ValueType.LINK, false, false);
            parentLinkFieldType.save();

            DocumentType parentLinkDocumentType = repository.getRepositorySchema().createDocumentType("ParentLinkDocType");
            parentLinkDocumentType.addFieldType(parentLinkFieldType, false);
            parentLinkDocumentType.save();

            Document docA = repository.createDocument("A", "ParentLinkDocType");
            docA.save();

            Document docB = repository.createDocument("B", "ParentLinkDocType");
            docB.setField("ParentLinkField", docA.getVariantKey());
            docB.save();

            Document docC = repository.createDocument("C", "ParentLinkDocType");
            docC.setField("ParentLinkField", docB.getVariantKey());
            docC.save();

            Document docD = repository.createDocument("D", "ParentLinkDocType");
            docD.save();

            FieldType plFieldType = repository.getRepositorySchema().createFieldType("FieldTypeWithParentLinkedList", ValueType.LINK, false, true);
            plFieldType.createParentLinkedSelectionList("documentType = 'ParentLinkDocType'", "ParentLinkField", true);
            plFieldType.save();
            FieldType freshFieldType = repository.getRepositorySchema().getFieldTypeByName("FieldTypeWithParentLinkedList", true);
            assertEquals(plFieldType, freshFieldType);
            assertEquals(2, freshFieldType.getSelectionList().getItems(1, 1, Locale.US).size());
            //dumpList(freshFieldType.getSelectionList());
        }

        // TODO's for later
        //  - test only users with appropriate rights can do this stuff
        //  - verify that field/part/document types cannot be deleted while in use by documents/versions of documents

    }

    private void dumpList(SelectionList selectionList) {
        for (ListItem item : selectionList.getItems(1, 1, Locale.US)) {
            dumpListItem(item, 0);
        }
    }

    private void dumpListItem(ListItem listItem, int depth) {
        for (int i = 0; i < depth; i++)
            System.out.print(" ");
        System.out.println(listItem.getValue() + " - " + listItem.getLabel(Locale.US));
        for (ListItem item : listItem.getItems()) {
            dumpListItem(item, depth + 1);
        }
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;
}
