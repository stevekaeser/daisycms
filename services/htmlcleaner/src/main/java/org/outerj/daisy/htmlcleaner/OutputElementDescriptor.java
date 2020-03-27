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
package org.outerj.daisy.htmlcleaner;

class OutputElementDescriptor {
    public final int newLinesBeforeOpenTag;
    public final int newLinesAfterOpenTag;
    public final int newLinesBeforeCloseTag;
    public final int newLinesAfterCloseTag;
    public final boolean inline;

    public OutputElementDescriptor(int newLinesBeforeOpenTag, int newLinesAfterOpenTag, int newLinesBeforeCloseTag,
            int newLinesAfterCloseTag, boolean inline) {
        this.newLinesBeforeOpenTag = newLinesBeforeOpenTag;
        this.newLinesAfterOpenTag = newLinesAfterOpenTag;
        this.newLinesBeforeCloseTag = newLinesBeforeCloseTag;
        this.newLinesAfterCloseTag = newLinesAfterCloseTag;
        this.inline = inline;
    }

    public int getNewLinesBeforeOpenTag() {
        return newLinesBeforeOpenTag;
    }

    public int getNewLinesAfterOpenTag() {
        return newLinesAfterOpenTag;
    }

    public int getNewLinesBeforeCloseTag() {
        return newLinesBeforeCloseTag;
    }

    public int getNewLinesAfterCloseTag() {
        return newLinesAfterCloseTag;
    }

    public boolean isInline() {
        return inline;
    }
}
