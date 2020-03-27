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
package org.outerj.daisy.books.frontend;

import java.text.DateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.xml.AttributesImpl;
import org.apache.cocoon.xml.EmbeddedXMLPipe;
import org.apache.cocoon.xml.SaxBuffer;
import org.apache.xmlbeans.XmlCursor;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.books.store.BookInstanceMetaData;
import org.outerj.daisy.books.store.BookStore;
import org.outerj.daisy.books.store.BookStoreAccessDeniedException;
import org.outerj.daisy.books.store.BookStoreUtil;
import org.outerj.daisy.books.store.NonExistingBookInstanceException;
import org.outerj.daisy.books.store.PublicationInfo;
import org.outerj.daisy.books.store.PublicationsInfo;
import org.outerj.daisy.frontend.RequestUtil;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.EncodingUtil;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.ResponseUtil;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.user.UserManager;
import org.outerx.daisy.x10Bookstoremeta.BookInstanceMetaDataDocument;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class ManageBookInstancesApple extends AbstractDaisyApple implements StatelessAppleController {
    private DateFormat dateFormat;
    private UserManager userManager;

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();
        String bookInstanceName = appleRequest.getSitemapParameter("bookInstanceName");
        BookStore bookStore = (BookStore)repository.getExtension("BookStore");

        if (bookInstanceName == null) { // The request is not specific to a certain book instance
            if (request.getMethod().equals("GET")) {
                // Show overview page of book instances
                Locale locale = frontEndContext.getLocale();
                this.dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
                this.userManager = repository.getUserManager();
                Collection<BookInstance> bookInstances = bookStore.getBookInstances();
                BookGroup group = buildHierarchicalBookInstanceIndex(bookInstances);
                SaxBuffer hierarchicalBookInstanceIndex = new SaxBuffer();
                group.generateSaxFragment(hierarchicalBookInstanceIndex);

                Map<String, Object> viewData = new HashMap<String, Object>();
                viewData.put("pageContext", frontEndContext.getPageContext());
                viewData.put("hierarchicalBookInstanceIndex", hierarchicalBookInstanceIndex);

                appleResponse.sendPage("BookInstanceManagementPipe", viewData);
            } else {
                throw new HttpMethodNotAllowedException(request.getMethod());
            }
        } else {
            if (request.getMethod().equals("POST")) {
                String action = request.getParameter("action");
                if (action == null) {
                    throw new Exception("Missing 'action' request parameter.");
                } else if (action.equals("delete")) {
                    bookStore.deleteBookInstance(bookInstanceName);
                    appleResponse.redirectTo(EncodingUtil.encodePath(getMountPoint() + "/books/"));
                } else if (action.equals("changeLabel")) {
                    String newLabel = RequestUtil.getStringParameter(request, "newLabel");
                    if (newLabel.trim().length() == 0)
                        throw new Exception("Cannot change book instance label: new label is all whitespace.");
                    BookInstance bookInstance = bookStore.getBookInstance(bookInstanceName);
                    bookInstance.lock();
                    try {
                        BookInstanceMetaData metadata = bookInstance.getMetaData();
                        metadata.setLabel(newLabel);
                        bookInstance.setMetaData(metadata);
                    } finally {
                        bookInstance.unlock();
                    }
                    String returnTo = request.getParameter("returnTo");
                    ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo != null? returnTo : getMountPoint() + "/books/");
                } else if (action.equals("changeName")) {
                    String newName = RequestUtil.getStringParameter(request, "newName");
                    if (newName.trim().length() == 0)
                        throw new Exception("Cannot change book instance name: new name is all whitespace.");
                    bookStore.renameBookInstance(bookInstanceName, newName);
                    String returnTo = request.getParameter("returnTo");
                    ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo != null? returnTo : getMountPoint() + "/books/");
                } else if (action.equals("changePublicationLabel")) {
                    String publicationName = RequestUtil.getStringParameter(request, "publicationName");
                    String newLabel = RequestUtil.getStringParameter(request, "newLabel");
                    if (newLabel.trim().length() == 0)
                        throw new Exception("Cannot change publication label: new label is all whitespace.");
                    BookInstance bookInstance = bookStore.getBookInstance(bookInstanceName);
                    bookInstance.lock();
                    try {
                        PublicationInfo[] publicationInfos = bookInstance.getPublicationsInfo().getInfos();
                        PublicationInfo[] resultPublicationInfos = new PublicationInfo[publicationInfos.length];
                        boolean found = false;
                        for (int i = 0; i < publicationInfos.length; i++) {
                            if (publicationInfos[i].getName().equals(publicationName)) {
                                resultPublicationInfos[i] = new PublicationInfo(publicationInfos[i].getName(),
                                        newLabel, publicationInfos[i].getStartResource(), publicationInfos[i].getBookPackage(), 
                                        publicationInfos[i].getPublishedBy(), publicationInfos[i].getPublishedOn());
                                found = true;
                            } else {
                                resultPublicationInfos[i] = publicationInfos[i];
                            }
                        }
                        if (!found)
                            throw new Exception("There is no publication with name " + publicationName);
                        bookInstance.setPublications(new PublicationsInfo(resultPublicationInfos));
                    } finally {
                        bookInstance.unlock();
                    }
                    String returnTo = request.getParameter("returnTo");
                    ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo != null? returnTo : getMountPoint() + "/books/");
                } else if (action.equals("changePublicationName")) {
                    String publicationName = RequestUtil.getStringParameter(request, "publicationName");
                    String newName = RequestUtil.getStringParameter(request, "newName");
                    if (newName.trim().length() == 0)
                        throw new Exception("Cannot change publication label: new name is all whitespace.");
                    // publication names follow same restrictions as book instance names
                    String error = BookStoreUtil.isValidBookInstanceName(newName);
                    if (error != null)
                        throw new Exception(error);
                    BookInstance bookInstance = bookStore.getBookInstance(bookInstanceName);
                    bookInstance.lock();
                    try {
                        boolean success = bookInstance.rename(BookInstanceLayout.getPublicationOutputPath(publicationName), newName);
                        if (!success)
                            throw new Exception("Rename failed, maybe there is another publication with the name \"" + newName + "\"?");
                        PublicationInfo[] publicationInfos = bookInstance.getPublicationsInfo().getInfos();
                        PublicationInfo[] resultPublicationInfos = new PublicationInfo[publicationInfos.length];
                        boolean found = false;
                        for (int i = 0; i < publicationInfos.length; i++) {
                            if (publicationInfos[i].getName().equals(publicationName)) {
                                resultPublicationInfos[i] = new PublicationInfo(newName,
                                        publicationInfos[i].getLabel(), publicationInfos[i].getStartResource(),
                                        publicationInfos[i].getBookPackage(), publicationInfos[i].getPublishedBy(),
                                        publicationInfos[i].getPublishedOn());
                                found = true;
                            } else {
                                resultPublicationInfos[i] = publicationInfos[i];
                            }
                        }
                        if (!found)
                            throw new Exception("Error updating publication infos: there is no publication with name " + publicationName);
                        bookInstance.setPublications(new PublicationsInfo(resultPublicationInfos));
                    } finally {
                        bookInstance.unlock();
                    }
                    String returnTo = request.getParameter("returnTo");
                    ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo != null? returnTo : getMountPoint() + "/books/");
                } else {
                    throw new Exception("Invalid value for 'action' request parameter: \"" + action + "\".");
                }
            } else {
                throw new HttpMethodNotAllowedException(request.getMethod());
            }
        }
    }

    private BookGroup buildHierarchicalBookInstanceIndex(Collection<BookInstance> bookInstances) {
        BookGroup rootGroup = new BookGroup("root");
        for (BookInstance bookInstance : bookInstances) {
            try {
                BookInstanceMetaData metaData = bookInstance.getMetaData();
                BookInstanceMetaDataDocument metaDataDocument = metaData.getXml();
                annotateMetaData(metaDataDocument);
                BookInstanceNode node = new BookInstanceNode(bookInstance.getName(), metaDataDocument, bookInstance.getPublicationsInfo(), bookInstance.canManage());
                String path = metaData.getBookPath() != null ? metaData.getBookPath() : "";
                BookGroup group = rootGroup.getGroup(path);
                group.addChild(node);
            } catch (NonExistingBookInstanceException e) {
                // ignore, book instance must have been deleted since we retrieved the list
            } catch (BookStoreAccessDeniedException e) {
                // ignore, book instance state must have changed since we retrieved the list
            }
        }
        return rootGroup;
    }

    private void annotateMetaData(BookInstanceMetaDataDocument metaDataDocument) {
        BookInstanceMetaDataDocument.BookInstanceMetaData metaDataXml = metaDataDocument.getBookInstanceMetaData();
        String createdOn = dateFormat.format(metaDataXml.getCreatedOn().getTime());
        String createdBy;
        try {
            createdBy = userManager.getUserDisplayName(metaDataXml.getCreatedBy());
        } catch (RepositoryException e) {
            createdBy = "(error)";
        }
        XmlCursor cursor = metaDataXml.newCursor();
        cursor.toNextToken();
        cursor.insertAttributeWithValue("createdOnFormatted", createdOn);
        cursor.insertAttributeWithValue("createdByDisplayName", createdBy);
        cursor.dispose();
    }

    static class BookInstanceNode implements BookGroup.BookGroupChild {
        private final String name;
        private final BookInstanceMetaDataDocument metaDataDocument;
        private final PublicationsInfo publicationsInfo;
        private final boolean canManage;

        public BookInstanceNode(String name, BookInstanceMetaDataDocument metaData, PublicationsInfo publicationsInfo, boolean canManage) {
            this.name = name;
            this.metaDataDocument = metaData;
            this.publicationsInfo = publicationsInfo;
            this.canManage = canManage;
        }

        public int compareTo(Object o) {
            BookInstanceNode otherBookInstanceNode = (BookInstanceNode)o;
            return name.compareTo(otherBookInstanceNode.name);
        }

        public void generateSaxFragment(ContentHandler contentHandler) throws SAXException {
            EmbeddedXMLPipe consumer = new EmbeddedXMLPipe(contentHandler);

            AttributesImpl bookInstanceAttrs = new AttributesImpl();
            bookInstanceAttrs.addCDATAAttribute("name", name);
            bookInstanceAttrs.addCDATAAttribute("canManage", String.valueOf(canManage));
            consumer.startElement("", "bookInstance", "bookInstance", bookInstanceAttrs);
            metaDataDocument.save(consumer, consumer);
            publicationsInfo.getXml().save(consumer, consumer);
            consumer.endElement("", "bookInstance", "bookInstance");
        }
    }
}
