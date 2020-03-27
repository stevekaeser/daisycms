/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.frontend.components.siteconf;

import java.util.regex.Pattern;

/**
 * Simple wildcard patterns.
 *
 * <p>The syntax is:
 * <ul>
 *  <li>* is the wildcard matching multiple (or no) characters
 *  <li>? is the wildcard matching exactly one character
 *  <li>\* and \? escapes the wildcard
 *  <li>\\ escapes backslash
 * <li>backslash followed by any other character has no special meaning (thus escaping is only necessary when followed by a *)
 * </ul>
 */
public class WildcardPattern {
    private CompiledWildcardPattern compiledPattern;

    public WildcardPattern(String pattern) {
        this.compiledPattern = compile(pattern);
    }

    public boolean matches(String value) {
        return compiledPattern.matches(value);
    }

    private CompiledWildcardPattern compile(String wildcardPattern) {
        if (wildcardPattern.indexOf('*') == -1 && wildcardPattern.indexOf('?') == -1 && wildcardPattern.indexOf("\\*") == -1 && wildcardPattern.indexOf("\\\\") == -1) {
            return new ConstantWildcardPattern(wildcardPattern);
        }

        StringBuilder regexPattern = new StringBuilder();
        StringBuilder constantBuffer = new StringBuilder();

        regexPattern.append("^");

        for (int i = 0; i < wildcardPattern.length(); i++) {
            char c = wildcardPattern.charAt(i);
            switch (c) {
                case '*':
                    if (constantBuffer.length() > 0) {
                        regexPattern.append(Pattern.quote(constantBuffer.toString()));
                        constantBuffer.setLength(0);
                    }
                    regexPattern.append(".*");
                    break;
                case '?':
                    if (constantBuffer.length() > 0) {
                        regexPattern.append(Pattern.quote(constantBuffer.toString()));
                        constantBuffer.setLength(0);
                    }
                    regexPattern.append(".{1}");
                    break;
                case '\\':
                    if (i + 1 < wildcardPattern.length()) {
                        char next = wildcardPattern.charAt(++i);
                        switch (next) {
                            case '*':
                                constantBuffer.append(next);
                                break;
                            case '?':
                                constantBuffer.append(next);
                                break;
                            case '\\':
                                constantBuffer.append(next);
                                break;
                            default:
                                constantBuffer.append(c).append(next);
                        }
                    } else {
                        constantBuffer.append(c);
                    }
                    break;
                default:
                    constantBuffer.append(c);
            }
        }

        if (constantBuffer.length() > 0) {
            regexPattern.append(Pattern.quote(constantBuffer.toString()));
        }

        regexPattern.append("$");

        Pattern pattern = Pattern.compile(regexPattern.toString());
        return new RegexWildcardPattern(pattern);
    }

    private static interface CompiledWildcardPattern {
        boolean matches(String text);
    }

    private static class RegexWildcardPattern implements CompiledWildcardPattern {
        private final Pattern pattern;

        public RegexWildcardPattern(Pattern pattern) {
            this.pattern = pattern;
        }

        public boolean matches(String text) {
            return pattern.matcher(text).matches();
        }
    }

    private static class ConstantWildcardPattern implements CompiledWildcardPattern {
        private final String pattern;

        public ConstantWildcardPattern(String pattern) {
            this.pattern = pattern;
        }

        public boolean matches(String text) {
            return pattern.equals(text);
        }
    }
}
