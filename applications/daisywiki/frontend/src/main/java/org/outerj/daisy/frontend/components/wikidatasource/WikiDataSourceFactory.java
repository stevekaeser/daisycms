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
package org.outerj.daisy.frontend.components.wikidatasource;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.cocoon.components.ContextHelper;
import org.apache.cocoon.environment.Request;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceFactory;
import org.outerj.daisy.frontend.util.WikiPropertiesHelper;
import org.outerj.daisy.frontend.WikiHelper;

/**
 * This is a sort of 'fallback' source: it first checks if the request source
 * exists in the wikidata directory, and otherwise falls back to the daisy
 * directory in the webapp.
 *
 * <p>Note: this should be read as wikidata-source, not as wiki-datasource.
 * It is a source called wikidata, not a datasource called wiki :-)
 */
public class WikiDataSourceFactory extends AbstractLogEnabled implements SourceFactory, Contextualizable, ThreadSafe, Initializable {

    private Context context;
    private File wikiDataDir;
    private volatile File wikiDataFallbackDir;
    private final static String FALLBACK_INDICATION = "/(webapp)";

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public void initialize() throws Exception {
        this.wikiDataDir = new File(WikiPropertiesHelper.getWikiDataDir(context)).getAbsoluteFile();
    }

    public Source getSource(String location, Map parameters) throws IOException, MalformedURLException {

        if (wikiDataFallbackDir == null) {
            synchronized(this) {
                if (wikiDataFallbackDir == null) {
                    Request request = ContextHelper.getRequest(context);
                    wikiDataFallbackDir = new File(new URL(WikiHelper.getDaisyContextPath(request)).getPath()).getAbsoluteFile();
                }
            }
        }

        String filePath = location.substring(location.indexOf(":") + 1);

        // The check on just '..' is a bit rude, but avoids needing to check both '../' and '..\'
        if (filePath.indexOf("..") != -1) {
            throw new MalformedURLException("The 'wikidata:' source does not allow relative URLs.");
        }

        File resource;
        File fallbackResource;

        // If FALLBACK_INDICATION is present in the filePath, always take the file
        // from the fallback location (the webapp), not the wikidata dir
        boolean forceFallback = filePath.startsWith(FALLBACK_INDICATION);

        if (forceFallback) {
            filePath = filePath.substring(FALLBACK_INDICATION.length());
            // twice wikiFallbackDir on purpose!
            resource = new File (wikiDataFallbackDir, filePath);
            fallbackResource = new File(wikiDataFallbackDir, filePath);
        } else {
            // the normal case
            resource = new File (wikiDataDir, filePath);
            fallbackResource = new File(wikiDataFallbackDir, filePath);
        }

        StringBuilder absolutePath = new StringBuilder(150);
        absolutePath.append("wikidata:");
        if (forceFallback)
            absolutePath.append("/(webapp)");
        absolutePath.append(filePath);

        return new WikiDataSource(resource, fallbackResource, absolutePath.toString());
    }

    public void release(Source source) {}

}
