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
package org.outerj.daisy.repository.test;

import org.outerj.daisy.repository.testsupport.AbstractDaisyTestCase;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.user.Role;
import org.outerx.daisy.x10.SearchResultDocument;

import java.util.Locale;

/**
 * Testcase for translation-management related functionality.
 */
public abstract class AbstractTMTest  extends AbstractDaisyTestCase {
    protected boolean resetDataStores() {
        return true;
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;

    public void testTM() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();
        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);

        //
        // Create some languages
        //
        Language en = repository.getVariantManager().createLanguage("en");
        en.save();

        Language nl = repository.getVariantManager().createLanguage("nl");
        nl.save();

        Language fr = repository.getVariantManager().createLanguage("fr");
        fr.save();

        Language it = repository.getVariantManager().createLanguage("it");
        it.save();

        //
        // Create some schema types
        //
        RepositorySchema repositorySchema = repository.getRepositorySchema();

        PartType partType1 = repositorySchema.createPartType("partType1", "");
        partType1.setDaisyHtml(true);
        partType1.save();

        FieldType fieldType1 = repositorySchema.createFieldType("fieldType1", ValueType.STRING);
        fieldType1.save();

        DocumentType documentType1 = repositorySchema.createDocumentType("doctype1");
        documentType1.addPartType(partType1, false);
        documentType1.addFieldType(fieldType1, false);
        documentType1.save();

        DocumentType documentType2 = repositorySchema.createDocumentType("doctype2");
        documentType2.addPartType(partType1, false);
        documentType2.addFieldType(fieldType1, false);
        documentType2.save();

        QueryManager qm = repository.getQueryManager();

        //
        // test translation management document properties
        //
        {
            // "en" is always used as the reference language version
            //
            // these version are created and after every stage, the calculated variant-level fields are checked
            // note: change type does not influence the calculated fields, only the queries later on
            // note: what is the effect of setting 'synced_with' on the reference variant?? (always null?)
            //
            // l  v  state synced_with => (fr)last_synced_with (fr)live_synced_with
            // en V1 d     -
            // en V2 d     -
            // en V3 P     -
            // en V4 P     -
            // en V5 d     -
            //
            // fr V1 d     null            null                      null
            // fr V2 d     null            null                      null
            // fr V3 d     en:v3           null                      null
            // fr V4 P     en:v3           en:v3                     en:v3
            // fr V5 d     en:v5           en:v5                     en:v3
            //
            // then unpublish fr:v4
            // fr V4 d     en:v3           en:v5                     null
            // then publish fr:v5
            // fr V5 P     en:v5           en:v5                     en:v5
            // then republish fr:v4 (should not change last_ and live_synced_with
            // fr V4 d     en:v3           en:v5                     en:v5

            byte[] part1Data = "<html><body><p>Hello this is the content of part1</p></body></html>".getBytes();

            Document enDoc = repository.createDocument("reference variant", documentType1.getId(), 1L, en.getId());
            enDoc.setNewVersionState(VersionState.DRAFT);
            enDoc.setPart(partType1.getId(), "text/xml", part1Data);
            enDoc.save(); // v1
            assertEquals(1L, enDoc.getLastVersionId());
            enDoc.setPart(partType1.getId(), "text/xml", part1Data);
            Thread.sleep(1000);
            enDoc.save(); // v2
            assertEquals(2L, enDoc.getLastVersionId());
            enDoc.setNewVersionState(VersionState.PUBLISH);
            enDoc.setPart(partType1.getId(), "text/xml", part1Data);
            Thread.sleep(1000);
            enDoc.save(); // v3
            assertEquals(3L, enDoc.getLastVersionId());
            enDoc.setPart(partType1.getId(), "text/xml", part1Data);
            Thread.sleep(1000);
            enDoc.save(); // v4
            assertEquals(4L, enDoc.getLastVersionId());
            enDoc.setNewVersionState(VersionState.PUBLISH);
            enDoc.setPart(partType1.getId(), "text/xml", part1Data);
            Thread.sleep(1000);
            enDoc.save(); // v5
            assertEquals(5L, enDoc.getLastVersionId());

            Document frDoc = repository.createVariant(enDoc.getId(), enDoc.getBranchId(), enDoc.getLanguageId(), enDoc.getLastVersionId(), enDoc.getBranchId(), fr.getId(), true);
            Version frDocLast;
            frDoc.setName("translated variant"); // createVariant = v1, changing the name = v2
            frDoc.setNewVersionState(VersionState.DRAFT);
            frDoc.setRequestedLiveVersionId(-2);
            Thread.sleep(1000);
            frDoc.save(); // v2
            assertEquals(2L, frDoc.getLastVersionId());

            VersionKey en_v3 = new VersionKey(enDoc.getId(), enDoc.getBranchId(), enDoc.getLanguageId(), 3L);
            VersionKey en_v4 = new VersionKey(enDoc.getId(), enDoc.getBranchId(), enDoc.getLanguageId(), 4L);
            VersionKey en_v5 = new VersionKey(enDoc.getId(), enDoc.getBranchId(), enDoc.getLanguageId(), 5L);
            VersionKey en_v6 = new VersionKey(enDoc.getId(), enDoc.getBranchId(), enDoc.getLanguageId(), 6L);

            frDoc.setPart(partType1.getId(), "text/xml", part1Data);
            frDoc.setNewSyncedWith(en.getId(), 3L);
            frDoc.setRequestedLiveVersionId(-2);
            Thread.sleep(1000);
            frDoc.save(); // v3;
            assertEquals(3L, frDoc.getLastVersionId());
            // should work without and with refetching the document
            // frDoc = repository.getDocument(frDoc.getId(), 1L, fr.getId(), true);
            frDocLast = frDoc.getLastVersion();
            assertEquals(en_v3, frDocLast.getSyncedWith());
            // note: not really relevant in the context of translation management, but it does not hurt
            assertNull("live version should be null when document has no published versions but was" + frDoc.getLiveVersion(), frDoc.getLiveVersion());

            // try unsetting synced with
            {
                VersionKey currentSyncedWith = frDocLast.getSyncedWith();
                frDocLast.setSyncedWith(-1, -1);
                assertNull(frDocLast.getSyncedWith());
                Thread.sleep(1000);
                frDocLast.save();
                assertNull(frDocLast.getSyncedWith());
                frDocLast.setSyncedWith(currentSyncedWith.getLanguageId(), currentSyncedWith.getVersionId());
                Thread.sleep(1000);
                frDocLast.save();
            }


            frDoc.setPart(partType1.getId(), "text/xml", part1Data);
            frDoc.setNewVersionState(VersionState.PUBLISH);
            Thread.sleep(1000);
            frDoc.save(); // v4;
            frDocLast = frDoc.getLastVersion();
            assertEquals(4L, frDoc.getLastVersionId());
            assertEquals(4L, frDoc.getLiveVersionId());
            assertEquals(en_v3, frDocLast.getSyncedWith());
            assertEquals(en_v3, frDoc.getLiveVersion().getSyncedWith());

            frDoc.setPart(partType1.getId(), "text/xml", part1Data);
            frDoc.setNewVersionState(VersionState.DRAFT);
            frDoc.setNewSyncedWith(en.getId(), 5L);
            Thread.sleep(1000);
            frDoc.save(); // v5;
            frDocLast = frDoc.getLastVersion();
            assertEquals(5L, frDoc.getLastVersionId());
            assertEquals(4L, frDoc.getLiveVersionId());
            assertEquals(en_v5, frDocLast.getSyncedWith());
            assertEquals(4L, frDoc.getLiveVersion().getId());
            assertEquals(en_v3, frDoc.getLiveVersion().getSyncedWith());

            // now 4 gets unpublished (no live version anymore)
            Version frVersion4 = frDoc.getVersion(4);
            frVersion4.setState(VersionState.DRAFT);
            frVersion4.save();
            frDoc.setRequestedLiveVersionId(-2);
            frDoc.save();
            assertEquals(-1L, frDoc.getLiveVersionId());
            assertEquals(en_v5, frDocLast.getSyncedWith());
            assertNull("live version should be null when document has no published versions but was" + frDoc.getLiveVersion(), frDoc.getLiveVersion());

            // now v5 gets published (live = v5)
            frDoc.setRequestedLiveVersionId(5);
            frDoc.save();
            Version frVersion5 = frDoc.getVersion(5);
            
            // need to refetch the document because live version is not updated.  TODO: fix this :)
            frDoc = repository.getDocument(frDoc.getId(), frDoc.getBranchId(), frDoc.getLanguageId(), true);
            assertEquals(5L, frDoc.getLiveVersionId());
            assertEquals(en_v5, frDocLast.getSyncedWith());
            assertEquals(en_v5, frDoc.getLiveVersion().getSyncedWith());

            try {
                frDoc.setNewSyncedWith(en.getId(), 6L);
                frDoc.setPart(partType1.getId(), "text/xml", part1Data); // make sure a new version would be created
                Thread.sleep(1000);
                frDoc.save();
                fail("setting syncedWith to non-existing version should throw an exception, but it did not");
            } catch (Exception e) {
                //intentionally empty
            }

            try {
                frVersion5.setSyncedWith(en.getId(), 6L);
                frVersion5.save();
                fail("setting sycnedwith to non-existing version should throw an exception, but it did not");
            } catch (Exception e) {
                //intentionally empty
            }

            // after deleting the reference variant, referenceLanguageId should be set to null
            repository.deleteVariant(enDoc.getVariantKey());
            assertEquals("The reference language should be -1 after deleting the reference variant", -1, frDoc.getReferenceLanguageId());

            // need to refetch the document because versions objects are not updated.
            frDoc = repository.getDocument(frDoc.getId(), frDoc.getBranchId(), frDoc.getLanguageId(), true);
            for (Version version: frDoc.getVersions().getArray()) {
                assertNull("After a variant is deleted, the syncedWith-fields can no longer refer to that language (" + en.getName() + ")", version.getSyncedWith());
            }
        }

        //
        // Test translation management queries
        //
        {
            Document document = repository.createDocument("doc", "doctype2", "main", "en");
            document.setReferenceLanguageId(en.getId());
            document.setField("fieldType1", "en value 1");
            document.setNewVersionState(VersionState.DRAFT);
            document.save();

            document.setField("fieldType1", "en value 2");
            document.setNewVersionState(VersionState.DRAFT);
            Thread.sleep(1000);
            document.save();

            // Make fr variant, synced with first version
            document = repository.createVariant(document.getId(), "main", "en", -1, "main", "fr", false);
            document.setField("fieldType1", "fr value 1");
            document.setNewSyncedWith("en", 1);
            document.setNewVersionState(VersionState.DRAFT);
            Thread.sleep(1000);
            document.save();

            // Make nl variant, synced with second version
            document = repository.createVariant(document.getId(), "main", "en", -1, "main", "nl", false);
            document.setField("fieldType1", "nl value 1");
            document.setNewSyncedWith("en", 2);
            document.setNewVersionState(VersionState.DRAFT);
            Thread.sleep(1000);
            document.save();

            // Make 'it' variant, synced with nothing
            document = repository.createVariant(document.getId(), "main", "en", -1, "main", "it", false);
            document.setField("fieldType1", "it value 1");
            document.setNewVersionState(VersionState.DRAFT);
            Thread.sleep(1000);
            document.save();


            String defaultConditions = "documentType = 'doctype2' and referenceLanguage is not null and languageId != referenceLanguageId";
            String lastOption = " option search_last_version = 'true'";

            // Test the fr translation is out of sync, the nl one in sync.
            SearchResultDocument result = qm.performQuery("select id, language where LangNotInSync() and " + defaultConditions + " order by language " + lastOption, Locale.US);
            System.out.println(result);
            assertEquals(2, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals("fr", result.getSearchResult().getRows().getRowArray(0).getValueArray(1));
            assertEquals("it", result.getSearchResult().getRows().getRowArray(1).getValueArray(1));

            result = qm.performQuery("select id, language where LangInSync() and " + defaultConditions + lastOption, Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals("nl", result.getSearchResult().getRows().getRowArray(0).getValueArray(1));

            // Since there are no live versions yet, there is no liveMajorChangeId so we can't be in or not in sync with
            result = qm.performQuery("select id, language where LangNotInSync('live') and " + defaultConditions + lastOption, Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals("it", result.getSearchResult().getRows().getRowArray(0).getValueArray(1));

            result = qm.performQuery("select id, language where LangInSync('live') and " + defaultConditions + lastOption, Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            // Test reverse search
            result = qm.performQuery("select id, language where ReverseLangNotInSync('fr', 'last') and documentType = 'doctype2' " + lastOption, Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals("en", result.getSearchResult().getRows().getRowArray(0).getValueArray(1));

            result = qm.performQuery("select id, language where language = 'en' and ReverseLangNotInSync('fr', 'live') and documentType = 'doctype2' " + lastOption, Locale.US);
            // there is no live version of fr, so it is not in sync with the en language
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals("en", result.getSearchResult().getRows().getRowArray(0).getValueArray(1));

            result = qm.performQuery("select id, language where ReverseLangInSync('fr', 'last') and documentType = 'doctype2' " + lastOption, Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = qm.performQuery("select id, language where ReverseLangInSync('nl', 'last') and documentType = 'doctype2' " + lastOption, Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals("en", result.getSearchResult().getRows().getRowArray(0).getValueArray(1));

            result = qm.performQuery("select id, language where ReverseLangNotInSync('nl', 'last') and documentType = 'doctype2' " + lastOption, Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = qm.performQuery("select id, language where ReverseLangNotInSync('default', 'last') and documentType = 'doctype2' " + lastOption, Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = qm.performQuery("select id, language where ReverseLangNotInSync('fr', 'last') and documentType = 'doctype2' " + lastOption, Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = qm.performQuery("select id, language where language = 'en' and ReverseLangNotInSync('fr', 'live') and documentType = 'doctype2' " + lastOption, Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray()); // there is no fr live version, this is consider not-in-sync

            result = qm.performQuery("select id, language where language = 'en' and ReverseLangNotInSync('it', 'last') and documentType = 'doctype2' " + lastOption, Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = qm.performQuery("select id, language where language = 'en' and ReverseLangInSync('it', 'last') and documentType = 'doctype2' " + lastOption, Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            // Create a new nl version, do not set synced with
            document = repository.getDocument(document.getId(), "main", "nl", true);
            document.setField("fieldType1", "nl value 2");
            document.setNewVersionState(VersionState.DRAFT);
            Thread.sleep(1000);
            document.save();

            // synced with pointer is null for last version, which means we are not in sync
            result = qm.performQuery("select id, language where LangInSync() and " + defaultConditions + lastOption, Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());
        }

        //
        // TM queries: test behavior in case major change version is not defined
        //
        {
            DocumentCollection collection = repository.getCollectionManager().createCollection("test3");
            collection.save();
            
            //  en                    fr
            //  1 minor               1 live (synced with en:1)     (LangInSync('last') and LangInSync('live') both true)
            //  2 live+last minor     2 last (sync with not synced  (LangInSync('last') and LangInSync('live') both false)
            //

            Document document = repository.createDocument("doc", "doctype2", "main", "en");
            document.addToCollection(collection);
            document.setReferenceLanguageId(en.getId());
            document.setField("fieldType1", "en value 1");
            document.setNewChangeType(ChangeType.MINOR);
            document.setNewVersionState(VersionState.DRAFT);
            document.save();

            document.setField("fieldType1", "en value 2");
            document.setNewChangeType(ChangeType.MINOR);
            document.setNewVersionState(VersionState.PUBLISH);
            Thread.sleep(1000);
            document.save();

            // Make fr variant, synced with first version
            document = repository.createVariant(document.getId(), "main", "en", -1, "main", "fr", false);
            document.addToCollection(collection);
            document.setField("fieldType1", "fr value 1");
            document.setNewSyncedWith("en", 1);
            document.setNewVersionState(VersionState.PUBLISH);
            Thread.sleep(1000);
            document.save();

            // Make another fr version, not synced at all
            document.setField("fieldType1", "notsynced");
            document.setNewSyncedWith(null);
            document.setNewVersionState(VersionState.DRAFT);
            Thread.sleep(1000);
            document.save();

            SearchResultDocument result;
            
            result = qm.performQuery("select id, language, $fieldType1 where language = 'fr' and LangInSync() and InCollection('test3')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals("fr value 1", result.getSearchResult().getRows().getRowArray(0).getValueArray(2));

            result = qm.performQuery("select id, language, $fieldType1 where language = 'fr' and LangInSync('live') and InCollection('test3')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals("fr value 1", result.getSearchResult().getRows().getRowArray(0).getValueArray(2));

            // fr:live is synced with en:1, (and in sync with en:last because there are no major changes between en:1 and en:live) 
            result = qm.performQuery("select id, language, $fieldType1 where language = 'fr' and LangNotInSync() and InCollection('test3')", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = qm.performQuery("select id, language, $fieldType1 where language = 'fr' and LangNotInSync('live') and InCollection('test3')", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            // fr:last is not synced
            result = qm.performQuery("select id, language where language = 'en' and ReverseLangInSync('fr') and InCollection('test3')", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = qm.performQuery("select id, language where language = 'en' and ReverseLangNotInSync('fr') and InCollection('test3')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            // fr:live is synced with en:1 (and in sync with en:live because there are no major changes between en:1 and en:live)
            result = qm.performQuery("select id, language where language = 'en' and ReverseLangInSync('fr', 'live') and InCollection('test3')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = qm.performQuery("select id, language where language = 'en' and ReverseLangNotInSync('fr', 'live') and InCollection('test3')", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());
        }

        // Test the change-type and change-comment fields
        {
            // situations tested:
            // v1 state v1 c-type v2 state v2 c-type   |||  lastMajorCVersion  liveMajorCVersion
            //   P        M         P        M                   2                  2
            //   P        M         P        m                   1                  1
            //   P        M         D        M                   2                  1
            //   P        M         D        m                   1                  1
            //   P        m         P        M                   2                  2
            //   P        m         P        m                  -1                 -1
            //   P        m         D        M                   2                 -1
            //   P        m         D        m                  -1                 -1
            //   D        M         P        M                   2                  2
            //   D        M         P        m                   1                  1
            //   D        M         D        M                   2                 -1
            //   D        M         D        m                   1                 -1
            //   D        m         P        M                   2                  2
            //   D        m         P        m                  -1                 -1
            //   D        m         D        M                   2                 -1
            //   D        m         D        m                  -1                 -1
            
            Document document = repository.createDocument("doc", "doctype2", "main", "en");
            document.setField("fieldType1", "version 1");
            document.setNewChangeComment("comment for version 1");
            document.save();
            
            assertEquals("New versions default to change type 'major'", ChangeType.MAJOR, document.getLastVersion().getChangeType());
            
            document.setField("fieldType1", "version 2");
            document.setNewChangeComment("comment for version 2");
            Thread.sleep(1000);
            document.save();

            VersionState v1State[] = new VersionState[16];
            VersionState v2State[] = new VersionState[16];
            ChangeType v1CType[] = new ChangeType[16];
            ChangeType v2CType[] = new ChangeType[16];
            
            int[] lastMCVersion = new int[16];
            for (int i=0;i<16;i++) {
                v1State[i] = (i / 8) % 2 == 0 ? VersionState.PUBLISH : VersionState.DRAFT;
                v1CType[i] = (i / 4) % 2 == 0 ? ChangeType.MAJOR : ChangeType.MINOR;
                v2State[i] = (i / 2) % 2 == 0 ? VersionState.PUBLISH : VersionState.DRAFT;
                v2CType[i] = (i / 1) % 2 == 0 ? ChangeType.MAJOR : ChangeType.MINOR;
                
                lastMCVersion[i] = (i / 1) % 2 == 0 ? 2 : (i / 4) % 2 == 0 ? 1 : -1;
            }
            int[] liveMCVersion = new int[]{ 2,1,1,1,2,-1,-1,-1,2,1,-1,-1,2,-1,-1,-1 };
            
            for (int i = 0; i < 16; i ++) {
                String id = String.format("v1: %s %s   v2: %s %s  ", v1State[i], v1CType[i], v2State[i], v2CType[i]);
                Version v1 = document.getVersion(1);
                v1.setState(v1State[i]);
                v1.setChangeType(v1CType[i]);
                v1.save();
                Version v2 = document.getVersion(2);
                v2.setState(v2State[i]);
                v2.setChangeType(v2CType[i]);
                v2.save();
                document = repository.getDocument(document.getId(), "main", "en", true);
                assertEquals(id + " last major change version", lastMCVersion[i], document.getLastMajorChangeVersionId());
                assertEquals(id + " live major change version", liveMCVersion[i], document.getLiveMajorChangeVersionId());
            }
        }
    }
}

