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

import org.outerj.daisy.books.store.*;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlObject;
import org.outerx.daisy.x10Bookstoremeta.ResourcePropertiesDocument;

import java.io.*;
import java.net.URI;
import java.util.Date;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

/**
 * A book instance. Instances of this object can be used by multiple users/thread, this
 * object is wrapped inside a {@link UserBookInstance} by the {@link CommonBookStore}
 * before returning it to a particular user.
 */
public class CommonBookInstance implements BookInstance {
    private final File bookInstanceDirectory;
    private final AclResource aclResource;
    private final PublicationsInfoResource publicationsInfoResource;
    private final MetaDataResource metaDataResource;
    private final CommonBookStore owner;

    private static final int BUFFER_SIZE = 32768;
    static final String ACL_FILENAME = "acl.xml";
    static final String PUBLICATIONS_INFO_FILENAME = "publications_info.xml";
    static final String METADATA_FILENAME = "metadata.xml";
    static final String[] META_FILES = {ACL_FILENAME, PUBLICATIONS_INFO_FILENAME, METADATA_FILENAME};
    static final String LOCK_FILE_NAME = "lock";

    public CommonBookInstance(File bookInstanceDirectory, CommonBookStore owner) {
        this.bookInstanceDirectory = bookInstanceDirectory;
        this.owner = owner;
        this.aclResource = new AclResource(new File(bookInstanceDirectory, ACL_FILENAME));
        this.publicationsInfoResource = new PublicationsInfoResource(new File(bookInstanceDirectory, PUBLICATIONS_INFO_FILENAME));
        this.metaDataResource = new MetaDataResource(new File(bookInstanceDirectory, METADATA_FILENAME));
    }

    File getDirectory() {
        return bookInstanceDirectory;
    }

    public String getName() {
        return bookInstanceDirectory.getName();
    }

    public InputStream getResource(String path) {
        File file = getFile(path);

        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new BookResourceNotFoundException("Resource not found: " + path);
        }
        return new BufferedInputStream(fis);
    }

    public ResourcePropertiesDocument getResourceProperties(String path) {
        String metaFileName = path + ".meta.xml";
        File metaFile = getFile(metaFileName);
        if (metaFile.exists()) {
            try {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                ResourcePropertiesDocument propertiesDocument = ResourcePropertiesDocument.Factory.parse(metaFile, xmlOptions);
                return propertiesDocument;
            } catch (Exception e) {
                throw new RuntimeException("Error reading resource properties.", e);
            }
        }
        return null;
    }

    public void storeResourceProperties(String path, ResourcePropertiesDocument resourcePropertiesDocument) {
        XmlOptions xmlOptions = new XmlOptions();
        xmlOptions.setSavePrettyPrint();
        xmlOptions.setUseDefaultNamespace();
        storeResource(path + ".meta.xml", resourcePropertiesDocument.newInputStream(xmlOptions));
    }

    public void storeResource(String path, InputStream is) {
        BookStoreException exception = null;
        try {
            File file = getFileForStorage(path);
            FileOutputStream fos = null;
            try {
                try {
                    fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        bos.write(buffer, 0, read);
                    }
                    bos.flush();
                    fos.getFD().sync();
                } finally {
                    if (fos != null)
                        fos.close();
                }
            } catch (IOException e) {
                exception = new BookStoreException("Error storing resource in book instance.", e);
                throw exception;
            }
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                if (exception != null) {
                    // don't hide original exception
                    throw new BookStoreException("Error closing input stream, but also got an earlier exception.", exception);
                } else {
                    throw new BookStoreException("Error closing input stream.", e);
                }
            }
        }
    }

    public OutputStream getResourceOutputStream(String path) throws IOException {
        File file = getFileForStorage(path);
        return new BufferedOutputStream(new FileOutputStream(file));
    }

    public boolean rename(String path, String newName) {
        if (path == null)
            throw new IllegalArgumentException("path argument cannot be null");
        if (newName == null)
            throw new IllegalArgumentException("newName argument cannot be null");
        if (newName.indexOf(System.getProperty("file.separator")) != -1)
            throw new IllegalArgumentException("New name may not contain a slash.");

        File file = new File(bookInstanceDirectory, path);
        if (!BookUtil.isWithin(bookInstanceDirectory, file))
            throw new BookStoreException("It is not allowed to access a file outside of the book instance directory.");

        return file.renameTo(new File(file.getParent(), newName));
    }

    private File getFileForStorage(String path) {
        File file = new File(bookInstanceDirectory, path);
        if (!BookUtil.isWithin(bookInstanceDirectory, file))
        	throw new BookStoreException("It is not allowed to write a file outside of the book instance directory.");

        File parentDir = file.getParentFile();
        if (!parentDir.exists())
            parentDir.mkdirs();

        return file;
    }

    public boolean exists(String path) {
        return getFile(path).exists();
    }

    public long getLastModified(String path) {
        return getFile(path).lastModified();
    }

    public long getContentLength(String path) {
        return getFile(path).length();
    }

    private File getFile(String path) {
        File file = new File(bookInstanceDirectory, path);
        if (!BookUtil.isWithin(bookInstanceDirectory, file))
            throw new BookStoreException("It is not allowed to access a file outside of the book instance directory.");
        return file;
    }

    public boolean isLocked() {
        File lockFile = new File(getDirectory(), CommonBookInstance.LOCK_FILE_NAME);
        return lockFile.exists();
    }

    public void lock() {
        throw new BookStoreException("This method should never be called.");
    }

    public void unlock() {
        throw new BookStoreException("This method should never be called.");
    }

    public boolean canManage() {
        throw new BookStoreException("This method should never be called.");
    }

    public BookAcl getAcl() {
        return aclResource.get();
    }

    public void setAcl(BookAcl bookAcl) {
        aclResource.store(bookAcl);
    }

    public PublicationsInfo getPublicationsInfo() {
        return publicationsInfoResource.get();
    }

    public void addPublication(PublicationInfo publicationInfo) {
        PublicationInfo[] infos = getPublicationsInfo().getInfos();
        PublicationInfo[] newInfos = new PublicationInfo[infos.length + 1];
        System.arraycopy(infos, 0, newInfos, 0, infos.length);
        newInfos[newInfos.length - 1] = publicationInfo;
        publicationsInfoResource.store(new PublicationsInfo(newInfos));
    }

    public void setPublications(PublicationsInfo publicationsInfo) {
        publicationsInfoResource.store(publicationsInfo);
    }

    public BookInstanceMetaData getMetaData() {
        return (BookInstanceMetaData)metaDataResource.get().clone();
    }

    public void setMetaData(BookInstanceMetaData metaData) {
        metaDataResource.store((BookInstanceMetaData)metaData.clone());
    }

    public URI getResourceURI(String path) {
        return getFile(path).toURI();
    }

    public String[] getDescendantPaths(String path) {
        File file = getFile(path);
        if (!file.isDirectory()) {
            throw new RuntimeException("path is not a directory: " + path);
        }

        List<String> result = new ArrayList<String>();
        collectPaths(file.listFiles(), result, bookInstanceDirectory.getAbsolutePath());
        return result.toArray(new String[result.size()]);
    }

    private void collectPaths(File[] files, Collection<String> result, String refPath) {
        for (File file : files) {
            if (file.isDirectory()) {
                collectPaths(file.listFiles(), result, refPath);
            } else {
                String path = file.getAbsolutePath();
                if (!path.startsWith(refPath))
                    throw new RuntimeException("Assertion error: path does not start with refPath");
                result.add(path.substring(refPath.length()));
            }
        }
    }

    /**
     * Method to be called upon initial creation of the book instance.
     */
    public void initialize(String label, BookAcl initialAcl, long creator,  long dataBranchId, long dataLanguageId, VersionMode dataVersion, String bookDefinition, String updateFrom) {
        BookInstanceMetaData metaData = new BookInstanceMetaData(label, new Date(), creator, dataBranchId, dataLanguageId, dataVersion, bookDefinition, updateFrom);
        // First write a lock file, before writing the other meta files, so that no other user can get a lock on the book instance
        try {
            new File(bookInstanceDirectory, LOCK_FILE_NAME).createNewFile();
        } catch (IOException e) {
            throw new BookStoreException("Error locking newly created book instance.", e);
        }
        metaDataResource.store(metaData);
        publicationsInfoResource.store(new PublicationsInfo(new PublicationInfo[0]));
        aclResource.store(initialAcl);
    }

    private static XmlOptions getMetaXmlOptions() {
        XmlOptions xmlOptions = new XmlOptions();
        xmlOptions.setSavePrettyPrint();
        xmlOptions.setUseDefaultNamespace();
        return xmlOptions;
    }

    abstract class RefreshableResource {
        private final File file;
        private long lastModifed;
        private Object object;
        private final String what;

        public RefreshableResource(File file, String what) {
            this.file = file;
            this.what = what;
        }

        protected Object getObject() {
            if (object == null || (lastModifed != file.lastModified()))
                loadObject();
            return object;
        }

        protected void loadObject() {
            synchronized(file) {
                long lastModified = file.lastModified();
                Object object;
                try {
                    object = create(new BufferedInputStream(new FileInputStream(file)));
                } catch (FileNotFoundException e) {
                    owner.notifyBookInstanceDeleted(CommonBookInstance.this);
                    throw new NonExistingBookInstanceException(getName());
                } catch (Exception e) {
                    throw new BookStoreException("Error loading book " + what, e);
                }
                this.object = object;
                this.lastModifed = lastModified;
            }
        }

        protected abstract Object create(InputStream is) throws Exception;

        protected void store(Object object, XmlObject xmlObject) {
            synchronized(file) {
                try {
                    OutputStream os = null;
                    try {
                        os = new BufferedOutputStream(new FileOutputStream(file));
                        xmlObject.save(os, getMetaXmlOptions());
                    } finally {
                        if (os != null)
                            os.close();
                    }
                } catch (Exception e) {
                    throw new BookStoreException("Error storing book ACL.", e);
                }
                this.object = object;
                this.lastModifed = file.lastModified();
            }
        }
    }

    class AclResource extends RefreshableResource {
        public AclResource(File file) {
            super(file, "ACL");
        }

        protected Object create(InputStream is) throws Exception {
            return BookAclBuilder.build(is);
        }

        public BookAcl get() {
            return (BookAcl)getObject();
        }

        public void store(BookAcl bookAcl) {
            store(bookAcl, bookAcl.getXml());
        }
    }

    class PublicationsInfoResource extends RefreshableResource {
        public PublicationsInfoResource(File file) {
            super(file, "publications info");
        }

        protected Object create(InputStream is) throws Exception {
            return PublicationsInfoBuilder.build(is);
        }

        PublicationsInfo get() {
            return (PublicationsInfo)getObject();
        }

        void store(PublicationsInfo publicationsInfo) {
            store(publicationsInfo, publicationsInfo.getXml());
        }
    }

    class MetaDataResource extends RefreshableResource {
        public MetaDataResource(File file) {
            super(file, "meta data");
        }

        protected Object create(InputStream is) throws Exception {
            return BookInstanceMetaDataBuilder.build(is);
        }

        public BookInstanceMetaData get() {
            return (BookInstanceMetaData)getObject();
        }

        public void store(BookInstanceMetaData metaData) {
            store(metaData, metaData.getXml());
        }
    }
}
