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
package org.outerj.daisy.ftindex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;

public class DocumentHitCollector extends Collector implements Hits {
    private Scorer scorer;
    private int docBase;
    
    private List<VariantKey> sortedVariantKeys;
    private LinkedHashMap<VariantKey, ScoreDoc> varKeyDocMap = new LinkedHashMap<VariantKey, ScoreDoc>();
    
    private Highlighter highlighter;
    private IndexSearchObjects indexSearchObjects;
    private XmlOptions xmlOptions;
    
    private float maxScore = 1.0f;
    
    public DocumentHitCollector(Highlighter highLighter, IndexSearchObjects indexSearchObjects) throws Exception{
        this.highlighter = highLighter;
        this.indexSearchObjects = indexSearchObjects;

        xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
        return true;
    }
    
    @Override
    public void collect(int luceneDoc) throws IOException {
        Document doc;
        try {
            doc = this.indexSearchObjects.getIndexSearcher().doc(docBase + luceneDoc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        float score = scorer.score();
        
        maxScore = score > maxScore ? score : maxScore;
        
        String docId = doc.get("DocID");
        long branchId = Long.parseLong(doc.get("BranchID"));
        long languageId = Long.parseLong(doc.get("LangID"));
        
        VariantKey variantKey = new VariantKey(docId, branchId, languageId);
        ScoreDoc scoreDoc = new ScoreDoc(docBase + luceneDoc, score, variantKey);
        
        varKeyDocMap.put(variantKey, scoreDoc);
    }

    @Override
    public void setNextReader(IndexReader reader, int docBase) throws IOException {
        this.docBase = docBase;
    }

    @Override
    public void setScorer(Scorer scorer) throws IOException {
        this.scorer = scorer;
    }

    public XmlObject contextFragments(int n, int fragmentAmount) throws Exception {
        return contextFragments(sortedVariantKeys.get(n), fragmentAmount);
    }

    public XmlObject contextFragments(VariantKey key, int fragmentAmount) throws Exception {
        if (!varKeyDocMap.containsKey(key))
            throw new IndexOutOfBoundsException("Invalid document variant: " + key);
        return contextFragments(varKeyDocMap.get(key), fragmentAmount);
    }

    public void dispose() {
        indexSearchObjects.removeRef();
    }

    public VariantKey getVariantKey(int n) {
        if (sortedVariantKeys == null)
            throw new IndexOutOfBoundsException ("Hits have not been preprocessed. Please run the preprocess() method before accessing this method");
        ScoreDoc scoreDoc = varKeyDocMap.get(sortedVariantKeys.get(n));
        return scoreDoc.getVariantKey();
    }

    public int length() {
        return varKeyDocMap.size();
    }

    public float score(int n) {
        if (sortedVariantKeys == null) 
            throw new IndexOutOfBoundsException ("Hits have not been preprocessed. Please run the preprocess() method before accessing this method");
        ScoreDoc scoreDoc = varKeyDocMap.get(sortedVariantKeys.get(n));
        return scoreDoc.getRelativeScore();
    }

    public float score(VariantKey key) {
        if (!varKeyDocMap.containsKey(key))
            throw new IndexOutOfBoundsException("Invalid document variant: " + key);
        ScoreDoc scoreDoc = varKeyDocMap.get(key);
        return scoreDoc.getRelativeScore();
    }
    
    public List<VariantKey> getAllVariantKeys() {
        return Collections.unmodifiableList(sortedVariantKeys);
    }
    
    protected void postProcess () {
        ScoreDoc[] scoreDocArray = new ScoreDoc[varKeyDocMap.size()];
        Arrays.sort(varKeyDocMap.values().toArray(scoreDocArray), new Comparator<ScoreDoc> () {
            public int compare(ScoreDoc o1, ScoreDoc o2) {
                if (o1.score > o2.score)
                    return -1;
                else if(o1.score < o2.score)
                    return 1;
                else 
                    return 0;
            }            
        });
        sortedVariantKeys = new ArrayList<VariantKey>(scoreDocArray.length);
        for (ScoreDoc scoreDoc : scoreDocArray) {
            scoreDoc.setRelativeScore(scoreDoc.getScore() / maxScore);
            sortedVariantKeys.add(scoreDoc.getVariantKey());
        }        
    }
    
    private XmlObject contextFragments(ScoreDoc scoreDoc, int fragmentAmount) throws Exception {
        XmlObject xml = XmlObject.Factory.newInstance(xmlOptions);
        XmlCursor cursor = xml.newCursor();        
        cursor.toNextToken();
        cursor.beginElement("html");
        cursor.beginElement("body");
        
        Field contentField = indexSearchObjects.getIndexSearcher().doc(scoreDoc.getLuceneDoc()).getField("content");
        
        if (contentField != null) {
            TermFreqVector termFreqVector = indexSearchObjects.getIndexReader().getTermFreqVector(scoreDoc.getLuceneDoc(), "content");
            // When there are no terms the termFreqVector == null
            if (termFreqVector instanceof TermPositionVector) {
                TermPositionVector termPositionVector = (TermPositionVector)termFreqVector;
                TokenStream tokenStream = TokenSources.getTokenStream(termPositionVector);

                String text = contentField.stringValue();
                String[] snippets = highlighter.getBestFragments(tokenStream, text, fragmentAmount);

                for (String snippet : snippets) {
                    cursor.beginElement("div");
                    cursor.insertAttributeWithValue("class", "fulltext-fragment");
                    insertText(cursor, snippet);
                    cursor.toParent();
                }
            }
        }

        cursor.dispose();
        return xml;
    }
    
    private void insertText(XmlCursor cursor, String text) {
        int startPos = text.indexOf("<ft-hit>");
        if (startPos >= 0) {
          int endPos = text.indexOf("</ft-hit>");
          cursor.insertChars(text.substring(0, startPos));
          cursor.beginElement("span");
          cursor.insertAttributeWithValue("class", "fulltext-hit");
          cursor.insertChars(text.substring(startPos + 8, endPos));
          cursor.toNextToken();        
          insertText(cursor, text.substring(endPos + 9));
        } else {
          cursor.insertChars(text);
        }      
      }
    
    private class ScoreDoc {
        private int luceneDoc;
        private float score;
        private float relativeScore;
        private VariantKey variantKey;
        
        public ScoreDoc(int luceneDoc, float score, VariantKey variantKey) {
            this.luceneDoc = luceneDoc;
            this.score = score;
            this.variantKey = variantKey;
        }

        public int getLuceneDoc() {
            return luceneDoc;
        }

        public float getScore() {
            return score;
        }

        public VariantKey getVariantKey() {
            return variantKey;
        }

        public float getRelativeScore() {
            return relativeScore;
        }

        public void setRelativeScore(float relativeScore) {
            this.relativeScore = relativeScore;
        }
    }
    
}
