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
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.credentialsprovider.CredentialsProvider;
import org.outerj.daisy.jms.JmsClient;
import org.outerj.daisy.jms.Sender;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentNotFoundException;
import org.outerj.daisy.repository.DocumentVariantNotFoundException;
import org.outerj.daisy.repository.Field;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.LiveHistoryEntry;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.textextraction.TextExtractorManager;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10.DocumentDocument;
import org.outerx.daisy.x10.DocumentVariantDeletedDocument;
import org.outerx.daisy.x10.LiveHistoryEntryDocument;
import org.outerx.daisy.x10.TimelineDocument;
import org.outerx.daisy.x10.TimelineUpdatedDocument;
import org.outerx.daisy.x10.TimelineUpdatedDocument.TimelineUpdated;

/**
 * This component receives repository events from JMS, and if the event would influence
 * the full text index, adds an event upon the fulltext task queue. Also, it processes
 * the message on the fulltext task queue, by extracting indexable content from the documents
 * and calling the {@link FullTextIndex} component to update its index for the document.
 *
 */
public class FullTextIndexUpdater implements FullTextIndexUpdaterMBean {
    private String subscriptionName;
    private EventListener eventListener =  new EventListener();
    private MessageListener fullTextQueueListener = new FullTextQueueListener();
    private RepositoryManager repositoryManager;
    private Repository repository;
    private String repositoryKey;
    private CredentialsProvider credentialsProvider;
    private TextExtractorManager textExtractorManager;
    private MBeanServer mbeanServer;
    private ObjectName mbeanName = new ObjectName("Daisy:name=FullTextIndexUpdater");
    private FullTextIndex fullTextIndex;
    private File logExtractedTextFile;
    private String jmsTopicName;
    private String jmsQueueName;
    private Sender fullTextQueueSender;
    private JmsClient jmsClient;
    private long dataMaxSize;
    private String reindexStatus = "No re-index process started.";
    private Thread reindexThread;
    private final String IDLE_MESSAGE = "Idle";
    private String textExtractionStatus = IDLE_MESSAGE;
    private boolean currentLiveOnly = false;
    private final Log log = LogFactory.getLog(getClass());

    public FullTextIndexUpdater(Configuration configuration, RepositoryManager repositoryManager,
            FullTextIndex fullTextIndex, TextExtractorManager textExtractorManager, JmsClient jmsClient, MBeanServer mbeanServer, CredentialsProvider credentialsProvider) throws Exception {
        this.repositoryManager = repositoryManager;
        this.fullTextIndex = fullTextIndex;
        this.textExtractorManager = textExtractorManager;
        this.jmsClient = jmsClient;
        this.mbeanServer = mbeanServer;
        this.credentialsProvider = credentialsProvider;
        this.configure(configuration);
        this.initialize();
    }

    private Repository getRepository() {
        return repository;
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        this.jmsTopicName = configuration.getChild("jmsTopic").getValue();
        this.jmsQueueName = configuration.getChild("jmsQueue").getValue();
        
        subscriptionName = configuration.getChild("jmsSubscriptionName").getValue();

        Configuration logExtractTextConf = configuration.getChild("logExtractedText", false);
        if (logExtractTextConf != null) {
            String fileName = logExtractTextConf.getAttribute("file");
            logExtractedTextFile = new File(fileName);
        }

        dataMaxSize = configuration.getChild("dataMaxSize").getValueAsLong();
        
        currentLiveOnly = configuration.getChild("currentLiveOnly").getValueAsBoolean(false);
        
        repositoryKey = configuration.getChild("repositoryKey").getValue("internal");
    }

    private void initialize() throws Exception {
        repository = repositoryManager.getRepository(credentialsProvider.getCredentials(repositoryKey));
        fullTextQueueSender = jmsClient.getSender(jmsQueueName);
        jmsClient.registerDurableTopicListener(jmsTopicName, subscriptionName, eventListener);
        jmsClient.registerListener(jmsQueueName, fullTextQueueListener);
        mbeanServer.registerMBean(this, mbeanName);
    }

    @PreDestroy
    public void destroy() {
        jmsClient.unregisterListener(eventListener);
        jmsClient.unregisterListener(fullTextQueueListener);
        jmsClient.unregisterSender(fullTextQueueSender);

        if (reindexThread != null && reindexThread.isAlive()) {
            log.info("Waiting for reindex thread to end.");
            reindexThread.interrupt();
            try {
                reindexThread.join(0);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        try {
            mbeanServer.unregisterMBean(mbeanName);
        } catch (Exception e) {
            log.error("Error unregistering MBean", e);
        }
    }


    public synchronized void reIndexDocuments(String query) throws Exception {
        if (reindexThread != null && reindexThread.isAlive()) {
            throw new Exception("A reindex operation is already running.");
        }

        reindexThread = new Thread(new Reindexer(query, repository), "Daisy fulltext reindex scheduling");
        reindexThread.setDaemon(true);
        reindexThread.start();
    }

    public void reIndexAllDocuments() throws Exception {
        reIndexDocuments("select id where true");
    }

    public String getReindexStatus() {
        return reindexStatus;
    }

    public String getTextExtractionStatus() {
        return textExtractionStatus;
    }

    /**
     * Listens for events in the daisy repository and adds a message to the full text index update queue
     * if appropriate.
     */
    private class EventListener implements MessageListener {
        public void onMessage(Message aMessage) {
            try {
                TextMessage message = (TextMessage)aMessage;
                String messageType = message.getStringProperty("type");

                if (messageType.equals("DocumentVariantTimelineUpdated")) {
                    XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                    TimelineUpdatedDocument timelineUpdatedDocument = TimelineUpdatedDocument.Factory.parse(new StringReader(message.getText()), xmlOptions);
                    TimelineUpdated timelineUpdated = timelineUpdatedDocument.getTimelineUpdated();
                    TimelineDocument.Timeline oldTimelineXml = timelineUpdated.getOldTimeline().getTimeline();
                    TimelineDocument.Timeline newTimelineXml = timelineUpdated.getNewTimeline().getTimeline();
                    
                    String documentId = timelineUpdated.getDocumentId();
                    long branchId = timelineUpdated.getBranchId();
                    long languageId = timelineUpdated.getLanguageId();
                    
                    if (currentLiveOnly) {
                        if (oldTimelineXml.getLiveVersionId() != newTimelineXml.getLiveVersionId()) {
                            if (newTimelineXml.getLiveVersionId() == -1) {
                                scheduleIndexRequest(fullTextQueueSender, "unindex", documentId, branchId, languageId, new Date(0), null);
                            } else {
                                scheduleIndexRequest(fullTextQueueSender, "index", documentId, branchId, languageId, new Date(0), null);
                            }
                        }
                    } else {
                        Set<Long> oldIds = new HashSet<Long>();
                        Set<Long> newIds = new HashSet<Long>();
                        for (LiveHistoryEntryDocument.LiveHistoryEntry entry: oldTimelineXml.getLiveHistoryEntryList()) {
                            oldIds.add(entry.getId());
                        }
                        for (LiveHistoryEntryDocument.LiveHistoryEntry entry: newTimelineXml.getLiveHistoryEntryList()) {
                            newIds.add(entry.getId());
                        }
    
                        for (LiveHistoryEntryDocument.LiveHistoryEntry entry: oldTimelineXml.getLiveHistoryEntryList()) {
                            if (!newIds.contains(entry.getId())) {
                                Date beginDate = entry.getBeginDate().getTime();
                                Date endDate = entry.getEndDate() == null ? null : entry.getEndDate().getTime();
                                scheduleIndexRequest(fullTextQueueSender, "unindex", documentId, branchId, languageId, beginDate, endDate);
                            }
                        }
                        for (LiveHistoryEntryDocument.LiveHistoryEntry entry: newTimelineXml.getLiveHistoryEntryList()) {
                            if (!oldIds.contains(entry.getId())) {
                                Date beginDate = entry.getBeginDate().getTime();
                                Date endDate = entry.getEndDate() == null ? null : entry.getEndDate().getTime();
                                scheduleIndexRequest(fullTextQueueSender, "index", documentId, branchId, languageId, beginDate, endDate);
                            }
                        }
                    }
                } else if (messageType.equals("DocumentVariantDeleted")) {
                    XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                    DocumentVariantDeletedDocument variantDeletedDocument = DocumentVariantDeletedDocument.Factory.parse(new StringReader(message.getText()), xmlOptions);
                    DocumentDocument.Document documentXml = variantDeletedDocument.getDocumentVariantDeleted().getDeletedDocumentVariant().getDocument();
                    String documentId = documentXml.getId();
                    long branchId = documentXml.getBranchId();
                    long languageId = documentXml.getLanguageId();

                    // unindex every version for this variant
                    scheduleIndexRequest(fullTextQueueSender, "unindex", documentId, branchId, languageId, null, null);
                }

            } catch (Throwable e) {
                log.error("Error processing JMS message.", e);
            }
        }
    }

    private void scheduleIndexRequest(Sender queueSender, String action, String documentId, long branchId, long languageId, Date beginDate, Date endDate) throws Exception {
        if (log.isDebugEnabled()) {
            String beginDateFormatted = beginDate.toString();
            String endDateFormatted = endDate == null ? "-": endDate.toString();
            log.debug("Queueing an " + action + " request for document " + documentId + ", branch " + branchId + ", language " + languageId + ", from: " + beginDateFormatted + " to: " + endDateFormatted);
        }

        MapMessage fullTextTaskMessage = queueSender.createMapMessage();
        fullTextTaskMessage.setString("action", action);
        fullTextTaskMessage.setString("documentId", documentId);
        fullTextTaskMessage.setLong("branchId", branchId);
        fullTextTaskMessage.setLong("languageId", languageId);
        if (beginDate != null) {
            fullTextTaskMessage.setLong("beginDate", beginDate.getTime());
        }
        if (endDate != null) {
            fullTextTaskMessage.setLong("endDate", endDate.getTime());
        }
        queueSender.send(fullTextTaskMessage);
    }

    private class FullTextQueueListener implements MessageListener {
        public void onMessage(Message aMessage) {
            try {
                MapMessage message = (MapMessage)aMessage;
                String action = message.getString("action");
                String documentId = message.getString("documentId");
                long branchId = message.getLong("branchId");
                long languageId = message.getLong("languageId");
                Date beginDate = null;
                if (message.itemExists("beginDate")) {
                    beginDate = new Date(message.getLong("beginDate"));
                }
                Date endDate = null;
                if (message.itemExists("endDate")) {
                    endDate = new Date(message.getLong("endDate"));
                }
                
                if ("unindex".equals(action)) {
                    if (beginDate == null) {
                        fullTextIndex.unindex(documentId, branchId, languageId);
                    } else {
                        fullTextIndex.unindex(documentId, branchId, languageId, beginDate, endDate);
                    }
                } else if ("index".equals(action)) {
                    Document document = null;
                    try {
                        document = getRepository().getDocument(documentId, branchId, languageId, false);
                    } catch (DocumentNotFoundException e) {
                        // is ok, document will be null
                    } catch (DocumentVariantNotFoundException e) {
                        // is ok, document will be null
                    }
                    
                    if (document != null) {
                        if (currentLiveOnly) {
                            extractAndIndex(documentId, branchId, languageId, document.getLiveVersion(), new Date(0), null);
                        } else {
                            if (beginDate == null) {
                                //index all live history entries
                                for (LiveHistoryEntry lhe: document.getTimeline().getLiveHistory()) {
                                    Version version = document.getVersion(lhe.getVersionId());
                                    extractAndIndex(documentId, branchId, languageId, version, lhe.getBeginDate(), lhe.getEndDate());
                                }
                            } else {
                                // find live version.
                                long versionId = document.getTimeline().getVersionId(beginDate);
                                if (versionId == -1) {
                                    log.info("There was no live version at " + beginDate); // + attempt to remove from index?
                                } else {
                                    Version version = document.getVersion(versionId);
                                    extractAndIndex(documentId, branchId, languageId, version, beginDate, endDate);
                                }
                            }
                        }
                    } else {
                        // not absolutely necessary: an unindex message should already be queued 
                        if (log.isDebugEnabled())
                            log.debug("Document " + documentId + " is deleted, will remove from fulltext index (if any exists).");
                        fullTextIndex.unindex(documentId, branchId, languageId);
                    }
                } else {
                    throw new RuntimeException("Unknown index action: " + action);
                }
            } catch (Throwable e) {
                log.error("Error processing JMS message.", e);
                textExtractionStatus = IDLE_MESSAGE;
            }
        }

        private void extractAndIndex(String documentId, long branchId,
                long languageId, Version version, Date beginDate, Date endDate) throws Exception {
            textExtractionStatus = "Extracting content from document " + documentId + "~" + branchId + "~" + languageId + " version " + version.getId() + " (started at " + new Date() + ")";
            // Collect the content of the parts
            StringBuilder content = new StringBuilder();
            Part[] parts = version.getParts().getArray();
            for (int p = 0; p < parts.length; p++) {
                Part part = parts[p];
                String text = null;

                if (part.getSize() > dataMaxSize) {
                    log.info("Will not extract text from document " + documentId + ", branch ID " + branchId + ", language ID " + languageId + ", part ID " + part.getTypeId() + " because the part data is too large (" + part.getSize() + " > " + dataMaxSize + ")");
                } else if (textExtractorManager.supportsMimeType(part.getMimeType())) {
                    try {
                        // Note: before and after text extraction are logged to be able to check if
                        //       the textextractor nicely returns (and doesn't go in endless loops)
                        if (log.isDebugEnabled())
                            log.debug("Before calling textextractor for document " + documentId + ", branch " + branchId + ", language " + languageId + ", part type " + part.getTypeId());

                        text = textExtractorManager.getText(part.getMimeType(), part.getDataStream());

                        if (log.isDebugEnabled())
                            log.debug("After calling textextractor for document " + documentId + ", branch " + branchId + ", language " + languageId + ", part type " + part.getTypeId());

                        logExtractedText(text, documentId, branchId, languageId, part);
                    } catch (Throwable e) {
                        log.error("Error extracting text from part data (document: " + documentId + ", branch ID: " + branchId + ", language ID: " + languageId + ", part ID: " + part.getTypeId() + ")", e);
                        continue;
                    }
                }

                if (text != null) {
                    content.append(text).append(" ");
                } else {
                    if (log.isDebugEnabled())
                        log.debug("Textextractor wasn't able to extract anything for document " + documentId + ", branch " + branchId + ", language " + languageId + ", part type " + part.getTypeId() + " (mimetype = " + part.getMimeType() + ")");
                }
            }

            // collect the content of the fields
            StringBuilder fieldsContent = new StringBuilder();
            Field[] fields = version.getFields().getArray();
            for (Field field : fields) {
                if (field.getValueType() == ValueType.STRING) {
                    Object[] values = field.isMultiValue() ? (Object[])field.getValue() : new Object[]{field.getValue()};
                    for (Object value : values) {
                        Object[] pathValues = field.isHierarchical() ? ((HierarchyPath)value).getElements() : new Object[]{value};
                        for (Object pathValue : pathValues) {
                            fieldsContent.append(pathValue);
                            fieldsContent.append(' ');
                        }
                    }
                }
            }
            textExtractionStatus = IDLE_MESSAGE;

            fullTextIndex.index(documentId, branchId, languageId, beginDate, endDate, version.getDocumentName(), content.toString(), fieldsContent.toString());
        }
    }

    private void logExtractedText(String text, String documentId, long branchId, long languageId, Part part) {
        if (logExtractedTextFile != null) {
            Writer writer = null;
            try {
                FileOutputStream fos = new FileOutputStream(logExtractedTextFile, true);
                writer = new OutputStreamWriter(fos, Charset.forName("UTF-8"));
                writer.write("\n\n********************************************************************************************\n");
                writer.write("Document " + documentId + ", branch " + branchId + ", language " + languageId + " -- part type " + part.getTypeId() + " -- mimetype " + part.getMimeType() + "\n");
                writer.write(text);
            } catch (Throwable e) {
                log.error("Error logging extracted text to " + logExtractedTextFile.getAbsolutePath(), e);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Throwable e) {
                        // ignore
                    }
                }
            }
        }
    }

    /**
     * Runnable for performing a reindexing process.
     */
    private class Reindexer implements Runnable {
        private String query;
        private Repository repository;
        private static final int COMMIT_INTERVAL = 500;

        public Reindexer(String query, Repository repository) {
            this.query = query;
            this.repository = repository;
        }

        public void run() {
            VariantKey[] allVariantKeys;
            try {
                setStatus("Querying the repository to retrieve the list of documents to re-index (started at " + new Date() + ")");

                QueryManager queryManager = repository.getQueryManager();
                allVariantKeys = queryManager.performQueryReturnKeys(query, Locale.getDefault());
            } catch (Throwable e) {
                setEndError("querying the repository failed: " + e.getMessage());
                return;
            }

            Sender sender = null;
            try {
                sender = jmsClient.getSender(jmsQueueName, true);
                long oldPercentage = -1;
                for (int i = 0; i < allVariantKeys.length; i++) {
                    // check if we're not interrupted
                    if (Thread.interrupted()) {
                        log.info("Fulltext index update thread was interrupted.");
                        setEndError("thread was interrupted.");
                        return;
                    }

                    // update progress indication
                    long percentage = i * 100 / allVariantKeys.length;
                    if (oldPercentage != percentage) {
                        setStatus("Reindex: pushing documents on work queue: " + percentage + "% of " + allVariantKeys.length + " documents");
                        oldPercentage = percentage;
                    }

                    // remove document and reindex
                    scheduleIndexRequest(sender, "unindex", allVariantKeys[i].getDocumentId(), allVariantKeys[i].getBranchId(), allVariantKeys[i].getLanguageId(), null, null);
                    scheduleIndexRequest(sender, "index", allVariantKeys[i].getDocumentId(), allVariantKeys[i].getBranchId(), allVariantKeys[i].getLanguageId(), null, null);

                    if (i >= COMMIT_INTERVAL && (i % COMMIT_INTERVAL) == 0) {
                        sender.commit();
                    }
                }
                sender.commit();
                setEndSuccess("successfully scheduled the indexing of " + allVariantKeys.length + " documents.");
            } catch (Throwable e) {
                setEndError("error scheduling index requests: " + e.getMessage());
            } finally {
                if (sender != null) {
                    jmsClient.unregisterSender(sender);
                }
            }
        }

        private void setStatus(String message) {
            reindexStatus = message;
        }

        private void setEndSuccess(String message) {
            reindexStatus = "Reindex ended at " + new Date() + ": " + message;
        }

        private void setEndError(String message) {
            reindexStatus = "Reindex ended with error at " + new Date() + ": " + message;
        }
    }
}
