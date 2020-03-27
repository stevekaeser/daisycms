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
package org.outerj.daisy.books.publisher.impl.bookmodel;

import org.outerx.daisy.x10Bookdef.SectionContainerXml;
import org.outerx.daisy.x10Bookdef.SectionDocument;

import java.util.List;
import java.util.ArrayList;

public class SectionContainerHelper implements SectionContainer {
    private List<Section> sections;

    public Section[] getSections() {
        if (sections == null)
            return new Section[0];
        else
            return sections.toArray(new Section[sections.size()]);
    }

    public void addSection(Section section) {
        if (sections == null)
            sections = new ArrayList<Section>();
        sections.add(section);
    }

    public void addXml(SectionContainerXml sectionContainerXml) {
        if (sections == null)
            return;

        Section[] sections = getSections();
        SectionDocument.Section[] sectionsXml = new SectionDocument.Section[sections.length];
        for (int i = 0; i < sections.length; i++) {
            sectionsXml[i] = sections[i].getXml();
        }
        sectionContainerXml.setSectionArray(sectionsXml);
    }

}
