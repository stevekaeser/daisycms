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
package org.outerj.daisy.ftindex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PreDestroy;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.outerj.daisy.backuplock.spi.SuspendableProcess;
import org.outerj.daisy.configutil.PropertyResolver;
import org.outerj.daisy.plugin.PluginHandle;
import org.outerj.daisy.plugin.PluginRegistry;
import org.outerj.daisy.plugin.PluginUser;
import org.outerj.daisy.repository.query.QueryException;

public class FullTextIndexImpl implements FullTextIndex, FullTextIndexImplMBean, SuspendableProcess {
    private static final int PRECISION_STEP_LONG = 6;
    private String indexAnalyzerName;
    private Analyzer indexAnalyzer;
    private Analyzer fallbackAnalyzer = new StandardAnalyzer(Version.LUCENE_30);
    private String defaultQueryAnalyzerName;
    private Map<String, Analyzer> analyzersByName = new HashMap<String, Analyzer>();
    private File indexDirectoryFile;
    private Directory indexDirectory;
    private IndexWriter indexWriter;
    private boolean indexWriterDirty = false;
    private String indexerStatus;
    private final Lock indexWriteLock = new ReentrantLock();
    private int indexFlushInterval = 5000;
    private Thread indexFlushThread = null;
    private IndexOptimizeThread indexOptimizeThread = null;
    private PluginUser<Analyzer> analyzerPluginUser = new AnalyzerPluginUser();
    private boolean initDone = false;
    
    /**
     * Reusing NumericField instances reduces overhead - these are not thread-safe, but we're only using them in one thread. 
     */
    NumericField beginDateField = new NumericField(FIELD_BEGINDATE, PRECISION_STEP_LONG, Field.Store.YES, true);
    NumericField endDateField = new NumericField(FIELD_ENDDATE, PRECISION_STEP_LONG, Field.Store.YES, true);

    /**
     * The indexSearchLock is to control that not more than one party can modify
     * the indexSearchObjects instance variable at the same time.
     */
    private final Object indexSearchLock = new Object();
    private IndexSearchObjects indexSearchObjects;
    private static final String INDEXER_INACTIVE_MSG = "Indexer idle.";
    private MBeanServer mbeanServer;
    private ObjectName mbeanName = new ObjectName("Daisy:name=FullTextIndexer");
    private PluginRegistry pluginRegistry;
    private static final String SUSPEND_PROCESS_NAME = "Fulltext index";
    private final Log log = LogFactory.getLog(getClass());

    private static final String FIELD_DOCUMENTID = "DocID";
    private static final String FIELD_BRANCHID = "BranchID";
    private static final String FIELD_LANGID = "LangID";
    private static final String FIELD_VARIANTKEY = "VariantKey";
    private static final String FIELD_INDEXKEY = "IndexKey";
    private static final String FIELD_BEGINDATE = "BeginDate";
    private static final String FIELD_ENDDATE = "EndDate";

    public FullTextIndexImpl(Configuration configuration, MBeanServer mbeanServer, PluginRegistry pluginRegistry) throws Exception {
        this.mbeanServer = mbeanServer;
        this.pluginRegistry = pluginRegistry;
        this.configure(configuration);
        this.initialize();
        this.start();
    }

    @PreDestroy
    public void destroy() throws Exception {
        pluginRegistry.unsetPluginUser(Analyzer.class, analyzerPluginUser);

        this.stop();
        this.dispose();
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        String directoryName = PropertyResolver.resolveProperties(configuration.getChild("indexDirectory").getValue());
        indexDirectoryFile = new File(directoryName);
        if (!indexDirectoryFile.exists())
            throw new ConfigurationException("The specified directory does not exist: " + directoryName);
        if (!indexDirectoryFile.isDirectory())
            throw new ConfigurationException("The specified directory is not a directory: " + directoryName);
        log.debug("Using the following as directory to store indexes: " + indexDirectoryFile);
        
        indexAnalyzerName = configuration.getChild("indexAnalyzer").getValue(null);
        defaultQueryAnalyzerName = configuration.getChild("defaultQueryAnalyzer").getValue(null);

        this.indexFlushInterval = configuration.getChild("indexFlushInterval").getValueAsInteger(indexFlushInterval);
    }

    private void initialize() throws Exception {
        indexDirectory = FSDirectory.open(indexDirectoryFile);
        if (IndexWriter.isLocked(indexDirectory)) {
            IndexWriter.unlock(indexDirectory);

            log.error("IMPORTANT WARNING: the fulltext index directory was locked, which indicates improper shutdown of the Daisy repository server. Some index updates might not have been flushed to disk.");

            System.err.println("");
            System.err.println("_________________________________ IMPORTANT WARNING _________________________________");
            System.err.println("The fulltext index directory was locked, which is likely caused by improper shutdown");
            System.err.println("of the repository server. Some index updates might not have been flushed to disk.");
            System.err.println("_____________________________________________________________________________________");
            System.err.println("");
        }
        
        // register the plugin user
        pluginRegistry.setPluginUser(Analyzer.class, analyzerPluginUser);
        
        // fetch the indexAnalyzer;
        indexAnalyzer = getAnalyzer(indexAnalyzerName);

        // Make the initial IndexWriter instance
        updateWriter();
        
        // register with the mbean server
        mbeanServer.registerMBean(this, mbeanName);

        indexerStatus = INDEXER_INACTIVE_MSG;

        pluginRegistry.addPlugin(SuspendableProcess.class, SUSPEND_PROCESS_NAME, this);
        initDone = true;
    }

    private void start() throws Exception {
        indexFlushThread = new Thread(new IndexFlusher(), "Daisy index flusher");
        indexFlushThread.setDaemon(true);
        indexFlushThread.start();
    }

    private void stop() throws Exception {
        if (indexOptimizeThread != null) {
           try {
               log.info("Waiting for index optimalization thread to end.");
               indexOptimizeThread.join();
           } catch (InterruptedException e) {
               // ignore
           }
        }

        if (indexFlushThread != null) {
            try {
                log.info("Waiting for index flush thread to end.");
                indexFlushThread.interrupt();
                indexFlushThread.join();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private synchronized void dispose() {
        pluginRegistry.removePlugin(SuspendableProcess.class, SUSPEND_PROCESS_NAME, this);

        indexWriteLock.lock();
        try {
            closeIndexWriter();
        } catch (IOException e) {
            log.error("Error closing fulltext index writer on shutdown.", e);
        }
        indexWriteLock.unlock();

        try {
            mbeanServer.unregisterMBean(mbeanName);
        } catch (Exception e) {
            log.error("Error unregistering MBean", e);
        }
    }

    public boolean suspendExecution(long msecs) throws InterruptedException {
        boolean suspend = indexWriteLock.tryLock(msecs, TimeUnit.MILLISECONDS);
        if (suspend) {
            try {
                closeIndexWriter();
            } catch (Throwable e) {
                log.error("Error closing index writer when suspending full text indexer.", e);
                // continue anyway
            }
        }
        return suspend;
    }

    public void resumeExecution() {
        try {
            updateWriter();
        } catch (Throwable e) {
            log.error("Error opening index writer when resuming execution.", e);
        }
        indexWriteLock.unlock();
    }

    public Hits search(String analyzerName, String queryAsString, long branchId, long languageId, Date date, boolean searchName, boolean searchContent, boolean searchFields) throws QueryException {
        Analyzer analyzer;
        if (analyzerName == null && defaultQueryAnalyzerName == null) {
            analyzer = fallbackAnalyzer;
        } else {
            if (analyzerName == null) {
                analyzerName = defaultQueryAnalyzerName;
            }
            if (!analyzersByName.containsKey(analyzerName)) {
                throw new QueryException("No analyzer named " + analyzerName + " is registered");
            }
            analyzer = getAnalyzer(analyzerName);
        }
        BooleanQuery query = new BooleanQuery();
        try {
            List<String> fieldstoSearch = new ArrayList<String>(3);
            if (searchName)
                fieldstoSearch.add("name");
            if (searchContent)
                fieldstoSearch.add("content");
            if (searchFields)
                fieldstoSearch.add("fields");

            String[] strings = new String[fieldstoSearch.size()];
            Arrays.fill(strings, queryAsString);// analyzer = new org.apache.lucene.analysis.fr.FrenchAnalyzer(Version.LUCENE_30);
            Query baseQuery = MultiFieldQueryParser.parse(Version.LUCENE_30, strings, fieldstoSearch.toArray(new String[fieldstoSearch.size()]), analyzer);
            
            query.add(baseQuery, BooleanClause.Occur.MUST);

            if (branchId != -1 || languageId != -1) {
                if (branchId != -1)
                    query.add(new TermQuery(new Term("BranchID", String.valueOf(branchId))), BooleanClause.Occur.MUST);
                if (languageId != -1)
                    query.add(new TermQuery(new Term("LangID", String.valueOf(languageId))), BooleanClause.Occur.MUST);
            }
            
            if (date == null) {
                date = new Date();
            }
            query.add(NumericRangeQuery.newLongRange(FIELD_BEGINDATE, PRECISION_STEP_LONG, Long.MIN_VALUE, date.getTime(), true, true), BooleanClause.Occur.MUST);
            query.add(NumericRangeQuery.newLongRange(FIELD_ENDDATE, PRECISION_STEP_LONG, date.getTime(), Long.MAX_VALUE, false, true), BooleanClause.Occur.MUST);

        } catch (ParseException e) {
            throw new QueryException("Error parsing query.", e);
        }

        IndexSearchObjects myIndexSearchObjects = null;
        DocumentHitCollector resultHits = null;
        boolean errorInAddRef = false;
        try {

            synchronized (indexSearchLock) {
                if (indexSearchObjects == null) {
                    IndexReader indexReader = IndexReader.open(indexDirectory);
                    IndexSearcher indexSearcher = new IndexSearcher(indexReader);
                    indexSearchObjects = new IndexSearchObjects(indexReader, indexSearcher, log);
                    myIndexSearchObjects = indexSearchObjects;
                } else {
                    myIndexSearchObjects = indexSearchObjects;
                }
                // Note: addRef also needs to be done inside this synchornized block, otherwise
                // the IndexSearchObjects might be closed before we're able to augment their reference count.
                try {
                    myIndexSearchObjects.addRef();
                } catch (Throwable e) {
                    errorInAddRef = true; // to avoid we call removeRef below when addRef failed
                    throw new RuntimeException("Unexpected: error augmenting ref count.", e);
                }
            }

            Highlighter highLighter = new Highlighter(new SimpleHTMLFormatter("<ft-hit>", "</ft-hit>"),
                    new DefaultEncoder(),
                    new QueryScorer(query.rewrite(myIndexSearchObjects.getIndexReader())));
            resultHits = new DocumentHitCollector(highLighter, indexSearchObjects);
            myIndexSearchObjects.getIndexSearcher().search(query, resultHits);
            resultHits.postProcess();
        } catch (Exception e) {
            throw new QueryException("Error performing fulltext query.", e);
        } finally {
            // In case of an error (= when resultHits is null), we should deref the IndexSearchObjects ourselves
            if (myIndexSearchObjects != null && resultHits == null && !errorInAddRef)
                myIndexSearchObjects.removeRef();
        }
        return resultHits;
    }

    public void unindex(String documentId, long branchId, long languageId) throws Exception {
        indexWriteLock.lockInterruptibly();
        String variantKey = createVariantKey(documentId, branchId, languageId);
        try {
            indexerStatus = "Updating index for document ID " + documentId + ", branch ID " + branchId + ", language ID " + languageId;

            indexWriter.deleteDocuments(new Term(FIELD_VARIANTKEY, variantKey));
            indexWriterDirty = true;
        } catch (Exception e) {
            throw new Exception("Error while indexing content for document " + documentId, e);
        } finally {
            indexerStatus = INDEXER_INACTIVE_MSG;

            indexWriteLock.unlock();

            if (log.isDebugEnabled())
                log.debug("Finished deleting content from index for " + variantKey);

        }
    }
    
    public void unindex(String documentId, long branchId, long languageId, Date beginDate, Date endDate) throws Exception {
        indexWriteLock.lockInterruptibly();
        String indexKey = createIndexKey(documentId, branchId, languageId, beginDate, endDate);
        try {
            indexerStatus = "Unindexig " + indexKey; 

            indexWriter.deleteDocuments(new Term(FIELD_INDEXKEY, indexKey));
            indexWriterDirty = true;
        } catch (Exception e) {
            throw new Exception("Error while indexing content for document " + documentId, e);
        } finally {
            indexerStatus = INDEXER_INACTIVE_MSG;

            indexWriteLock.unlock();

            if (log.isDebugEnabled())
                log.debug("Finished deleting content from index for " + indexKey);

        }
    }
    
    public void index(String documentId, long branchId, long languageId, Date beginDate, Date endDate, String documentName, String content, String fields) throws Exception {
        indexWriteLock.lockInterruptibly();

        String variantString = createVariantKey(documentId, branchId, languageId);
        String indexKey = createIndexKey(documentId, branchId, languageId, beginDate, endDate);
        try {
            indexerStatus = "Updating index for document ID " + documentId + ", branch ID " + branchId + ", language ID " + languageId;

            if (log.isDebugEnabled())
                log.debug("Indexing content for document ID " + documentId + ", branch ID " + branchId + ", language ID " + languageId);

            if (documentName == null || content == null || fields == null) {
                indexWriter.deleteDocuments(new Term(FIELD_INDEXKEY, variantString));
            } else {
                Document luceneDocument = new Document();
                luceneDocument.add(new Field(FIELD_DOCUMENTID, documentId, Field.Store.YES, Field.Index.NOT_ANALYZED));
                luceneDocument.add(new Field(FIELD_BRANCHID, String.valueOf(branchId), Field.Store.YES, Field.Index.NOT_ANALYZED));
                luceneDocument.add(new Field(FIELD_LANGID, String.valueOf(languageId), Field.Store.YES, Field.Index.NOT_ANALYZED));
                luceneDocument.add(new Field(FIELD_VARIANTKEY, variantString, Field.Store.YES, Field.Index.NOT_ANALYZED));
                
                beginDateField.setLongValue(beginDate.getTime());
                luceneDocument.add(beginDateField);
                
                endDateField.setLongValue(endDate == null ? Long.MAX_VALUE : endDate.getTime());
                luceneDocument.add(endDateField);
                
                luceneDocument.add(new Field(FIELD_INDEXKEY, indexKey, Field.Store.YES, Field.Index.NOT_ANALYZED));
    
                if (documentName != null) {
                    luceneDocument.add(new Field("name", documentName, Field.Store.NO, Field.Index.ANALYZED));
                }
    
                if (content != null && content.length() > 0) {
                    luceneDocument.add(new Field("content", content, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
                }
    
                if (fields != null && fields.length() > 0) {
                    luceneDocument.add(new Field("fields", fields, Field.Store.NO, Field.Index.ANALYZED));
                }

                indexWriter.updateDocument(new Term(FIELD_INDEXKEY, indexKey), luceneDocument);
            }
            
            indexWriterDirty = true;
        } catch (Exception e) {
            throw new Exception("Error while indexing content for document " + documentId, e);
        } finally {
            indexerStatus = INDEXER_INACTIVE_MSG;

            indexWriteLock.unlock();

            if (log.isDebugEnabled())
                log.debug("Finished indexing content for " + indexKey);

        }
    }

    private String createVariantKey(String documentId, long branchId,
            long languageId) {
        return documentId + "~" + branchId + "~" + languageId;
    }

    private String createIndexKey(String documentId, long branchId,
            long languageId, Date beginDate, Date endDate) {
        return documentId + "~" + branchId + "~" + languageId + "@" + beginDate.getTime() + (endDate == null ? "" : ("-" + endDate.getTime()));
    }

    private void updateWriter() throws IOException {
        log.debug("Closing and reopening lucene index writer in order to flush changes.");
        closeIndexWriter();
        this.indexWriter = constructIndexWriter();
    }

    private IndexWriter constructIndexWriter() throws IOException {
        return new IndexWriter(indexDirectory, indexAnalyzer, MaxFieldLength.UNLIMITED);
    }

    /**
     * Should only be called by methods that have the indexWriteLock.
     */
    private void closeIndexWriter() throws IOException {
        if (this.indexWriter != null) {
            this.indexWriter.close();
            this.indexWriter = null;
            this.indexWriterDirty = false;

            synchronized (indexSearchLock) {
                if (indexSearchObjects != null) {
                    indexSearchObjects.close();
                    indexSearchObjects = null;
                }
            }
        }
    }

    public synchronized String optimizeIndex() {
        if (indexOptimizeThread != null)
            return "Index optimization is already running or triggered.";
        else {
            indexOptimizeThread = new IndexOptimizeThread();
            indexOptimizeThread.start();
            return "Launched index optimization background thread.";
        }
    }

    public String getIndexerStatus() {
        return indexerStatus;
    }

    public class IndexFlusher implements Runnable {
        public void run() {
            try {
                while (true) {
                    if (Thread.interrupted())
                        return;

                    Thread.sleep(indexFlushInterval);

                    indexWriteLock.lockInterruptibly();
                    try {
                        if (indexWriterDirty) {
                            try {
                                updateWriter();
                            } catch (Throwable e) {
                                log.error("Error updating fulltext index writer.", e);
                            }
                        }
                    } finally {
                        indexWriteLock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                // ingore
            } finally {
                log.info("Index flush thread ended.");

            }
        }
    }

    public class IndexOptimizeThread extends Thread {
        public void run() {
            IndexWriter indexWriter = null;

            try {
                indexWriteLock.lockInterruptibly();
            } catch (InterruptedException e) {
                indexOptimizeThread = null;
                return;
            }

            try {
                indexerStatus = "Running index optimization.";
                log.info("Starting index optimization.");
                closeIndexWriter();
                indexWriter = constructIndexWriter();
                indexWriter.optimize();
            } catch (IOException e) {
                log.error("Error optimizing index.", e);
            } finally {
                if (indexWriter != null) {
                    try {
                        indexWriter.close();
                        log.info("Index optimization ended.");
                    } catch (Throwable e) {
                        log.error("Error closing index writer after index optimization.", e);
                    }
                }
                indexerStatus = INDEXER_INACTIVE_MSG;
                try {
                    updateWriter();
                } catch (Throwable e) {
                    log.error("Error opening index writer after index optimize thread.", e);
                }
                indexWriteLock.unlock();
                synchronized(FullTextIndexImpl.this) {
                    indexOptimizeThread = null;
                }
            }
        }
    }

    public Analyzer getAnalyzer(String name) {
        Analyzer result = null; 
        if (name != null) {
            result = analyzersByName.get(name);
        }
        if (result == null) {
            return fallbackAnalyzer;
        }
        return result;
    }
    
    private class AnalyzerPluginUser implements PluginUser<Analyzer> {
        public void pluginAdded(PluginHandle<Analyzer> pluginHandle) {
            synchronized (analyzersByName) {
                if (analyzersByName.containsKey(pluginHandle.getName())) {
                    log.warn(String.format("Analyzer with name %s already registered, not registering it again", pluginHandle.getName()));
                } else {
                    log.info("Registering Lucene Analyzer with name " + pluginHandle.getName());
                    analyzersByName.put(pluginHandle.getName(), pluginHandle.getPlugin());
                    if (pluginHandle.getName().equals(indexAnalyzerName) && !FullTextIndexImpl.this.initDone) {
                        log.error("Refusing to set the indexAnalyzer after starting the repository to avoid indexing errors");
                    }
                }
            }
        }

        public void pluginRemoved(PluginHandle<Analyzer> pluginHandle) {
            analyzersByName.remove(pluginHandle.getName());
        }
    }
}
