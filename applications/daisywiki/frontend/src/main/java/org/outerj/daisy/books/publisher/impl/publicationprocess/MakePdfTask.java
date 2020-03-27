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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FormattingResults;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.apps.PageSequenceResults;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerj.daisy.books.publisher.impl.util.PublicationLogAppender;

public class MakePdfTask implements PublicationProcessTask {
    private final String input;
    private final String output;
    private final String configPath;

    private FopFactory fopFactory = FopFactory.newInstance();
    
    public MakePdfTask(String input, String output, String configPath) {
        this.input = input;
        this.output = output;
        this.configPath = configPath;
    }

    public void run(PublicationContext context) throws Exception {
        InputStream is = null;
        OutputStream os = null;

        try {
            PublicationLogAppender.setPublicationLog(context.getPublicationLog());
            
            File configFile = null;
            if (configPath != null) {
                configFile = new File(configPath);
                if (!configFile.exists()) {
                    context.getPublicationLog().error("specified configPath does not point to an existing file");
                }
            } else if (System.getProperty("daisy.books.makepdf") != null) {
                configFile = new File(System.getProperty("daisy.books.makepdf"));
                if (!configFile.exists()) {
                    context.getPublicationLog().error("daisy.books.makepdf property exists but does not point to an existing file");
                }
            }
            
            if (configFile != null && configFile.exists()) {
                try {
                    context.getPublicationLog().info("Using configuration file " + configFile.getPath());
                    fopFactory.setUserConfig(configFile);
                } catch (Exception ex) {
                    context.getPublicationLog().error("Failed to load fop config file.", ex);
                }
            }

            context.getPublicationLog().info("Will begin PDF production.");
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            String pubPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());
            is = context.getBookInstance().getResource(pubPath + input);
            os = context.getBookInstance().getResourceOutputStream(pubPath + output);
            os = new BufferedOutputStream(os); // BufferedOutputStream useful here? or should that decision be made when creating the publication context?
            
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, os);

            TransformerFactory factory = SAXTransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(); // identity transformer
            
            Source src = new StreamSource(is);
            Result res = new SAXResult(fop.getDefaultHandler());
            
            transformer.transform(src, res);
            
            FormattingResults foResults = fop.getResults();
            for (PageSequenceResults pageSequenceResults: (List<PageSequenceResults>)foResults.getPageSequences()) {
                
                context.getPublicationLog().info("PageSequence " 
                        + (String.valueOf(pageSequenceResults.getID()).length() > 0 
                                ? pageSequenceResults.getID() : "<no id>") 
                        + " generated " + pageSequenceResults.getPageCount() + " pages.");
            }
            context.getPublicationLog().info("Produced a PDF containg " + foResults.getPageCount() + " pages.");

        } catch (Throwable t) {
            throw new Exception("Error calling Fop.", t);
        } finally {
            if (is != null)
                try { is.close(); } catch (Throwable e) { context.getPublicationLog().error("Error closing fo is.", e); }
            if (os != null)
                try { os.close(); } catch (Throwable e) { context.getPublicationLog().error("Error closing pdf os.", e); }
            PublicationLogAppender.setPublicationLog(null);
        }
        
        
    }

}
