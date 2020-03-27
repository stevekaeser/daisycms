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

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.outerj.daisy.repository.ChangeType;
import org.outerj.daisy.repository.CollectionManager;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.LockType;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.query.EvaluationContext;
import org.outerj.daisy.repository.query.QueryHelper;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.testsupport.AbstractDaisyTestCase;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.variant.Language;
import org.outerx.daisy.x10.LinkValueType;
import org.outerx.daisy.x10.SearchResultDocument;

/**
 * Additional query tests: test of all identifiers, functions, other constructs.
 */
public abstract class AbstractQuery2Test extends AbstractDaisyTestCase {

    private DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, Locale.US);
    private DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
    
    private DateTimeFormatter isoDateTimeFormat = ISODateTimeFormat.dateTimeNoMillis();

    public void testQuery() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();
        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);

        UserManager userManager = repository.getUserManager();
        User user1 = userManager.createUser("user1");
        user1.setPassword("user1");
        user1.setFirstName("User1First");
        user1.setLastName("User1Last");
        user1.addToRole(userManager.getRole(1, false));
        user1.save();
        User user2 = userManager.createUser("user2");
        user2.setPassword("user2");
        user2.setFirstName("User2First");
        user2.setLastName("User2Last");
        user2.addToRole(userManager.getRole(1, false));
        user2.save();

        CollectionManager collectionManager = repository.getCollectionManager();
        DocumentCollection collection1 = collectionManager.createCollection("collection 1");
        collection1.save();
        DocumentCollection collection2 = collectionManager.createCollection("collection 2");
        collection2.save();
        DocumentCollection collection3 = collectionManager.createCollection("collection 3");
        collection3.save();
        DocumentCollection collection4 = collectionManager.createCollection("collection 4");
        collection4.save();

        RepositorySchema schema = repository.getRepositorySchema();
        PartType partType = schema.createPartType("HtmlPart", "");
        partType.setDaisyHtml(true);
        partType.save();

        FieldType fieldType1 = schema.createFieldType("StringField", ValueType.STRING);
        fieldType1.save();
        FieldType fieldType2 = schema.createFieldType("DecimalField1", ValueType.DECIMAL);
        fieldType2.save();
        FieldType fieldType2b = schema.createFieldType("DecimalField2", ValueType.DECIMAL);
        fieldType2b.save();
        FieldType fieldType3 = schema.createFieldType("MVField", ValueType.STRING, true);
        fieldType3.save();
        FieldType fieldType4 = schema.createFieldType("LinkField", ValueType.LINK);
        fieldType4.save();
        FieldType fieldType5 = schema.createFieldType("MVLinkField", ValueType.LINK, true);
        fieldType5.save();
        FieldType fieldType6 = schema.createFieldType("HStringField", ValueType.STRING, false, true);
        fieldType6.save();
        FieldType fieldType7 = schema.createFieldType("MVHLinkField", ValueType.LINK, true, true);
        fieldType7.save();

        DocumentType docType = schema.createDocumentType("DocType");
        docType.addPartType(partType, false);
        docType.addFieldType(fieldType1, false);
        docType.addFieldType(fieldType2, false);
        docType.addFieldType(fieldType2b, false);
        docType.addFieldType(fieldType3, false);
        docType.addFieldType(fieldType4, false);
        docType.addFieldType(fieldType5, false);
        docType.addFieldType(fieldType6, false);
        docType.addFieldType(fieldType7, false);
        docType.save();

        Language nlLanguage = repository.getVariantManager().createLanguage("nl");
        nlLanguage.save();

        Repository user1Repo = repositoryManager.getRepository(new Credentials("user1", "user1"));
        Repository user2Repo = repositoryManager.getRepository(new Credentials("user2", "user2"));

        Document document = user1Repo.createDocument("My Document", "DocType");
        document.setPart("HtmlPart", "text/xml", "<html><body><p>hello!</p></body></html>".getBytes("UTF-8"));
        document.setField("StringField", "abc");
        document.setField("DecimalField1", new BigDecimal("123.45"));
        document.setField("DecimalField2", new BigDecimal("300.89"));
        document.setField("MVField", new String[] {"aaa", "bbb"});
        document.setField("LinkField", new VariantKey("5", -1, -1));
        document.setField("MVLinkField", new VariantKey[] { new VariantKey("20", 1, 1), new VariantKey("21", 1, -1) });
        document.setField("HStringField", new HierarchyPath(new Object[] { "ABC", "DEF", "GHI" }));
        document.setField("MVHLinkField", new Object[] {
                new HierarchyPath(new Object[] { new VariantKey("50", 1, 1), new VariantKey("51-DSYTEST", 1, 1) }),
                new HierarchyPath(new Object[] { new VariantKey("52", 1, 1), new VariantKey("53-DSYTEST", 1, 1) }) });
        document.addToCollection(collection1);
        document.setNewChangeComment("my change comment 1");
        document.save();

        // sleeping for a bit so the versionCreationTimes are not identical
        Thread.sleep(1000);
        
        // create a draft last version
        document = user2Repo.getDocument(document.getId(), true);
        document.setName("My Document updated");
        document.setPart("HtmlPart", "text/xml", "<html><body><p>hello, world!</p></body></html>".getBytes("UTF-8"));
        document.setField("StringField", "uyt");
        document.setField("DecimalField1", new BigDecimal("100"));
        document.setField("DecimalField2", new BigDecimal("200"));
        document.setField("MVField", new String[] {"aaa", "bbb", "ccc"});
        document.setField("LinkField", new VariantKey("5", 1, 1));
        document.addToCollection(collection2);
        document.setNewVersionState(VersionState.DRAFT);
        document.setNewChangeComment("my change comment 2");
        document.save();

        // create test docs for synced with
        Document syncDoc = user1Repo.createDocument("Sync test doc", "DocType");
        syncDoc.save();
        syncDoc.setName("Sync test doc new name");
        syncDoc.setNewVersionState(VersionState.DRAFT);
        syncDoc.save();
        syncDoc.setName("Sync test doc new name 2");
        syncDoc.setNewChangeType(ChangeType.MINOR);
        syncDoc.save();

        Document syncVariant = user1Repo.createVariant(syncDoc.getId(), "main", "default", -1, "main", "nl", false);
        syncVariant.setNewSyncedWith("default", 1);
        syncVariant.save();

        document.lock(1000000, LockType.PESSIMISTIC);

        //
        //
        // Test all identifiers
        //   - for live and last version
        //   - in the 'select' part (evaluated by the repository server) and the where clause (evaluated by the SQL engine)
        //
        //
        QueryManager queryManager = repository.getQueryManager();
        SearchResultDocument result;

        // link function
        doLinkQueryTest(queryManager, "select link ('20-DSYTEST', 1, 1) where id=1", "daisy:20-DSYTEST@1:1", "daisy:20-DSYTEST@1:1");

        doStringQueryTest(queryManager, "select id where id=1 and $MVLinkField=link('20-DSYTEST',1,1)", document.getId(), document.getId());

        // id
        doStringQueryTest(queryManager, String.format("select id where id = '%s'", document.getId()), document.getId(), document.getId());

        doStringQueryTest(queryManager, String.format("select id where id = '%s'", document.getId()), document.getId(), document.getId());

        // if the document ID contains a namespace that does not exist in the repo, it should not fail but return 0 docs
        doStringQueryTest(queryManager, String.format("select id where id = '%d-%s'", document.getSeqId(), "foo_bar"), null, null);

        // namespace
        doStringQueryTest(queryManager, String.format("select namespace where namespace = 'DSYTEST' and id = '%s'", document.getId()), document.getNamespace(), document.getNamespace());

        // searching on a non-registered namespace should not fail
        doStringQueryTest(queryManager, String.format("select namespace where namespace = 'foo_bar'"), null, null);

        // documentType
        doStringQueryTest(queryManager, String.format("select documentType where documentType = 'DocType' and id = '%s'",document.getId()), "DocType", "DocType");

        // name
        doStringQueryTest(queryManager, String.format("select name where name = 'My Document' and id='%s'", document.getId()), "My Document", null);
        doStringQueryTest(queryManager, String.format("select name where name = 'My Document updated' and id='%s'", document.getId()), null, "My Document updated");

        // creationTime
        String creationTime = dateTimeFormat.format(document.getCreated());
        doStringQueryTest(queryManager, String.format("select creationTime where creationTime = %s and id = '%s'", QueryHelper.formatDateTime(document.getCreated()), document.getId()), creationTime, creationTime);

        // summary (version dependent since daisy 2.4)
        doStringQueryTest(queryManager, String.format("select summary where id = '%s'", document.getId()), "hello!", "hello, world!");

        // versionCreationTime
        String liveCreationTime = dateTimeFormat.format(document.getLiveVersion().getCreated());
        String lastCreationTime = dateTimeFormat.format(document.getLastVersion().getCreated());
        doStringQueryTest(queryManager, String.format("select versionCreationTime where versionCreationTime = %s and id = '%s'", QueryHelper.formatDateTime(document.getLiveVersion().getCreated()), document.getId()), liveCreationTime, null);
        doStringQueryTest(queryManager, String.format("select versionCreationTime where versionCreationTime = %s and id = '%s'", QueryHelper.formatDateTime(document.getLastVersion().getCreated()), document.getId()), null, lastCreationTime);
        
        // versionCreatorId
        doStringQueryTest(queryManager, String.format("select versionCreatorId where versionCreatorId = %d and id = '%s'", user1.getId(), document.getId()), Long.toString(user1.getId()), null);
        doStringQueryTest(queryManager, String.format("select versionCreatorId where versionCreatorId = %d and id = '%s'", user2.getId(), document.getId()), null, Long.toString(user2.getId()));

        // versionCreatorName
        doStringQueryTest(queryManager, String.format("select versionCreatorName where id = '%s'", document.getId()), "User1First User1Last", "User2First User2Last");

        // versionCreatorLogin
        doStringQueryTest(queryManager, String.format("select versionCreatorLogin where versionCreatorLogin = 'user1' and id = '%s'", document.getId()), "user1", null);
        doStringQueryTest(queryManager, String.format("select versionCreatorLogin where versionCreatorLogin = 'user2' and id = '%s'", document.getId()), null, "user2");

        // versionState
        doStringQueryTest(queryManager, String.format("select versionState where versionState = 'publish' and id = '%s'", document.getId()), "publish", null);
        doStringQueryTest(queryManager, String.format("select versionState where versionState = 'draft' and id = '%s'", document.getId()), null, "draft");

        // totalSizeOfParts
        doStringQueryTest(queryManager, String.format("select totalSizeOfParts where totalSizeOfParts < 1000 and id = '%s'", document.getId()), "39", "46");

        // versionLastModified
        doStringQueryTest(queryManager, 
                String.format("select versionLastModified where versionLastModified <= %s and id = '%s'", QueryHelper.formatDateTime(new Date()), document.getId()),
                dateTimeFormat.format(document.getLiveVersion().getLastModified()), 
                dateTimeFormat.format(document.getLastVersion().getLastModified()));

        // retired
        doStringQueryTest(queryManager, String.format("select retired where retired = 'false' and id = '%s'", document.getId()), "false", "false");

        // private
        doStringQueryTest(queryManager, String.format("select private where private = 'false' and id = '%s'", document.getId()), "false", "false");

        // lockType
        doStringQueryTest(queryManager, String.format("select lockType where lockType = 'pessimistic' and id = '%s'", document.getId()), "pessimistic", "pessimistic");

        // lockOwnerId
        doStringQueryTest(queryManager, String.format("select lockOwnerId where lockOwnerId = " + user2.getId() + " and id = '%s'", document.getId()), String.valueOf(user2.getId()), String.valueOf(user2.getId()));

        // lockOwnerLogin
        doStringQueryTest(queryManager, String.format("select lockOwnerLogin where lockOwnerLogin = 'user2' and id = '%s'", document.getId()), "user2", "user2");

        // lockOwnerName
        doStringQueryTest(queryManager, String.format("select lockOwnerName where lockOwnerLogin = 'user2' and id = '%s'", document.getId()), "User2First User2Last", "User2First User2Last");

        // lockDuration
        doStringQueryTest(queryManager, String.format("select lockDuration where lockDuration = 1000000 and id = '%s'", document.getId()), "1000000", "1000000");

        // lockTimeAcquired
        String lockAcquired = dateTimeFormat.format(document.getLockInfo(true).getTimeAcquired());
        doStringQueryTest(queryManager, String.format("select lockTimeAcquired where lockTimeAcquired <= " + QueryHelper.formatDateTime(new Date()) + " and id = '%s'", document.getId()), lockAcquired, lockAcquired);

        // ownerId
        doStringQueryTest(queryManager, String.format("select ownerId where ownerId = " + user1.getId() + " and id = '%s'", document.getId()), String.valueOf(user1.getId()), String.valueOf(user1.getId()));

        // ownerLogin
        doStringQueryTest(queryManager, String.format("select ownerLogin where ownerLogin = 'user1' and id = '%s'", document.getId()), "user1", "user1");

        // ownerName
        doStringQueryTest(queryManager, String.format("select ownerName where id = '%s'", document.getId()), "User1First User1Last", "User1First User1Last");

        // lastModifierId
        doStringQueryTest(queryManager, String.format("select lastModifierId where lastModifierId = " + user1.getId() + " and id = '%s'", document.getId()), String.valueOf(user1.getId()), String.valueOf(user1.getId()));

        // lastModifierLogin
        doStringQueryTest(queryManager, String.format("select lastModifierLogin where lastModifierLogin = 'user1' and id = '%s'", document.getId()), "user1", "user1");

        // lastModifierName
        doStringQueryTest(queryManager, String.format("select lastModifierName where id = '%s'", document.getId()), "User1First User1Last", "User1First User1Last");

        // branchId
        doStringQueryTest(queryManager, String.format("select branchId where branchId = 1 and id = '%s'", document.getId()), "1", "1");

        // branchName
        doStringQueryTest(queryManager, String.format("select branch where branch = 'main' and id = '%s'", document.getId()), "main", "main"); 
        // languageId
        doStringQueryTest(queryManager, String.format("select languageId where languageId = 1 and id = '%s'", document.getId()), "1", "1");

        // languageName
        doStringQueryTest(queryManager, String.format("select language where language = 'default' and id = '%s'", document.getId()), "default", "default");

        // variantLastModified
        doStringQueryTest(queryManager, String.format("select variantLastModified where variantLastModified = " + QueryHelper.formatDateTime(document.getVariantLastModified()) + " and id = '%s'", document.getId()), dateTimeFormat.format(document.getVariantLastModified()), dateTimeFormat.format(document.getVariantLastModified()));

        // variantLastMofifierId
        doStringQueryTest(queryManager, String.format("select variantLastModifierId where variantLastModifierId = " + user2.getId() + " and id = '%s'", document.getId()), String.valueOf(user2.getId()), String.valueOf(user2.getId()));

        // variantLastModifierLogin
        doStringQueryTest(queryManager, String.format("select variantLastModifierLogin where variantLastModifierLogin = 'user2' and id = '%s'", document.getId()), "user2", "user2");

        // variantLastModifierName
        doStringQueryTest(queryManager, String.format("select variantLastModifierName where id = '%s'", document.getId()), "User2First User2Last", "User2First User2Last");

        // version comment
        doStringQueryTest(queryManager, String.format("select versionComment where versionComment = 'my change comment 1' and id = '%s'", document.getId()), "my change comment 1", null);
        doStringQueryTest(queryManager, String.format("select versionComment where versionComment = 'my change comment 2' and id = '%s'", document.getId()), null, "my change comment 2");

        // version change type
        doStringQueryTest(queryManager, String.format("select versionChangeType where versionChangeType = 'major' and id = '%s'", document.getId()), "major", "major");

        // live major change version
        doStringQueryTest(queryManager, String.format("select liveMajorChangeVersionId where liveMajorChangeVersionId = " + document.getLiveVersionId() + " and id = '%s'", document.getId()), String.valueOf(document.getLiveVersionId()), String.valueOf(document.getLiveVersionId()));

        // last major change version
        doStringQueryTest(queryManager, String.format("select lastMajorChangeVersionId where lastMajorChangeVersionId = " + document.getLastVersionId() + " and id = '%s'", document.getId()), String.valueOf(document.getLastVersionId()), String.valueOf(document.getLastVersionId()));

        // syncedWith
        SearchResultAsserter syncedWith = new SearchResultAsserter() {
          public void doAssert(SearchResultDocument result) {
            assertEquals(result.getSearchResult().getRows().getRowArray(0).getLinkValueArray(0).getLanguageId(), 1);
            assertEquals(result.getSearchResult().getRows().getRowArray(0).getValueArray(0), "1");
            assertEquals(result.getSearchResult().getRows().getRowArray(0).getValueArray(1), "1");
            assertEquals(result.getSearchResult().getRows().getRowArray(0).getValueArray(2), "default");
            assertEquals(result.getSearchResult().getRows().getRowArray(0).getValueArray(3), "2");
            assertEquals(result.getSearchResult().getRows().getRowArray(0).getValueArray(4), "1");
          }
        };
        doCustomQueryTest(queryManager, String.format("select syncedWith, syncedWith.versionId, syncedWith.languageId, syncedWith.language, syncedWith=>lastMajorChangeVersionId, syncedWith=>liveMajorChangeVersionId where syncedWith is not null and id = '" + syncVariant.getId() + "'"), syncedWith, syncedWith);

        // syncedWith.versionId
        doStringQueryTest(queryManager, String.format("select syncedWith.versionId where syncedWith.versionId = 1 and id = '" + syncVariant.getId() + "'"), "1", "1");

        // syncedWith.languageId
        doStringQueryTest(queryManager, String.format("select syncedWith.languageId where syncedWith.languageId = 1 and id = '" + syncVariant.getId() + "'"), "1", "1");

        // syncedWith.language
        doStringQueryTest(queryManager, String.format("select syncedWith.language where syncedWith.language = 'default' and id = '" + syncVariant.getId() + "'"), "default", "default");

        // syncedWith dereferencing
        doStringQueryTest(queryManager, String.format("select syncedWith=>lastMajorChangeVersionId where syncedWith=>lastMajorChangeVersionId = 2 and id = '" + syncVariant.getId() + "'"), "2", "2");

        // collections
        SearchResultAsserter collectionAssertFound = new SearchResultAsserter() {
          public void doAssert(SearchResultDocument result) {
            List<String> foundCollections = new ArrayList<String>();
            for (String collection : result.getSearchResult().getRows().getRowArray(0).getMultiValueArray(0).getValueArray()) {
                foundCollections.add(collection);
            }
            assertTrue(foundCollections.contains("collection 1"));
            assertTrue(foundCollections.contains("collection 2"));
          }
        };
        doCustomQueryTest(queryManager, String.format("select collections where collections has all ('collection 1', 'collection 2') and id = '%s'", document.getId()), collectionAssertFound, collectionAssertFound);

        doCustomQueryTest(queryManager, String.format("select collections where collections has all ('collection 2', 'collection 3') and id = '%s'", document.getId()), null, null);

        doCustomQueryTest(queryManager, String.format("select collections where collections has exactly ('collection 1', 'collection 2') and id = '%s'", document.getId()), collectionAssertFound, collectionAssertFound);

        doCustomQueryTest(queryManager, String.format("select collections where collections has exactly ('collection 2') and id = '%s'", document.getId()), null, null);

        doCustomQueryTest(queryManager, String.format("select collections where collections has any ('collection 2', 'collection 3') and id = '%s'", document.getId()), collectionAssertFound, collectionAssertFound);
        doCustomQueryTest(queryManager, String.format("select collections where collections has some ('collection 2', 'collection 3') and id = '%s'", document.getId()), collectionAssertFound, collectionAssertFound);

        doCustomQueryTest(queryManager, String.format("select collections where collections has any ('collection 3', 'collection 4') and id = '%s'", document.getId()), null, null);
        doCustomQueryTest(queryManager, String.format("select collections where collections has some ('collection 3', 'collection 4') and id = '%s'", document.getId()), null, null);

        doCustomQueryTest(queryManager, String.format("select collections where collections has none ('collection 3', 'collection 4') and id = '%s'", document.getId()), collectionAssertFound, collectionAssertFound);

        doCustomQueryTest(queryManager, String.format("select collections where collections has none ('collection 2', 'collection 3') and id = '%s'", document.getId()), null, null);

        // collections.valueCount
        doStringQueryTest(queryManager, String.format("select collections.valueCount where collections.valueCount = 2 and id = '%s'", document.getId()), "2", "2");


        // versionId
        doStringQueryTest(queryManager, String.format("select versionId where versionId = 1 and id = '%s'", document.getId()), "1", null);
        doStringQueryTest(queryManager, String.format("select versionId where versionId = 2 and id = '%s'", document.getId()), null, "2");

        // link field identifiers
        // FIXME: test each identifier individually (or use a searchresultasserter)
        doStringQueryTest(queryManager, String.format("select $LinkField.documentId, $LinkField.namespace, $LinkField.branch, $LinkField.branchId, $LinkField.language, $LinkField.languageId where $LinkField.documentId = '5-DSYTEST' and $LinkField.branchId = 1 and $LinkField.branch = 'main' and $LinkField.languageId = 1 and $LinkField.language = 'default' and $LinkField.namespace = 'DSYTEST'"), "5-DSYTEST", "5-DSYTEST");

        SearchResultAsserter linkAsserter2 = new SearchResultAsserter() {
            public void doAssert(SearchResultDocument result) throws Exception {
                assertEquals(result.getSearchResult().getRows().getRowArray().length, 1);
                assertEquals("20-DSYTEST", result.getSearchResult().getRows().getRowArray(0).getMultiValueArray(0).getValueArray(0));
                assertEquals("21-DSYTEST", result.getSearchResult().getRows().getRowArray(0).getMultiValueArray(0).getValueArray(1));
            }
        };
        doCustomQueryTest(queryManager, String.format("select $MVLinkField.documentId, $MVLinkField.namespace, $MVLinkField.branch, $MVLinkField.branchId, $MVLinkField.language, $MVLinkField.languageId where $MVLinkField.documentId = 20 and $MVLinkField.branchId = 1 and $MVLinkField.branch = 'main' and $MVLinkField.languageId = 1 and $MVLinkField.language = 'default' and $MVLinkField.namespace = 'DSYTEST'"), linkAsserter2, linkAsserter2);

        SearchResultAsserter linkAsserter3 = new SearchResultAsserter() {
            public void doAssert(SearchResultDocument result) throws Exception {
                assertEquals(result.getSearchResult().getRows().getRowArray().length, 1);
                LinkValueType linkResult = result.getSearchResult().getRows().getRowArray(0).getLinkValueArray(0);
                assertEquals("5-DSYTEST", linkResult.getDocumentId());
                assertEquals(1, linkResult.getBranchId());
                assertEquals(1, linkResult.getLanguageId());
            }
        };
        doCustomQueryTest(queryManager, String.format("select $LinkField, $MVLinkField where $LinkField = 'daisy:5@1:1' and $LinkField = 'daisy:5@main:default' and $LinkField = 'daisy:5' and $MVLinkField has all ('daisy:20', 'daisy:21')"), linkAsserter3, linkAsserter3);

        // FIXME: see DSY-778
        //doLinkQueryTest(queryManager, String.format("select link where link = 'daisy:1@1:1'"), "My Document", "My Document updated");

        // hierarchical string identifier
        SearchResultAsserter hierarchyAsserter = new SearchResultAsserter() {
            public void doAssert(SearchResultDocument result) throws Exception {
                assertEquals(1, result.getSearchResult().getRows().getRowArray(0).getHierarchyValueArray().length);
                assertEquals("ABC", result.getSearchResult().getRows().getRowArray(0).getHierarchyValueArray(0).getValueArray(0));
                assertEquals("DEF", result.getSearchResult().getRows().getRowArray(0).getHierarchyValueArray(0).getValueArray(1));
                assertEquals("GHI", result.getSearchResult().getRows().getRowArray(0).getHierarchyValueArray(0).getValueArray(2));
                assertEquals("52-DSYTEST", result.getSearchResult().getRows().getRowArray(0).getMultiValueArray(0).getHierarchyValueArray(1).getLinkValueArray(0).getDocumentId());
            }
        };
        doCustomQueryTest(queryManager, String.format("select $HStringField, $MVHLinkField where id='" + document.getId() + "'"), hierarchyAsserter, hierarchyAsserter);

        //
        // Test functions
        //
        // basic math functions
        doStringQueryTest(queryManager, "select $DecimalField1 + $DecimalField2 where $DecimalField1 + $DecimalField2 = 424.34 and id = '" + document.getId() + "'", "424.34", null);
        doStringQueryTest(queryManager, "select $DecimalField1 + $DecimalField2 where $DecimalField1 + $DecimalField2 = 300 and id = '" + document.getId() + "'", null, "300");

        doStringQueryTest(queryManager, "select $DecimalField1 - $DecimalField2 where $DecimalField1 - $DecimalField2 = -177.44 and id = '" + document.getId() + "'", "-177.44", null);
        doStringQueryTest(queryManager, "select $DecimalField1 - $DecimalField2 where $DecimalField1 - $DecimalField2 = -100 and id = '" + document.getId() + "'", null, "-100");

        doStringQueryTest(queryManager, "select $DecimalField1 / $DecimalField2 where $DecimalField1 / $DecimalField2 > 0.410 and $DecimalField1 / $DecimalField2 < 0.411 and id = '" + document.getId() + "'", "0.41", null);
        doStringQueryTest(queryManager, "select $DecimalField1 / $DecimalField2 where $DecimalField1 / $DecimalField2 > 0.49 and $DecimalField1 / $DecimalField2 < 0.51 and id = '" + document.getId() + "'", null, "0.5");

        doStringQueryTest(queryManager, "select $DecimalField1 * $DecimalField2 where $DecimalField1 * $DecimalField2 > 37144.85 and $DecimalField1 * $DecimalField2 < 37144.95 and id = '" + document.getId() + "'", "37,144.87", null);
        doStringQueryTest(queryManager, "select $DecimalField1 * $DecimalField2 where $DecimalField1 * $DecimalField2 = 20000 and id = '" + document.getId() + "'", null, "20,000");

        // precedence of operations and grouping
        doStringQueryTest(queryManager, "select 2 * 3 + 5 where 2 * 3 + 5 = 11 and id = '" + document.getId() + "'", "11", "11");

        doStringQueryTest(queryManager, "select 5 + 3 * 2 where 5 + 3 * 2 = 11 and id = '" + document.getId() + "'", "11", "11");

        doStringQueryTest(queryManager, "select 2 * (3 + 5) where 2 * (3 + 5) = 16 and id = '" + document.getId() + "'", "16", "16");

        doStringQueryTest(queryManager, "select (5 + 3) * 2 where (5 + 3) * 2 = 16 and id = '" + document.getId() + "'", "16", "16");

        doStringQueryTest(queryManager, "select 5 + 4 + 3 + 2 where 5 + 4 + 3 + 2 = 14 and id = '" + document.getId() + "'", "14", "14");

        // Random function
        result = queryManager.performQuery("select Random(), Random() where Random() != Random() and id = '" + document.getId() + "'", Locale.US);
        assertFalse(result.getSearchResult().getRows().getRowArray(0).getValueArray(0).equals(result.getSearchResult().getRows().getRowArray(0).getValueArray(1)));

        // Mod function
        doStringQueryTest(queryManager, "select Mod(10, 4) where Mod(10, 4) = 2 and id = '" + document.getId() + "'", "2", "2");

        // Abs function
        doStringQueryTest(queryManager, "select Abs(-5) where Abs(-5) = 5 and id = '" + document.getId() + "'", "5", "5");

        // Floor function
        doStringQueryTest(queryManager, "select Floor(5.22) where Floor(5.22) = 5 and id = '" + document.getId() + "'", "5", "5");

        // Ceiling function
        doStringQueryTest(queryManager, "select Ceiling(5.22) where Ceiling(5.22) = 6 and id = '" + document.getId() + "'", "6", "6");

        // Round function
        result = queryManager.performQuery("select Round(5.22, 1), Round(5.28, 1), Round(5.25, 1) where Round(5.22, 1) = 5.2 and Round(5.28, 1) = 5.3 and Round(5.25, 1) = 5.2 and id = '" + document.getId() + "'", Locale.US);
        assertEquals(result.getSearchResult().getRows().getRowArray(0).getValueArray(0), "5.2");
        assertEquals(result.getSearchResult().getRows().getRowArray(0).getValueArray(1), "5.3");
        assertEquals(result.getSearchResult().getRows().getRowArray(0).getValueArray(2), "5.2");

        // Length function
        doStringQueryTest(queryManager, "select Length(name) where Length(name) = 11 and id = '" + document.getId() + "'", "11", null);
        doStringQueryTest(queryManager, "select Length(name) where Length(name) = 19 and id = '" + document.getId() + "'", null, "19");

        // Concat function
        doStringQueryTest(queryManager, "select Length(Concat(name, 'abc')) where Length(Concat(name, 'abc')) = 14 and id = '" + document.getId() + "'", "14", null);
        doStringQueryTest(queryManager, "select Concat(name, 'abc') where Length(Concat(name, 'abc')) = 14 and id = '" + document.getId() + "'", "My Documentabc", null);
        doStringQueryTest(queryManager, "select Length(Concat(name, 'abc')) where Length(Concat(name, 'abc')) = 22 and id = '" + document.getId() + "'", null, "22");
        doStringQueryTest(queryManager, "select Concat(name, 'abc') where Length(Concat(name, 'abc')) = 22 and id = '" + document.getId() + "'", null, "My Document updatedabc");

        // Left function
        doStringQueryTest(queryManager, "select Left(name, 2) where Left(name, 2) = 'My' and id = '" + document.getId() + "'", "My", "My");

        doStringQueryTest(queryManager, "select Left(name, 200) where id = '" + document.getId() + "'", "My Document", "My Document updated");

        doStringQueryTest(queryManager, "select Left(name, 0) where id = '" + document.getId() + "'", "");

        // Right function
        doStringQueryTest(queryManager, "select Right(name, 2) where Right(name, 2) = 'nt' and id = '" + document.getId() + "'", "nt", null);
        doStringQueryTest(queryManager, "select Right(name, 2) where Right(name, 2) = 'ed' and id = '" + document.getId() + "'", null, "ed");

        doStringQueryTest(queryManager, "select Right(name, 200) where id = '" + document.getId() + "'", "My Document");

        doStringQueryTest(queryManager, "select name where Left(Right(name, 2), 1) = 'n' and id = '" + document.getId() + "'", "My Document", null);
        
        // Substring function
        doStringQueryTest(queryManager, "select Substring(name, 4, 4) where id = '" + document.getId() + "'", "Docu", "Docu");

        doStringQueryTest(queryManager, "select Substring(name, 1, 5000) where id = '" + document.getId() + "'", "My Document", "My Document updated");

        doStringQueryTest(queryManager, "select name where Substring(name, 4, 4) = 'Docu' and id = '" + document.getId() + "'", "My Document", "My Document updated");

        // UpperCase function
        doStringQueryTest(queryManager, "select UpperCase(name) where id = '" + document.getId() + "'", "MY DOCUMENT", "MY DOCUMENT UPDATED");

        // note: SQL doesn't search case sensitive so this just tests the SQL generation works ok
        doStringQueryTest(queryManager, "select name where UpperCase(name) = 'MY DOCUMENT' and id = '" + document.getId() + "'", "My Document", null);
        doStringQueryTest(queryManager, "select name where UpperCase(name) = 'MY DOCUMENT UPDATED' and id = '" + document.getId() + "'", null, "My Document updated");

        // LowerCase function
        doStringQueryTest(queryManager, "select LowerCase(name) where id = '" + document.getId() + "'", "my document", "my document updated");

        // note: SQL doesn't search case sensitive so this just tests the SQL generation works ok
        doStringQueryTest(queryManager, "select name where LowerCase(name) = 'MY DOCUMENT' and id = '" + document.getId() + "'", "My Document", null);
        doStringQueryTest(queryManager, "select name where LowerCase(name) = 'MY DOCUMENT UPDATED' and id = '" + document.getId() + "'", null, "My Document updated");

        // CurrentDate function
        Date nowDate = getDate();
        doStringQueryTest(queryManager, "select CurrentDate() where CurrentDate() = CurrentDate() and id = '" + document.getId() + "'", dateFormat.format(nowDate));

        // CurrentDateTime function
        Date nowDateTime = getDateTime();
        result = queryManager.performQuery("select CurrentDateTime() where lastModified < CurrentDateTime() and id = '" + document.getId() + "'", Locale.US);
        System.out.println(dateTimeFormat.parse(result.getSearchResult().getRows().getRowArray(0).getValueArray(0)) + " and " + nowDateTime);
        assertTrue(dateTimeFormat.parse(result.getSearchResult().getRows().getRowArray(0).getValueArray(0)).compareTo(nowDateTime) >= 0);

        // ContextDoc function
        final Document contextDoc = document;
        final Version contextVersion = document.getLiveVersion();
        EvaluationContext evaluationContext = new EvaluationContext();
        evaluationContext.setContextDocument(contextDoc, contextVersion);
        result = queryManager.performQuery("select ContextDoc(branch), ContextDoc(language) where ContextDoc(branch) = branch and ContextDoc(language) = language and id = '" + document.getId() + "'", Locale.US, evaluationContext);
        assertEquals(result.getSearchResult().getRows().getRowArray(0).getValueArray(0), "main");
        assertEquals(result.getSearchResult().getRows().getRowArray(0).getValueArray(1), "default");

        evaluationContext = new EvaluationContext();
        evaluationContext.pushContextDocument(contextDoc, contextVersion);
        evaluationContext.pushContextDocument(contextDoc, document.getLastVersion());
        result = queryManager.performQuery("select ContextDoc(name, 1), ContextDoc(name, 2) where id = '" + document.getId() + "'", Locale.US, evaluationContext);
        assertEquals(result.getSearchResult().getRows().getRowArray(0).getValueArray(0), "My Document updated");
        assertEquals(result.getSearchResult().getRows().getRowArray(0).getValueArray(1), "My Document");

        result = queryManager.performQuery("select id where $MVField has some ( ContextDoc($MVField) ) and id = '" + document.getId() + "'", Locale.US, evaluationContext);
        assertEquals(result.getSearchResult().getRows().getRowArray(0).getValueArray(0), document.getId());

        result = queryManager.performQuery("select id where $MVField has any ( ContextDoc($MVField) ) and id = '" + document.getId() + "'", Locale.US, evaluationContext);
        assertEquals(result.getSearchResult().getRows().getRowArray(0).getValueArray(0), document.getId());

        // Year function
        String currentYear = String.valueOf(new GregorianCalendar(Locale.US).get(Calendar.YEAR));
        doStringQueryTest(queryManager, "select Year(CurrentDate()) where Year(CurrentDate()) = " + currentYear + " and id = '" + document.getId() + "'", currentYear, currentYear);

        // Month function
        String currentMonth = String.valueOf(new GregorianCalendar(Locale.US).get(Calendar.MONTH) + 1);
        doStringQueryTest(queryManager, "select Month(CurrentDate()) where Month(CurrentDate()) = " + currentMonth + " and id = '" + document.getId() + "'", currentMonth, currentMonth);

        // Week function
        Calendar cal = new GregorianCalendar(Locale.US);
        cal.setMinimalDaysInFirstWeek(4);
        String currentWeek = String.valueOf(cal.get(Calendar.WEEK_OF_YEAR));
        doStringQueryTest(queryManager, "select Week(CurrentDate()) where Week(CurrentDate()) = " + currentWeek + " and id = '" + document.getId() + "'", currentWeek, currentWeek);

        // DayOfWeek function
        String currentDayOfWeek = String.valueOf(new GregorianCalendar(Locale.US).get(Calendar.DAY_OF_WEEK));
        doStringQueryTest(queryManager, "select DayOfWeek(CurrentDate()) where DayOfWeek(CurrentDate()) = " + currentDayOfWeek + " and id = '" + document.getId() + "'", currentDayOfWeek, currentDayOfWeek);

        // DayOfMonth function
        String currentDayOfMonth = String.valueOf(new GregorianCalendar(Locale.US).get(Calendar.DAY_OF_MONTH));
        doStringQueryTest(queryManager, "select DayOfMonth(CurrentDate()) where DayOfMonth(CurrentDate()) = " + currentDayOfMonth + " and id = '" + document.getId() + "'", currentDayOfMonth, currentDayOfMonth);

        // DayOfYear function
        String currentDayOfYear = String.valueOf(new GregorianCalendar(Locale.US).get(Calendar.DAY_OF_YEAR));
        doStringQueryTest(queryManager, "select DayOfYear(CurrentDate()) where DayOfYear(CurrentDate()) = " + currentDayOfYear + " and id = '" + document.getId() + "'", currentDayOfYear, currentDayOfYear);

        // CurrentDate function with argument
        Calendar tenYearsAgo = new GregorianCalendar();
        tenYearsAgo.set(Calendar.HOUR_OF_DAY, 0);
        tenYearsAgo.set(Calendar.MINUTE, 0);
        tenYearsAgo.set(Calendar.SECOND, 0);
        tenYearsAgo.set(Calendar.MILLISECOND, 0);
        tenYearsAgo.add(Calendar.YEAR, -10);
        String tenYearsAgoStr = dateFormat.format(tenYearsAgo.getTime());
        doStringQueryTest(queryManager, "select CurrentDate('- 10 years') where CurrentDate('- 10 years') = " + QueryHelper.formatDate(tenYearsAgo.getTime()) + " and id = '" + document.getId() + "'", tenYearsAgoStr, tenYearsAgoStr);

        // CurrentDateTime function
        result = queryManager.performQuery("select CurrentDateTime('+1hours') where CurrentDateTime('+    1  hours') > " + QueryHelper.formatDateTime(new Date()) + " and id = '" + document.getId() + "'", Locale.US);
        assertTrue(dateTimeFormat.parse(result.getSearchResult().getRows().getRowArray(0).getValueArray(0)).compareTo(new Date()) > 0);

        result = queryManager.performQuery("select RelativeDate('start next week') where RelativeDate('start next week') > " + QueryHelper.formatDate(new Date()) + " and id = '" + document.getId() + "'", Locale.US);
        assertTrue(dateFormat.parse(result.getSearchResult().getRows().getRowArray(0).getValueArray(0)).compareTo(new Date()) > 0);

        result = queryManager.performQuery("select RelativeDateTime('start this week') where RelativeDateTime('start this week') < " + QueryHelper.formatDateTime(new Date()) + " and id = '" + document.getId() + "'", Locale.US);
        assertTrue(dateTimeFormat.parse(result.getSearchResult().getRows().getRowArray(0).getValueArray(0)).compareTo(new Date()) < 0);

        // Test Year function with datetime arguments
        Calendar lastModCal = new GregorianCalendar();
        lastModCal.setTime(document.getLastModified());
        String lastModYear = String.valueOf(lastModCal.get(Calendar.YEAR));
        doStringQueryTest(queryManager, "select Year(lastModified) where Year(lastModified) = " + lastModYear + " and id = '" + document.getId() + "'", lastModYear, lastModYear);
        doStringQueryTest(queryManager, "select Year(" + QueryHelper.formatDateTime(document.getLastModified()) + ") where Year(lastModified) = " + lastModYear + " and id = '" + document.getId() + "'", lastModYear, lastModYear);

        // UserId function
        doStringQueryTest(user1Repo.getQueryManager(), "select UserId() where ownerId = UserId() and id = '" + document.getId() + "'", String.valueOf(user1.getId()), String.valueOf(user1.getId()));
        doStringQueryTest(user2Repo.getQueryManager(), "select UserId() where ownerId = UserId() and id = '" + document.getId() + "'", null, null);

        // multivalue index
        result = queryManager.performQuery("select id where $MVField[1] = 'aaa' and $MVField[1 + 1] = 'bbb' and id = '" + document.getId() + "'", Locale.US, evaluationContext);
        assertEquals(document.getId(), result.getSearchResult().getRows().getRowArray(0).getValueArray(0));

        result = queryManager.performQuery("select id where $MVField[1] = 'aaa' and $MVField[-1] = 'bbb' and id = '" + document.getId() + "'", Locale.US, evaluationContext);
        assertEquals(document.getId(), result.getSearchResult().getRows().getRowArray(0).getValueArray(0));

        result = queryManager.performQuery("select id where $MVField[1] = 'aaa' and $MVField[-38] = 'bbb' and id = '" + document.getId() + "'", Locale.US, evaluationContext);
        assertEquals(0, result.getSearchResult().getRows().getRowArray().length);

        result = queryManager.performQuery("select id where $MVField[234342] = 'aaa' and $MVField[-1] = 'bbb' and id = '" + document.getId() + "'", Locale.US, evaluationContext);
        assertEquals(0, result.getSearchResult().getRows().getRowArray().length);

        result = queryManager.performQuery("select id where $MVHLinkField[*][1] = 'daisy:50' and id = '" + document.getId() + "'", Locale.US, evaluationContext);
        assertEquals(document.getId(), result.getSearchResult().getRows().getRowArray(0).getValueArray(0));

        result = queryManager.performQuery("select id where $MVHLinkField[*][1] = 'daisy:52' and id = '" + document.getId() + "'", Locale.US, evaluationContext);
        assertEquals(document.getId(), result.getSearchResult().getRows().getRowArray(0).getValueArray(0));

        result = queryManager.performQuery("select id, $MVHLinkField[*][1], $MVHLinkField[1][1], $MVHLinkField[*][2], $MVHLinkField[*][3], $MVHLinkField[2], $MVHLinkField[*][2].branch where id = '" + document.getId() + "'", Locale.US, evaluationContext);
        assertEquals("main", result.getSearchResult().getRows().getRowArray(0).getMultiValueArray(3).getValueArray(0));
        assertEquals("main", result.getSearchResult().getRows().getRowArray(0).getMultiValueArray(3).getValueArray(1));

        result = queryManager.performQuery("select id where $MVHLinkField[*][1] has exactly('daisy:50', 'daisy:52') and id = '" + document.getId() + "'", Locale.US, evaluationContext);
        assertEquals(document.getId(), result.getSearchResult().getRows().getRowArray(0).getValueArray(0));

        result = queryManager.performQuery("select id where $MVHLinkField[*][2] has exactly('daisy:50', 'daisy:52') and id = '" + document.getId() + "'", Locale.US, evaluationContext);
        assertEquals(0, result.getSearchResult().getRows().getRowArray().length);

        result = queryManager.performQuery("select id where $MVHLinkField[2] matchesPath('daisy:52/daisy:53') and id = '" + document.getId() + "'", Locale.US, evaluationContext);
        assertEquals(document.getId() ,result.getSearchResult().getRows().getRowArray(0).getValueArray(0));

        result = queryManager.performQuery("select id, $MVHLinkField[1] where $MVHLinkField[1] matchesPath('daisy:52/daisy:53') and id = '" + document.getId() + "'", Locale.US, evaluationContext);
        assertEquals(0, result.getSearchResult().getRows().getRowArray().length);
    }

    private void doStringQueryTest(QueryManager queryManager, String query,
            String value) throws RepositoryException {
        SearchResultDocument result;
        result = queryManager.performQuery(query, Locale.US);
        if (value == null) {
            assertEquals("query should not return any results: " + query, 0, result.getSearchResult().getRows().getRowArray().length);
        } else {
            assertEquals("query should return exactly one result: " + query, 1, result.getSearchResult().getRows().getRowArray().length);
            assertEquals("first value of query result should be correct: " + query, value, result.getSearchResult().getRows().getRowArray(0).getValueArray(0));
        }
    }

    private void doLinkQueryTest(QueryManager queryManager, String query,
            String value) throws RepositoryException {
        SearchResultDocument result;
        result = queryManager.performQuery(query, Locale.US);
        if (value == null) {
            assertEquals("query should not return any results: " + query, 0, result.getSearchResult().getRows().getRowArray().length);
        } else {
            assertEquals("query should return exactly one result: " + query, 1, result.getSearchResult().getRows().getRowArray().length);
            System.out.println(result.xmlText());
            assertEquals("first value of query result should be correct: " + query, value, result.getSearchResult().getRows().getRowArray(0).getLinkValueArray(0).getStringValue());
        }
    }

    private void doCustomQueryTest(QueryManager queryManager, String query,
            SearchResultAsserter asserter) throws Exception {
        SearchResultDocument result;
        result = queryManager.performQuery(query, Locale.US);
        if (asserter == null) {
            assertEquals("query should not return any results: " + query, 0, result.getSearchResult().getRows().getRowArray().length);
        } else {
            assertEquals("query should return exactly one result: " + query, 1, result.getSearchResult().getRows().getRowArray().length);
            asserter.doAssert(result);
        }
    }

    private void doStringQueryTest(QueryManager queryManager, String query,
            String liveValue, String lastValue) throws RepositoryException {
        doStringQueryTest(queryManager, query, liveValue);
        doStringQueryTest(queryManager, query + " option point_in_time = 'live'", liveValue);
        doStringQueryTest(queryManager, query + String.format(" option point_in_time = '%s'", isoDateTimeFormat.print(new Date().getTime())), liveValue);
        
        doStringQueryTest(queryManager, query + " option search_last_version = 'true'", lastValue);
        doStringQueryTest(queryManager, query + " option point_in_time = 'last'", lastValue);
        
    }

    private void doLinkQueryTest(QueryManager queryManager, String query, String liveValue,
            String lastValue) throws RepositoryException {
        doLinkQueryTest(queryManager, query, liveValue);
        doLinkQueryTest(queryManager, query + " option point_in_time = 'live'", liveValue);
        doLinkQueryTest(queryManager, query + String.format(" option point_in_time = '%s'", isoDateTimeFormat.print(new Date().getTime())), liveValue);
        
        doLinkQueryTest(queryManager, query + " option search_last_version = 'true'", lastValue);
        doLinkQueryTest(queryManager, query + " option point_in_time = 'last'", lastValue);
        
    }

    private void doCustomQueryTest(QueryManager queryManager, String query, SearchResultAsserter liveAsserter,
            SearchResultAsserter lastAsserter) throws Exception {
        doCustomQueryTest(queryManager, query, liveAsserter);
        doCustomQueryTest(queryManager, query + " option point_in_time = 'live'", liveAsserter);
        doCustomQueryTest(queryManager, query + String.format(" option point_in_time = '%s'", isoDateTimeFormat.print(new Date().getTime())), liveAsserter);
        
        doCustomQueryTest(queryManager, query, lastAsserter);
        doCustomQueryTest(queryManager, query + " option search_last_version = 'true'", lastAsserter);
        doCustomQueryTest(queryManager, query + " option point_in_time = 'last'", lastAsserter);
        
    }

    private Date getDate() {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date getDateTime() {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;

    protected boolean resetDataStores() {
        return true;
    }
    
    public interface SearchResultAsserter {
        public void doAssert(SearchResultDocument result) throws Exception;
    }
}
