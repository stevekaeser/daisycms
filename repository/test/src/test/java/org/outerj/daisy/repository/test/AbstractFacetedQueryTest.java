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
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.query.FacetConf;
import org.outerx.daisy.x10.FacetedQueryResultDocument;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public abstract class AbstractFacetedQueryTest extends AbstractDaisyTestCase {
    protected boolean resetDataStores() {
        return true;
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;

    public void testFacetedQuery() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();
        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.setActiveRoleIds(new long[] {Role.ADMINISTRATOR});

        RepositorySchema schema = repository.getRepositorySchema();
        FieldType fieldType1 =  schema.createFieldType("testfield1", ValueType.STRING);
        fieldType1.save();
        FieldType fieldType2 =  schema.createFieldType("testfield2", ValueType.STRING, true);
        fieldType2.save();
        DocumentType documentType = schema.createDocumentType("doctype");
        documentType.addFieldType(fieldType1, false);
        documentType.addFieldType(fieldType2, false);
        documentType.save();

        Document document1 = repository.createDocument("mydoc1", documentType.getId());
        document1.setField("testfield1", "cow");
        document1.setField("testfield2", new String[] {"blue", "black", "green"});
        document1.save();

        Document document2 = repository.createDocument("mydoc2", documentType.getId());
        document2.setField("testfield1", "rabbit");
        document2.setField("testfield2", new String[] {"blue", "purple", "green", "green"});
        document2.save();

        Map<String, String> queryOptions = new HashMap<String, String>();
        queryOptions.put("chunk_offset", "0");
        queryOptions.put("chunk_length", "10");        

        QueryManager queryManager = repository.getQueryManager();
        FacetConf[] facetConfs = new FacetConf[] {new FacetConf(), new FacetConf(), new FacetConf()};
        FacetedQueryResultDocument result = queryManager.performFacetedQuery("select documentType, $testfield1, $testfield2 where true", facetConfs, queryOptions, Locale.US);

        // Test that the value count is correct in case a multivalue field contains the same value more than once
        FacetedQueryResultDocument.FacetedQueryResult.Facets.Facet.Value greenValue = result.getFacetedQueryResult().getFacets().getFacetArray(2).getValueArray(2);
        assertEquals("green", greenValue.getUserFormat());
        assertEquals(2, greenValue.getCount());

        System.out.println(result);
    }
}
