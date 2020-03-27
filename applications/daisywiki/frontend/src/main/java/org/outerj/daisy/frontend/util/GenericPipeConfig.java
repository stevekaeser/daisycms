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
package org.outerj.daisy.frontend.util;

/**
 * Contains configuration information for the generic pipeline (internal/genericPipe)
 * which is shared for displaying various pages.
 */
public class GenericPipeConfig {
    private String template = "resources/xml/page.xml";
    private boolean applyI18n = true;
    private boolean applyLayout = true;
    private String stylesheet = null;
    private String serializer = "html";
    private boolean transformLinks = false;
    private boolean stripNamespaces = false;
    private boolean applyExternalInclude = true;

    public GenericPipeConfig() {

    }

    public static GenericPipeConfig templatePipe(String template) {
        GenericPipeConfig conf = new GenericPipeConfig();
        conf.setTemplate(template);
        return conf;
    }

    public static GenericPipeConfig templateOnlyPipe(String template) {
        GenericPipeConfig conf = new GenericPipeConfig();
        conf.setTemplate(template);
        conf.setApplyLayout(false);
        return conf;
    }

    public static GenericPipeConfig stylesheetPipe(String stylesheet) {
        GenericPipeConfig conf = new GenericPipeConfig();
        conf.setStylesheet(stylesheet);
        return conf;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public void setStripNamespaces(boolean stripNamespaces) {
        this.stripNamespaces = stripNamespaces;
    }

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    public void setXmlSerializer() {
        this.serializer = "xml";
    }
    
    public void setPdfSerializer() {
        this.serializer = "pdf";
    }
    
    public void setApplyLayout(boolean applyLayout) {
        this.applyLayout = applyLayout;
    }

    public void setApplyI18n(boolean applyI18n) {
        this.applyI18n = applyI18n;
    }

    public void setApplyExternalInclude(boolean applyExternalInclude) {
        this.applyExternalInclude = applyExternalInclude;
    }

    public boolean getHasStylesheet() {
        return stylesheet != null;
    }

    public boolean getApplyI18n() {
        return applyI18n;
    }

    public boolean getApplyLayout() {
        return applyLayout;
    }

    public String getTemplate() {
        return template;
    }

    public String getSerializer() {
        return serializer;
    }

    public boolean getStripNamespaces() {
        return stripNamespaces;
    }
    
    public boolean getApplyExternalInclude() {
        return applyExternalInclude;
    }

    public String getStylesheet() {
        return stylesheet;
    }

    public boolean getTransformLinks() {
        return transformLinks;
    }

    public void setTransformLinks(boolean transformLinks) {
        this.transformLinks = transformLinks;
    }
}
