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
package org.outerj.daisy.publisher.serverimpl.resolving;

import org.outerj.daisy.publisher.PublisherException;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.query.PredicateExpression;
import org.outerj.daisy.repository.query.QueryException;
import org.outerx.daisy.x10Publishermapping.PublisherMappingDocument;
import org.apache.xmlbeans.XmlOptions;
import org.apache.commons.logging.Log;

import java.io.*;

public class ResolveDefinitionBuilder {
    private Repository repository;
    private Log log;

    public ResolveDefinitionBuilder(Repository repository, Log log) {
        this.repository = repository;
        this.log = log;
    }

    public ResolveDefinition build(File mappingFile, String pubReqSetName) throws PublisherException {
        if (!mappingFile.exists()) {
            throw new PublisherException("Mapping file does not exist at " + mappingFile.getAbsolutePath());
        }

        InputStream is = null;
        try {
            try {
                is = new BufferedInputStream(new FileInputStream(mappingFile));
            } catch (FileNotFoundException e) {
                throw new PublisherException("Error accessing publisher request mapping.", e);
            }
            return build(is, pubReqSetName);
        } finally {
            if (is != null)
                try { is.close(); } catch (Exception e) { log.error("Error closing InputStream in finally.", e); }
        }
    }

    public ResolveDefinition build(InputStream is, String pubReqSetName) throws PublisherException {
        PublisherMappingDocument mappingDocument;
        try {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            mappingDocument = PublisherMappingDocument.Factory.parse(is, xmlOptions);
        } catch (Exception e) {
            throw new PublisherException("Error parsing publisher request mapping.", e);
        }

        if (!mappingDocument.validate()) {
            throw new PublisherException("Mapping file for \"" + pubReqSetName + "\" does not conform to schema.");
        }

        ResolveDefinition resolveDefinition = new ResolveDefinition();

        QueryManager queryManager = repository.getQueryManager();
        for (PublisherMappingDocument.PublisherMapping.When when : mappingDocument.getPublisherMapping().getWhenList()) {
            String expressionString = when.getTest();
            PredicateExpression expression = null;
            try {
                expression = queryManager.parsePredicateExpression(expressionString);
            } catch (QueryException e) {
                String message = "Error parsing expression in mapping file for \"" + pubReqSetName + "\": " + expression;
                throw new PublisherException(message, e);
            }
            String publisherRequestName = when.getUse();
            resolveDefinition.addRule(expressionString, expression, publisherRequestName);
        }

        return resolveDefinition;
    }
}
