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

// Various utility functions for usage inside flowscripts

function Daisy() {
    this.avalonHelper = cocoon.createObject(Packages.org.outerj.daisy.frontend.util.FlowAvalonHelper);
}

Daisy.INSTANCE = new Daisy();

function getDaisy() {
    return Daisy.INSTANCE;
}

/**
 * If no parameters are specified: returns the Daisy Repository object for the current user
 * (or guest user if not logged in).
 */
Daisy.prototype.getRepository = function(username, password) {
    if (arguments.length == 0) {
        var repository = this.getFrontEndContext().getRepository();
        return repository;
    } else {
        var repository = this.getFrontEndContext().getRepository(username, password);
        return repository;
    }
}

Daisy.prototype.getGuestRepository = function() {
    var repository = this.getFrontEndContext().getGuestRepository();
    return repository;
}

Daisy.prototype.getPageContext = function(repository) {
    if (repository == null)
        repository = this.getRepository();
    return this.getFrontEndContext().getPageContext(repository);
}

/**
 * Resolves an URL to a more absolute form.
 */
Daisy.prototype.resolve = function(uri) {
    var sourceResolver = null;
    var source = null;
    try {
        sourceResolver = cocoon.getComponent(Packages.org.apache.excalibur.source.SourceResolver.ROLE);
        source = sourceResolver.resolveURI(uri);
        return source.getURI();
    } finally {
        if (source != null)
            sourceResolver.release(source);
        if (sourceResolver != null)
            cocoon.releaseComponent(sourceResolver);
    }
}

/**
 * Utility code to instantiate a HTML cleaner (including caching etc).
 */
Daisy.prototype.getHTMLCleaner = function(config) {
    if (config == null)
        config = "wikidata:/resources/conf/htmlcleaner.xml";
    var source = null;
    var sourceResolver = null;
    var cacheManager = null;
    try {
        cacheManager = cocoon.getComponent(Packages.org.apache.cocoon.forms.CacheManager.ROLE);
        sourceResolver = cocoon.getComponent(Packages.org.apache.excalibur.source.SourceResolver.ROLE);
        source = sourceResolver.resolveURI(config);

        var prefix = "org.outerj.daisy.htmlcleaner.HtmlCleanerTemplate";
        var template = cacheManager.get(source, prefix);
        if (template == null) {
            var factory = new Packages.org.outerj.daisy.htmlcleaner.HtmlCleanerFactory();
            var is = Packages.org.apache.cocoon.components.source.SourceUtil.getInputSource(source);
            template = factory.buildTemplate(is);
            cacheManager.set(template, source, prefix);
        }

        return template.newHtmlCleaner();
    } finally {
        if (source != null)
            sourceResolver.release(source);
        if (sourceResolver != null)
            cocoon.releaseComponent(sourceResolver);
        if (cacheManager != null)
            cocoon.releaseComponent(cacheManager);
    }
}

/**
 * Performs a publisher request, based on a request loaded from a cocoon pipe,
 * and processed preparedDocuments in the result to apply document type specific styling.
 */
Daisy.prototype.performPublisherRequest = function(pipe, params, publishType, repository, stylesheetProvider) {
    var wikiPublisherHelper;
    var displayContext = null;

    if (repository != null) {
        wikiPublisherHelper = Packages.org.outerj.daisy.frontend.WikiPublisherHelper(repository, cocoon.request,
            this.avalonHelper.getContext(), this.avalonHelper.getServiceManager());
    } else {
        wikiPublisherHelper = Packages.org.outerj.daisy.frontend.WikiPublisherHelper(cocoon.request,
            this.avalonHelper.getContext(), this.avalonHelper.getServiceManager());
    }
    if (stylesheetProvider) {
        return wikiPublisherHelper.performPublisherRequest(pipe, params, publishType, stylesheetProvider);
	} else {
        return wikiPublisherHelper.performPublisherRequest(pipe, params, publishType);
    }
}

Daisy.prototype.buildPublisherRequest = function(pipe, params) {
    var wikiPublisherHelper = Packages.org.outerj.daisy.frontend.WikiPublisherHelper(cocoon.request,
        this.avalonHelper.getContext(), this.avalonHelper.getServiceManager());
    return wikiPublisherHelper.buildPublisherRequest(pipe, params);
}

Daisy.prototype.getSiteConf = function() {
    return this.getFrontEndContext().getSiteConf();
}

Daisy.prototype.getMountPoint = function() {
    return this.getFrontEndContext().getMountPoint();
}

Daisy.prototype.getDaisyContextPath = function() {
    return this.getFrontEndContext().getDaisyContextPath();
}

Daisy.prototype.getDaisyCocoonPath = function() {
    return this.getFrontEndContext().getDaisyCocoonPath();
}

Daisy.prototype.getLocale = function() {
    return this.getFrontEndContext().getLocale();
}

Daisy.prototype.getLocaleAsString = function() {
    return this.getFrontEndContext().getLocaleAsString();
}

Daisy.prototype.getVersionMode = function() {
    return this.getFrontEndContext().getVersionMode();
}

Daisy.prototype.getFrontEndContext = function() {
    return Packages.org.outerj.daisy.frontend.FrontEndContext.get(cocoon.request);
}
