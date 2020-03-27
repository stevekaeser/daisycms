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

import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerj.daisy.repository.LocaleHelper;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.HashMap;

public class ApplyPipelineTask implements PublicationProcessTask {
    private final String input;
    private final String output;
    private final String pipe;

    public ApplyPipelineTask(String input, String output, String pipe) {
        this.input = input;
        this.output = output;
        this.pipe = pipe;
    }

    public void run(PublicationContext context) throws Exception {
        context.getPublicationLog().info("Running apply pipeline task.");
        String publicationOutputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());
        String startXmlLocation = publicationOutputPath + input;
        applyPipeline(context, pipe, startXmlLocation, publicationOutputPath + output);
    }

    /**
     *
     * @param input absolute path within the book instance
     * @param output absolute path within the book instance
     */
    public static void applyPipeline(PublicationContext context, String pipe, String input, String output) throws Exception {
        BookInstance bookInstance = context.getBookInstance();
        InputStream is = null;
        OutputStream os = null;
        try {
            is = bookInstance.getResource(input);
            os = bookInstance.getResourceOutputStream(output);
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("bookXmlInputStream", is);
            params.put("bookInstanceName", bookInstance.getName());
            params.put("bookInstance", bookInstance);
            params.put("locale", context.getLocale());
            params.put("pubProps", context.getProperties());
            params.put("bookMetadata", context.getBookMetadata());
            params.put("localeAsString", LocaleHelper.getString(context.getLocale()));
            params.put("publicationTypeName", context.getPublicationTypeName());
            params.put("publicationOutputName", context.getPublicationOutputName());
            PublicationProcessTaskUtil.executePipeline(context.getDaisyCocoonPath() + "/books/publicationTypes/" + context.getPublicationTypeName() + "/" + pipe, params, os, context);
        } finally {
            if (is != null)
                try { is.close(); } catch (Exception e) {}
            if (os != null)
                try { os.close(); } catch (Exception e) {}
        }
    }
}
