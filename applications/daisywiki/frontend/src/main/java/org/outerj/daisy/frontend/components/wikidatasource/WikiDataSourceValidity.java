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
package org.outerj.daisy.frontend.components.wikidatasource;

import org.apache.excalibur.source.SourceValidity;

import java.io.File;

public class WikiDataSourceValidity implements SourceValidity {
    private File file;
    private long lastModified;

    public WikiDataSourceValidity(File file) {
        this.file = file;
        this.lastModified = file.lastModified();
    }

    public int isValid() {
        return 0;
    }

    public int isValid(SourceValidity sourceValidity) {
        if (sourceValidity instanceof WikiDataSourceValidity) {
            WikiDataSourceValidity newValidity = (WikiDataSourceValidity)sourceValidity;
            if (newValidity.file.equals(file) && lastModified == file.lastModified())
                return SourceValidity.VALID;
            else
                return SourceValidity.INVALID;
        }
        return SourceValidity.INVALID;
    }
}
