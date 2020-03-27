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
package org.outerj.daisy.tools.importexport.import_.variants;

import org.outerj.daisy.tools.importexport.import_.fs.ImportFile;
import org.outerj.daisy.tools.importexport.import_.ImportListener;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.model.variant.ImpExpVariants;
import org.outerj.daisy.tools.importexport.model.variant.ImpExpVariantsDexmlizer;
import org.outerj.daisy.tools.importexport.model.variant.ImpExpVariant;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.variant.*;

import java.io.InputStream;

public class VariantImporter {
    private ImportFile importFile;
    private ImportListener listener;
    private Repository repository;
    private static final String VARIANTS_FILE_PATH = "info/variants.xml";

    public static void run(ImportFile importFile, ImportListener listener, Repository repository) throws Exception {
        new VariantImporter(importFile, listener, repository).run();
    }

    private VariantImporter(ImportFile importFile, ImportListener listener, Repository repository) {
        this.importFile = importFile;
        this.listener = listener;
        this.repository = repository;
    }


    private void run() throws Exception {
        listener.startActivity("Will check variants and register if necessary.");

        if (!importFile.exists(VARIANTS_FILE_PATH)) {
            listener.info("No " + VARIANTS_FILE_PATH + " found.");
            return;
        }

        InputStream is = null;
        ImpExpVariants variants;
        try {
            is = importFile.getPath(VARIANTS_FILE_PATH).getInputStream();
            variants = ImpExpVariantsDexmlizer.fromXml(is);
        } catch (Throwable e) {
            throw new ImportExportException("Failed reading " + VARIANTS_FILE_PATH, e);
        } finally {
            if (is != null)
                try { is.close(); } catch (Exception e) { /* ignore */ }
        }

        VariantManager variantManager = repository.getVariantManager();

        // check/register branches
        {
            ImpExpVariant[] branches = variants.getBranches();
            for (int i = 0; i < branches.length; i++) {
                boolean found = true;
                try {
                    variantManager.getBranch(branches[i].getName(), false);
                } catch (BranchNotFoundException e) {
                    found = false;
                }

                if (found) {
                    listener.info("Branch " + branches[i].getName() + " available.");
                } else {
                    if (!repository.isInRole(Role.ADMINISTRATOR)) {
                        if (branches[i].isRequired()) {
                            throw new ImportExportException("Required branch " + branches[i].getName() + " does not exist, Administrator privileges needed to create it not present.");
                        } else {
                            listener.info("Branch " + branches[i].getName() + " does not exist, and Aministrator privileges to create it not present, but the branch is not required either.");
                        }
                    } else {
                        Branch branch = variantManager.createBranch(branches[i].getName());
                        branch.setDescription(branches[i].getDescription());
                        try {
                            branch.save();
                        } catch (RepositoryException e1) {
                            throw new ImportExportException("Failed to create branch " + branches[i].getName());
                        }
                        listener.info("Created branch " + branches[i].getName());
                    }
                }
            }
        }

        // check/register languages
        {
            ImpExpVariant[] languages = variants.getLanguages();
            for (int i = 0; i < languages.length; i++) {
                boolean found = true;
                try {
                    variantManager.getLanguage(languages[i].getName(), false);
                } catch (LanguageNotFoundException e) {
                    found = false;
                }

                if (found) {
                    listener.info("Language " + languages[i].getName() + " available.");
                } else {
                    if (!repository.isInRole(Role.ADMINISTRATOR)) {
                        if (languages[i].isRequired()) {
                            throw new ImportExportException("Required language " + languages[i].getName() + " does not exist, Administrator privileges needed to create it not present.");
                        } else {
                            listener.info("Language " + languages[i].getName() + " does not exist, and Aministrator privileges to create it not present, but the language is not required either.");
                        }
                    } else {
                        Language language = variantManager.createLanguage(languages[i].getName());
                        language.setDescription(languages[i].getDescription());
                        try {
                            language.save();
                        } catch (RepositoryException e1) {
                            throw new ImportExportException("Failed to create branch " + languages[i].getName());
                        }
                        listener.info("Created language " + languages[i].getName());
                    }
                }
            }
        }

        listener.info("Variants checking and registering done.");
    }
}
