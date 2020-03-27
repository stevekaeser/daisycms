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

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.LiveHistoryEntry;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.Timeline;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.testsupport.AbstractDaisyTestCase;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;
import org.outerx.daisy.x10.SearchResultDocument.SearchResult;

public abstract class AbstractLiveHistoryTest extends AbstractDaisyTestCase {
    
    private DateTimeFormatter iso8601 = ISODateTimeFormat.dateTimeNoMillis();
    
    protected boolean resetDataStores() {
        return true;
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;

    public void testLiveHistory() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();
        Repository adminRepository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        adminRepository.switchRole(Role.ADMINISTRATOR);

        // create a user (without admin rights) which will do all operations
        UserManager userManager = adminRepository.getUserManager();
        Role role = userManager.getRole("User", false);
        User user = userManager.createUser("ordinaryUser");
        user.addToRole(role);
        user.setDefaultRole(role);
        user.setPassword("secret");
        user.save();
        Repository repository = repositoryManager.getRepository(new Credentials("ordinaryUser", "secret"));

        RepositorySchema schema = adminRepository.getRepositorySchema();

        // Create some field types
        FieldType stringField1 = schema.createFieldType("StringField1", ValueType.STRING);
        stringField1.save();

        // Create a documenttype
        DocumentType documentType1 = schema.createDocumentType("DocumentType1");
        documentType1.addFieldType(stringField1, true);
        documentType1.save();

        //
        // Create a document with two versions
        //
        Document document1 = adminRepository.createDocument("Document 1", documentType1.getId());

        // set a field using, once using id, once using name
        document1.setField(stringField1.getId(), "i am old");
        document1.save();
        
        Thread.sleep(1000);
        
        document1.setField(stringField1.getId(), "i am live");
        document1.save();
        
        Thread.sleep(1000);
        
        document1.setField(stringField1.getId(), "i am last");
        document1.setNewVersionState(VersionState.DRAFT);
        document1.save();
        
        Timeline timeline = document1.getTimeline();
        LiveHistoryEntry[] lh = timeline.getLiveHistory();
        assertEquals(2, lh.length);
        assertNotNull(lh[0].getEndDate());
        assertNull(lh[1].getEndDate());
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(lh[0].getBeginDate());
        cal.add(Calendar.DATE, -2);
        Date t_min_two = cal.getTime();
        cal.add(Calendar.DATE, 1);
        Date t_min_one = cal.getTime();
        timeline.addLiveHistoryEntry(t_min_two, t_min_one, 1);
        timeline.save();
        
        Document origDocument = document1;
        document1 = adminRepository.getDocument(document1.getVariantKey(), true);
        
        lh = origDocument.getTimeline().getLiveHistory();
        LiveHistoryEntry[] lh2 = document1.getTimeline().getLiveHistory();
        assertEquals(lh.length, lh2.length);
        for (int i=0; i < lh.length; i++) {
            assertEquals(lh[i].getBeginDate(), lh2[i].getBeginDate());
            assertEquals(lh[i].getEndDate(), lh2[i].getEndDate());
            assertEquals(lh[i].getVersionId(), lh2[i].getVersionId());
        }

        // Give the fulltext indexer some time to do its job
        Thread.sleep(10000);
        
        runQueryTest(adminRepository, document1, null, String.format("option point_in_time='%s'", iso8601.print(t_min_two.getTime())), "i am old");

        // Search
        // 1. seach_last_version='false' -> live
        runQueryTest(adminRepository, document1, null, null, "i am live");
        runQueryTest(adminRepository, document1, "live", null, "i am live");
        runQueryTest(adminRepository, document1, "notfound", null, null);

        // 2. search_last_version='true' -> last
        runQueryTest(adminRepository, document1, null, String.format("option search_last_version='true'"), "i am last");
        // does not return a result as 'last' is not in the fulltext index (because it doesn't have a live history entry)
        runQueryTest(adminRepository, document1, "last", String.format("option search_last_version='true'"), null);
        runQueryTest(adminRepository, document1, "notfound", String.format("option search_last_version='true'"), null);
        
        // 3. point_in_time = t_min_two: 'i am old'
        runQueryTest(adminRepository, document1, null, String.format("option point_in_time='%s'", iso8601.print(t_min_two.getTime())), "i am old");
        runQueryTest(adminRepository, document1, "old", String.format("option point_in_time='%s'", iso8601.print(t_min_two.getTime())), "i am old");
        runQueryTest(adminRepository, document1, "notfound", String.format("option point_in_time='%s'", iso8601.print(t_min_two.getTime())), null);

        // 4. point_in_time = t_min_one: (none)
        runQueryTest(adminRepository, document1, null, String.format("option point_in_time='%s'", iso8601.print(t_min_one.getTime())), null);
        runQueryTest(adminRepository, document1, "old", String.format("option point_in_time='%s'", iso8601.print(t_min_one.getTime())), null);
        runQueryTest(adminRepository, document1, "live", String.format("option point_in_time='%s'", iso8601.print(t_min_one.getTime())), null);
        runQueryTest(adminRepository, document1, "last", String.format("option point_in_time='%s'", iso8601.print(t_min_one.getTime())), null);
        
        // 5. point_in_time = now (should be the same as search_last_version=false)
        // (except when live history entries have dates 2 minutes in the future - this is allowed to survive small date/time offsets between 
        // client and server, but this doesn't happen in this test case)
        runQueryTest(adminRepository, document1, null, String.format("option point_in_time='%s'", iso8601.print(new Date().getTime())), "i am live");
        runQueryTest(adminRepository, document1, "live", String.format("option point_in_time='%s'", iso8601.print(new Date().getTime())), "i am live");
        runQueryTest(adminRepository, document1, "notfound", String.format("option point_in_time='%s'", iso8601.print(new Date().getTime())), null);

        // more tests: make the first version live again, and launch some more queries 
    }

    private void runQueryTest(Repository repo, Document doc, String fullText, String options, String expected) throws RepositoryException {
        StringBuffer query = new StringBuffer("select id, name, $StringField1, versionId where");
        if (fullText != null) {
            query.append(" FullText('").append(fullText).append("')");
        } else {
            query.append(" true");
        }
        query.append(" and id='").append(doc.getId()).append("'");
        if (options != null) {
            query.append(" ").append(options);
        }
        SearchResult result = repo.getQueryManager().performQuery(query.toString(), Locale.US).getSearchResult();
        if (expected == null) {
            assertEquals("Did not expect results for " + query.toString(), 0, result.getRows().getRowList().size());
        } else {
            assertEquals("Expected exactly one result for " + query.toString(), 1, result.getRows().getRowList().size());
            assertEquals("Query did not yield the expected data: " + query.toString(), expected, result.getRows().getRowArray(0).getValueArray(2));
        }
    }
    
}
