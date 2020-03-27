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

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.query.QueryHelper;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.query.SortOrder;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.testsupport.AbstractDaisyTestCase;
import org.outerj.daisy.repository.user.Role;
import org.outerx.daisy.x10.DistinctSearchResultDocument;
import org.outerx.daisy.x10.SearchResultDocument;

public abstract class AbstractQueryTest extends AbstractDaisyTestCase {

    public void testQuerying() throws Exception {
        DateTimeFormatter isoDateTimeNoMillis = ISODateTimeFormat.dateTimeNoMillis();
        
        RepositoryManager repositoryManager = getRepositoryManager();

        TestContentCreator contentCreator = new TestContentCreator();
        contentCreator.run(repositoryManager);

        System.err.println("Sleeping a little while to give fulltextinder time to do its job.");
        Thread.sleep(10000);

        Repository testuserRepository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        testuserRepository.switchRole(Role.ADMINISTRATOR);
        QueryManager queryManager = testuserRepository.getQueryManager();

        // Admin user should have access to all documents
        String query = "select id where true order by id";
        VariantKey[] resultKeys = queryManager.performQueryReturnKeys(query, Locale.US);
        assertEquals(12, resultKeys.length);


        Repository user1Repository = repositoryManager.getRepository(new Credentials("user1", "user1"));
        queryManager = user1Repository.getQueryManager();

        //
        // Test fulltext search
        //
        SearchResultDocument searchResultDoc = queryManager.performQuery("select name where FullText('verzamelaars')", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);
        assertEquals("Document1", searchResultDoc.getSearchResult().getRows().getRowArray()[0].getValueArray()[0]);
        String document1Id = searchResultDoc.getSearchResult().getRows().getRowArray()[0].getDocumentId();

        //
        // Test ACL filtering
        //
        resultKeys = queryManager.performQueryReturnKeys("select name where name = 'Document3'", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select name where name = 'Document6'", Locale.US);
        assertEquals(0, resultKeys.length);

        //
        // Test LinksTo function / link extraction
        //
        resultKeys = queryManager.performQueryReturnKeys("select name where LinksTo(" + QueryHelper.formatString(document1Id) + ")", Locale.US);
        assertEquals(1, resultKeys.length);

        //
        // Test LinksFrom function / link extraction
        //
        resultKeys = queryManager.performQueryReturnKeys("select name where LinksFrom(" + QueryHelper.formatString(resultKeys[0].getDocumentId()) + ")", Locale.US);
        assertEquals(1, resultKeys.length);

        //
        // Test IsLinked function / link extraction
        //
        resultKeys = queryManager.performQueryReturnKeys("select name where name='Document1' and IsLinked()", Locale.US);
        assertEquals(1, resultKeys.length);
        resultKeys = queryManager.performQueryReturnKeys("select name where name='Document3' and IsLinked()", Locale.US);
        assertEquals(0, resultKeys.length);

        //
        // Test IsNotLinked function / link extraction
        //
        resultKeys = queryManager.performQueryReturnKeys("select name where name='Document1' and IsNotLinked()", Locale.US);
        assertEquals(0, resultKeys.length);
        resultKeys = queryManager.performQueryReturnKeys("select name where name='Document3' and IsNotLinked()", Locale.US);
        assertEquals(1, resultKeys.length);

        //
        //
        //
        Repository user2Repository = repositoryManager.getRepository(new Credentials("user2", "user2"));
        queryManager = user2Repository.getQueryManager();

        resultKeys = queryManager.performQueryReturnKeys("select name where InCollection('collection2')", Locale.US);
        assertEquals(1, resultKeys.length);

        //
        // Some tests on IS NULL
        //                                                                                   
        resultKeys = queryManager.performQueryReturnKeys("select id where $field1 like 'something-%' and ($field2 != 2323 or $field2 is null)", Locale.US);
        assertEquals(1, resultKeys.length);

        queryManager = user1Repository.getQueryManager();
        resultKeys = queryManager.performQueryReturnKeys("select id where $field2 is null and InCollection('collection1')", Locale.US);
        assertEquals(4, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where InCollection('collection2') and $field2 is null", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $field2 is null and InCollection('collection2') and $field2 is null", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $field2 is null and InCollection('collection2') and $field2 is null", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $field1 is not null", Locale.US);
        assertEquals(2, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where #xyz is null", Locale.US);
        assertEquals(10, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where #xyz is not null", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where #xyz = 123", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where #xyz is null or #xyz != 123", Locale.US);
        assertEquals(10, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where #xyz != 123 or #xyz is null", Locale.US);
        assertEquals(10, resultKeys.length);

        //
        // Some tests on InCollection
        //
        resultKeys = queryManager.performQueryReturnKeys("select id where InCollection('collection2') or InCollection('collection1')", Locale.US);
        assertEquals(5, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where InCollection('collection2','collection1')", Locale.US);
        assertEquals(5, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where InCollection('collection3') and InCollection('collection4')", Locale.US);
        assertEquals(1, resultKeys.length);

        //
        // Test various conditions on various kinds of fields
        //
        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'hello' and $DateField1 = '2004-12-06' and $DateTimeField1 = '2004-10-14 12:13:14' and $DecimalField1 = 678.94321 and $DoubleField1 = 123.456", Locale.US);
        assertEquals(1, resultKeys.length);

        // dates
        resultKeys = queryManager.performQueryReturnKeys("select id where $DateField1 >= '2004-12-06' and $DateField1 <= '2004-12-06'", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $DateField1 >= '2004-12-06' and $DateField1 < '2004-12-11'", Locale.US);
        assertEquals(2, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $DateField1 != '2004-12-06' and InCollection('collection5')", Locale.US);
        assertEquals(1, resultKeys.length);

        // strings
        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 like '%hello' and InCollection('collection5')", Locale.US);
        assertEquals(2, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 not like '%hello' and InCollection('collection5')", Locale.US);
        assertEquals(0, resultKeys.length);

        // longs
        resultKeys = queryManager.performQueryReturnKeys("select id where $LongField1 = 1978 or $LongField1 = 1985 OR $LongField1 = 1990 and $LongField1 != 1991 and InCollection('collection5')", Locale.US);
        assertEquals(3, resultKeys.length);

           // - same but with InCollection in another location
        resultKeys = queryManager.performQueryReturnKeys("select id where $LongField1 = 1978 or $LongField1 = 1985 OR $LongField1 = 1990 and InCollection('collection5') and $LongField1 != 1991", Locale.US);
        assertEquals(3, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where ($LongField1 = 1978 and InCollection('collection5')) or ($LongField1 = 1985 and InCollection('collection5'))", Locale.US);
        assertEquals(2, resultKeys.length);

        queryManager = user2Repository.getQueryManager();
        resultKeys = queryManager.performQueryReturnKeys("select id where ($LongField1 = 1978 and InCollection('collection5')) or ($field2 = 34 and InCollection('collection2'))", Locale.US);
        assertEquals(2, resultKeys.length);

        queryManager = user1Repository.getQueryManager();
        resultKeys = queryManager.performQueryReturnKeys("select id where $LongField1 between 1977 and 1979", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $LongField1 in (1978,1985)", Locale.US);
        assertEquals(2, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $LongField1 not in (1978,1985)", Locale.US);
        assertEquals(1, resultKeys.length);

        //doubles
        resultKeys = queryManager.performQueryReturnKeys("select id where $DoubleField1 > 123 and $DoubleField1 < 124", Locale.US);
        assertEquals(1, resultKeys.length);

        //
        // Combinations (checks that eg joining works well)
        //
        resultKeys = queryManager.performQueryReturnKeys("select id where name = 'Document11' and $LongField1 = 1990 and documentType = 'doctype3'", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $LongField1 = 1990 and name = 'Document11' and documentType = 'doctype3'", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $LongField1 = 1990 and documentType = 'doctype3' and name = 'Document11'", Locale.US);
        assertEquals(1, resultKeys.length);

        //
        // Test query options
        //
        String liveDate = isoDateTimeNoMillis.print(new Date().getTime());
        
        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'boe'", Locale.US);
        assertEquals(0, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'boe' option point_in_time = 'live'", Locale.US);
        assertEquals(0, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'boe' option point_in_time = '" + liveDate + "'", Locale.US);
        assertEquals(0, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'boe' option point_in_time = 'last'", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'boe' option search_last_version = 'true'", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'ba'", Locale.US);
        assertEquals(0, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'ba' option point_in_time = 'live'", Locale.US);
        assertEquals(0, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'ba' option point_in_time = '" + liveDate + "'", Locale.US);
        assertEquals(0, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'ba' option point_in_time = 'last'", Locale.US);
        assertEquals(0, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'ba' option search_last_version = 'true'", Locale.US);
        assertEquals(0, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'ba' option include_retired = 'true'", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'ba' option include_retired = 'true', point_in_time = 'live'", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'ba' option include_retired = 'true', point_in_time = '" + liveDate + "'", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'ba' option include_retired = 'true', point_in_time = 'last'", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'ba' option include_retired = 'true', search_last_version = 'true'", Locale.US);
        assertEquals(1, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 in('boe','ba') option include_retired = 'true', point_in_time = 'last'", Locale.US);
        assertEquals(2, resultKeys.length);

        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 in('boe','ba') option include_retired = 'true', search_last_version = 'true'", Locale.US);
        assertEquals(2, resultKeys.length);

        //
        // Test externally defined query options
        //
        Map myQueryOptions = new HashMap();
        myQueryOptions.put("search_last_version", "true");
        resultKeys = queryManager.performQueryReturnKeys("select id where $StringField1 = 'boe'", null, myQueryOptions, Locale.US);
        assertEquals(1, resultKeys.length);

        //
        // Test limit clause
        //
        resultKeys = queryManager.performQueryReturnKeys("select id where InCollection('collection2','collection1') limit 2", Locale.US);
        assertEquals(2, resultKeys.length);

        //
        // Test order by
        //
        searchResultDoc = queryManager.performQuery("select name, $StringField1 where $StringField1 like '%hello' and InCollection('collection5') order by $StringField1 asc, id desc", Locale.US);
        assertEquals("another hello", searchResultDoc.getSearchResult().getRows().getRowArray()[0].getValueArray()[1]);
        assertEquals("hello", searchResultDoc.getSearchResult().getRows().getRowArray()[1].getValueArray()[1]);

        //
        // Test multi-values
        //
        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);
        RepositorySchema schema = repository.getRepositorySchema();
        FieldType multiValueField1 = schema.createFieldType("multiValueField1", ValueType.STRING, true);
        multiValueField1.save();
        DocumentType documentType = schema.createDocumentType("docWithMultiValueFields");
        documentType.addFieldType(multiValueField1, true);
        documentType.save();
        Document mvDoc1 = repository.createDocument("mv doc 1", documentType.getId());
        mvDoc1.setField("multiValueField1", new Object[] {"mv 1", "mv 2", "mv 3"});
        mvDoc1.save();
        Document mvDoc2 = repository.createDocument("mv doc 2", documentType.getId());
        mvDoc2.setField("multiValueField1", new Object[] {"mv 1", "abc"});
        mvDoc2.save();

        searchResultDoc = queryManager.performQuery("select name, $multiValueField1 where documentType = 'docWithMultiValueFields'", Locale.US);
        SearchResultDocument.SearchResult.Rows.Row.MultiValue[] multiValues = searchResultDoc.getSearchResult().getRows().getRowArray()[0].getMultiValueArray();
        assertEquals("mv 1", multiValues[0].getValueArray(0));
        assertEquals("mv 2", multiValues[0].getValueArray(1));
        assertEquals("mv 3", multiValues[0].getValueArray(2));

        // .valueCount
        searchResultDoc = queryManager.performQuery("select name, $multiValueField1 where $multiValueField1.valueCount = 3", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);
        searchResultDoc = queryManager.performQuery("select name, $multiValueField1 where documentType= 'docWithMultiValueFields' and $multiValueField1.valueCount = 1", Locale.US);
        assertEquals(0, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        // has all
        searchResultDoc = queryManager.performQuery("select name, $multiValueField1 where documentType= 'docWithMultiValueFields' and $multiValueField1 has all ('mv 1', 'mv 2')", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);
        searchResultDoc = queryManager.performQuery("select name, $multiValueField1 where documentType= 'docWithMultiValueFields' and $multiValueField1 has all ('mv 1')", Locale.US);
        assertEquals(2, searchResultDoc.getSearchResult().getRows().getRowArray().length);
        searchResultDoc = queryManager.performQuery("select name, $multiValueField1 where documentType= 'docWithMultiValueFields' and $multiValueField1 has all ('rrrrrr')", Locale.US);
        assertEquals(0, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        // has exactly
        searchResultDoc = queryManager.performQuery("select name, $multiValueField1 where documentType= 'docWithMultiValueFields' and $multiValueField1 has exactly ('mv 1', 'abc')", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);
        searchResultDoc = queryManager.performQuery("select name, $multiValueField1 where documentType= 'docWithMultiValueFields' and $multiValueField1 has exactly ('mv 1', 'mv 2')", Locale.US);
        assertEquals(0, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        // has any
        searchResultDoc = queryManager.performQuery("select name, $multiValueField1 where documentType= 'docWithMultiValueFields' and $multiValueField1 has any ('mv 1', 'koekoek', 'mv 2', 'jaja')", Locale.US);
        assertEquals(2, searchResultDoc.getSearchResult().getRows().getRowArray().length);
        searchResultDoc = queryManager.performQuery("select name, $multiValueField1 where documentType= 'docWithMultiValueFields' and $multiValueField1 has some ('koekoek', 'mv 2', 'jaja')", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        // has none
        searchResultDoc = queryManager.performQuery("select name, $multiValueField1 where documentType= 'docWithMultiValueFields' and $multiValueField1 has none ('mv 1')", Locale.US);
        assertEquals(0, searchResultDoc.getSearchResult().getRows().getRowArray().length);
        searchResultDoc = queryManager.performQuery("select name, $multiValueField1 where documentType= 'docWithMultiValueFields' and $multiValueField1 has none ('patati patata')", Locale.US);
        assertEquals(2, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        //
        // Test link field type
        //
        searchResultDoc = queryManager.performQuery("select name, $LinkField1, $LinkField2 where $LinkField1 is not null", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);
        assertEquals("daisy:666-DSYTEST@1:1", searchResultDoc.getSearchResult().getRows().getRowArray(0).getMultiValueArray(0).getLinkValueArray(1).getStringValue());

        searchResultDoc = queryManager.performQuery("select name, $LinkField1, $LinkField2 where $LinkField1.branchId = 1 and $LinkField1.languageId = 1", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select name, $LinkField1, $LinkField2 where $LinkField1.branch = 'main' and $LinkField1.language = 'default'", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        //
        // Distinct query test
        //
        DistinctSearchResultDocument distinctResultDoc = queryManager.performDistinctQuery("select $StringField1 where $StringField1 is not null", SortOrder.NONE, Locale.US);
        assertEquals(2, distinctResultDoc.getDistinctSearchResult().getValues().getValueArray().length);

        //
        // Test Hierarchical field type
        //
        contentCreator.createAdditionalDocumentsForHierarchicalFieldTests(repositoryManager);

        try {
            queryManager.performQuery("select $HierField1 where $HierField1 has all('A', 'B', 'C')", Locale.US);
            fail("Using has all with a non-multivalue field field should fail.");
        } catch (Exception e) {}

        try {
            // $LinkField2 is multivalue, and of type link, but non-hierarhical
            queryManager.performQuery("select id where $LinkField2 has all( Path('/daisy:1000-DSYTEST/daisy:1001-DSYTEST'))", Locale.US);
            fail("Using has all with hierarchical argumetns but a non-hierarchical field should fail.");
        } catch (Exception e) {}

        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 has all( Path('/daisy:1000-DSYTEST/daisy:1001-DSYTEST'))", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 has exactly( Path('/daisy:1000-DSYTEST/daisy:1001-DSYTEST'))", Locale.US);
        assertEquals(0, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 has all( Path('/daisy:1000-DSYTEST/daisy:1001-DSYTEST'), Path('/daisy:1002-DSYTEST'))", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 has exactly( Path('/daisy:1000-DSYTEST/daisy:1001-DSYTEST'), Path('/daisy:1002-DSYTEST'))", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        // two times same argument, should not matter
        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 has all( Path('/daisy:1000-DSYTEST/daisy:1001-DSYTEST'), Path('/daisy:1002-DSYTEST'), Path('/daisy:1002-DSYTEST'))", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        // one dummy value more, should not find anything
        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 has all( Path('/daisy:1000-DSYTEST/daisy:1001-DSYTEST'), Path('/daisy:1002-DSYTEST'), Path('/daisy:1234-DSYTEST'))", Locale.US);
        assertEquals(0, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        // adding one element more to the path should not give results
        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 has all( Path('/daisy:1000-DSYTEST/daisy:1001-DSYTEST/daisy:1002-DSYTEST'))", Locale.US);
        assertEquals(0, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        // one element less in the path should not give results either
        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 has all( Path('/daisy:1000-DSYTEST'))", Locale.US);
        assertEquals(0, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 has all( Path('/daisy:1005-DSYTEST/daisy:1006-DSYTEST'))", Locale.US);
        assertEquals(2, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 has all( Path('/daisy:1005-DSYTEST/daisy:1006-DSYTEST'), Path('/daisy:1007-DSYTEST'))", Locale.US);
        assertEquals(2, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 has all( Path('/daisy:1005-DSYTEST/daisy:1006-DSYTEST'), Path('/daisy:1007-DSYTEST'), Path('/daisy:1009/daisy:1010/daisy:1009'))", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 has any( Path('/daisy:1007-DSYTEST'), Path('/daisy:1002-DSYTEST'))", Locale.US);
        assertEquals(3, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 has any( Path('/daisy:1007-DSYTEST'), Path('/daisy:1009/daisy:1010/daisy:1009'))", Locale.US);
        assertEquals(2, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 is not null and $HierField2 has none( Path('/daisy:1007-DSYTEST'), Path('/daisy:1009/daisy:1010/daisy:1009'))", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 is not null and $HierField2 has none( Path('/daisy:1007-DSYTEST'), Path('/daisy:1009/daisy:1010/daisy:1009'), Path('/daisy:1000/daisy:1001'))", Locale.US);
        assertEquals(0, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 is not null and $HierField2 has none( Path('/daisy:2000-DSYTEST'))", Locale.US);
        assertEquals(3, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select $HierField2 where $HierField2 is not null and $HierField2 has none( Path('/daisy:2000-DSYTEST'), Path('/daisy:2000-DSYTEST/daisy:2000-DSYTEST'))", Locale.US);
        assertEquals(3, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select id where $HierField1 matchesPath('/X/Y')", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select id where $HierField1 matchesPath('/X/*')", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select id where $HierField1 matchesPath('/*/*')", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select id where $HierField1 matchesPath('/Aaa/**')", Locale.US);
        assertEquals(3, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select id where $HierField1 matchesPath('**/Cee')", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select id where $HierField1 matchesPath('**/Cee/Dee')", Locale.US);
        assertEquals(2, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select id where $HierField1 matchesPath('**/*/Dee')", Locale.US);
        assertEquals(2, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select id where $HierField1 matchesPath('**/Cee') or $HierField1 matchesPath('**/Dee')", Locale.US);
        assertEquals(3, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        searchResultDoc = queryManager.performQuery("select id where $HierField1 matchesPath('/Aaa/*/Cee')", Locale.US);
        assertEquals(1, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        // equals operator should evaluate to true whenever the value occurs somewhere in the hierarchical field
        searchResultDoc = queryManager.performQuery("select id where $HierField1 = 'Cee'", Locale.US);
        assertEquals(3, searchResultDoc.getSearchResult().getRows().getRowArray().length);

        // Test transport of very long queries: this could fail in the remote api implementation if the request is done using HTTP GET
        StringBuilder longThing = new StringBuilder(20000);
        for (int i = 0; i < 20000; i++) {
            longThing.append(' ');
        }
        queryManager.performQueryReturnKeys("select name where InCollection('collection2')" + longThing, Locale.US);
    }

    protected boolean resetDataStores() {
        return true;
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;
}
