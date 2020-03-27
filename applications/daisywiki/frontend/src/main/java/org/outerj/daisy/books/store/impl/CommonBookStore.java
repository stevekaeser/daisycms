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
package org.outerj.daisy.books.store.impl;

import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.excalibur.source.SourceResolver;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.impl.FileSource;
import org.outerj.daisy.books.store.*;
import org.outerj.daisy.configutil.PropertyResolver;
import org.outerj.daisy.repository.spi.ExtensionProvider;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.clientimpl.ExtensionRegistrar;
import org.outerj.daisy.frontend.util.WikiPropertiesHelper;

import java.io.File;
import java.util.*;

/**
 * The book store. This class should be a singleton. It registers an extension with the Daisy repository manager.
 * Users should access the book store through the repository, i.e. repository.getExtension("BookStore").
 */
public class CommonBookStore implements ThreadSafe, Configurable, Initializable, Serviceable, Contextualizable,
        ExtensionProvider, Disposable, LogEnabled {
    protected String storageDirectoryPath;
    protected File storageDirectory;
    protected ServiceManager serviceManager;
    protected Map<String, CommonBookInstance> bookInstances = Collections.synchronizedMap(new HashMap<String, CommonBookInstance>());
    protected Map<String, Collection<CommonBookInstance>> bookInstancesByDefintion = Collections.synchronizedMap(new HashMap<String, Collection<CommonBookInstance>>());
    protected boolean bookInstancesLoaded = false;
    protected long checkChangesInterval;
    protected Thread checkChangesThread;
    protected Logger logger;
    protected Context context;

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public void configure(Configuration configuration) throws ConfigurationException {
        storageDirectoryPath = PropertyResolver.resolveProperties(configuration.getChild("storageDirectory").getValue(), WikiPropertiesHelper.getResolveProperties(context));
        checkChangesInterval = configuration.getChild("checkChangesInterval").getValueAsLong(60) * 1000;
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    public void initialize() throws Exception {
        SourceResolver sourceResolver = (SourceResolver)serviceManager.lookup(SourceResolver.ROLE);
        Source source = null;
        try {
            source = sourceResolver.resolveURI(storageDirectoryPath);
            if (source instanceof FileSource) {
                storageDirectory = ((FileSource)source).getFile();
                if (!storageDirectory.exists())
                    throw new Exception("Specified BookStore storageDirectory points to a non-existing location: " + source.getURI());
                if (!storageDirectory.isDirectory())
                    throw new Exception("Specified BookStore storageDirectory does not point to a directory: " + source.getURI());
            } else {
                throw new Exception("Specified BookStore storageDirectory does not point to a filesystem location: " + source.getURI());
            }
        } finally {
            if (source != null)
                sourceResolver.release(source);
            if (sourceResolver != null)
                serviceManager.release(sourceResolver);
        }

        ExtensionRegistrar extensionRegistrar = null;
        try {
            extensionRegistrar = (ExtensionRegistrar)serviceManager.lookup("daisy-repository-manager");
            extensionRegistrar.registerExtension("BookStore", this);
        } finally {
            serviceManager.release(extensionRegistrar);
        }

        checkChangesThread = new Thread(new NewBookDetector());
        checkChangesThread.setName("Book Store Change Detection Thread");
        checkChangesThread.setDaemon(true);
        checkChangesThread.start();
    }

    public void dispose() {
        if (checkChangesThread != null) {
            checkChangesThread.interrupt();
            try {
                checkChangesThread.join(5);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    public Object createExtension(Repository repository) {
        return new BookStoreImpl(this, repository);
    }

    public BookInstance createBookInstance(String name, String label, long dataBranchId, long dataLanguageId, VersionMode dataVersion, Repository repository, String bookDefinition) {
        name = name.toLowerCase();
        checkValidBookInstanceName(name);

        File bookInstanceDir = getBookInstanceDir(name);

        if (!bookInstanceDir.mkdir())
            throw new BookStoreException("Could not create the book instance directory, probably a book instance named \"" + name + "\" already exists, or the name contains illegal characters, or there is no write access to the bookstore directory, or something similar.");

        BookAcl bookAcl = new BookAcl(new BookAclEntry[] { new BookAclEntry(BookAclSubjectType.EVERYONE, -1, BookAclActionType.GRANT, BookAclActionType.GRANT)});
        

        CommonBookInstance commonBookInstance = new CommonBookInstance(bookInstanceDir, this);
        commonBookInstance.initialize(label, bookAcl, repository.getUserId(), dataBranchId, dataLanguageId, dataVersion, bookDefinition, null);
        this.addBookInstance(bookInstanceDir.getName(), commonBookInstance);
        
        UserBookInstance userBookInstance = new UserBookInstance(commonBookInstance, repository, true);
        return userBookInstance;
    }



    
    public BookInstance getBookInstance(String name, Repository repository) {
        assureBookInstancesLoaded();
        CommonBookInstance commonBookInstance = bookInstances.get(name);
        if (commonBookInstance == null) {
            File bookInstanceDir = getBookInstanceDir(name);
            if (isBookInstanceDir(bookInstanceDir)) {
                commonBookInstance = new CommonBookInstance(bookInstanceDir, this);
                this.addBookInstance(bookInstanceDir.getName(), commonBookInstance);
            } else {
                throw new NonExistingBookInstanceException(name);
            }
        }

        AclResult aclResult = BookAclEvaluator.evaluate(commonBookInstance.getAcl(), repository.getUserId(), repository.getActiveRoleIds());
        if (!aclResult.canRead())
            throw new BookStoreAccessDeniedException();

        UserBookInstance userBookInstance = new UserBookInstance(commonBookInstance, repository, false);
        return userBookInstance;
    }

    public  Collection<BookInstance> getBookInstances(Repository repository) {
        assureBookInstancesLoaded();
        Collection<CommonBookInstance> bookInstances = this.bookInstances.values();

        return this.filterBooksByACL(bookInstances, repository);
    }
    
    public  Collection<BookInstance> getBookInstances(String bookDefinition, Repository repository) {
        assureBookInstancesLoaded();
        Collection<CommonBookInstance> bookInstances = this.bookInstancesByDefintion.get(bookDefinition);

        return this.filterBooksByACL(bookInstances, repository);
    }
    
    private Collection<BookInstance> filterBooksByACL (Collection<CommonBookInstance> bookInstances, Repository repository) {
    	long userId = repository.getUserId();
        long[] activeRoleIds = repository.getActiveRoleIds();

        List<BookInstance> result = new ArrayList<BookInstance>();
        if (bookInstances != null) {
	        for (CommonBookInstance bookInstance : bookInstances) {
	            try {
	                AclResult aclResult = BookAclEvaluator.evaluate(bookInstance.getAcl(), userId, activeRoleIds);
	                if (aclResult.canRead() && !bookInstance.isLocked()) {
	                    result.add(new UserBookInstance(bookInstance, repository, false));
	                }
	            } catch (NonExistingBookInstanceException e) {
	                // book instance got deleted while we were doing this, simply skip it
	            }
	        }
        }

        return result;
    }

    public void deleteBookInstance(String name, Repository repository) {
        UserBookInstance bookInstance = (UserBookInstance)getBookInstance(name, repository);
        String bookDefinition = bookInstance.getMetaData().getBookDefinition();
        bookInstance.lock();
        boolean fileDeleted = false;
        // first remove the the references to the instance
        this.removeBookInstance(this.bookInstances.get(name), bookDefinition);
        
        try {
            File directory = bookInstance.getDirectory();

            // first delete the meta files, so that reading from this book instance will fail as fast as possible
            for (String metaFile : CommonBookInstance.META_FILES) {
                deleteFile(new File(directory, metaFile));
                fileDeleted = true;
            }

            deleteRecursive(directory);
            directory.delete();
        } catch (Throwable e) {
            throw new BookStoreException("Error while deleting book instance \"" + name + "\".", e);
        } finally {
            if (!fileDeleted) {
                // no files were removed, thus the book instance still exists
                bookInstance.unlock();
                // so it must be added to the store
                this.addBookInstance(bookInstance.getDirectory().getName(), new CommonBookInstance(bookInstance.getDirectory(), CommonBookStore.this));
            }
        }       
    }

    private void deleteRecursive(File file) {
        File[] children = file.listFiles();
        for (File child : children) {
            if (child.isDirectory()) {
                deleteRecursive(child);
            }
            deleteFile(child);
        }
    }

    private void deleteFile(File file) {
        if (!file.delete())
            throw new BookStoreException("Error deleting book instance: could not delete the following file: " + file.getName());
    }
    
    private void addBookInstance (String bookInstanceDir, CommonBookInstance commonBookInstance) {
    	String bookDefinition = commonBookInstance.getMetaData().getBookDefinition();
    	
    	if (!bookInstancesByDefintion.containsKey(bookDefinition)) {
        	bookInstancesByDefintion.put(bookDefinition, new TreeSet<CommonBookInstance>(new BookInstanceComparator()));
        }
        bookInstancesByDefintion.get(bookDefinition).add(commonBookInstance);
        
        bookInstances.put(bookInstanceDir, commonBookInstance);
    }
    
    private void removeBookInstance(CommonBookInstance commonBookInstance, String bookDefinition) {  	
    	if (commonBookInstance != null) {
    		if (bookDefinition == null) {
    			// if we don't have the bookDefinition then the bookInstance metadata is not loaded and
    			// may possibly not exist. Since we can not access the set of instances without comparing metadata
    			// we place the instances in a collection without special comparators. 
        		for(String bookDef: this.bookInstancesByDefintion.keySet()) {
        			Collection<CommonBookInstance> instances = this.bookInstancesByDefintion.get(bookDef);
        			HashSet<CommonBookInstance> instanceSet = new HashSet<CommonBookInstance>(instances);
        			if (instanceSet.remove(commonBookInstance)) {
        				TreeSet<CommonBookInstance> newInstances = new TreeSet<CommonBookInstance>(new BookInstanceComparator());
        				newInstances.addAll(instanceSet);
        				this.bookInstancesByDefintion.put(bookDef, newInstances);
        			}        			
        		}
        	} else if (bookInstancesByDefintion.containsKey(bookDefinition)) {
	    		bookInstancesByDefintion.get(bookDefinition).remove(commonBookInstance);
	    	}
	    	bookInstances.remove(commonBookInstance.getDirectory().getName());
    	}
    }

    public void renameBookInstance(String oldName, String newName, Repository repository) {
        checkValidBookInstanceName(newName);
        UserBookInstance bookInstance = (UserBookInstance)getBookInstance(oldName, repository);
        bookInstance.lock();
        File oldDir = bookInstance.getDirectory();
        File newDir = new File(oldDir.getParent(), newName);
        if (newDir.exists()) {
            bookInstance.unlock();
            // Note: in the exception, we do not explicitely tell whether it exists or not, as the user
            // might not have access to the other book instance
            throw new BookStoreException("Renamed failed, there might be another book instance named \"" + newName + "\".");
        }
        boolean success = oldDir.renameTo(newDir);
        if (!success) {
            bookInstance.unlock();
            throw new BookStoreException("Renamed failed, there might be another book instance named \"" + newName + "\".");
        }

        File lockFile = new File(newDir, CommonBookInstance.LOCK_FILE_NAME);
        lockFile.delete();
        this.addBookInstance(newDir.getName(), new CommonBookInstance(newDir, CommonBookStore.this));

    }

    public boolean existsBookInstance(String name, Repository repository) {
        return new File(storageDirectory, name.toLowerCase()).exists();
    }

    protected File getBookInstanceDir(String name) {
        File bookInstanceDir = new File(storageDirectory, name);
        if (!BookUtil.isWithin(storageDirectory, bookInstanceDir))
            throw new BookStoreException("Tried to access a directory outside the book store.");
        return bookInstanceDir;
    }

    /**
     * Assumes the name has been lower cased.
     */
    protected void checkValidBookInstanceName(String name) {
        String error = BookStoreUtil.isValidBookInstanceName(name);
        if (error != null)
            throw new BookStoreException(error);
    }

    /**
     * Does the initial loading of the book instances on first need.
     */
    protected synchronized void assureBookInstancesLoaded() {
        if (bookInstancesLoaded)
            return;

        File[] files = storageDirectory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                if (isBookInstanceDir(file)) {
                	this.addBookInstance(file.getName(), new CommonBookInstance(file, this));
                }
            }
        }

        this.bookInstancesLoaded = true;
    }

    protected boolean isBookInstanceDir(File file) {
        // When is a directory a valid book instance directory? If it contains all required meta files.
        for (String metaFileName : CommonBookInstance.META_FILES) {
            File metaFile = new File(file, metaFileName);
            if (!metaFile.exists()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Called by a {@link CommonBookInstance} when it detects it has been deleted.
     */
    void notifyBookInstanceDeleted(CommonBookInstance bookInstance) {
    	this.removeBookInstance(bookInstance, null);        
    }

    class NewBookDetector implements Runnable {
        public void run() {
            // Note: this thread only needs to detect new books, deleted books are automatically
            //       detected when accessing them (see also notifyBookInstanceDeleted())
            while (true) {
                try {
                    Thread.sleep(checkChangesInterval);
                    if (bookInstancesLoaded) {
                        File[] files = storageDirectory.listFiles();
                        for (File file : files) {
                            String name = file.getName();
                            if (!bookInstances.containsKey(name) && isBookInstanceDir(file)) {
                                addBookInstance(name, new CommonBookInstance(file, CommonBookStore.this));
                            }
                        }
                    }
                } catch (Throwable e) {
                    if (e instanceof InterruptedException) {
                        // time to stop working
                        return;
                    } else {
                        logger.error("[Daisy books] Error in new book detection thread.", e);
                    }
                }
            }
        }
    }
    
    class BookInstanceComparator implements Comparator<CommonBookInstance> {
    	public int compare(CommonBookInstance o1, CommonBookInstance o2) {
			// order by dates, latest first
			return o2.getMetaData().getCreatedOn().compareTo(o1.getMetaData().getCreatedOn());
		}
    }
}
