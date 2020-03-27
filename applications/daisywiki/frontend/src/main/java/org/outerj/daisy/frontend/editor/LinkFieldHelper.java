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
package org.outerj.daisy.frontend.editor;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.variant.BranchNotFoundException;
import org.outerj.daisy.repository.variant.LanguageNotFoundException;
import org.outerj.daisy.util.Constants;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.MultiValueField;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.validation.ValidationErrorAware;

import java.util.regex.Matcher;

/**
 * Utility methods for textual editing of link-type fields.
 */
public class LinkFieldHelper {
    public static VariantKey parseVariantKey(String link, VariantManager variantManager) {
        VersionKey versionKey = LinkFieldHelper.parseVersionKey(link, variantManager);
        return new VariantKey (versionKey.getDocumentId(), versionKey.getBranchId(), versionKey.getLanguageId());        
    }
    
    public static VersionKey parseVersionKey(String link, VariantManager variantManager) {
        Matcher matcher = Constants.DAISY_LINK_PATTERN.matcher(link);
        if (!matcher.matches())
            throw new IllegalArgumentException("Invalid link: " + link);

        String documentId = matcher.group(1);
        String branchInput = matcher.group(2);
        String languageInput = matcher.group(3);
        String versionInput = matcher.group(4);
        long branchId, languageId, versionId;

        if (branchInput != null && branchInput.length() > 0) {
            try {
                branchId = variantManager.getBranch(branchInput, false).getId();
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        } else {
            branchId = -1;
        }

        if (languageInput != null && languageInput.length() > 0) {
            try {
                languageId = variantManager.getLanguage(languageInput, false).getId();
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        } else {
            languageId = -1;
        }
        
        if (versionInput != null && versionInput.length() > 0) {
            try {
                versionId = Long.parseLong(versionInput);
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }
        } else {
            versionId = -1;
        }

        return new VersionKey(documentId, branchId, languageId, versionId);
    }

    public static String variantKeyToString(VariantKey variantKey, VariantManager variantManager) {
        StringBuilder text = new StringBuilder(20);
        text.append("daisy:");
        text.append(variantKey.getDocumentId());
        if (variantKey.getBranchId() != -1 || variantKey.getLanguageId() != -1) {
            text.append("@");
            if (variantKey.getBranchId() != -1) {
                String branchName;
                try {
                    branchName = variantManager.getBranch(variantKey.getBranchId(), false).getName();
                } catch (RepositoryException e) {
                    branchName = String.valueOf(variantKey.getBranchId());
                }
                text.append(branchName);
            }
            if (variantKey.getLanguageId() != -1) {
                text.append(":");
                String languageName;
                try {
                    languageName = variantManager.getLanguage(variantKey.getLanguageId(), false).getName();
                } catch (RepositoryException e) {
                    languageName = String.valueOf(variantKey.getLanguageId());
                }
                text.append(languageName);
            }
        }
        return text.toString();
    }
    
    
    /**
     * Converts a variant key to string representation, but does not add the branch and language if they
     * are the same as the document that is being edited.
     */
    public static String variantKeyToStringFilterBranchLanguage(VariantKey variantKey, VariantManager variantManager, long documentBranchId, long documentLanguageId) {
        StringBuilder text = new StringBuilder(20);
        text.append("daisy:");
        text.append(variantKey.getDocumentId());
        long branchId = variantKey.getBranchId() == -1 ? documentBranchId : variantKey.getBranchId();
        long languageId = variantKey.getLanguageId() == -1 ? documentLanguageId : variantKey.getLanguageId();
        if (branchId != documentBranchId || languageId != documentLanguageId) {
            text.append("@");
            if (branchId != documentBranchId) {
                String branchName;
                try {
                    branchName = variantManager.getBranch(variantKey.getBranchId(), false).getName();
                } catch (RepositoryException e) {
                    branchName = String.valueOf(variantKey.getBranchId());
                }
                text.append(branchName);
            }
            if (languageId != documentLanguageId) {
                text.append(":");
                String languageName;
                try {
                    languageName = variantManager.getLanguage(variantKey.getLanguageId(), false).getName();
                } catch (RepositoryException e) {
                    languageName = String.valueOf(variantKey.getLanguageId());
                }
                text.append(languageName);
            }
        }
        return text.toString();
    }

    public static String versionKeyToString (VersionKey versionKey, VariantManager variantManager ) {
        String variantKey = LinkFieldHelper.variantKeyToString(new VariantKey(versionKey.getDocumentId(), versionKey.getBranchId(), versionKey.getLanguageId()), variantManager);
        StringBuilder keyBuilder = new StringBuilder(variantKey);
        
        keyBuilder.append(":").append(versionKey.getVersionId());
        return keyBuilder.toString();
    }
    
    public static boolean validate(Widget widget, boolean isHierarchical, Repository repository) {
        if (widget.getValue() == null)
            return true;

        if (widget instanceof MultiValueField) {
            Object[] values = (Object[])widget.getValue();
            boolean result = true;
            for (int i = 0; i < values.length; i++) {
                result = validateLink((String)values[i], isHierarchical, (ValidationErrorAware)widget, repository);
                if (!result)
                    return result;
            }
            return result;
        } else if (widget instanceof Field) {
            return validateLink((String)widget.getValue(), isHierarchical, (ValidationErrorAware)widget, repository);
        } else {
            throw new RuntimeException("Unexpected type of widget: " + widget.getClass().getName());
        }
    }

    private static boolean validateHierarchicalLink(String link, ValidationErrorAware widget, Repository repository) {
        String[] parts = HierarchicalFieldHelper.parseHierarchicalInput(link);
        for (String part : parts) {
            boolean result = validateLink(part, false, widget, repository);
            if (!result)
                return result;
        }
        return true;
    }

    private static boolean validateLink(String link, boolean isHierarchical, ValidationErrorAware widget, Repository repository) {
        if (isHierarchical)
            return validateHierarchicalLink(link, widget, repository);

        link = link.trim();
        Matcher matcher = Constants.DAISY_LINK_PATTERN.matcher(link);
        if (matcher.matches()) {
            String docId = matcher.group(1);
            String branchInput = matcher.group(2);
            String languageInput = matcher.group(3);

            Matcher docIdMatcher = Constants.DAISY_COMPAT_DOCID_PATTERN.matcher(docId);
            if (!docIdMatcher.matches()) {
                widget.setValidationError(new ValidationError("editdoc.link-no-valid-docid", new String[] {docId}));
                return false;
            }

            // check numeric part of the document ID falls in the long boundaries
            try {
                Long.parseLong(docIdMatcher.group(1));
            } catch (NumberFormatException e) {
                widget.setValidationError(new ValidationError("editdoc.link-no-valid-docid", new String[] {docId}));
                return false;
            }

            // check the namespace of the document ID exists
            String namespace = docIdMatcher.group(2);
            if (namespace != null) {
                try {
                    repository.getNamespaceManager().getNamespace(namespace);
                } catch (NamespaceNotFoundException e) {
                    widget.setValidationError(new ValidationError("editdoc.link-no-valid-namespace", new String[] {namespace}));
                    return false;
                }
            }

            // test the specified branch and language (if any) exist
            VariantManager variantManager = repository.getVariantManager();
            if (branchInput != null && branchInput.length() > 0) {
                try {
                    variantManager.getBranch(branchInput, false);
                } catch (BranchNotFoundException e) {
                    widget.setValidationError(new ValidationError("editdoc.link-no-valid-branch", new String[] {branchInput}));
                    return false;
                } catch (RepositoryException e) {
                    widget.setValidationError(new ValidationError("Error testing branch existence: " + e.toString(), false));
                    return false;
                }
            }

            if (languageInput != null && languageInput.length() > 0) {
                try {
                    variantManager.getLanguage(languageInput, false);
                } catch (LanguageNotFoundException e) {
                    widget.setValidationError(new ValidationError("editdoc.link-no-valid-language", new String[] {languageInput}));
                    return false;
                } catch (RepositoryException e) {
                    widget.setValidationError(new ValidationError("Error testing language existence: " + e.toString()));
                    return false;
                }
            }
        } else {
            widget.setValidationError(new ValidationError("editdoc.link-not-valid", new String[] {link}));
            return false;
        }
        return true;
    }
}
