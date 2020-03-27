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
package org.outerj.daisy.frontend.docbrowser;

import java.util.ArrayList;
import java.util.List;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

public class DocbrowserConfiguration {
	private boolean showMeta=true;
    private boolean expandMeta=false;
    private boolean showFullText=true;
    private boolean expandFullText=true;
    private boolean showPredefined=false;
    private boolean expandPredefined=false;
    private boolean showFaceted=false;
    private boolean expandFaceted=false;
    
    private boolean fullTextName = true;
    private boolean fullTextNameShow = false;
    private boolean fullTextNameReadonly = false;

    private boolean fullTextContent = true;
    private boolean fullTextContentShow = false;
    private boolean fullTextContentReadonly = false;

    private boolean fullTextFields = true;
    private boolean fullTextFieldsShow = false;
    private boolean fullTextFieldsReadonly = false;
    
    private boolean loadImmediately = true;
    private String startMode = "previewMode";
    
    // meta filter can be configured on itself
    private MetafilterConfiguration metaConfig=new MetafilterConfiguration();
    
    private int chunkSize=15;
    
    // name of faceted config
    private String facetedConf;
    
    // a baseCondition can be defined
    private String baseCondition;
    
	private List<Column> columns = new ArrayList<Column>();
	private List<WhereClause> whereClauses = new ArrayList<WhereClause>();
    
    public DocbrowserConfiguration(){
    	super();
    }
    
    /***
     * Read configuration of the docbrowser GUI
     * example:
     * <browserconf>
     * 
     * <chunk size="15"/>
     * <filters>
     * 	<meta show="true"/>
     *  <fulltext show="true"/>
     * 	<predefined show="true"/>
     * 	<faceted show="true">example</faceted>
     * </filters>
     * 
     * <columns>
     * 	<column name="id" label="myLabel" doSorting="true" sortingValue="id"/>
     * 	<column name="name"/>
     * 	<column name="lastModifierLogin"/>
     * 	<column name="lastModified"/>
     * </columns>
     * 
     * </browserconf>
     * @param avalonConfig
     * @throws ConfigurationException
     */
    public DocbrowserConfiguration(Configuration avalonConfig) throws ConfigurationException {
        if (avalonConfig == null) {
            return;
        }
        Configuration conf = avalonConfig.getChild("chunk", false);
        if(conf!=null){
        	chunkSize = conf.getAttributeAsInteger("size");
        }
        
        /* Summary of the rules implemented below:
         *
         * if the 'filters' element is missing, use the defaults (show meta & fulltext, only meta is expanded)
         * 
         * else: per child of <filters>:
         *   if a child element is not present, don't show it
         *   if a child element is present, use the values of @show and @expanded (if absent, default to 'true' for both) 
         */
        
        loadImmediately = avalonConfig.getAttributeAsBoolean("loadImmediately", true);
        startMode = avalonConfig.getAttribute("startMode", "previewMode");
        
        conf = avalonConfig.getChild("filters", false);
        if (conf != null) {
            showMeta = checkBooleanChildAttribute(conf, "meta", false, "show", true);
            expandMeta = checkBooleanChildAttribute(conf, "meta", false, "expanded", true);
            
            Configuration avalonMetaConf = conf.getChild("meta", false);
            if (avalonMetaConf != null) {
                metaConfig.setShowLimitToSite(checkBooleanChildAttribute(avalonMetaConf, "limitToSite", false, "show", true));
                metaConfig.setShowExclude(checkBooleanChildAttribute(avalonMetaConf, "exclude", false, "show", true));
                metaConfig.setShowLanguage(checkBooleanChildAttribute(avalonMetaConf, "language", false, "show", true));
                metaConfig.setLimitToSiteLanguage(checkBooleanChildAttribute(avalonMetaConf, "language", false, "limitToSite", false));
                metaConfig.setShowBranch(checkBooleanChildAttribute(avalonMetaConf, "branch", false, "show", true));
                metaConfig.setLimitToSiteBranch(checkBooleanChildAttribute(avalonMetaConf, "branch", false, "limitToSite", false));
                metaConfig.setShowVersion(checkBooleanChildAttribute(avalonMetaConf, "version", false, "show", true));
            }
            
            showPredefined = checkBooleanChildAttribute(conf, "predefined", false, "show", true);
            expandPredefined = checkBooleanChildAttribute(conf, "predefined", false, "expanded", true);
            
            showFullText = checkBooleanChildAttribute(conf, "fulltext", false, "show", true);
            expandFullText = checkBooleanChildAttribute(conf, "fulltext", false, "expanded", true);
            if (showFullText) {
                Configuration conf2 = conf.getChild("fulltext");
                fullTextName = checkBooleanChildAttribute(conf2, "name", true, "default", true);
                fullTextNameShow = checkBooleanChildAttribute(conf2, "name", false, "show", false);
                fullTextNameReadonly = checkBooleanChildAttribute(conf2, "name", false, "readonly", false);

                fullTextContent = checkBooleanChildAttribute(conf2, "content", true, "default", true);
                fullTextContentShow = checkBooleanChildAttribute(conf2, "content", false, "show", false);
                fullTextContentReadonly = checkBooleanChildAttribute(conf2, "content", false, "readonly", false);

                fullTextFields = checkBooleanChildAttribute(conf2, "fields", true, "default", true);
                fullTextFieldsShow = checkBooleanChildAttribute(conf2, "fields", false, "show", false);
                fullTextFieldsReadonly = checkBooleanChildAttribute(conf2, "fields", false, "readonly", false);
            }

            showFaceted = checkBooleanChildAttribute(conf, "faceted", false, "show", true);
            expandFaceted = checkBooleanChildAttribute(conf, "faceted", false, "expanded", true);
            if (showFaceted) {
                Configuration avalonFacetedConf = conf.getChild("faceted");
                facetedConf = avalonFacetedConf.getChild("config").getValue();
            }
        }

        conf = avalonConfig.getChild("columns", false);
        
        for(Configuration colConf : conf.getChildren("column")){
        	Column column = new Column();
        	String colName=colConf.getAttribute("name");
        	column.setName(colName);
        	column.setLabel(colConf.getAttribute("label", colName));
        	boolean doSorting = colConf.getAttributeAsBoolean("doSorting", true);
        	if(doSorting)
        		column.setSortingValue(colConf.getAttribute("sortingValue", colName));
        	else
        		column.setSortingValue(null);
        	columns.add(column);
        }

        conf = avalonConfig.getChild("baseCondition", false);
        if(conf != null){
        	baseCondition = conf.getValue();
        }
        
        conf = avalonConfig.getChild("predefined", false);
        
        if(conf!=null){
        	Configuration[] whereCls = conf.getChildren("whereclause");
			if (whereCls == null || whereCls.length == 0) {
				showPredefined = false;
			} else {
				for (Configuration colConf : whereCls) {
					whereClauses.add(new WhereClause(colConf.getAttribute("id"), colConf.getChild("name").getValue(), colConf.getChild("value").getValue(), colConf.getChild("description").getValue()));
				}
			}
        }else{
        	showPredefined = false;
        }
        
    }

    /**
     * Returns the value of an optional boolean attribute on an optional child element.
     * (With two defaults: one when the child element is not present, one when the attribute is missing)
     * @param conf
     * @param childName
     * @param attrName
     * @return
     */
    private boolean checkBooleanChildAttribute(Configuration conf,
            String childName, boolean defaultChildMissing, String attrName, boolean defaultAttributeMissing) {
        Configuration child = conf.getChild(childName);
        if (child == null) {
            return defaultChildMissing;
        }
        return child.getAttributeAsBoolean(attrName, defaultAttributeMissing);
    }

    public boolean isShowMeta() {
		return showMeta;
	}

	public List<WhereClause> getWhereClauses() {
		return whereClauses;
	}

	public void setWhereClauses(List<WhereClause> whereClauses) {
		this.whereClauses = whereClauses;
	}

	public void setShowMeta(boolean showMeta) {
		this.showMeta = showMeta;
	}

	public boolean isShowFullText() {
		return showFullText;
	}

	public void setShowFullText(boolean showFullText) {
		this.showFullText = showFullText;
	}

	public boolean isShowPredefined() {
		return showPredefined;
	}

	public void setShowPredefined(boolean showPredefined) {
		this.showPredefined = showPredefined;
	}

	public boolean isShowFaceted() {
		return showFaceted;
	}

	public void setShowFaceted(boolean showFaceted) {
		this.showFaceted = showFaceted;
	}

    public int getChunkSize() {
		return chunkSize;
	}

	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}
	
	public class WhereClause {
		private int id;
		private String name;
		private String value;
		private String description;
		
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public WhereClause(String id, String name, String value, String description) {
			super();
			this.id = Integer.parseInt(id);
			this.name = name;
			this.value = value;
			this.description = description;
		}
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}

	public class Column {
		private String name;
		private String label;
		private boolean doSorting=true;
		private String sortingValue;
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getLabel() {
			return label;
		}
		public void setLabel(String label) {
			this.label = label;
		}
		public boolean isDoSorting() {
			return doSorting;
		}
		public void setDoSorting(boolean doSorting) {
			this.doSorting = doSorting;
		}
		public String getSortingValue() {
			return sortingValue;
		}
		public void setSortingValue(String sortingValue) {
			this.sortingValue = sortingValue;
		}
		
	}
	
	public class MetafilterConfiguration {
		private boolean showLimitToSite=true;
		private boolean showExclude=true;
		private boolean showLanguage=true;
		private boolean limitToSiteLanguage=false;
		private boolean showBranch=true;
		private boolean limitToSiteBranch=false;
		private boolean showVersion=true;

		public MetafilterConfiguration() {
			super();
		}
		
		public boolean isShowLimitToSite() {
			return showLimitToSite;
		}
		public void setShowLimitToSite(boolean showLimitToSite) {
			this.showLimitToSite = showLimitToSite;
		}
		public boolean isShowExclude() {
			return showExclude;
		}
		public void setShowExclude(boolean showExclude) {
			this.showExclude = showExclude;
		}
		public boolean isShowLanguage() {
			return showLanguage;
		}
		public void setShowLanguage(boolean showLanguage) {
			this.showLanguage = showLanguage;
		}
		public boolean isShowBranch() {
			return showBranch;
		}
		public void setShowBranch(boolean showBranch) {
			this.showBranch = showBranch;
		}
		public boolean isShowVersion() {
			return showVersion;
		}
		public void setShowVersion(boolean showVersion) {
			this.showVersion = showVersion;
		}
		public boolean isLimitToSiteLanguage() {
			return limitToSiteLanguage;
		}
		public void setLimitToSiteLanguage(boolean limitToSiteLanguage) {
			this.limitToSiteLanguage = limitToSiteLanguage;
		}
		public boolean isLimitToSiteBranch() {
			return limitToSiteBranch;
		}
		public void setLimitToSiteBranch(boolean limitToSiteBranch) {
			this.limitToSiteBranch = limitToSiteBranch;
		}
	}
	
	public String getFacetedConf() {
		return facetedConf;
	}

	public void setFacetedConf(String facetedConf) {
		this.facetedConf = facetedConf;
	}

	public MetafilterConfiguration getMetaConfig() {
		return metaConfig;
	}

	public void setMetaConfig(MetafilterConfiguration metaConfig) {
		this.metaConfig = metaConfig;
	}

	public String getBaseCondition() {
		return baseCondition;
	}

	public void setBaseCondition(String baseCondition) {
		this.baseCondition = baseCondition;
	}

   public boolean isExpandMeta() {
        return expandMeta;
    }

    public void setExpandMeta(boolean expandMeta) {
        this.expandMeta = expandMeta;
    }

    public boolean isExpandFullText() {
        return expandFullText;
    }

    public void setExpandFullText(boolean expandFullText) {
        this.expandFullText = expandFullText;
    }

    public boolean isExpandPredefined() {
        return expandPredefined;
    }

    public void setExpandPredefined(boolean expandPredefined) {
        this.expandPredefined = expandPredefined;
    }

    public boolean isExpandFaceted() {
        return expandFaceted;
    }

    public void setExpandFaceted(boolean expandFaceted) {
        this.expandFaceted = expandFaceted;
    }

	public List<Column> getColumns() {
		return columns;
	}

	public void setColumns(List<Column> columns) {
		this.columns = columns;
	}

    public boolean isLoadImmediately() {
        return loadImmediately;
    }

    public void setLoadImmediately(boolean loadImmediately) {
        this.loadImmediately = loadImmediately;
    }

    public String getStartMode() {
        return startMode;
    }

    public void setStartMode(String startMode) {
        this.startMode = startMode;
    }

    public boolean isFullTextName() {
        return fullTextName;
    }

    public boolean isFullTextNameShow() {
        return fullTextNameShow;
    }

    public boolean isFullTextNameReadonly() {
        return fullTextNameReadonly;
    }

    public boolean isFullTextContent() {
        return fullTextContent;
    }

    public boolean isFullTextContentShow() {
        return fullTextContentShow;
    }

    public boolean isFullTextContentReadonly() {
        return fullTextContentReadonly;
    }

    public boolean isFullTextFields() {
        return fullTextFields;
    }

    public boolean isFullTextFieldsShow() {
        return fullTextFieldsShow;
    }

    public boolean isFullTextFieldsReadonly() {
        return fullTextFieldsReadonly;
    }
    
    
}
