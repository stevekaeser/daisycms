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

import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerj.daisy.books.store.BookInstance;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

public class ZipTask implements PublicationProcessTask {
    private static final int BUFFER_SIZE = 32768;

    public void run(PublicationContext context) throws Exception {
        BookInstance bookInstance = context.getBookInstance();
        // file name is fixed -- if this would ever change also check PublicationType where
        // the name gets registered with the bookInstance
        String zipName = bookInstance.getName() + "-" + context.getPublicationOutputName();
        String zipFileName = "output/" + zipName + ".zip";

        String publicationOutputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());
        String[] paths = context.getBookInstance().getDescendantPaths(publicationOutputPath + "output");
        String prefix = publicationOutputPath + "output";

        OutputStream os = null;
        InputStream is = null;
        try {
            os = context.getBookInstance().getResourceOutputStream(publicationOutputPath + zipFileName);
            ZipOutputStream zos = new ZipOutputStream(os);
            for (String path : paths) {
                String name = zipName + path.substring(prefix.length());
                ZipEntry zipEntry = new ZipEntry(name);
                zos.putNextEntry(zipEntry);
                is = bookInstance.getResource(path);
                copy(is, zos);
                is.close();
                zos.closeEntry();
            }
            zos.finish();
            zos.flush();
        } finally {
            if (is != null)
                is.close();
            if (os != null)
                os.close();
        }
    }

    private void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = is.read(buffer)) != -1) {
            os.write(buffer, 0, read);
        }
    }
}
