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

//
// Class StringBuffer
//   This class is not used in the base tree editor but is useful in specific tree editors
//   for serializing the XML
//

function StringBuffer() {
    this.result = [];
}

StringBuffer.prototype.append = function(text) {
    this.result.push(text);
    return this;
}

StringBuffer.prototype.toString = function() {
    return this.result.join("");
}


//
// Utility function to parse XML
//

treeEditorParseXML = function(xml) {
    var xmldoc = null;
    var error = null;

    var parser = null;
    try {
        parser = new DOMParser();
    } catch (e) {
        // ignore
    }

    try {
        if (parser != null) {
            // Mozilla
            xmldoc = parser.parseFromString(xml, "text/xml");
            var roottag = xmldoc.documentElement;
            if ((roottag.tagName == "parserError") ||
                (roottag.namespaceURI == "http://www.mozilla.org/newlayout/xml/parsererror.xml")){
                error = "Error parsing XML: ";
                var childNodes = roottag.childNodes;
                for (var i = 0; i < childNodes.length; i++) {
                    if (childNodes[i].nodeValue != null)
                        error = error + childNodes[i].nodeValue;
                }
            }
        } else {
            // Internet explorer
            try {
                xmldoc = new ActiveXObject("Microsoft.XMLDOM");
            } catch (e) {
                error = "Failed to parse XML using your browser.";
            }
            if (error == null) {
                xmldoc.async = "false";
                xmldoc.loadXML(xml);

                if (xmldoc.parseError.errorCode != 0) {
                    error = "Error parsing XML: " + xmldoc.parseError.reason + " (line " + xmldoc.parseError.line + ").";
                }
            }
        }
    } catch (e) {
        error = "Error parsing XML: " + e;
    }

    if (xmldoc == null && error == null) {
        error = "Error parsing XML: parser returned null document.";
    }

    if (error != null) {
        throw error;
    }

    return xmldoc;
}