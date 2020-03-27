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
import org.outerj.daisy.repository.acl.*;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.query.PredicateExpression;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.DocumentType;

import java.math.BigDecimal;
import java.util.GregorianCalendar;

public class PredicateExpressionTest extends AbstractDaisyTestCase {
    private Repository repository;

    protected boolean resetDataStores() {
        return true;
    }

    protected void setUp() throws Exception {
        super.setUp();

        RepositoryManager repositoryManager = getLocalRepositoryManager();

        repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);
    }

    public void testPredicateEvaluation() throws Exception {

        // make sure the ACL doesn't get in our way
        Acl acl = repository.getAccessManager().getStagingAcl();
        AclObject aclObject = acl.createNewObject("true");
        acl.add(aclObject);
        AclEntry aclEntry = aclObject.createNewEntry(AclSubjectType.EVERYONE, -1);
        aclEntry.set(AclPermission.READ, AclActionType.GRANT);
        aclEntry.set(AclPermission.WRITE, AclActionType.GRANT);
        aclObject.add(aclEntry);
        acl.save();
        repository.getAccessManager().copyStagingToLive();

        CollectionManager collectionManager = repository.getCollectionManager();
        DocumentCollection collection1 = collectionManager.createCollection("collection1");
        collection1.save();

        DocumentCollection collection2 = collectionManager.createCollection("collection2");
        collection2.save();

        RepositorySchema repositorySchema = repository.getRepositorySchema();

        FieldType stringField = repositorySchema.createFieldType("stringfield", ValueType.STRING);
        stringField.save();

        FieldType longField = repositorySchema.createFieldType("longfield", ValueType.LONG);
        longField.save();

        FieldType doubleField = repositorySchema.createFieldType("doublefield", ValueType.DOUBLE);
        doubleField.save();

        FieldType decimalField = repositorySchema.createFieldType("decimalfield", ValueType.DECIMAL);
        decimalField.save();

        FieldType dateField = repositorySchema.createFieldType("datefield", ValueType.DATE);
        dateField.save();

        FieldType dateTimeField = repositorySchema.createFieldType("datetimefield", ValueType.DATETIME);
        dateTimeField.save();

        FieldType booleanField = repositorySchema.createFieldType("booleanfield", ValueType.BOOLEAN);
        booleanField.save();

        FieldType hierField = repositorySchema.createFieldType("hierField", ValueType.STRING, false, true);
        hierField.save();

        FieldType hierMvField = repositorySchema.createFieldType("hierMvField", ValueType.STRING, true, true);
        hierMvField.save();

        FieldType hierMvLinkField = repositorySchema.createFieldType("hierMvLinkField", ValueType.LINK, true, true);
        hierMvLinkField.save();

        FieldType multiValueStringField = repositorySchema.createFieldType("mv-stringfield", ValueType.STRING, true);
        multiValueStringField.save();

        FieldType linkField = repositorySchema.createFieldType("linkField", ValueType.LINK);
        linkField.save();

        DocumentType documentType = repositorySchema.createDocumentType("doctype");
        documentType.addFieldType(stringField, false);
        documentType.addFieldType(longField, false);
        documentType.addFieldType(doubleField, false);
        documentType.addFieldType(decimalField, false);
        documentType.addFieldType(dateField, false);
        documentType.addFieldType(dateTimeField, false);
        documentType.addFieldType(booleanField, false);
        documentType.addFieldType(multiValueStringField, false);
        documentType.addFieldType(hierField, false);
        documentType.addFieldType(hierMvField, false);
        documentType.addFieldType(hierMvLinkField, false);
        documentType.addFieldType(linkField, false);
        documentType.save();

        Document document = repository.createDocument("test", documentType.getId());
        document.setField(stringField.getId(), "some value");
        document.setField(longField.getId(), new Long(123));
        document.setField(doubleField.getId(), new Double(123.45));
        document.setField(decimalField.getId(), new BigDecimal("123.45678"));
        document.setField(dateField.getId(), new GregorianCalendar(2004, 11, 6).getTime());
        document.setField(dateTimeField.getId(), new GregorianCalendar(2004, 11, 6, 12, 15, 6).getTime());
        document.setField(booleanField.getId(), Boolean.TRUE);
        document.setField(multiValueStringField.getId(), new String[] {"value 1", "value 2"});
        document.setField(hierField.getId(), new HierarchyPath(new Object[] {"Aaa", "Bee", "Cee"}));
        document.setField(hierMvField.getId(), new Object[] { new HierarchyPath(new Object[] { "X", "Y", "Z"}), new HierarchyPath(new Object[] {"T"}) });
        document.setField(hierMvLinkField.getId(), new Object[] {
                new HierarchyPath(new Object[] { new VariantKey("10", 1, 1), new VariantKey("11", 1, 1)}),
                new HierarchyPath(new Object[] {new VariantKey("12", 1, 1)})
        });

        document.addToCollection(collection1);
        document.save();


        Document document2 = repository.createDocument("test2", documentType.getId());
        document2.setField(linkField.getId(), new VariantKey(document.getId(), -1, -1));
        document2.save();

        PredicateExpression expr;

        //
        // Test various combinations of datatypes and operators
        //

        // string
        testBoth(true, "$stringfield = 'some value'", document, document2);
        testBoth(false, "$stringfield = 'some other value'", document, document2);
        testBoth(true, "$stringfield LIKE '%value'", document, document2);
        testBoth(true, "$stringfield LIKE 'some_value'", document, document2);
        testBoth(false, "$stringfield LIKE 'some\\_value'", document, document2);

        // long
        testBoth(true, "$longfield = 123", document, document2);
        testBoth(false, "$longfield = 124", document, document2);

        // double
        testBoth(true, "$doublefield < 124", document, document2);
        testBoth(true, "$doublefield > 122", document, document2);

        // date
        testBoth(true, "$datefield = '2004-12-06'", document, document2);
        testBoth(true, "$datefield >= '2004-12-06'", document, document2);
        testBoth(true, "$datefield <= '2004-12-06'", document, document2);
        testBoth(true, "$datefield > '2004-12-05'", document, document2);
        testBoth(true, "$datefield != '2004-12-05'", document, document2);

        // datetime
        testBoth(true, "$datetimefield = '2004-12-06 12:15:06'", document, document2);
        testBoth(true, "$datetimefield >= '2004-12-06 12:15:06'", document, document2);
        testBoth(true, "$datetimefield <= '2004-12-06 12:15:06'", document, document2);
        testBoth(true, "$datetimefield > '2004-12-05 12:15:06'", document, document2);
        testBoth(true, "$datetimefield != '2004-12-05 12:15:06'", document, document2);

        // decimal
        testBoth(true, "$decimalfield IN (3232.3232, 123.45678, 890.542)", document, document2);
        testBoth(false, "$decimalfield IN (3232.3232, 890.542)", document, document2);
        testBoth(true, "$decimalfield NOT IN (3232.3232, 890.542)", document, document2);
        testBoth(true, "$decimalfield BETWEEN 89.232 AND 123.456789", document, document2);

        // boolean
        testBoth(true, "$booleanfield = 'true'", document, document2);

        //
        // Test more complex expresions
        //
        expr = makeExpression("$decimalfield IN (3232.3232, 123.45678, 890.542) and $stringfield between 'a' and 'z'");
        assertEquals(true, expr.evaluate(document, null));

        expr = makeExpression("$decimalfield IN (5) or $stringfield between 'a' and 'z'");
        assertEquals(true, expr.evaluate(document, null));

        expr = makeExpression("($decimalfield IN (5) or $stringfield between 'a' and 'z') and $longfield > 100");
        assertEquals(true, expr.evaluate(document, null));

        //
        // Test non-metadata fields
        //
        testBoth(true, "documentType = '" + documentType.getName() + "'", document, document2);

        expr = makeExpression("InCollection('" + collection1.getName() + "')");
        assertEquals(true, expr.evaluate(document, null));

        expr = makeExpression("InCollection('" + collection2.getName() + "')");
        assertEquals(false, expr.evaluate(document, null));

        testBoth(true, "collections has all ('" + collection1.getName() + "')", document, document2);

        testBoth(true, "variants has all ('main:default')", document, document2);

        testBoth(true, "variants has exactly ('main:default')", document, document2);

        testBoth(true, "variants.valueCount = 1", document, document2);

        //
        // Test multivalue fields
        //
        testBoth(true, "$mv-stringfield.valueCount = 2", document, document2);

        expr = makeExpression("$mv-stringfield.valueCount > 1 and $mv-stringfield.valueCount < 3");
        assertTrue(expr.evaluate(document, null));

        testBoth(false, "$mv-stringfield.valueCount != 2", document, document2);

        testBoth(true, "$mv-stringfield has all ('value 1')", document, document2);

        testBoth(true, "$mv-stringfield has all ('value 1', 'value 2')", document, document2);

        testBoth(false, "$mv-stringfield has exactly ('value 1')", document, document2);

        testBoth(true, "$mv-stringfield has exactly ('value 1', 'value 2')", document, document2);

        testBoth(true, "$mv-stringfield has any ('value 1', 'xyz')", document, document2);

        testBoth(true, "$mv-stringfield has some ('value 1', 'xyz')", document, document2);

        testBoth(false, "$mv-stringfield has some ('abc', 'xyz')", document, document2);

        testBoth(true, "$mv-stringfield has some ('value 1', 'value 2')", document, document2);

        testBoth(true, "$mv-stringfield = 'value 1'", document, document2);

        testBoth(true, "$mv-stringfield like 'value%'", document, document2);

        // TODO test other functions like LinksTo

        // Test hierarchical fields
        testBoth(true, "$hierMvField has all ( Path('/X/Y/Z'), Path('/T')) ", document, document2);

        testBoth(false, "$hierMvField has all ( Path('/X/Y/Z'), Path('/T1')) ", document, document2);

        testBoth(true, "$hierMvField has any ( Path('/X/Y/Z'), Path('/T1')) ", document, document2);

        testBoth(true, "$hierMvField has exactly ( Path('/X/Y/Z'), Path('/T')) ", document, document2);

        testBoth(false, "$hierMvField has exactly ( Path('/X/Y/Z'), Path('/T'), Path('/X')) ", document, document2);

        testBoth(false, "$hierMvField has none ( Path('/X/Y/Z'), Path('/T'), Path('/X')) ", document, document2);

        testBoth(true, "$hierMvField has none ( Path('/X/Y/Za'), Path('/Tb'), Path('/X')) ", document, document2);

        testBoth(true, "$hierField matchesPath('/Aaa/Bee/Cee')", document, document2);

        testBoth(false, "$hierField matchesPath('/Aaa/Bee/Ceexxx')", document, document2);

        testBoth(true, "$hierField matchesPath('**/Cee')", document, document2);

        testBoth(true, "$hierField matchesPath('**/Bee/Cee')", document, document2);

        testBoth(true, "$hierField matchesPath('**/*/Cee')", document, document2);

        testBoth(true, "$hierField matchesPath('/Aaa/**')", document, document2);

        testBoth(true, "$hierField matchesPath('/Aaa/Bee/**')", document, document2);

        testBoth(true, "$hierField matchesPath('/Aaa/Bee/*')", document, document2);

        testBoth(true, "$hierField matchesPath('/Aaa/*/*')", document, document2);

        testBoth(true, "$hierField matchesPath('/*/*/*')", document, document2);

        testBoth(false, "$hierField matchesPath('/*/*/*/*')", document, document2);

        testBoth(true, "$hierMvField matchesPath('/X/Y/Z')", document, document2);

        testBoth(true, "$hierMvField matchesPath('/T')", document, document2);

        testBoth(false, "$hierMvField matchesPath('/P')", document, document2);

        testBoth(true, "$hierMvLinkField matchesPath('/daisy:12')", document, document2);

        // equals operator should evaluate to true whenever the value occurs somewhere in the hierarchical field
        testBoth(true, "$hierMvLinkField = 'daisy:12'", document, document2);

        testBoth(true, "$hierField = 'Bee'", document, document2);
    }

    private void testBoth(boolean expectedResult, String expression, Document document, Document linkedDoc) throws Exception {
        PredicateExpression expr = makeExpression(expression);

        if (expectedResult)
            assertTrue(expr.evaluate(document, null));
        else
            assertFalse(expr.evaluate(document, null));

        expr = makeExpression("$linkField=>" + expression);
        if (expectedResult)
            assertTrue(expr.evaluate(linkedDoc, null));
        else
            assertFalse(expr.evaluate(linkedDoc, null));
    }

    private PredicateExpression makeExpression(String expression) throws Exception {
        return repository.getQueryManager().parsePredicateExpression(expression);
    }
}
