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
package org.outerj.daisy.frontend.components.daisysource;

import org.apache.excalibur.source.SourceFactory;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.components.ContextHelper;
import org.apache.cocoon.environment.Request;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.frontend.WikiHelper;
import org.outerj.daisy.frontend.FrontEndContext;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.publisher.BlobInfo;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Cocoon source which reads its data from a part in a Daisy document. The format of the
 * source is the usual daisy link format (which is not really a valid URL given the invalid
 * use of the @ and : symbols, hope this doesn't give problems). The part type must be
 * specified by adding an "!" to the URL, followed by the name of the part type,
 * e.g. daisy:123!ImageData (branch, language and version can of course also be specified).
 */
public class DaisySourceFactory implements SourceFactory, Contextualizable, ThreadSafe {
    public static final Pattern DAISY_SOURCE_PATTERN = Pattern.compile("^daisy:([0-9]{1,19}(?:-[a-zA-Z0-9_]{1,200})?)(?:@([^:#?!]*)(?::([^:#?!]*))?(?::([^:#?!]*))?)?(?:!([^#?]*))?(?:\\?([^#]*))?(#.*)?$");
    private Context context;

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public Source getSource(String url, Map map) throws IOException {
        Matcher matcher = DAISY_SOURCE_PATTERN.matcher(url);
        if (!matcher.matches())
            throw new MalformedURLException("Invalid daisy source URL: " + url);

        Request request = ContextHelper.getRequest(context);
        FrontEndContext frontEndContext = FrontEndContext.get(request);

        try {
            Repository repository = frontEndContext.getRepository();
            SiteConf siteConf = frontEndContext.getSiteConf();
            Publisher publisher = (Publisher)repository.getExtension("Publisher");

            String documentId = matcher.group(1);
            String branch = matcher.group(2);
            long branchId = branch == null ? siteConf.getBranchId() : repository.getVariantManager().getBranch(branch, false).getId();
            String language = matcher.group(3);
            long languageId = language == null ? siteConf.getLanguageId() : repository.getVariantManager().getLanguage(language, false).getId();
            String version = matcher.group(4);
            if (version == null || version.equalsIgnoreCase("default"))
                version = frontEndContext.getVersionMode().toString();
            String partType = matcher.group(5);
            if (partType == null)
                throw new MalformedURLException("No part type specified in daisy URL: " + url);

            BlobInfo blobInfo = publisher.getBlobInfo(new VariantKey(documentId, branchId, languageId), version, partType);

            return new DaisySource(blobInfo, url);
        } catch (MalformedURLException e) {
            throw e;
        } catch (Exception e) {
            throw new SourceException("Error constructing daisy source for url " + url, e);
        }
    }

    public void release(Source source) {
        if (source instanceof DaisySource) { // should always be
            DaisySource daisySource = (DaisySource)source;
            daisySource.dispose();
        }
    }
}
