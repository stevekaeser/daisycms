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
package org.outerj.daisy.emailnotifier.serverimpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.PreDestroy;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.credentialsprovider.CredentialsProvider;
import org.outerj.daisy.emailer.Emailer;
import org.outerj.daisy.emailnotifier.EmailSubscriptionManager;
import org.outerj.daisy.emailnotifier.Subscriber;
import org.outerj.daisy.emailnotifier.serverimpl.formatters.CommentCreatedTemplateFactory;
import org.outerj.daisy.emailnotifier.serverimpl.formatters.CommentDeletedTemplateFactory;
import org.outerj.daisy.emailnotifier.serverimpl.formatters.DocumentVariantCreatedTemplateFactory;
import org.outerj.daisy.emailnotifier.serverimpl.formatters.DocumentVariantDeletedTemplateFactory;
import org.outerj.daisy.emailnotifier.serverimpl.formatters.DocumentVariantUpdatedTemplateFactory;
import org.outerj.daisy.emailnotifier.serverimpl.formatters.UserCreatedTemplateFactory;
import org.outerj.daisy.emailnotifier.serverimpl.formatters.UserDeletedTemplateFactory;
import org.outerj.daisy.emailnotifier.serverimpl.formatters.UserUpdatedTemplateFactory;
import org.outerj.daisy.emailnotifier.serverimpl.formatters.VersionUpdatedTemplateFactory;
import org.outerj.daisy.jms.JmsClient;
import org.outerj.daisy.plugin.PluginRegistry;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.DocumentNotFoundException;
import org.outerj.daisy.repository.DocumentVariantNotFoundException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.UserNotFoundException;
import org.outerj.daisy.util.ListUtil;
import org.outerx.daisy.x10.AclUpdatedDocument;
import org.outerx.daisy.x10.CollectionCreatedDocument;
import org.outerx.daisy.x10.CollectionDeletedDocument;
import org.outerx.daisy.x10.CollectionUpdatedDocument;
import org.outerx.daisy.x10.CommentCreatedDocument;
import org.outerx.daisy.x10.CommentDeletedDocument;
import org.outerx.daisy.x10.CommentDocument;
import org.outerx.daisy.x10.DocumentDeletedDocument;
import org.outerx.daisy.x10.DocumentDocument;
import org.outerx.daisy.x10.DocumentTypeCreatedDocument;
import org.outerx.daisy.x10.DocumentTypeDeletedDocument;
import org.outerx.daisy.x10.DocumentTypeUpdatedDocument;
import org.outerx.daisy.x10.DocumentVariantCreatedDocument;
import org.outerx.daisy.x10.DocumentVariantDeletedDocument;
import org.outerx.daisy.x10.DocumentVariantUpdatedDocument;
import org.outerx.daisy.x10.FieldTypeCreatedDocument;
import org.outerx.daisy.x10.FieldTypeDeletedDocument;
import org.outerx.daisy.x10.FieldTypeUpdatedDocument;
import org.outerx.daisy.x10.PartTypeCreatedDocument;
import org.outerx.daisy.x10.PartTypeDeletedDocument;
import org.outerx.daisy.x10.PartTypeUpdatedDocument;
import org.outerx.daisy.x10.RoleCreatedDocument;
import org.outerx.daisy.x10.RoleDeletedDocument;
import org.outerx.daisy.x10.RoleUpdatedDocument;
import org.outerx.daisy.x10.UserCreatedDocument;
import org.outerx.daisy.x10.UserDeletedDocument;
import org.outerx.daisy.x10.UserUpdatedDocument;
import org.outerx.daisy.x10.VersionUpdatedDocument;

public class EmailNotifierImpl {
    private MBeanServer mbeanServer;
    private ObjectName mbeanName = new ObjectName("Daisy:name=EmailNotifier");
    private Repository repository;
    private RepositoryManager repositoryManager;
    private String repositoryKey;
    private CredentialsProvider credentialsProvider;
    private Emailer emailer;
    private JmsClient jmsClient;
    private String jmsTopicName;
    private String subscriptionName;
    private EventListener eventListener = new EventListener();
    private String subjectPrefix;
    private Map<String, MailTemplateFactory> mailTemplateFactories;
    private DocumentURLProviderImpl documentURLProvider = new DocumentURLProviderImpl();
    /** For modifications made by these users, no emails will be sent. */
    private Set<String> ignoredUsers = Collections.synchronizedSet(new TreeSet<String>());
    /** Global enabled flag. When not enabled, JMS events are still received, but
     *  no e-mail messages will be created for these events.
     */
    private boolean enabled;
    private final Log log = LogFactory.getLog(getClass());

    public EmailNotifierImpl(Configuration configuration, JmsClient jmsClient,
            RepositoryManager repositoryManager, MBeanServer mbeanServer, PluginRegistry pluginRegistry, CredentialsProvider credentialsProvider) throws Exception {
        this.jmsClient = jmsClient;
        this.repositoryManager = repositoryManager;
        this.mbeanServer = mbeanServer;
        this.credentialsProvider = credentialsProvider;
        configure(configuration);
        initialize();
    }

    @PreDestroy
    public void destroy() {
        dispose();
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        this.jmsTopicName = configuration.getChild("jmsTopic").getValue();
        this.subscriptionName = configuration.getChild("jmsSubscriptionName").getValue();

        repositoryKey = configuration.getChild("repositoryKey").getValue("internal");

        subjectPrefix = configuration.getChild("emailSubjectPrefix").getValue("");
        if (subjectPrefix.length() > 0)
            subjectPrefix += " ";

        Configuration[] documentURLs = configuration.getChild("documentURLs", true).getChildren("documentURL");
        for (Configuration documentURL : documentURLs) {
            String collection = documentURL.getAttribute("collection", null);
            String branch = documentURL.getAttribute("branch", null);
            String language = documentURL.getAttribute("language", null);
            String url = documentURL.getAttribute("url");
            documentURLProvider.addRule(collection, branch, language, url);
        }

        // Previously, the config contained a typo. If someone used this, warn the user about it.
        Configuration typoConf = configuration.getChild("eventsIngoredUsers", false);
        if (typoConf != null)
            throw new ConfigurationException("You have an old configuration with a typo: eventsIngoredUsers should be eventsIgnoredUsers at " + typoConf.getLocation());

        Configuration[] ignoredUsers = configuration.getChild("eventsIgnoredUsers", true).getChildren("user");
        for (Configuration ignoredUser : ignoredUsers) {
            String login = ignoredUser.getValue();
            this.ignoredUsers.add(login);
        }

        if (configuration.getChild("enabled") != null) {
            enabled = configuration.getChild("enabled").getValueAsBoolean(true);
        } else {
            enabled = true;
        }
    }

    private void initialize() throws Exception {
        try {
            repository = repositoryManager.getRepository(credentialsProvider.getCredentials(repositoryKey));
        } catch (Throwable e) {
            throw new Exception("Problem getting repository.", e);
        }

        this.emailer = (Emailer)repository.getExtension("Emailer");

        mailTemplateFactories = new HashMap<String, MailTemplateFactory>();
        mailTemplateFactories.put("DocumentVariantCreated", new DocumentVariantCreatedTemplateFactory());
        mailTemplateFactories.put("DocumentVariantUpdated", new DocumentVariantUpdatedTemplateFactory());
        mailTemplateFactories.put("DocumentVariantDeleted", new DocumentVariantDeletedTemplateFactory());
        mailTemplateFactories.put("VersionUpdated", new VersionUpdatedTemplateFactory());
        mailTemplateFactories.put("CommentCreated", new CommentCreatedTemplateFactory());
        mailTemplateFactories.put("CommentDeleted", new CommentDeletedTemplateFactory());
        mailTemplateFactories.put("UserCreated", new UserCreatedTemplateFactory());
        mailTemplateFactories.put("UserUpdated", new UserUpdatedTemplateFactory());
        mailTemplateFactories.put("UserDeleted", new UserDeletedTemplateFactory());

        // Register MBean
        StandardMBean mbean = new StandardMBean(new EmailNotifierMgmt(), EmailNotifierMBean.class);
        mbeanServer.registerMBean(mbean, mbeanName);

        // Start listening to the JMS events
        jmsClient.registerDurableTopicListener(jmsTopicName, subscriptionName, eventListener);
    }

    private void dispose() {
        jmsClient.unregisterListener(eventListener);
        try {
            mbeanServer.unregisterMBean(mbeanName);
        } catch (Exception e) {
            log.error("Error unregistering MBean", e);
        }
    }

    class EventListener implements MessageListener {
        public void onMessage(Message aMessage) {
            try {
                TextMessage message = (TextMessage)aMessage;
                String eventType = message.getStringProperty("type");

                if (eventType == null) {
                    log.error("Missing type property on JMS message.");
                    return;
                }

                if (!enabled) {
                    return;
                }

                EventParseResult eventParseResult = parseEventDescription(message.getText(), eventType);
                if (eventParseResult == null)
                    return;
                XmlObject eventDescription = eventParseResult.getData();

                try {
                    String actorLogin = repository.getUserManager().getUserLogin(eventParseResult.getActor());
                    if (ignoredUsers.contains(actorLogin)) {
                        // this event is skipped
                        return;
                    }
                } catch (UserNotFoundException e) {
                    /* ignore, just continue assuming without user-based filtering */
                }

                EmailSubscriptionManager subscriptionManager = (EmailSubscriptionManager)repository.getExtension("EmailSubscriptionManager");

                try {
                    // In case it is a document related event, retrieve the document ID to which it applies
                    String documentId = null;
                    long branchId = -1;
                    long languageId = -1;
                    long[] collectionIds = null;
                    String commentVisibility = null;
                    if (eventType.startsWith("Document")) {
                        DocumentDocument.Document documentXml = null;
                        if (eventType.equals("DocumentVariantCreated")) {
                            documentXml = ((DocumentVariantCreatedDocument)eventDescription).getDocumentVariantCreated().getNewDocumentVariant().getDocument();

                        } else if (eventType.equals("DocumentVariantUpdated")) {
                            documentXml = ((DocumentVariantUpdatedDocument)eventDescription).getDocumentVariantUpdated().getNewDocumentVariant().getDocument();
                        } else if (eventType.equals("DocumentVariantDeleted")) {
                            documentXml = ((DocumentVariantDeletedDocument)eventDescription).getDocumentVariantDeleted().getDeletedDocumentVariant().getDocument();
                            collectionIds = ListUtil.toArray(documentXml.getCollectionIds().getCollectionIdList());
                        }/* else if (eventType.equals("DocumentUpdated")) {
                            documentXml = ((DocumentUpdatedDocument)eventDescription).getDocumentUpdated().getNewDocument().getDocument();
                        } */

                        if (documentXml != null) {
                            documentId = documentXml.getId();
                            branchId = documentXml.getBranchId();
                            languageId = documentXml.getLanguageId();
                        }
                    } else if (eventType.equals("VersionUpdated")) {
                        VersionUpdatedDocument.VersionUpdated versionUpdatedXml = ((VersionUpdatedDocument)eventDescription).getVersionUpdated();
                        documentId = versionUpdatedXml.getDocumentId();
                        branchId = versionUpdatedXml.getBranchId();
                        languageId = versionUpdatedXml.getLanguageId();
                    } else if (eventType.equals("CommentCreated")) {
                        CommentDocument.Comment commentXml = ((CommentCreatedDocument)eventDescription).getCommentCreated().getNewComment().getComment();
                        documentId = commentXml.getDocumentId();
                        branchId = commentXml.getBranchId();
                        languageId = commentXml.getLanguageId();
                        commentVisibility = commentXml.getVisibility().toString();
                    } else if (eventType.equals("CommentDeleted")) {
                        CommentDocument.Comment commentXml = ((CommentDeletedDocument)eventDescription).getCommentDeleted().getDeletedComment().getComment();
                        documentId = commentXml.getDocumentId();
                        branchId = commentXml.getBranchId();
                        languageId = commentXml.getLanguageId();
                        commentVisibility = commentXml.getVisibility().toString();
                    }

                    if (documentId != null && collectionIds == null) {
                        try {
                            DocumentCollection[] documentCollections = repository.getDocument(documentId, branchId, languageId, false).getCollections().getArray();
                            collectionIds = new long[documentCollections.length];
                            for (int i = 0; i < documentCollections.length; i++)
                                collectionIds[i] = documentCollections[i].getId();
                        } catch (DocumentNotFoundException e) {
                            // document has been deleted since we got this event, skip it (the event)
                            return;
                        } catch (DocumentVariantNotFoundException e) {
                            // document variant has been deleted since we got this event, skip it (the event)
                            return;
                        }
                    }

                    Subscriber[] subscribers = null;
                    if (eventType.equals("VersionUpdated")) {
                        if (documentId != null) {
                            subscribers = subscriptionManager.getAllDocumentEventSubscribers(documentId, branchId, languageId, collectionIds).getArray();
                        }
                    } else if (eventType.equals("DocumentVariantCreated") || eventType.equals("DocumentVariantDeleted")  || eventType.equals("DocumentVariantUpdated") || eventType.equals("DocumentUpdated")) {
                        subscribers = subscriptionManager.getAllDocumentEventSubscribers(documentId, branchId, languageId, collectionIds).getArray();
                    } else if (eventType.startsWith("FieldType") || eventType.startsWith("PartType") || eventType.startsWith("DocumentType")) {
                        subscribers = subscriptionManager.getAllSchemaEventSubscribers().getArray();
                    } else if (eventType.startsWith("User") || eventType.startsWith("Role")) {
                        subscribers = subscriptionManager.getAllUserEventSubscribers().getArray();
                    } else if (eventType.startsWith("Collection")) {
                        subscribers = subscriptionManager.getAllCollectionEventSubscribers().getArray();
                    } else if (eventType.startsWith("Acl")) {
                        subscribers = subscriptionManager.getAllAclEventSubscribers().getArray();
                    } else if (eventType.startsWith("Comment")) {
                        subscribers = subscriptionManager.getAllCommentEventSubscribers(documentId, branchId, languageId, collectionIds).getArray();
                    } else {
                        if (log.isDebugEnabled())
                            log.debug("Unrecognized event type: " + eventType);
                        return;
                    }

                    if (subscribers != null && subscribers.length > 0) {
                        UserManager userManager = repository.getUserManager();

                        MailTemplate mailTemplate = null;

                        for (int i = 0; i < subscribers.length; i++) {
                            long userId = subscribers[i].getUserId();
                            User user;
                            try {
                                user = userManager.getUser(userId, false);
                            } catch (UserNotFoundException e) {
                                // user does not exist anymore, cleanup his subscription
                                subscriptionManager.deleteSubscription(userId);
                                continue;
                            }

                            String email = user.getEmail();
                            if (email == null || email.equals("")) {
                                log.warn("User " + user.getId() + " (" + user.getDisplayName() + ") is subscribed for email notifications but doens't have an email address configured.");
                                continue;
                            }

                            if ((eventType.startsWith("User") || eventType.startsWith("Role")) && !user.hasRole(Role.ADMINISTRATOR)) {
                                continue;
                            }

                            // If a document is deleted, it is impossible to check the access rights for the document
                            // (well, it might be possible by creating a dummy document object based on the XML document
                            // description embedded in the JMS event, but ignore this for now), therefore we only
                            // send document deletion events to administrators.
                            if (eventType.equals("DocumentVariantDeleted") && !user.hasRole(Role.ADMINISTRATOR)) {
                                continue;
                            }

                            // If it is a document-related event, check ACL
                            boolean isReadAllowed = false;
                            boolean isWriteAllowed = false;
                            if (documentId != null && !eventType.equals("DocumentVariantDeleted")) {
                                try {
                                    // check if the user has a role which allows him/her to access this document
                                    long[] roles = user.getAllRoleIds();
                                    AclResultInfo aclResultInfo = repository.getAccessManager().getAclInfoOnLive(userId, roles, documentId, branchId, languageId);
                                    if (aclResultInfo.isAllowed(AclPermission.WRITE)) {
                                        isWriteAllowed = true;
                                    }
                                    // Since the e-mail messages currently don't take the fine-grained read
                                    // permissions into account (includes difficulties such as not sending
                                    // an e-mail when nothing readable has changed), we require full read
                                    // access to receive notification mails.
                                    if (aclResultInfo.isFullyAllowed(AclPermission.READ)) {
                                        isReadAllowed = true;
                                    }
                                } catch (DocumentNotFoundException e) {
                                    // the document has been deleted since this event occured, ignore it
                                    continue;
                                } catch (DocumentVariantNotFoundException e) {
                                    // the document variant has been deleted since this event occured, ignore it
                                    continue;
                                }
                                if (!isReadAllowed)
                                    continue; // skip this event for this user
                            }

                            if (eventType.startsWith("Comment")) {
                                if (commentVisibility.equals("private")) {
                                    // don't send mail for private comments
                                    continue;
                                }
                                if (commentVisibility.equals("editors") && !isWriteAllowed) {
                                    continue;
                                }
                            }

                            if (mailTemplate == null)
                                mailTemplate = getMailTemplate(eventType, eventDescription);

                            Locale locale = subscribers[i].getLocale();
                            if (locale == null)
                                locale = Locale.getDefault();
                            
                            long actorId = eventParseResult.getActor();
                            User actor = repository.getUserManager().getUser(actorId, false);
                            StringBuffer subject = new StringBuffer(subjectPrefix)
                                .append(mailTemplate.getSubject(locale))
                                .append(" (")
                                .append(actor.getDisplayName()==null?actor.getLogin():actor.getDisplayName())
                                .append(")");
                            emailer.send(email, subject.toString(), mailTemplate.getMessage(locale));
                        }
                    }
                } finally {
                    cleanupSubscriptions(eventType, eventDescription, subscriptionManager);
                }
            } catch (Throwable e) {
                log.error("Error processing JMS event.", e);
            }
        }
    }

    private void cleanupSubscriptions(String eventType, XmlObject eventDescription, EmailSubscriptionManager subscriptionManager) {
        try {
            if (eventType.equals("DocumentDeleted")) {
                DocumentDocument.Document documentXml = ((DocumentDeletedDocument)eventDescription).getDocumentDeleted().getDeletedDocument().getDocument();
                subscriptionManager.deleteAllSubscriptionsForDocument(documentXml.getId());
            } if (eventType.equals("DocumentVariantDeleted")) {
                DocumentDocument.Document documentXml = ((DocumentVariantDeletedDocument)eventDescription).getDocumentVariantDeleted().getDeletedDocumentVariant().getDocument();
                VariantKey variantKey = new VariantKey(documentXml.getId(), documentXml.getBranchId(), documentXml.getLanguageId());
                subscriptionManager.deleteAllSubscriptionsForDocumentVariant(variantKey);
            } else if (eventType.equals("CollectionDeleted")) {
                CollectionDeletedDocument collectionDeletedDocument = (CollectionDeletedDocument)eventDescription;
                long id = collectionDeletedDocument.getCollectionDeleted().getDeletedCollection().getCollection().getId();
                subscriptionManager.deleteAllSubscriptionsForCollection(id);
            }
        } catch (Throwable e) {
            log.error("Error in subscription cleanup handling.", e);
        }
    }

    static interface EventParser {
        EventParseResult parse(String data) throws XmlException;
    }

    static class EventParseResult {
        XmlObject data;
        long actor;

        public EventParseResult(XmlObject data, long actor) {
            this.data = data;
            this.actor = actor;
        }

        public XmlObject getData() {
            return data;
        }

        public long getActor() {
            return actor;
        }
    }

    private static final Map<String, EventParser> EVENT_PARSERS = new HashMap<String, EventParser>();
    static {
        try {
            EVENT_PARSERS.put("DocumentDeleted",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            DocumentDeletedDocument doc = DocumentDeletedDocument.Factory.parse(data);
                            long actor = doc.getDocumentDeleted().getDeleterId();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("DocumentVariantCreated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            DocumentVariantCreatedDocument doc = DocumentVariantCreatedDocument.Factory.parse(data);
                            long actor = doc.getDocumentVariantCreated().getNewDocumentVariant().getDocument().getLastModifier();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("DocumentVariantUpdated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            DocumentVariantUpdatedDocument doc = DocumentVariantUpdatedDocument.Factory.parse(data);
                            long actor = doc.getDocumentVariantUpdated().getNewDocumentVariant().getDocument().getLastModifier();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("DocumentVariantDeleted",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            DocumentVariantDeletedDocument doc = DocumentVariantDeletedDocument.Factory.parse(data);
                            long actor = doc.getDocumentVariantDeleted().getDeleterId();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("VersionUpdated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            VersionUpdatedDocument doc = VersionUpdatedDocument.Factory.parse(data);
                            long actor = doc.getVersionUpdated().getNewVersion().getVersion().getLastModifier();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("FieldTypeCreated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            FieldTypeCreatedDocument doc = FieldTypeCreatedDocument.Factory.parse(data);
                            long actor = doc.getFieldTypeCreated().getNewFieldType().getFieldType().getLastModifier();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("FieldTypeUpdated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            FieldTypeUpdatedDocument doc = FieldTypeUpdatedDocument.Factory.parse(data);
                            long actor = doc.getFieldTypeUpdated().getNewFieldType().getFieldType().getLastModifier();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("FieldTypeDeleted",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            FieldTypeDeletedDocument doc = FieldTypeDeletedDocument.Factory.parse(data);
                            long actor = doc.getFieldTypeDeleted().getDeleterId();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("PartTypeCreated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            PartTypeCreatedDocument doc = PartTypeCreatedDocument.Factory.parse(data);
                            long actor = doc.getPartTypeCreated().getNewPartType().getPartType().getLastModifier();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("PartTypeUpdated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            PartTypeUpdatedDocument doc = PartTypeUpdatedDocument.Factory.parse(data);
                            long actor = doc.getPartTypeUpdated().getNewPartType().getPartType().getLastModifier();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("PartTypeDeleted",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            PartTypeDeletedDocument doc = PartTypeDeletedDocument.Factory.parse(data);
                            long actor = doc.getPartTypeDeleted().getDeleterId();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("DocumentTypeCreated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            DocumentTypeCreatedDocument doc = DocumentTypeCreatedDocument.Factory.parse(data);
                            long actor = doc.getDocumentTypeCreated().getNewDocumentType().getDocumentType().getLastModifier();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("DocumentTypeUpdated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            DocumentTypeUpdatedDocument doc = DocumentTypeUpdatedDocument.Factory.parse(data);
                            long actor = doc.getDocumentTypeUpdated().getNewDocumentType().getDocumentType().getLastModifier();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("DocumentTypeDeleted",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            DocumentTypeDeletedDocument doc = DocumentTypeDeletedDocument.Factory.parse(data);
                            long actor = doc.getDocumentTypeDeleted().getDeleterId();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("UserCreated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            UserCreatedDocument doc = UserCreatedDocument.Factory.parse(data);
                            long actor = doc.getUserCreated().getNewUser().getUser().getLastModifier();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("UserUpdated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            UserUpdatedDocument doc = UserUpdatedDocument.Factory.parse(data);
                            long actor = doc.getUserUpdated().getNewUser().getUser().getLastModifier();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("UserDeleted",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            UserDeletedDocument doc = UserDeletedDocument.Factory.parse(data);
                            long actor = doc.getUserDeleted().getDeleterId();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("RoleCreated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            RoleCreatedDocument doc = RoleCreatedDocument.Factory.parse(data);
                            long actor = doc.getRoleCreated().getNewRole().getRole().getLastModifier();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("RoleUpdated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            RoleUpdatedDocument doc = RoleUpdatedDocument.Factory.parse(data);
                            long actor = doc.getRoleUpdated().getNewRole().getRole().getLastModifier();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("RoleDeleted",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            RoleDeletedDocument doc = RoleDeletedDocument.Factory.parse(data);
                            long actor = doc.getRoleDeleted().getDeleterId();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("CollectionCreated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            CollectionCreatedDocument doc = CollectionCreatedDocument.Factory.parse(data);
                            long actor = doc.getCollectionCreated().getNewCollection().getCollection().getLastModifier();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("CollectionUpdated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            CollectionUpdatedDocument doc = CollectionUpdatedDocument.Factory.parse(data);
                            long actor = doc.getCollectionUpdated().getNewCollection().getCollection().getLastModifier();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("CollectionDeleted",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            CollectionDeletedDocument doc = CollectionDeletedDocument.Factory.parse(data);
                            long actor = doc.getCollectionDeleted().getDeleterId();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("AclUpdated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            AclUpdatedDocument doc = AclUpdatedDocument.Factory.parse(data);
                            long actor = doc.getAclUpdated().getNewAcl().getAcl().getLastModifier();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("CommentCreated",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            CommentCreatedDocument doc = CommentCreatedDocument.Factory.parse(data);
                            long actor = doc.getCommentCreated().getNewComment().getComment().getCreatedBy();
                            return new EventParseResult(doc, actor);
                        }
                    });
            EVENT_PARSERS.put("CommentDeleted",
                    new EventParser() {
                        public EventParseResult parse(String data) throws XmlException {
                            CommentDeletedDocument doc = CommentDeletedDocument.Factory.parse(data);
                            long actor = doc.getCommentDeleted().getDeleterId();
                            return new EventParseResult(doc, actor);
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException("Error initializing event parsers map.", e);
        }
    }


    private EventParseResult parseEventDescription(String data, String eventType) {
        try {
            EventParser eventParser = EVENT_PARSERS.get(eventType);
            if (eventParser != null) {
                return eventParser.parse(data);
            }
        } catch (Exception e) {
            log.error("Error parsing event description XML.", e);
        }
        return null;
    }

    private MailTemplate getMailTemplate(String eventType, XmlObject eventDescription) throws Exception {
        MailTemplateFactory factory = mailTemplateFactories.get(eventType);
        if (factory != null) {
            return factory.createMailTemplate(eventDescription, repository, documentURLProvider);
        } else {
            return new DummyMailTemplate(eventType, eventDescription);
        }
    }

    class DocumentURLProviderImpl implements DocumentURLProvider {
        private List<DocumentURLRule> documentURLRules = new ArrayList<DocumentURLRule>();

        public String getURL(Document document) {
            String branch;
            String language;
            try {
                branch = repository.getVariantManager().getBranch(document.getBranchId(), false).getName();
                language = repository.getVariantManager().getLanguage(document.getLanguageId(), false).getName();
            } catch (RepositoryException e) {
                // rather exceptional exception, just keep going
                return null;
            }
            for (DocumentURLRule rule : documentURLRules) {
                boolean variantMatch = (rule.branch == null || branch.equals(rule.branch))
                        && (rule.language == null || language.equals(rule.language));
                if (variantMatch) {
                    if (rule.collection == null) {
                        return rule.getURL(document.getId(), document.getBranchId(), document.getLanguageId());
                    } else {
                        DocumentCollection[] collections = document.getCollections().getArray();
                        for (DocumentCollection collection : collections) {
                            if (collection.getName().equals(rule.collection)) {
                                return rule.getURL(document.getId(), document.getBranchId(), document.getLanguageId());
                            }
                        }
                    }
                }
            }
            return null;
        }

        public void addRule(String collection, String branch, String language, String URL) {
            DocumentURLRule rule = new DocumentURLRule();
            rule.collection = collection;
            rule.branch = branch;
            rule.language = language;
            rule.URL = URL;
            documentURLRules.add(rule);
        }

        class DocumentURLRule {
            public String collection;
            public String branch;
            public String language;
            public String URL;

            public String getURL(String documentId, long branchId, long languageId) {
                String result = StringUtils.replace(URL, "{id}", documentId);
                result = StringUtils.replace(result, "{branch}", String.valueOf(branchId));
                result = StringUtils.replace(result, "{language}", String.valueOf(languageId));
                return result;
            }
        }
    }

    public class EmailNotifierMgmt implements EmailNotifierMBean {
        private EmailNotifierMgmt() {}

        public void addIgnoreUser(String login) {
            ignoredUsers.add(login);
        }

        public void removeIgnoreUser(String login) {
            ignoredUsers.remove(login);
        }

        public int getIgnoredUserCount() {
            return ignoredUsers.size();
        }

        public String[] getIgnoredUsers() {
            return ignoredUsers.toArray(new String[0]);
        }

        public boolean getEnabled() {
            return enabled;
        }

        public void enable() {
            enabled = true;
        }

        public void disable() {
            enabled = false;
        }
    }

}