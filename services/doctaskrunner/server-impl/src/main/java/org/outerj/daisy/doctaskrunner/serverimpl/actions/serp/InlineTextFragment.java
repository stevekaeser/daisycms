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
package org.outerj.daisy.doctaskrunner.serverimpl.actions.serp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.outerj.daisy.doctaskrunner.serverimpl.actions.serp.DocumentMatches.SharedAttributesStartElement;
import org.outerj.daisy.xmlutil.SaxBuffer.Characters;
import org.outerj.daisy.xmlutil.SaxBuffer.EndElement;
import org.outerj.daisy.xmlutil.SaxBuffer.SaxBit;
import org.outerj.daisy.xmlutil.SaxBuffer.StartElement;

public class InlineTextFragment implements TextFragment {

    private List<SaxBit> charactersBits;
    private List<SaxBit> saxBits;
    private final String text;
    
    private Map<String, String> attributes;
    /**
     * @param text
     * @param partInfo 
     * @param characterBits the character bits that make up this text block
     * @param the document's complete list of saxBits
     */
    public InlineTextFragment(String text, List<SaxBit> charactersBits, List<SaxBit> saxBits, Map<String, String> partInfo) {
        // calculate the te
        this.text = text;
        this.saxBits = saxBits;
        this.charactersBits = new ArrayList<SaxBit>(charactersBits);

        if (charactersBits.isEmpty()) {
            throw new IllegalArgumentException("charactersBits should not be empty");
        }
        
        Map<String, String> tmp = new HashMap<String, String>(partInfo);
        tmp.put("type", "inline");
        attributes = Collections.unmodifiableMap(tmp);
    }
    
    public String getOriginalText() {
        return text;
    }

    /**
     * alters the current saxbits
     */
    public int replace(Pattern pattern, String replacement, boolean useSensibleCase) {
        int replacements = 0;
        Matcher matcher = pattern.matcher(text);
        
        Characters currentCharacters = (Characters)charactersBits.get(0);
        
        int lastEnd = 0;
        int offset = 0; //the sum of the length of already passed characters bits
        while (matcher.find()) {
            if (matcher.start() < lastEnd) // skip verlapping matches
                continue;
            
            // find the characters bit that contains the current matcher.start()
            while (matcher.start() >= offset + currentCharacters.ch.length) {
                offset += currentCharacters.ch.length;
                charactersBits.remove(0);
                currentCharacters = (Characters)charactersBits.get(0);
            }
            Characters startCharacters = currentCharacters;
            int startOffset = offset;
            int startCharactersIndex = saxBits.indexOf(currentCharacters);
            
            while (matcher.end() > offset + currentCharacters.ch.length) {
                offset += currentCharacters.ch.length;
                charactersBits.remove(0);
                currentCharacters = (Characters)charactersBits.get(0);
            }
            Characters endCharacters = currentCharacters; 
            int endCharactersIndex = saxBits.indexOf(currentCharacters);
            
            // now get the characters that are not part of the match
            Characters preCharacters = new Characters(startCharacters.ch, 0, matcher.start() - startOffset);
            Characters postCharacters = new Characters(endCharacters.ch , matcher.end() - offset, endCharacters.ch.length - matcher.end() + offset);

            // The replacement can contain regexp group references ($0, $1, ...)
            // so we can not just append 'replacement', we have to let the matcher
            // perform the replacement and append the result of that.
            StringBuffer buf = new StringBuffer();
            matcher.appendReplacement(buf, replacement);
            String actualReplacementText = buf.substring(matcher.start() - lastEnd);
            if (useSensibleCase) {
                actualReplacementText = SearchAndReplaceUtil.caseMatch(matcher.group(), actualReplacementText);
            }
            
            List<SaxBit> replacementBits = new ArrayList<SaxBit>();
            replacementBits.add(preCharacters);
            replacementBits.add(new Characters(actualReplacementText.toCharArray(), 0, actualReplacementText.length()));
            
            List<SaxBit> openTags = new ArrayList<SaxBit>();
            for (int i = startCharactersIndex + 1; i <= endCharactersIndex; i++) {
                SaxBit bit = saxBits.get(i);
                if (bit instanceof SharedAttributesStartElement || bit instanceof StartElement){
                    openTags.add(bit);
                } else if (bit instanceof EndElement) {
                    if (openTags.size() == 0) {
                        replacementBits.add(bit); // closing tag
                    } else {
                        openTags.remove(openTags.size() - 1); // drop pair of tags
                    }
                } else {
                    // drop all other bits (TODO: are unmatched closing and unmatched opening tags the only bits we need to take into account?) 
                }
            }
            replacementBits.addAll(openTags);
            replacementBits.add(postCharacters);
            
            // now remove the bits
            for (int i = startCharactersIndex; i <= endCharactersIndex; i++) {
                saxBits.remove(startCharactersIndex);
            }
            // and put the new bits in place
            for (int i = startCharactersIndex, j = 0; j < replacementBits.size(); i++, j++) {
                saxBits.add(i, replacementBits.get(j));
            }
            
            // replace the previous characters bit with postCharacters
            currentCharacters = postCharacters;
            charactersBits.set(0, postCharacters);
            // fix offset:
            offset += endCharacters.ch.length - postCharacters.ch.length;
            
            lastEnd = matcher.end();
            replacements++;
        }
        return replacements;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

}
