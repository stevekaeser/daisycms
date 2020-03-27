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
package org.outerj.daisy.docdiff;

import org.outerj.daisy.repository.*;

public interface DocDiffOutput {
    public void begin() throws Exception;

    public void end() throws Exception;

    public void beginPartChanges() throws Exception;

    public void partRemoved(Part removedPart) throws Exception;

    public void partAdded(Part addedPart) throws Exception;

    public void partUnchanged(Part unchangedPart) throws Exception;

    /**
     * The parameters part1Data and part2Data are either both null or both have a value.
     * The have a value if the parts contain textual data and the data of both parts is different.
     */
    public void partUpdated(Part version1Part, Part version2Part, String part1Data, String part2Data) throws Exception;

    public void partMightBeUpdated(Part version2Part) throws Exception;

    public void endPartChanges() throws Exception;

    public void beginFieldChanges() throws Exception;

    public void endFieldChanges() throws Exception;

    public void fieldAdded(Field addedField) throws Exception;

    public void fieldRemoved(Field removedField) throws Exception;

    public void fieldUpdated(Field version1Field, Field version2Field) throws Exception;

    public void beginLinkChanges() throws Exception;

    public void linkRemoved(Link link) throws Exception;

    public void linkAdded(Link link) throws Exception;

    public void endLinkChanges() throws Exception;
}
