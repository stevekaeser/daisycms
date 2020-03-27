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
package org.outerj.daisy.books.publisher.impl.dataretrieval;

import org.outerx.daisy.x10Bookdeps.BookDependenciesDocument;
import org.outerx.daisy.x10Bookdeps.BookDependenciesDocument.BookDependencies.Dependency;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

public class BookDependencies {
    private HashSet keys = new HashSet();

    public void addDependency(VersionKey versionKey) {
        keys.add(versionKey);
    }

    public void removeDependency(VersionKey versionKey) {
        keys.remove(versionKey);
    }
    
    public void store(OutputStream os) throws IOException {
        BookDependenciesDocument.BookDependencies.Dependency[] dependenciesXml = new BookDependenciesDocument.BookDependencies.Dependency[keys.size()];

        Iterator keysIt = keys.iterator();
        for (int i = 0; keysIt.hasNext(); i++) {
            VersionKey key = (VersionKey)keysIt.next();
            BookDependenciesDocument.BookDependencies.Dependency dependencyXml = BookDependenciesDocument.BookDependencies.Dependency.Factory.newInstance();
            dependencyXml.setDocumentId(key.getDocumentId());
            dependencyXml.setBranchId(key.getBranchId());
            dependencyXml.setLanguageId(key.getLanguageId());
            dependencyXml.setVersionId(key.getVersionId());
            dependenciesXml[i] = dependencyXml;
        }

        BookDependenciesDocument bookDependenciesDocument = BookDependenciesDocument.Factory.newInstance();
        bookDependenciesDocument.addNewBookDependencies().setDependencyArray(dependenciesXml);

        XmlOptions xmlOptions = new XmlOptions();
        xmlOptions.setSavePrettyPrint();
        bookDependenciesDocument.save(os, xmlOptions);
    }

    /***
     * Load a list of versionKeys specified in a dependency file
     * @param dependencyStream the inputstream of the file containing te dependencies
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws XmlException
     * @throws IOException
     */
	public static List<VersionKey> loadVersionKeyList(InputStream dependencyStream) throws ParserConfigurationException, SAXException, XmlException, IOException {
       List<VersionKey> versionKeyList = null;
        
       XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
        
        if (dependencyStream != null) {
        	versionKeyList = new ArrayList<VersionKey>();
        	org.outerx.daisy.x10Bookdeps.BookDependenciesDocument.BookDependencies bookDependencies = BookDependenciesDocument.Factory.parse(dependencyStream, xmlOptions).getBookDependencies();
        	List<Dependency> dependencyList = bookDependencies.getDependencyList();
        	
        	for(Dependency dependency : dependencyList){
        		long branchId = dependency.getBranchId();
                long languageId = dependency.getLanguageId();
                long versionId = dependency.getVersionId();
                String documentId = dependency.getDocumentId();
                VersionKey key = new VersionKey(documentId, branchId, languageId, versionId);
        		versionKeyList.add(key);
        	}
        }
    
        return versionKeyList;
	}

    @SuppressWarnings("unchecked")
    public HashSet<VersionKey> getKeys() {
        return keys;
    }
	
}
