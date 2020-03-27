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
package org.outerj.daisy.frontend;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Locale;
import java.util.Properties;

import org.apache.cocoon.environment.Request;
import org.apache.cocoon.xml.AttributesImpl;
import org.apache.cocoon.xml.SaxBuffer;
import org.apache.excalibur.xml.sax.XMLizable;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.util.PublisherRequestHelper;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.util.VersionHelper;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * An object holding useful contextual information to be passed to the view layer
 * for each page. Since it implements XMLizable, it is directly streamable as XML in templates.
 */
public class PageContext implements XMLizable {
    private String mountPoint;
    private SiteConf siteConf;
    private Repository repository;
    private String layoutType;
    private String skin;
    private SaxBuffer skinConf;
    private VersionMode versionMode;
    private FrontEndContext frontEndContext;
    private Locale locale;
    private String localeAsString;
    private String language;
    private Request request;
    private String requestURI;
    private String requestMethod;
    private String requestServer;
    private static final Attributes EMPTY_ATTRIBUTES = new AttributesImpl();
    private static final Attributes versionAttributes;
    static {
        AttributesImpl attrs = new AttributesImpl();

        Properties versionProps;
        try {
            versionProps = VersionHelper.getVersionProperties(SetVersionHeaderAction.class.getClassLoader(),
                    "org/outerj/daisy/frontend/versioninfo.properties");
        } catch (IOException e) {
            throw new RuntimeException("Could not load Daisy Wiki version information.");
        }

        attrs.addAttribute("", "version", "version", "CDATA", versionProps.getProperty("artifact.version"));
        attrs.addAttribute("", "buildHostName", "buildHostName", "CDATA", versionProps.getProperty("build.hostname"));
        attrs.addAttribute("", "buildDateTime", "buildDateTime", "CDATA", versionProps.getProperty("build.datetime"));

        versionAttributes = attrs;
    }

    public PageContext(Request request) {
        init(null, null, request);
    }

    public PageContext(Repository repository, String layoutType, Request request) {
        init(repository, layoutType, request);
    }

    private void init(Repository repository, String layoutType, Request request) {
        // Note: this init code is written with the intent that it won't
        // fail easily, even in case of errors (e.g. can't get repository etc.)

        frontEndContext = FrontEndContext.get(request);
        
        this.mountPoint = frontEndContext.getMountPoint();

        if (layoutType == null)
            layoutType = frontEndContext.getLayoutType();
        this.layoutType = layoutType == null ? "default" : layoutType;

        if (repository == null) {
            try {
                this.repository = frontEndContext.getRepository();
            } catch (Throwable e) {
                // continue without repository
            }
        } else {
            this.repository = repository;
        }

        if (frontEndContext.inSite())
            this.siteConf = frontEndContext.getSiteConf();

        if (frontEndContext.isSkinSet())
            this.skin = frontEndContext.getSkin();

        try {
            this.skinConf = frontEndContext.inSite() ? frontEndContext.getSiteConf().getSkinConf() : frontEndContext.getGlobalSkinConf();
        } catch (Throwable e) {
            // continue without skinconf
        }

        this.versionMode = frontEndContext.getVersionMode();

        this.locale = frontEndContext.getLocale();
        this.localeAsString = frontEndContext.getLocaleAsString();
        this.language = frontEndContext.getLocale().getLanguage();
        
        this.request = request;
        requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null) {
            requestURI = requestURI + "?" + queryString;
        }
        requestMethod = request.getMethod();
        requestServer = RequestUtil.getServer(request);
    }

    public String getMountPoint() {
        return mountPoint;
    }

    public SiteConf getSiteConf() {
        return siteConf;
    }

    public Repository getRepository() {
        return repository;
    }

    public String getSkin() {
        return skin;
    }

    public String getLayoutType() {
        return layoutType;
    }

    public String getVersionMode() {
        return versionMode.toString();
    }
    
    public String getLocalVersionMode() {
        String modeString = versionMode.toString();
        if (versionMode.isLast() || versionMode.isLive()) {
            return modeString;
        } else {
            if (modeString.indexOf("T") < 0) {
                return DateFormat.getDateInstance(DateFormat.SHORT, locale).format(versionMode.getDate());
            } else {
                return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale).format(versionMode.getDate());
            }
        }
    }

    public void toSAX(ContentHandler contentHandler) throws SAXException {
        // Note: this code is written such that it doesn't fail when any of the data
        // (repository, siteConf, ...) is null, which can be the case in
        // the error-handling pipeline. In addition, siteConf will be null for all pages
        // outside the context of site.
        try {
            contentHandler.startElement("", "context", "context", EMPTY_ATTRIBUTES);

            // version
            contentHandler.startElement("", "versionInfo", "versionInfo", versionAttributes);
            contentHandler.endElement("", "versionInfo", "versionInfo");

            // mountPoint
            if (mountPoint != null)
                generateStringElement("mountPoint", mountPoint, contentHandler);

            // version mode
            if (versionMode != null) {
                generateStringElement("versionMode", versionMode.toString(), contentHandler);
                generateStringElement("localVersionMode", getLocalVersionMode(), contentHandler);
            }

            // site
            if (siteConf != null && repository != null) {
                AttributesImpl siteAttrs = new AttributesImpl();
                siteAttrs.addCDATAAttribute("name", siteConf.getName());
                siteAttrs.addCDATAAttribute("title", siteConf.getTitle());
                siteAttrs.addCDATAAttribute("description", siteConf.getDescription());
                siteAttrs.addCDATAAttribute("navigationDocId", String.valueOf(siteConf.getNavigationDocId()));
                siteAttrs.addCDATAAttribute("collectionId", String.valueOf(siteConf.getCollectionId()));
                siteAttrs.addCDATAAttribute("collection", repository.getCollectionManager().getCollection(siteConf.getCollectionId(), false).getName());
                siteAttrs.addCDATAAttribute("branchId", String.valueOf(siteConf.getBranchId()));
                VariantManager variantManager = repository.getVariantManager();
                siteAttrs.addCDATAAttribute("branch", variantManager.getBranch(siteConf.getBranchId(), false).getName());
                siteAttrs.addCDATAAttribute("languageId", String.valueOf(siteConf.getLanguageId()));
                siteAttrs.addCDATAAttribute("language", variantManager.getLanguage(siteConf.getLanguageId(), false).getName());
                siteAttrs.addCDATAAttribute("publisherRequestSet", siteConf.getPublisherRequestSet());

                contentHandler.startElement("", "site", "site", siteAttrs);
                contentHandler.endElement("", "site", "site");
            }

            SaxBuffer skinConf;
            if (siteConf != null)
                skinConf = siteConf.getSkinConf();
            else
                skinConf = this.skinConf;
            if (skinConf != null)
                skinConf.toSAX(contentHandler);

            // user
            if (repository != null) {
                UserInfoStreamer.streamUserInfo(repository, contentHandler);
            }

            generateStringElement("locale", localeAsString, contentHandler);
            generateStringElement("language", language, contentHandler);
            generateStringElement("layoutType", layoutType, contentHandler);

            if (request != null) {
                AttributesImpl requestAttrs = new AttributesImpl();
                requestAttrs.addAttribute("", "uri", "uri", "CDATA", requestURI);
                requestAttrs.addAttribute("", "method", "method", "CDATA", requestMethod);
                requestAttrs.addAttribute("", "server", "server", "CDATA", requestServer);
                contentHandler.startElement("", "request", "request", requestAttrs);
                contentHandler.endElement("", "request", "request");
            }

            if (skin != null)
                generateStringElement("skin", skin, contentHandler);

            contentHandler.endElement("", "context", "context");
        } catch (RepositoryException e) {
            throw new SAXException("Error in PageContext.toSAX", e);
        }
    }

    private void generateStringElement(String name, String value, ContentHandler contentHandler) throws SAXException {
        contentHandler.startElement("", name, name, EMPTY_ATTRIBUTES);
        contentHandler.characters(value.toCharArray(), 0, value.length());
        contentHandler.endElement("", name, name);
    }

    public String getRequestURI() {
        return requestURI;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public String getRequestServer() {
        return requestServer;
    }
    
    public String getLocaleAsString() {
        return localeAsString;
    }

    public String getLanguage() {
        return language;
    }

    public XMLizable getPublisherVariablesConfig() {
        return new XmlObjectXMLizable(PublisherRequestHelper.getVariablesConfig(frontEndContext), true);
    }
}
