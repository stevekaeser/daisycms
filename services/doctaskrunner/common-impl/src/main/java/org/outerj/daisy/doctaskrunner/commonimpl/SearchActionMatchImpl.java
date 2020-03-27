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
package org.outerj.daisy.doctaskrunner.commonimpl;

import java.util.HashMap;
import java.util.Map;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.doctaskrunner.SearchActionMatch;
import org.outerx.daisy.x10DocumentActions.MatchesDocument;
import org.outerx.daisy.x10DocumentActions.MatchesDocument.Matches;
import org.w3c.dom.Node;

public class SearchActionMatchImpl implements SearchActionMatch {
    
    private Map<String,String> attributes = new HashMap<String, String>();
    private XmlObject fragment;
    
    public SearchActionMatchImpl() {
    }
    
    public SearchActionMatchImpl(XmlObject fragment) {
        this.fragment = fragment;
    }
    
    public void addAttributes(Map<String, String> attributes) {
        if (attributes == null)
            return;
        this.attributes.putAll(attributes);
    }
    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public void addXml(Matches matchesXml) {
        MatchesDocument.Matches.Match xml = matchesXml.addNewMatch();
        XmlCursor cursor = xml.newCursor();
        cursor.toNextToken(); // move after the START token (ready to insert attributes)
        for (String attr: attributes.keySet()) {
            cursor.insertAttributeWithValue(attr, attributes.get(attr));
        }
        cursor.dispose();
        xml.addNewFragment().set(fragment);
    }

    public Node getFragment() {
        if (fragment == null)
            return null;
        return fragment.getDomNode();
    }
    
    public void setFromXml(MatchesDocument.Matches.Match matchXml) {
        this.fragment = matchXml.getFragment();
        XmlCursor cursor = matchXml.newCursor();
        cursor.toNextAttribute();
        while (cursor.toNextAttribute()) {
            attributes.put(cursor.getName().getLocalPart(), cursor.getTextValue());
        }
        cursor.dispose();
    }

}