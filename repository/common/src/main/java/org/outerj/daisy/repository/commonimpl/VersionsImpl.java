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
package org.outerj.daisy.repository.commonimpl;

import org.outerj.daisy.repository.Versions;
import org.outerj.daisy.repository.Version;
import org.outerx.daisy.x10.VersionsDocument;
import org.outerx.daisy.x10.VersionDocument;

import java.util.ArrayList;

public class VersionsImpl implements Versions {
    private final Version[] versions;

    public VersionsImpl(Version[] versions) {
        this.versions = versions;
    }

    public Version[] getArray() {
        return versions;
    }

    public VersionsDocument getXml() {
        VersionsDocument versionsDocument = VersionsDocument.Factory.newInstance();
        VersionsDocument.Versions versionsXml = versionsDocument.addNewVersions();

        ArrayList versionsList = new ArrayList();
        for (int i = 0; i < versions.length; i++) {
            if (versions[i] != null) {
                versionsList.add(versions[i].getShallowXml().getVersion());
            }
        }

        versionsXml.setVersionArray((VersionDocument.Version[])versionsList.toArray(new VersionDocument.Version[0]));

        return versionsDocument;
    }
}
