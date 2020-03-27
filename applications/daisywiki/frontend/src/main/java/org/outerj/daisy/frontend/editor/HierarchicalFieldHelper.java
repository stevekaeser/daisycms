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

import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.VariantKey;

import java.util.List;
import java.util.ArrayList;

/**
 * Helper methods related to editing hierarchical values as
 * one string, whereby the individual elements of the hierarchical
 * value are separated by slashes.
 *
 * <p>Double slash is used to escape slash when slash is used in a value.
 * Whitespace around the separator slashes is not considered to be significant
 * (so it is not possible to input values which have whitespace at the start
 * or end).
 */
public class HierarchicalFieldHelper {

    /**
     * Parses a hierarchical value inputted using the slash-separator syntax.
     */
    public static String[] parseHierarchicalInput(String input) {
        List<String> parts = new ArrayList<String>();
        StringBuilder currentPartBuffer = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '/':
                    if (i + 1 < input.length() && input.charAt(i + 1) == '/') {
                        // it is an escaped slash
                        currentPartBuffer.append(c);
                        i++; // skip next char
                    } else {
                        handlePartInput(currentPartBuffer, parts);
                    }
                    break;
                default:
                    currentPartBuffer.append(c);
            }
        }

        handlePartInput(currentPartBuffer, parts);

        return parts.toArray(new String[0]);
    }

    private static void handlePartInput(StringBuilder partInputBuffer, List<String> parts) {
        if (partInputBuffer.length() > 0) { // can be the case at the start of the string
            String part = partInputBuffer.toString().trim();
            if (part.length() > 0) // slashes separated by only whitespace are skipped
                parts.add(part);
            partInputBuffer.setLength(0);
        }
    }

    public static String[] convertHierarchyPathsToString(Object[] hierarchyPaths, ValueType valueType, Repository repository) {
        String[] strings = new String[hierarchyPaths.length];
        for (int i = 0; i < hierarchyPaths.length; i++) {
            strings[i] = convertHierarchyPathToString((HierarchyPath)hierarchyPaths[i], valueType, repository);
        }
        return strings;
    }

    public static String convertHierarchyPathToString(HierarchyPath hierarchyPath, ValueType valueType, Repository repository) {
        Object[] elements = hierarchyPath.getElements();
        StringBuilder builder = new StringBuilder();
        for (Object element : elements) {
            if (builder.length() > 0)
                builder.append(" / ");
            String value;
            if (valueType == ValueType.STRING) {
                value = (String)element;
            } else if (valueType == ValueType.LINK) {
                value = LinkFieldHelper.variantKeyToString((VariantKey)element, repository.getVariantManager());
            } else {
                throw new RuntimeException("Unexpected valuetype for hierarhical field: " + valueType);
            }
            // escape slashes with double slashes
            value = value.replaceAll("/", "//");
            builder.append(value);
        }
        return builder.toString();
    }

}
