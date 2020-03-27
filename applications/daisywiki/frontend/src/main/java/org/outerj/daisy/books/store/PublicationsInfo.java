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
package org.outerj.daisy.books.store;

import org.outerx.daisy.x10Bookstoremeta.PublicationsInfoDocument;

/**
 * An object holding information about the available publications in a book instance.
 * This is an immutable object.
 */
public final class PublicationsInfo {
    private final PublicationInfo[] infos;

    public PublicationsInfo(PublicationInfo[] infos) {
        this.infos = infos.clone();
    }

    public PublicationInfo[] getInfos() {
        return infos.clone();
    }

    public PublicationsInfoDocument getXml() {
        PublicationsInfoDocument.PublicationsInfo.PublicationInfo[] infosXml = new PublicationsInfoDocument.PublicationsInfo.PublicationInfo[infos.length];
        for (int i = 0; i < infos.length; i++) {
            infosXml[i] = infos[i].getXml();
        }

        PublicationsInfoDocument publicationsInfoDocument = PublicationsInfoDocument.Factory.newInstance();
        publicationsInfoDocument.addNewPublicationsInfo().setPublicationInfoArray(infosXml);

        return publicationsInfoDocument;
    }
}
