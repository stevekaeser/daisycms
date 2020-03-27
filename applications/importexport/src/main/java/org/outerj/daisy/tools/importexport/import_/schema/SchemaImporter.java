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
package org.outerj.daisy.tools.importexport.import_.schema;

import org.outerj.daisy.tools.importexport.import_.fs.ImportFile;
import org.outerj.daisy.tools.importexport.import_.ImportListener;
import org.outerj.daisy.tools.importexport.import_.config.ImportOptions;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.model.schema.ImpExpSchemaDexmlizer;
import org.outerj.daisy.tools.importexport.model.schema.ImpExpSchema;
import org.outerj.daisy.repository.Repository;

import java.io.InputStream;

/**
 * Uses the {@link SchemaLoader} to load a schema from an import file in the repository.
 */
public class SchemaImporter {
    private ImportFile importFile;
    private ImportListener listener;
    private Repository repository;
    private ImportOptions options;
    private static final String SCHEMA_FILE_PATH = "info/schema.xml";

    public static void run(ImportFile importFile, ImportOptions options, ImportListener listener, Repository repository) throws ImportExportException {
        new SchemaImporter(importFile, options, listener, repository).run();
    }

    private SchemaImporter(ImportFile importFile, ImportOptions options, ImportListener listener, Repository repository) {
        this.importFile = importFile;
        this.options = options;
        this.listener = listener;
        this.repository = repository;
    }

    private void run() throws ImportExportException {
        listener.startActivity("Will check/register required schema types.");

        if (!importFile.exists(SCHEMA_FILE_PATH)) {
            listener.info("No " + SCHEMA_FILE_PATH + " found.");
            return;
        }

        ImpExpSchema schema;
        InputStream is = null;
        try {
            is = importFile.getPath(SCHEMA_FILE_PATH).getInputStream();
            schema = ImpExpSchemaDexmlizer.fromXml(is, repository, new ImpExpSchemaDexmlizer.Listener() {
                public void info(String message) {
                    listener.info(message);
                }
            });
        } catch (Exception e) {
            throw new ImportExportException("Error parsing schema.", e);
        } finally {
            if (is != null)
                try { is.close(); } catch (Exception e) { /* ignore */ }
        }

        options.getSchemaCustomizer().customize(schema);

        try {
            SchemaLoader.load(schema, repository, options.getSchemaCreateOnly(), options.getSchemaClearLocalizedData(),
                    listener.getSchemaListener());
        } catch (Exception e) {
            throw new ImportExportException("Error importing schema.", e);
        } finally {
            listener.getSchemaListener().done();
        }
        listener.info("Schema types checking and registration done.");
    }

}
