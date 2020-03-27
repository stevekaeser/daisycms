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
package org.outerj.daisy.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.outerj.daisy.repository.CollectionManager;
import org.outerj.daisy.repository.CollectionNotFoundException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentCollection;

/**
 * <p>
 * This mojo will try to create a document with the requested name, type and id
 * in the given namespace. If no namespace is given, the default is used. If no
 * id is given, the id is auto-generated at the repository.
 * </p>
 * 
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>
 * mvn daisy:create-doc -Dname=&quot;My doc name&quot; -Dtype=SimpleDocument -Did=14 -Dns=MYNAMESPACE
 * </pre>
 * 
 * <p>
 * Note that a repository location should be configured as well in your pom. See
 * {@link AbstractDaisyMojo} for more configuration parameters.
 * </p>
 * 
 * @author Jan Hoskens
 * 
 * @goal create-doc
 * @aggregator 
 * @description Create a document in daisy.
 */
public class DaisyCreateDocumentMojo extends AbstractDaisyMojo {

	/**
	 * Daisy document name.
	 * 
	 * @parameter expression="${name}"
	 * @required
	 */
	private String name;

	/**
	 * Daisy document id. An id you like to request as document id if possible.
	 * If none given, the repository will assign one.
	 * 
	 * @parameter expression="${id}"
	 */
	private String id;

	/**
	 * Add the document to this collection.
	 * 
	 * @parameter expression="${collection}"
	 */
	private String collection;

	/**
	 * Daisy document type.
	 * 
	 * @parameter expression="${type}"
	 * @required
	 */
	private String type;

	
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			Document document = getRepository().createDocument(name, type);
			if (id != null) {
			    document.setRequestedId(id);
			}
			if (collection != null) {
                CollectionManager collectionManager = getCollectionManager();
			    DocumentCollection documentCollection = null;
			    try {
                    documentCollection = collectionManager.getCollectionByName(
    						collection, true);
			    } catch (CollectionNotFoundException cnfe) {
			        documentCollection = collectionManager.createCollection(collection);
			        documentCollection.save();
			    }
				document.addToCollection(documentCollection);
			}
			// save without validation
			document.save(false);
			getLog().info("Document name: " + document.getName());
			getLog().info("Document id: " + document.getId());
			getLog().info("Document version: " + document.getLastVersionId());
			getLog().info("Document type id: " + document.getDocumentTypeId());
			getLog().info("Document owner id: " + document.getOwner());

		} catch (Exception e) {
			getLog().error(e);
		}
	}

}
