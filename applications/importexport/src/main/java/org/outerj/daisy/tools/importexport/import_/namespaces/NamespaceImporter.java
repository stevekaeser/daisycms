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
package org.outerj.daisy.tools.importexport.import_.namespaces;

import org.outerj.daisy.tools.importexport.import_.fs.ImportFile;
import org.outerj.daisy.tools.importexport.import_.ImportListener;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.model.namespace.ImpExpNamespacesDexmlizer;
import org.outerj.daisy.tools.importexport.model.namespace.ImpExpNamespaces;
import org.outerj.daisy.tools.importexport.model.namespace.ImpExpNamespace;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.namespace.NamespaceManager;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;
import org.outerj.daisy.repository.namespace.Namespace;

import java.io.InputStream;

public class NamespaceImporter {
    private ImportFile importFile;
    private ImportListener listener;
    private Repository repository;
    private static final String NAMESPACE_FILE_PATH = "info/namespaces.xml";

    public static void run(ImportFile importFile, ImportListener listener, Repository repository) throws ImportExportException {
        new NamespaceImporter(importFile, listener, repository).run();
    }

    private NamespaceImporter(ImportFile importFile, ImportListener listener, Repository repository) {
        this.importFile = importFile;
        this.listener = listener;
        this.repository = repository;
    }


    private void run() throws ImportExportException {
        listener.startActivity("Will check namespaces and register if necessary.");
        InputStream is = null;
        ImpExpNamespaces namespaces;
        try {
            is = importFile.getPath(NAMESPACE_FILE_PATH).getInputStream();
            namespaces = ImpExpNamespacesDexmlizer.fromXml(is);
        } catch (Throwable e) {
            throw new ImportExportException("Failed reading " + NAMESPACE_FILE_PATH, e);
        } finally {
            if (is != null)
                try { is.close(); } catch (Exception e) { /* ignore */ }
        }

        ImpExpNamespace[] namespacesToImport = namespaces.getNamespaces();

        NamespaceManager nsManager = repository.getNamespaceManager();
        for (ImpExpNamespace ns : namespacesToImport) {
            boolean found = true;
            try {
                Namespace namespace = nsManager.getNamespace(ns.getName());
                if (!namespace.getFingerprint().equals(ns.getFingerprint())) {
                    throw new ImportExportException("Non-matching namespace fingerprint for namespace " + ns.getName());
                } else {
                    listener.info("Namespace " + ns.getName() + " available with matching fingerprint.");
                }
            } catch (NamespaceNotFoundException e) {
                found = false;
            }

            if (!found) {
                if (!repository.isInRole(Role.ADMINISTRATOR) && ns.isRequired()) {
                    throw new ImportExportException("Need to register namespace " + ns.getName() + " but the user is not in the Administrator role.");
                } else if (repository.isInRole(Role.ADMINISTRATOR)) {
                    try {
                        nsManager.registerNamespace(ns.getName(), ns.getFingerprint());
                        listener.info("Registered namespace " + ns.getName() + " with fingerprint " + ns.getFingerprint());
                    } catch (RepositoryException e) {
                        throw new ImportExportException("Error registering namespace " + ns.getName(), e);
                    }
                }
            }
        }
        listener.info("Namespace checking and registering done.");
    }

}
