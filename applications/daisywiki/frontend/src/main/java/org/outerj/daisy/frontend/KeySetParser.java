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
package org.outerj.daisy.frontend;

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.variant.*;
import org.outerj.daisy.emailnotifier.CollectionSubscriptionKey;
import org.outerj.daisy.util.Constants;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.util.I18nMessage;
import org.apache.cocoon.forms.validation.ValidationErrorAware;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.validation.WidgetValidator;
import org.apache.excalibur.xml.sax.XMLizable;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.IOException;

/**
 * This class provides utility methods for creating widget validators and parsing input for
 * a specific type of input whereby the entered data consists of lines following an
 * "item,branch,language" pattern. Since the same algorithm is needed in slightly different
 * ways on different locations, this class generalises all the use cases to avoid code duplication.
 */
public class KeySetParser {
    private static final Pattern LINE_PATTERN = Pattern.compile("([^,#]+)(\\s*,\\s*([a-zA-Z\\-_0-9*]+)(\\s*,\\s*([a-zA-Z\\-_0-9*]+)))\\s*(#.*)?");
    private static final Pattern LINE_PATTERN_VARIANT_OPTIONAL = Pattern.compile("([^,#]+)(\\s*,\\s*([a-zA-Z\\-_0-9*]+)(\\s*,\\s*([a-zA-Z\\-_0-9*]+))?)?\\s*(#.*)?");

    /**
     *
     * @param allowWildcards if true, then each of the components on a line can be a star (*). In this case
     *                       branch and language must be specified, while if this parameter is false branch
     *                       and language do not have to be specified and 1 will be taken as default. An input
     *                       of * will be translated to -1 when parsing.
     * @param needAtLeastOne at least one entry is needed, otherwise an error will be raised
     */
    public static VariantKey[] parseVariantKeys(String input, VariantManager variantManager, boolean allowWildcards, boolean needAtLeastOne) throws Exception {
        input = input == null ? "" : input;
        Set keys = (Set)processInput(input, variantManager, false, new VariantsCustomiser(), "Unexpected error in variants list: ", allowWildcards, needAtLeastOne);
        return (VariantKey[])keys.toArray(new VariantKey[keys.size()]);
    }

    public static CollectionSubscriptionKey[] parseCollectionKeys(String input, Repository repository) throws Exception {
        input = input == null ? "" : input;
        Set keys = (Set)processInput(input, repository.getVariantManager(), false, new CollectionsCustomiser(repository.getCollectionManager()), "Unexpected error in collection subscription list: ", true, false);
        return (CollectionSubscriptionKey[])keys.toArray(new CollectionSubscriptionKey[keys.size()]);
    }
    
    public static WidgetValidator getVariantKeysWidgetValidator(VariantManager variantManager, boolean allowWildcards, boolean needAtLeastOne) {
        return new KeyListValidator(variantManager, new VariantsCustomiser(), allowWildcards, needAtLeastOne);
    }

    public static WidgetValidator getCollectionSubscriptionKeysWidgetValidator(Repository repository, boolean allowWildcards, boolean needAtLeastOne) {
        return new KeyListValidator(repository.getVariantManager(),
                new CollectionsCustomiser(repository.getCollectionManager()), allowWildcards, needAtLeastOne);
    }

    /**
     * Abstraction so that the same algorithm is usable for document IDs
     * and collection IDs.
     */
    private interface KeySetParserCustomiser {
        Object parseItem(String value) throws Exception;

        Object getWildcard();

        XMLizable validateItem(String value, long lineNumber) throws Exception;

        Object instantiateObject(Object item, long branchId, long languageId);
    }

    private static class VariantsCustomiser implements KeySetParserCustomiser {
        public Object parseItem(String value) throws Exception {
            Matcher matcher = Constants.DAISY_COMPAT_DOCID_PATTERN.matcher(value);
            if (matcher.matches())
                return value;
            else
                throw new Exception("invalid document ID: " + value);
        }

        public Object getWildcard() {
            return "*";
        }

        public XMLizable validateItem(String value, long lineNumber) {
            Matcher matcher = Constants.DAISY_COMPAT_DOCID_PATTERN.matcher(value);
            if (matcher.matches())
                return null;
            else
                return new I18nMessage("keylist.invalid-docid", new String[] { String.valueOf(lineNumber)});
        }

        public Object instantiateObject(Object item, long branchId, long languageId) {
            return new VariantKey((String)item, branchId, languageId);
        }
    }

    private static class CollectionsCustomiser implements KeySetParserCustomiser {
        private final CollectionManager collectionManager;

        public CollectionsCustomiser(CollectionManager collectionManager) {
            this.collectionManager = collectionManager;
        }

        public Object getWildcard() {
            throw new IllegalStateException("Wildcards not allowed for collections.");
        }

        public Object parseItem(String value) throws Exception {
            return new Long(collectionManager.getCollection(value, false).getId());
        }

        public XMLizable validateItem(String value, long lineNumber) {
            try {
                collectionManager.getCollection(value, false).getId();
                return null;
            } catch (CollectionNotFoundException e) {
                return new I18nMessage("keylist.collection-does-not-exist", new String[] { String.valueOf(lineNumber)});
            } catch (RepositoryException e) {
                return new I18nMessage("keylist.collection-retrieval-error", new String[] { String.valueOf(lineNumber), e.toString()});
            }
        }

        public Object instantiateObject(Object item, long branchId, long languageId) {
            return new CollectionSubscriptionKey(((Long)item).longValue(), branchId, languageId);
        }
    }

    /**
     * 
     * @param allowWildcards if true, '*' is allowed instead of IDs or names (-1 will be put in the resulting ID),
     *                       and in this case there will also be no variant defaulting (i.e. all 3 parts on each
     *                       line are required)
     */ 
    private static Object processInput(String input, VariantManager variantManager, boolean validateMode,
            KeySetParserCustomiser customiser, String exceptionPrefix, boolean allowWildcards,
            boolean needsAtLeastOneEntry) throws Exception {
        
        LineNumberReader reader = new LineNumberReader(new StringReader(input));

        // the following variables are for when we're working in validate mode
        XMLizable error = null;
        boolean hasLines = false;
        // the following variable is for when we're working in parse mode
        HashSet keys = validateMode ? null : new HashSet();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && !(line.charAt(0) == '#')) {
                    Pattern pattern = allowWildcards ? LINE_PATTERN : LINE_PATTERN_VARIANT_OPTIONAL;
                    Matcher matcher = pattern.matcher(line);
                    if (!matcher.matches()) {
                        if (validateMode) {
                            error = new I18nMessage("keylist.entry-invalid", new String[] { String.valueOf(reader.getLineNumber())});
                            break;
                        } else {
                            throw new Exception(exceptionPrefix + "entry on line " + reader.getLineNumber() + " is not in the correct format.");
                        }
                    } else {
                        Object itemId = null;
                        String itemString = matcher.group(1);
                        if (allowWildcards && itemString.equals("*")) {
                            itemId = customiser.getWildcard();
                        } else {
                            if (validateMode) {
                                error = customiser.validateItem(matcher.group(1), reader.getLineNumber());
                                if (error != null)
                                    break;
                            } else {
                                try {
                                    itemId = customiser.parseItem(matcher.group(1));
                                } catch (Exception e) {
                                    throw new Exception(exceptionPrefix + "entry on line " + reader.getLineNumber() + " is not valid: " + e.toString());
                                }
                            }
                        }
                        long branchId = allowWildcards ? -1 : Branch.MAIN_BRANCH_ID;
                        long languageId = allowWildcards ? -1 : Language.DEFAULT_LANGUAGE_ID;
                        String branch = matcher.group(3);
                        if (!(allowWildcards && branch.equals("*"))) {
                            if (branch != null) {
                                try {
                                    branchId = variantManager.getBranch(branch, false).getId();
                                } catch (BranchNotFoundException e) {
                                    if (validateMode) {
                                        error = new I18nMessage("keylist.branch-does-not-exist", new String[] { String.valueOf(reader.getLineNumber())});
                                        break;
                                    } else {
                                        throw new Exception(exceptionPrefix + "branch specified on line " + reader.getLineNumber() + " does not exist.");
                                    }
                                } catch (RepositoryException e) {
                                    if (validateMode) {
                                        error = new I18nMessage("keylist.branch-retrieval-error", new String[] { String.valueOf(reader.getLineNumber()), e.getMessage()});
                                        break;
                                    } else {
                                        throw new Exception(exceptionPrefix + "problem getting branch specified on line " + reader.getLineNumber() + ": " + e.getMessage());
                                    }
                                }
                            }
                        }
                        String language = matcher.group(5);
                        if (!(allowWildcards && language.equals("*"))) {
                            if (language != null) {
                                try {
                                    languageId = variantManager.getLanguage(language, false).getId();
                                } catch (LanguageNotFoundException e) {
                                    if (validateMode) {
                                        error = new I18nMessage("keylist.lang-does-not-exist", new String[] { String.valueOf(reader.getLineNumber())});
                                        break;
                                    } else {
                                        throw new Exception(exceptionPrefix + "language specified on line " + reader.getLineNumber() + " does not exist.");
                                    }
                                } catch (RepositoryException e) {
                                    if (validateMode) {
                                        error = new I18nMessage("keylist.lang-retrieval-error", new String[] { String.valueOf(reader.getLineNumber()), e.getMessage()});
                                        break;
                                    } else {
                                        throw new Exception(exceptionPrefix + "problem getting language specified on line " + reader.getLineNumber() + ": " + e.getMessage());
                                    }
                                }
                            }
                        }
                        if (validateMode) {
                            if (!hasLines)
                                hasLines = true;
                        } else {
                            keys.add(customiser.instantiateObject(itemId, branchId, languageId));
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new Exception(exceptionPrefix + "highly unexpected IOException.", e);
        }

        if (validateMode) {
            if (error == null && !hasLines && needsAtLeastOneEntry)
                error = new I18nMessage("keylist.need-at-least-one-entry");

            return error;
        } else {
            return keys;
        }
    }
    
    private static class KeyListValidator implements WidgetValidator {
        private final VariantManager variantManager;
        private final KeySetParserCustomiser customiser;
        private final boolean allowWildcards;
        private final boolean needAtLeastOne;

        public KeyListValidator(VariantManager variantManager, KeySetParserCustomiser customiser, boolean allowWildcards,
                boolean needAtLeastOne) {
            this.variantManager = variantManager;
            this.customiser = customiser;
            this.allowWildcards = allowWildcards;
            this.needAtLeastOne = needAtLeastOne;
        }
        
        public boolean validate(Widget widget) {
            String input = (String)widget.getValue();
            if (input != null) {
                XMLizable error;
                try {
                    error = (XMLizable)processInput(input, variantManager, true, customiser, null, allowWildcards, needAtLeastOne);
                } catch (Exception e) {
                    throw new RuntimeException("Unexpected error during keylist validation: " + e.toString(), e);
                }
                if (error != null) {
                    ((ValidationErrorAware)widget).setValidationError(new ValidationError(error));
                    return false;
                }
            }
            return true;
        }
    }
}
