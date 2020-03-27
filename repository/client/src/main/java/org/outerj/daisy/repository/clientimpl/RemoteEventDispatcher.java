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
package org.outerj.daisy.repository.clientimpl;

import org.outerj.daisy.jms.JmsClient;
import org.outerj.daisy.repository.RepositoryEventType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.RepositoryListener;
import org.outerj.daisy.repository.schema.RepositorySchemaEventType;
import org.outerj.daisy.repository.schema.RepositorySchemaListener;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10.*;
import org.apache.xmlbeans.XmlOptions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.MessageListener;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.io.StringReader;
import java.util.*;

/**
 * This class distributes daisy events received from JMS to events
 * on the 'local' interfaces RepositoryListener and RepositorySchemaListener.
 */
public class RemoteEventDispatcher {
    private Log logger = LogFactory.getLog(getClass());
    private List<RepositoryListener> repositoryListeners = new ArrayList<RepositoryListener>();
    private List<RepositorySchemaListener> repositorySchemaListeners = new ArrayList<RepositorySchemaListener>();
    private static final Map<String, MessageHandler> messageHandlers = new HashMap<String, MessageHandler>();
    static {
        messageHandlers.put("DocumentCreated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                DocumentCreatedDocument.DocumentCreated documentCreated = DocumentCreatedDocument.Factory.parse(new StringReader(text), xmlOptions).getDocumentCreated();
                DocumentDocument.Document documentXml = documentCreated.getNewDocument().getDocument();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.DOCUMENT_CREATED, documentXml.getId(), documentXml.getUpdateCount());
            }
        });
        messageHandlers.put("DocumentUpdated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                DocumentUpdatedDocument.DocumentUpdated documentUpdated = DocumentUpdatedDocument.Factory.parse(new StringReader(text), xmlOptions).getDocumentUpdated();
                DocumentDocument.Document documentXml = documentUpdated.getNewDocument().getDocument();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.DOCUMENT_UPDATED, documentXml.getId(), documentXml.getUpdateCount());
            }
        });
        messageHandlers.put("DocumentDeleted", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                DocumentDeletedDocument.DocumentDeleted documentDeleted = DocumentDeletedDocument.Factory.parse(new StringReader(text), xmlOptions).getDocumentDeleted();
                DocumentDocument.Document documentXml = documentDeleted.getDeletedDocument().getDocument();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.DOCUMENT_DELETED, documentXml.getId(), documentXml.getUpdateCount());
            }
        });
        messageHandlers.put("UserCreated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                UserCreatedDocument.UserCreated userCreated = UserCreatedDocument.Factory.parse(new StringReader(text), xmlOptions).getUserCreated();
                UserDocument.User userXml = userCreated.getNewUser().getUser();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.USER_CREATED, new Long(userXml.getId()), userXml.getUpdateCount());
            }
        });
        messageHandlers.put("UserUpdated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                UserUpdatedDocument.UserUpdated userUpdated = UserUpdatedDocument.Factory.parse(new StringReader(text), xmlOptions).getUserUpdated();
                UserDocument.User userXml = userUpdated.getNewUser().getUser();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.USER_UPDATED, new Long(userXml.getId()), userXml.getUpdateCount());
            }
        });
        messageHandlers.put("UserDeleted", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                UserDeletedDocument.UserDeleted userDeleted = UserDeletedDocument.Factory.parse(new StringReader(text), xmlOptions).getUserDeleted();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.USER_DELETED, new Long(userDeleted.getDeletedUser().getUser().getId()), -1);
            }
        });
        messageHandlers.put("RoleCreated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                RoleCreatedDocument.RoleCreated roleCreated = RoleCreatedDocument.Factory.parse(new StringReader(text), xmlOptions).getRoleCreated();
                RoleDocument.Role roleXml = roleCreated.getNewRole().getRole();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.ROLE_CREATED, new Long(roleXml.getId()), roleXml.getUpdateCount());
            }
        });
        messageHandlers.put("RoleUpdated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                RoleUpdatedDocument.RoleUpdated roleUpdated = RoleUpdatedDocument.Factory.parse(new StringReader(text), xmlOptions).getRoleUpdated();
                RoleDocument.Role roleXml = roleUpdated.getNewRole().getRole();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.ROLE_UPDATED, new Long(roleXml.getId()), roleXml.getUpdateCount());
            }
        });
        messageHandlers.put("RoleDeleted", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                RoleDeletedDocument.RoleDeleted roleDeleted = RoleDeletedDocument.Factory.parse(new StringReader(text), xmlOptions).getRoleDeleted();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.ROLE_DELETED, new Long(roleDeleted.getDeletedRole().getRole().getId()), -1);
            }
        });
        messageHandlers.put("VersionUpdated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                VersionUpdatedDocument.VersionUpdated versionUpdated = VersionUpdatedDocument.Factory.parse(new StringReader(text), xmlOptions).getVersionUpdated();
                VariantKey variantKey = new VariantKey(versionUpdated.getDocumentId(), versionUpdated.getBranchId(), versionUpdated.getLanguageId());
                dispatcher.engenderRepositoryEvent(RepositoryEventType.VERSION_UPDATED, variantKey, -1);
            }
        });
        messageHandlers.put("CollectionCreated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                CollectionCreatedDocument.CollectionCreated collectionCreated = CollectionCreatedDocument.Factory.parse(new StringReader(text), xmlOptions).getCollectionCreated();
                CollectionDocument.Collection collectionXml = collectionCreated.getNewCollection().getCollection();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.COLLECTION_CREATED, new Long(collectionXml.getId()), collectionXml.getUpdatecount());
            }
        });
        messageHandlers.put("CollectionUpdated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                CollectionUpdatedDocument.CollectionUpdated collectionUpdated = CollectionUpdatedDocument.Factory.parse(new StringReader(text), xmlOptions).getCollectionUpdated();
                CollectionDocument.Collection collectionXml = collectionUpdated.getNewCollection().getCollection();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.COLLECTION_UPDATED, new Long(collectionXml.getId()), collectionXml.getUpdatecount());
            }
        });
        messageHandlers.put("CollectionDeleted", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                CollectionDeletedDocument.CollectionDeleted collectionDeleted = CollectionDeletedDocument.Factory.parse(new StringReader(text), xmlOptions).getCollectionDeleted();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.COLLECTION_DELETED, new Long(collectionDeleted.getDeletedCollection().getCollection().getId()), -1);
            }
        });
        messageHandlers.put("DocumentTypeCreated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                DocumentTypeCreatedDocument.DocumentTypeCreated documentTypeCreated = DocumentTypeCreatedDocument.Factory.parse(new StringReader(text), xmlOptions).getDocumentTypeCreated();
                DocumentTypeDocument.DocumentType documentTypeXml = documentTypeCreated.getNewDocumentType().getDocumentType();
                dispatcher.engenderRepositorySchemaEvent(RepositorySchemaEventType.DOCUMENT_TYPE_CREATED, documentTypeXml.getId(), documentTypeXml.getUpdateCount());
            }
        });
        messageHandlers.put("DocumentTypeUpdated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                DocumentTypeUpdatedDocument.DocumentTypeUpdated documentTypeUpdated = DocumentTypeUpdatedDocument.Factory.parse(new StringReader(text), xmlOptions).getDocumentTypeUpdated();
                DocumentTypeDocument.DocumentType documentTypeXml = documentTypeUpdated.getNewDocumentType().getDocumentType();
                dispatcher.engenderRepositorySchemaEvent(RepositorySchemaEventType.DOCUMENT_TYPE_UPDATED, documentTypeXml.getId(), documentTypeXml.getUpdateCount());
            }
        });
        messageHandlers.put("DocumentTypeDeleted", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                DocumentTypeDeletedDocument.DocumentTypeDeleted documentTypeDeleted = DocumentTypeDeletedDocument.Factory.parse(new StringReader(text), xmlOptions).getDocumentTypeDeleted();
                DocumentTypeDocument.DocumentType documentTypeXml = documentTypeDeleted.getDeletedDocumentType().getDocumentType();
                dispatcher.engenderRepositorySchemaEvent(RepositorySchemaEventType.DOCUMENT_TYPE_DELETED, documentTypeXml.getId(), documentTypeXml.getUpdateCount());
            }
        });
        messageHandlers.put("PartTypeCreated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                PartTypeCreatedDocument.PartTypeCreated partTypeCreated = PartTypeCreatedDocument.Factory.parse(new StringReader(text), xmlOptions).getPartTypeCreated();
                PartTypeDocument.PartType partTypeXml = partTypeCreated.getNewPartType().getPartType();
                dispatcher.engenderRepositorySchemaEvent(RepositorySchemaEventType.PART_TYPE_CREATED, partTypeXml.getId(), partTypeXml.getUpdateCount());
            }
        });
        messageHandlers.put("PartTypeUpdated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                PartTypeUpdatedDocument.PartTypeUpdated partTypeUpdated = PartTypeUpdatedDocument.Factory.parse(new StringReader(text), xmlOptions).getPartTypeUpdated();
                PartTypeDocument.PartType partTypeXml = partTypeUpdated.getNewPartType().getPartType();
                dispatcher.engenderRepositorySchemaEvent(RepositorySchemaEventType.PART_TYPE_UPDATED, partTypeXml.getId(), partTypeXml.getUpdateCount());
            }
        });
        messageHandlers.put("PartTypeDeleted", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                PartTypeDeletedDocument.PartTypeDeleted partTypeDeleted = PartTypeDeletedDocument.Factory.parse(new StringReader(text), xmlOptions).getPartTypeDeleted();
                PartTypeDocument.PartType partTypeXml = partTypeDeleted.getDeletedPartType().getPartType();
                dispatcher.engenderRepositorySchemaEvent(RepositorySchemaEventType.PART_TYPE_DELETED, partTypeXml.getId(), partTypeXml.getUpdateCount());
            }
        });
        messageHandlers.put("FieldTypeCreated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                FieldTypeCreatedDocument.FieldTypeCreated fieldTypeCreated = FieldTypeCreatedDocument.Factory.parse(new StringReader(text), xmlOptions).getFieldTypeCreated();
                FieldTypeDocument.FieldType fieldTypeXml = fieldTypeCreated.getNewFieldType().getFieldType();
                dispatcher.engenderRepositorySchemaEvent(RepositorySchemaEventType.FIELD_TYPE_CREATED, fieldTypeXml.getId(), fieldTypeXml.getUpdateCount());
            }
        });
        messageHandlers.put("FieldTypeUpdated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                FieldTypeUpdatedDocument.FieldTypeUpdated fieldTypeUpdated = FieldTypeUpdatedDocument.Factory.parse(new StringReader(text), xmlOptions).getFieldTypeUpdated();
                FieldTypeDocument.FieldType fieldTypeXml = fieldTypeUpdated.getNewFieldType().getFieldType();
                dispatcher.engenderRepositorySchemaEvent(RepositorySchemaEventType.FIELD_TYPE_UPDATED, fieldTypeXml.getId(), fieldTypeXml.getUpdateCount());
            }
        });
        messageHandlers.put("FieldTypeDeleted", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                FieldTypeDeletedDocument.FieldTypeDeleted fieldTypeDeleted = FieldTypeDeletedDocument.Factory.parse(new StringReader(text), xmlOptions).getFieldTypeDeleted();
                FieldTypeDocument.FieldType fieldTypeXml = fieldTypeDeleted.getDeletedFieldType().getFieldType();
                dispatcher.engenderRepositorySchemaEvent(RepositorySchemaEventType.FIELD_TYPE_DELETED, fieldTypeXml.getId(), fieldTypeXml.getUpdateCount());
            }
        });
        messageHandlers.put("BranchCreated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                BranchCreatedDocument.BranchCreated branchCreated = BranchCreatedDocument.Factory.parse(new StringReader(text), xmlOptions).getBranchCreated();
                BranchDocument.Branch branchXml = branchCreated.getNewBranch().getBranch();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.BRANCH_CREATED, new Long(branchXml.getId()), branchXml.getUpdateCount());
            }
        });
        messageHandlers.put("BranchUpdated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                BranchUpdatedDocument.BranchUpdated branchUpdated = BranchUpdatedDocument.Factory.parse(new StringReader(text), xmlOptions).getBranchUpdated();
                BranchDocument.Branch branchXml = branchUpdated.getNewBranch().getBranch();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.BRANCH_UPDATED, new Long(branchXml.getId()), branchXml.getUpdateCount());
            }
        });
        messageHandlers.put("BranchDeleted", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                BranchDeletedDocument.BranchDeleted branchDeleted = BranchDeletedDocument.Factory.parse(new StringReader(text), xmlOptions).getBranchDeleted();
                BranchDocument.Branch branchXml = branchDeleted.getDeletedBranch().getBranch();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.BRANCH_DELETED, new Long(branchXml.getId()), branchXml.getUpdateCount());
            }
        });
        messageHandlers.put("LanguageCreated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                LanguageCreatedDocument.LanguageCreated languageCreated = LanguageCreatedDocument.Factory.parse(new StringReader(text), xmlOptions).getLanguageCreated();
                LanguageDocument.Language languageXml = languageCreated.getNewLanguage().getLanguage();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.LANGUAGE_CREATED, new Long(languageXml.getId()), languageXml.getUpdateCount());
            }
        });
        messageHandlers.put("LanguageUpdated", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                LanguageUpdatedDocument.LanguageUpdated languageUpdated = LanguageUpdatedDocument.Factory.parse(new StringReader(text), xmlOptions).getLanguageUpdated();
                LanguageDocument.Language languageXml = languageUpdated.getNewLanguage().getLanguage();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.LANGUAGE_UPDATED, new Long(languageXml.getId()), languageXml.getUpdateCount());
            }
        });
        messageHandlers.put("LanguageDeleted", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                LanguageDeletedDocument.LanguageDeleted languageDeleted = LanguageDeletedDocument.Factory.parse(new StringReader(text), xmlOptions).getLanguageDeleted();
                LanguageDocument.Language languageXml = languageDeleted.getDeletedLanguage().getLanguage();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.LANGUAGE_DELETED, new Long(languageXml.getId()), languageXml.getUpdateCount());
            }
        });
        messageHandlers.put("NamespaceRegistered", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                NamespaceRegisteredDocument.NamespaceRegistered namespaceRegistered = NamespaceRegisteredDocument.Factory.parse(new StringReader(text), xmlOptions).getNamespaceRegistered();
                long id = namespaceRegistered.getRegisteredNamespace().getNamespace().getId();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.NAMESPACE_REGISTERED, new Long(id), 0);
            }
        });
        messageHandlers.put("NamespaceUnregistered", new MessageHandler() {
            public void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                NamespaceUnregisteredDocument.NamespaceUnregistered namespaceUnregistered = NamespaceUnregisteredDocument.Factory.parse(new StringReader(text), xmlOptions).getNamespaceUnregistered();
                long id = namespaceUnregistered.getUnregisteredNamespace().getNamespace().getId();
                dispatcher.engenderRepositoryEvent(RepositoryEventType.NAMESPACE_UNREGISTERED, new Long(id), 0);
            }
        });
    }

    public RemoteEventDispatcher(JmsClient jmsClient, String jmsTopic) throws Exception {
        jmsClient.registerListener(jmsTopic, new MyMessageListener());
    }

    interface MessageHandler {
        void handleMessage(String text, RemoteEventDispatcher dispatcher) throws Exception;
    }

    class MyMessageListener implements MessageListener {
        public void onMessage(Message aMessage) {
            try {
                TextMessage message = (TextMessage)aMessage;
                String type = message.getStringProperty("type");

                MessageHandler handler = messageHandlers.get(type);
                if (handler != null)
                    handler.handleMessage(message.getText(), RemoteEventDispatcher.this);

            } catch (Exception e) {
                logger.error("Error dispatching daisy JMS event.", e);
            }
        }
    }

    private void engenderRepositoryEvent(RepositoryEventType type, Object id, long updateCount) {
        for (RepositoryListener listener : repositoryListeners) {
            listener.repositoryEvent(type, id, updateCount);
        }
    }

    private void engenderRepositorySchemaEvent(RepositorySchemaEventType type, long id, long updateCount) {
        for (RepositorySchemaListener listener : repositorySchemaListeners) {
            listener.modelChange(type, id, updateCount);
        }
    }

    public void addRepositoryListener(RepositoryListener repositoryListener) {
        repositoryListeners.add(repositoryListener);
    }

    public void removeRepositoryListener(RepositoryListener repositoryListener) {
        repositoryListeners.remove(repositoryListener);
    }

    public void addRepositorySchemaListener(RepositorySchemaListener repositorySchemaListener) {
        repositorySchemaListeners.add(repositorySchemaListener);
    }

    public void removeRepositorySchemaListener(RepositorySchemaListener repositorySchemaListener) {
        repositorySchemaListeners.remove(repositorySchemaListener);
    }
}
