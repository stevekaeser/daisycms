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
package org.outerj.daisy.frontend.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.outerj.daisy.repository.CollectionManager;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.schema.RepositorySchema;

import edu.emory.mathcs.backport.java.util.Collections;

public class SearchConfiguration {
    
    private boolean documentNameShow;
    private boolean documentNameAutoWildcards;
    private boolean fullTextShow = true;
    private boolean fullTextRequired = true;
    private boolean collectionsShow = true;
    private boolean collectionsUseSiteCollection = true;
    private List<String> staticCollections = new ArrayList<String>();
    private boolean documentTypesShow;
    private List<String> staticDocumentTypes = new ArrayList<String>();;
    private boolean documentTypesExcludeShow;
    private boolean documentTypesExcludeDefault;
    private boolean partTypesShow;
    private List<String> staticPartTypes = new ArrayList<String>();
    private List<String> additionalSelectExpr = new ArrayList<String>();
    private String stylesheetPath = "daisyskin:xslt/fulltext_searchresult.xsl";
    private String extraCond;

    /**
     * Create a configured instance.  The default values are based on the default fullText screen behaviour.
     * @param avalonConfig
     */
    public SearchConfiguration(Configuration avalonConfig, Repository repository) throws RepositoryException, ConfigurationException {
        if (avalonConfig == null) {
            return;
        }
        CollectionManager collectionManager = repository.getCollectionManager();
        RepositorySchema schema = repository.getRepositorySchema();
        
        Configuration docNameConf = avalonConfig.getChild("documentName", false);
        Configuration fullTextConf = avalonConfig.getChild("fullText", false);
        Configuration collectionsConf = avalonConfig.getChild("collections", false);
        Configuration documentTypesConf = avalonConfig.getChild("documentTypes", false);
        Configuration partTypesConf = avalonConfig.getChild("partTypes", false);
        Configuration selects = avalonConfig.getChild("additionalSelectExpressions", false);

        if (docNameConf != null) {
            documentNameShow = docNameConf.getAttributeAsBoolean("show", documentNameShow);
            documentNameAutoWildcards = docNameConf.getAttributeAsBoolean("autoWildcards", documentNameAutoWildcards);
        }
        if (fullTextConf != null) {
            fullTextShow = fullTextConf.getAttributeAsBoolean("show", fullTextShow);
            fullTextRequired = fullTextConf.getAttributeAsBoolean("required", fullTextRequired);
        }
        if (collectionsConf != null) {
            collectionsShow = collectionsConf.getAttributeAsBoolean("show", collectionsShow);
            for  (Configuration child: collectionsConf.getChildren("collection")) {
                staticCollections.add(collectionManager.getCollection(child.getValue(), false).getName());
            }
        }
        if (documentTypesConf != null) {
            documentTypesShow = documentTypesConf.getAttributeAsBoolean("show", documentTypesShow);
            documentTypesExcludeShow = documentTypesShow && documentTypesConf.getAttributeAsBoolean("showExclude", false);
            documentTypesExcludeDefault = documentTypesExcludeShow && documentTypesConf.getAttributeAsBoolean("exclude", false);
        }
        if (partTypesConf != null) {
            partTypesShow = partTypesConf.getAttributeAsBoolean("show", documentTypesShow);
        }
        if (selects != null) {
        	for (Configuration child : selects.getChildren("expr")) {
        		this.additionalSelectExpr.add(child.getValue());
        	}        	
        }
        
        staticCollections = Collections.unmodifiableList(staticCollections);
        staticDocumentTypes = Collections.unmodifiableList(staticDocumentTypes);
        staticPartTypes = Collections.unmodifiableList(staticPartTypes);

        this.extraCond = avalonConfig.getChild("extraConditions").getValue(null);
        
        this.stylesheetPath = avalonConfig.getChild("stylesheetPath").getValue(this.stylesheetPath);
    }

    public boolean isDocumentNameShow() {
        return documentNameShow;
    }

    public void setDocumentNameShow(boolean documentNameShow) {
        this.documentNameShow = documentNameShow;
    }

    public boolean isDocumentNameAutoWildcards() {
        return documentNameAutoWildcards;
    }

    public void setDocumentNameAutoWildcards(boolean documentNameAutoWildcards) {
        this.documentNameAutoWildcards = documentNameAutoWildcards;
    }

    public boolean isFullTextShow() {
        return fullTextShow;
    }

    public void setFullTextShow(boolean fullTextShow) {
        this.fullTextShow = fullTextShow;
    }

    public boolean isFullTextRequired() {
        return fullTextRequired;
    }

    public void setFullTextRequired(boolean fullTextRequired) {
        this.fullTextRequired = fullTextRequired;
    }

    public boolean isCollectionsShow() {
        return collectionsShow;
    }

    public void setCollectionsShow(boolean collectionsShow) {
        this.collectionsShow = collectionsShow;
    }

    public boolean isCollectionsUseSiteCollection() {
        return collectionsUseSiteCollection;
    }

    public void setCollectionsUseSiteCollection(boolean collectionsUseSiteCollection) {
        this.collectionsUseSiteCollection = collectionsUseSiteCollection;
    }

    public List<String> getStaticCollections() {
        return staticCollections;
    }

    public void setStaticCollections(List<String> staticCollections) {
        this.staticCollections = staticCollections;
    }

    public boolean isDocumentTypesShow() {
        return documentTypesShow;
    }

    public void setDocumentTypesShow(boolean documentTypesShow) {
        this.documentTypesShow = documentTypesShow;
    }

    public List<String> getStaticDocumentTypes() {
        return staticDocumentTypes;
    }

    public void setStaticDocumentTypes(List<String> staticDocumentTypes) {
        this.staticDocumentTypes = staticDocumentTypes;
    }

    public boolean isDocumentTypesExcludeShow() {
        return documentTypesExcludeShow;
    }

    public void setDocumentTypesExcludeShow(boolean documentTypesExcludeShow) {
        this.documentTypesExcludeShow = documentTypesExcludeShow;
    }

    public boolean isDocumentTypesExcludeDefault() {
        return documentTypesExcludeDefault;
    }

    public void setDocumentTypesExcludeDefault(boolean documentTypesExcludeDefault) {
        this.documentTypesExcludeDefault = documentTypesExcludeDefault;
    }

    public boolean isPartTypesShow() {
        return partTypesShow;
    }

    public void setPartTypesShow(boolean partTypesShow) {
        this.partTypesShow = partTypesShow;
    }

    public List<String> getStaticPartTypes() {
        return staticPartTypes;
    }

    public void setStaticPartTypes(List<String> staticPartTypes) {
        this.staticPartTypes = staticPartTypes;
    }

    public String getExtraCond() {
        return extraCond;
    }

	public List<String> getAdditionalSelectExpr() {
		return additionalSelectExpr;
	}

	public String getStylesheetPath() {
		return stylesheetPath;
	}
}
