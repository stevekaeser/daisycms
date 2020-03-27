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
package org.outerj.daisy.books.publisher.impl.publicationprocess;

import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceResolver;
import org.apache.excalibur.source.impl.FileSource;
import org.apache.avalon.framework.service.ServiceManager;
import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerj.daisy.frontend.components.wikidatasource.WikiDataSource;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;

public class CopyResourceTask implements PublicationProcessTask {
    private final String fromPath;
    private final String baseToPath;

    public CopyResourceTask(String fromPath, String baseToPath) {
        this.fromPath = fromPath;
        this.baseToPath = baseToPath == null ? "" : baseToPath;
    }

    public void run(PublicationContext context) throws Exception {
        File fromFile = getFromFile(context);
        context.getPublicationLog().info("Running copy resource task, copy from = " + fromPath + " ( " + fromFile.getAbsolutePath() + " )");
        String outputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());        
        String toPath = outputPath + baseToPath;
        
        if (fromFile.isDirectory()) {
            copyRecursive(fromFile, fromFile.getCanonicalPath(), toPath, context.getBookInstance());
        } else {
            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(fromFile));
                context.getBookInstance().storeResource(outputPath + baseToPath, is);
            } finally {
                if (is != null)
                    is.close();
            }
        }
    }

    private void copyRecursive(File dir, String publicationTypePath, String toPath, BookInstance bookInstance) throws Exception {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                copyRecursive(file, publicationTypePath, toPath, bookInstance);
            } else {
                InputStream is = null;
                try {
                    String path = toPath + "/" + file.getCanonicalPath().substring(publicationTypePath.length());
                    is = new BufferedInputStream(new FileInputStream(file));
                    bookInstance.storeResource(path, is);
                } finally {
                    if (is != null)
                        is.close();
                }
            }
        }
    }

    private File getFromFile(PublicationContext context) throws Exception {
        ServiceManager serviceManager = context.getServiceManager();
        SourceResolver sourceResolver = null;
        Source source = null;
        try {
            sourceResolver = (SourceResolver)serviceManager.lookup(SourceResolver.ROLE);
            source = sourceResolver.resolveURI(fromPath);
            if (source instanceof WikiDataSource)
                return ((WikiDataSource)source).getFile();
            else if (source instanceof FileSource )
                return ((FileSource)source).getFile();
            else
                throw new Exception("Expected a WikiDataSource or FileSource for the publication type directory.");
            
        } finally {
            if (source != null)
                sourceResolver.release(source);
            if (sourceResolver != null)
                serviceManager.release(sourceResolver);
        }
    }
}
