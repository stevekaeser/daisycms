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
package org.outerj.daisy.books.publisher.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;

import noNamespace.PropertiesDocument;
import noNamespace.PropertiesDocument.Properties;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.components.thread.RunnableManager;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceResolver;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.books.publisher.BookPublisher;
import org.outerj.daisy.books.publisher.PublicationSpec;
import org.outerj.daisy.books.publisher.PublicationSpecProperty;
import org.outerj.daisy.books.publisher.PublicationTypeInfo;
import org.outerj.daisy.books.publisher.PublishTaskInfo;
import org.outerj.daisy.books.publisher.impl.publicationtype.PublicationTypeBuilder;
import org.outerj.daisy.books.store.BookAcl;
import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.books.store.BookStore;
import org.outerj.daisy.books.store.impl.AclResult;
import org.outerj.daisy.books.store.impl.BookAclEvaluator;
import org.outerj.daisy.frontend.components.wikidatasource.WikiDataSource;
import org.outerj.daisy.frontend.editor.LinkFieldHelper;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryImpl;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10Bookpubtype.PublicationTypeDocument;
import org.outerx.daisy.x10Bookpubtype.PublicationTypeDocument.PublicationType;
import org.xml.sax.SAXException;


public class BookPublisherImpl implements BookPublisher, Serviceable, Contextualizable, ThreadSafe, LogEnabled, Disposable, Initializable {
    private ServiceManager serviceManager;
    private Context context;
    private Logger logger;
    private Map<String, BackgroundTaskExecutor> activeTasks = new ConcurrentHashMap<String, BackgroundTaskExecutor>(16, .75f, 2);
    private RunnableManager runnableManager;
    private SecureRandom secureRandom;

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
        this.runnableManager = (RunnableManager)serviceManager.lookup(RunnableManager.ROLE);
    }

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public void initialize() throws Exception {
        secureRandom = SecureRandom.getInstance("SHA1PRNG");
    }

    public void dispose() {
        serviceManager.release(runnableManager);
    }

    public String[] publishBook(Repository repository, VariantKey bookDefinition, long dataBranchId, long dataLanguageId,
                            VersionMode dataVersion, Locale locale, String bookInstanceName, String bookInstanceLabel,
                            String daisyCocoonPath, String daisyContextPath, PublicationSpec[] specs, BookAcl acl) throws Exception {
        // check that no two publication output specs have the same name
        for (int i = 0; i < specs.length; i++) {
            String name = specs[i].getPublicationOutputName();
            for (int j = 0; j < specs.length; j++) {
                if (i != j && specs[j].getPublicationOutputName().equals(name)) {
                    throw new RuntimeException("Found duplicate publication name: \"" + name + "\".");
                }
            }
        }

        // check that the user doens't exclude access for herself with the given ACL
        AclResult result = BookAclEvaluator.evaluate(acl, repository.getUserId(), repository.getActiveRoleIds());
        if (!result.canManage())
            throw new RuntimeException("The specified ACL excludes manage permission for the person starting the publication, which is disallowed.");

        BookStore bookStore = (BookStore)repository.getExtension("BookStore");
        BookInstance bookInstance = bookStore.createBookInstance(bookInstanceName, bookInstanceLabel, dataBranchId, dataLanguageId, dataVersion, LinkFieldHelper.variantKeyToString(bookDefinition, repository.getVariantManager()));
        bookInstance.setAcl(acl);

        // Make a clone of the repository object, since it is not thread safe
        repository = (Repository)((RemoteRepositoryImpl)repository).clone();

        BookPublishTask bookPublishTask = new BookPublishTask(bookDefinition, dataBranchId, dataLanguageId, dataVersion,
                locale, specs, bookInstance, daisyContextPath, daisyCocoonPath, repository, serviceManager, context);

        BackgroundTaskExecutor bte = new BackgroundTaskExecutor(bookPublishTask, this, logger, context, serviceManager);
        synchronized(this) {
            String taskId = generateTaskId();
            while (activeTasks.containsKey(taskId)) {
                taskId = generateTaskId();
            }

            activeTasks.put(taskId, bte);
            try {
                bte.setTaskId(taskId);
                runnableManager.execute(bte);
            } catch (Throwable e) {
                activeTasks.remove(taskId);
                throw new Exception("Error starting background book publication task.", e);
            }
            return new String[] { taskId, bookInstance.getName() };
        }
    }

    public String[] getTaskState(String taskId) {
        BackgroundTaskExecutor bte = activeTasks.get(taskId);
        return bte != null ? bte.getBookPublishTask().getState() : null;
    }

    public PublishTaskInfo[] getTaskOverview(Locale locale) {
        List<PublishTaskInfo> result = new ArrayList<PublishTaskInfo>();
        BackgroundTaskExecutor[] executors = activeTasks.values().toArray(new BackgroundTaskExecutor[0]);
        DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
        for (BackgroundTaskExecutor executor : executors) {
            PublishTaskInfo taskInfo = new PublishTaskInfo(executor.getTaskId(),
                    executor.getBookPublishTask().getBookInstance().getName(),
                    executor.getBookPublishTask().getRepository().getUserId(),
                    executor.getBookPublishTask().getRepository().getUserDisplayName(),
                    executor.getBookPublishTask().getState(),
                    dateTimeFormat.format(executor.getStarted()));
            result.add(taskInfo);
        }
        return result.toArray(new PublishTaskInfo[result.size()]);
    }

    /**
     * Called by a BackgroundTaskExecutor to notify the task has ended.
     */
    protected void taskEnded(String taskId) {
        activeTasks.remove(taskId);
    }

    private synchronized String generateTaskId() throws Exception {
        byte[] bytes = new byte[15];
        secureRandom.nextBytes(bytes);
        return toHexString(bytes);
    }

    private static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(hexChar[(b[i] & 0xf0) >>> 4]);
            sb.append(hexChar[b[i] & 0x0f]);
        }
        return sb.toString();
    }

    private final static char[] hexChar = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public PublicationTypeInfo[] getAvailablePublicationTypes() throws Exception {
        SourceResolver sourceResolver = null;
        Source source = null;
        try {
            sourceResolver = (SourceResolver)serviceManager.lookup(SourceResolver.ROLE);
            source = sourceResolver.resolveURI("wikidata:/books/publicationtypes");
            if (!(source instanceof WikiDataSource)) { //FIXME
                throw new Exception("Expected a WikiDataSource when resolving the publicationtypes directory.");
            }
            WikiDataSource wikiDataSource = (WikiDataSource) source;
            Collection children = wikiDataSource.getChildren();
            List<PublicationTypeInfo> result = new ArrayList<PublicationTypeInfo>();
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            Iterator childIterator = children.iterator();
            while (childIterator.hasNext()){
                WikiDataSource child = (WikiDataSource) childIterator.next();
                File publicationTypeXmlFile = new File(child.getFile(), "publicationtype.xml");
                if (child.isCollection() && publicationTypeXmlFile.exists()) {
                    String label = null;
                    PublicationType publicationType=null;
                    try {
                    	publicationType = PublicationTypeDocument.Factory.parse(publicationTypeXmlFile, xmlOptions).getPublicationType();
                    } catch (Throwable e) {
                        logger.error("Error parsing publicationtype.xml at " + publicationTypeXmlFile.getAbsolutePath(), e);
                    }
                    if (publicationType != null && !publicationType.isSetBackendOnly()) {
                    	label = publicationType.getLabel();
                        if (label != null)
                            result.add(new PublicationTypeInfo(child.getName(), label));
                    }
                    
                }
            }
            return result.toArray(new PublicationTypeInfo[result.size()]);
        } finally {
            if (source != null)
                sourceResolver.release(source);
            if (sourceResolver != null)
                serviceManager.release(sourceResolver);
        }
    }

	public Map<String, String>  getDefaultProperties(String publicationTypeName) throws Exception{
    	return PublicationTypeBuilder.loadProperties(publicationTypeName, serviceManager); 
    } 
	
	/***
	 * 
	 * @param serviceManager
	 * @param publicationTypeName
	 * @return
	 * @throws ServiceException
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws XmlException
	 */
	private Map<String, PublicationSpecProperty>  getPubspecProperties(String publicationTypeName) throws ServiceException, MalformedURLException, IOException, ParserConfigurationException, SAXException, XmlException{
    	SourceResolver sourceResolver = null;
        Source source = null;
        try {
            sourceResolver = (SourceResolver)serviceManager.lookup(SourceResolver.ROLE);
            source = sourceResolver.resolveURI("wikidata:/books/publicationtypes/"+publicationTypeName+"/properties.xml");
            
            Map<String, PublicationSpecProperty> result = new LinkedHashMap<String, PublicationSpecProperty>();
            
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            
            if (source!=null && source.exists()) {
				Properties pubspecProperties = PropertiesDocument.Factory.parse(source.getInputStream(), xmlOptions).getProperties();
				List<Properties.Entry> entries = pubspecProperties.getEntryList();
					
				for(PropertiesDocument.Properties.Entry entry : entries){
					PublicationSpecProperty prop = new PublicationSpecProperty(publicationTypeName, entry.getKey(), entry.getStringValue(), entry.getRequired(), entry.getEditor());
					result.put(entry.getKey(),prop);
				}
            }
        
            return result;
        } finally {
            if (source != null)
                sourceResolver.release(source);
            if (sourceResolver != null)
                serviceManager.release(sourceResolver);
        }
	}
    
    
}
