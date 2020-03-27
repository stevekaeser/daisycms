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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceException;
import org.apache.excalibur.source.SourceNotFoundException;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.TraversableSource;

/**
 * See {@link WikiDataSourceFactory}.
 */
public class WikiDataSource implements Source, TraversableSource {
    private File actualFile;
    private File file;
    private File fallbackFile;
    private String uri;

    public WikiDataSource(File file, File fallbackFile, String uri) {
        this.file = file;
        this.fallbackFile = fallbackFile;
        this.uri = uri;
        determineActualFile();
    }

    private void determineActualFile() {
        this.actualFile = file.exists() ? file: fallbackFile;
    }

    public boolean exists() {
        return getFile().exists();
    }

    public InputStream getInputStream() throws IOException, SourceNotFoundException {
        try {
            return new FileInputStream(getFile());           
        } catch (FileNotFoundException fnfe) {
            throw new SourceNotFoundException(uri + " doesn't exist.", fnfe);
        }
    }

    public String getURI() {
        return uri;
    }

    public String getScheme() {
        return "wikidata";
    }

    public SourceValidity getValidity() {
        return new WikiDataSourceValidity(file);
    }

    public void refresh() {
        determineActualFile();
    }

    public String getMimeType() {
        // file and fallback file have always the same name
        return URLConnection.getFileNameMap().getContentTypeFor(getFile().getName());
    }

    public long getContentLength() {
        return getFile().length();
    }

    public long getLastModified() {
        return getFile().lastModified();
    }

    public Source getChild(String child) throws SourceException {
        return new WikiDataSource(new File(file, child), new File(fallbackFile, child), uri + "/" + child);
    }

    public Collection getChildren() throws SourceException {
        Set<String> children = new HashSet<String>();
        children.addAll(Arrays.asList(file.list()));
        children.addAll(Arrays.asList(fallbackFile.list()));
        List<WikiDataSource> sources = new ArrayList<WikiDataSource>(children.size());
        for (String child : children) {
            sources.add(new WikiDataSource(new File(file, child), new File(fallbackFile, child), uri + "/" + child));
        }
        return sources;
    }

    public String getName() {
        return getFile().getName();
    }

    public Source getParent() throws SourceException {
        // TODO we could check that we don't go above the "root", i.e. outside the wikidata directory
        //      though this doesn't matter much as its currently not used
        return new WikiDataSource(file.getParentFile(), fallbackFile.getParentFile(), uri.substring(0, uri.lastIndexOf("/")));
    }

    public boolean isCollection() {
        return getFile().isDirectory();
    }
    
    public File getFile () {                
        return actualFile;
    }
}
