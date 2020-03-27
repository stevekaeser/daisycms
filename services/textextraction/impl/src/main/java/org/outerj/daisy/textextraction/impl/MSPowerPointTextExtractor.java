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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.outerj.daisy.textextraction.impl;

import org.apache.poi.util.LittleEndian;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.outerj.daisy.textextraction.TextExtractor;
import org.outerj.daisy.plugin.PluginRegistry;

import java.io.*;
import java.util.List;

/**
 * Text extractor for Microsoft PowerPoint files.
 */
public class MSPowerPointTextExtractor extends AbstractTextExtractor implements TextExtractor {

    public MSPowerPointTextExtractor() {
        super();
    }

    public MSPowerPointTextExtractor(List<String> mimeTypes, PluginRegistry pluginRegistry) {
        super(mimeTypes, pluginRegistry);
    }

    protected String getName() {
        return getClass().getName();
    }

    public String getText(InputStream is) throws Exception {
        return new Extractor().getText(is);
    }

    private static class Extractor implements POIFSReaderListener {
        //private StringBuffer contentBuffer = new StringBuffer();
        private ByteArrayOutputStream writer = new ByteArrayOutputStream();

        public String getText(InputStream is) throws Exception {
               POIFSReader reader = new POIFSReader();
               reader.registerListener(this);
               reader.read(is);

               return writer.toString();
        }

        public void processPOIFSReaderEvent(POIFSReaderEvent event) {
            try {
                DocumentInputStream input = event.getStream();

                byte[] buffer = new byte[input.available()];
                input.read(buffer, 0, input.available());

                for(int i=0; i<buffer.length-20; i++) {
                    long type = LittleEndian.getUShort(buffer,i+2);
                    long size = LittleEndian.getUInt(buffer,i+4);

                    if(type==4008) {
                        writer.write(buffer, i + 4 + 1, (int)size +3);
                        i = i + 4 + 1 + (int)size -1;
                    }
                }
            }
            catch (Exception e) {

            }
        }

    }
}
