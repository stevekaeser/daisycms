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
package org.outerj.daisy.repository.serverimpl;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryEventType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryRuntimeException;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.RepositoryStrategy;
import org.outerj.daisy.repository.commonimpl.namespace.NamespaceImpl;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;
import org.outerj.daisy.util.VersionHelper;
import org.outerx.daisy.x10.NamespaceManageDocument;
import org.outerx.daisy.x10.NamespaceRegisteredDocument;
import org.outerx.daisy.x10.NamespaceUnmanageDocument;
import org.outerx.daisy.x10.NamespaceUnregisteredDocument;

public class LocalRepositoryStrategy implements RepositoryStrategy {
    private SecureRandom random = null;
    private static final String NAMESPACE_REGEXP = "[a-zA-Z0-9_]+";
    private Pattern namespacePattern = Pattern.compile(NAMESPACE_REGEXP);
    private LocalRepositoryManager.Context context;
    private JdbcHelper jdbcHelper;
    private EventHelper eventHelper;

    public LocalRepositoryStrategy(LocalRepositoryManager.Context context, JdbcHelper jdbcHelper) {
        this.context = context;
        this.jdbcHelper = jdbcHelper;
        this.eventHelper = new EventHelper(context, jdbcHelper);

        // init secure random generator
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        }
        catch(java.security.NoSuchAlgorithmException nsae) {
            // maybe we are on IBM's SDK
            try {
                random = SecureRandom.getInstance("IBMSecureRandom");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Error setting up LocalRepositoryStrategy secure random.", e);
            }
        }
    }

    public String getClientVersion(AuthenticatedUser user) {
        // in the local implementation, client and server version are the same
        return getServerVersion(user);
    }

    public String getServerVersion(AuthenticatedUser user) {
        Properties versionProps;
        try {
            versionProps = VersionHelper.getVersionProperties(getClass().getClassLoader(), "org/outerj/daisy/repository/serverimpl/versioninfo.properties");
        } catch (IOException e) {
            throw new RepositoryRuntimeException("Error getting version information.", e);
        }
        String version = VersionHelper.getVersion(versionProps);
        if (version != null)
            return version;
        else
            throw new RepositoryRuntimeException("Version unknown.");
    }

    public NamespaceImpl registerNamespace(String namespaceName, String fingerprint, AuthenticatedUser user) throws RepositoryException {
        if (!user.isInAdministratorRole())
            throw new RepositoryException("Only users in Administrator role can register new namespaces.");

        if (namespaceName == null || namespaceName.length() == 0)
            throw new IllegalArgumentException("Namespace cannot be null or empty string.");

        if (namespaceName.length() > 200)
            throw new RepositoryException("A namespace name should not be longer than 200 characters.");

        if (fingerprint == null || fingerprint.length() == 0)
            throw new IllegalArgumentException("Namespace fingerprint cannot be null or empty string.");

        if (fingerprint.length() > 255)
            throw new RepositoryException("A namespace fingerprint should not be longer than 255 characters.");

        Matcher matcher = namespacePattern.matcher(namespaceName);
        if (!matcher.matches())
            throw new IllegalArgumentException("Namespace contains illegal characters: \"" + namespaceName + "\". It should confirm to this regexp: " + NAMESPACE_REGEXP);

        long id;
        Date registeredOn = new Date();
        NamespaceImpl namespace;
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement("insert into daisy_namespaces(id,name_,fingerprint,registered_by,registered_on) values(?,?,?,?,?)");
            id = context.getNextNamespaceId();
            stmt.setLong(1, id);
            stmt.setString(2, namespaceName);
            stmt.setString(3, fingerprint);
            stmt.setLong(4, user.getId());
            stmt.setTimestamp(5, new Timestamp(registeredOn.getTime()));
            stmt.execute();
            
            namespace = new NamespaceImpl(id, namespaceName, fingerprint, user.getId(), registeredOn, 0, false);
            XmlObject eventDescription = createRegisterNamespaceEvent(namespace);
            eventHelper.createEvent(eventDescription, "NamespaceRegistered", conn);

            conn.commit();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("Error registering namespace.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

        // notify creation of namespace through synchronous event
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.NAMESPACE_REGISTERED, new Long(id), 0);

        return namespace;
    }

    private NamespaceRegisteredDocument createRegisterNamespaceEvent(Namespace namespace) {
        NamespaceRegisteredDocument registeredDoc = NamespaceRegisteredDocument.Factory.newInstance();
        NamespaceRegisteredDocument.NamespaceRegistered registeredXml = registeredDoc.addNewNamespaceRegistered();
        registeredXml.addNewRegisteredNamespace().setNamespace(namespace.getXml().getNamespace());
        return registeredDoc;
    }

    public NamespaceImpl registerNamespace(String namespace, AuthenticatedUser user) throws RepositoryException {
        // generate a fingerprint
        int KEYLENGTH = 30;
        byte[] bytes = new byte[KEYLENGTH];
        char[] result = new char[KEYLENGTH * 2];
        random.nextBytes(bytes);

        for (int i = 0; i < KEYLENGTH; i++) {
            byte ch = bytes[i];
            result[2 * i] = Character.forDigit(Math.abs(ch >> 4), 16);
            result[2 * i + 1] = Character.forDigit(Math.abs(ch & 0x0f), 16);
        }

        String fingerprint = new String(result);
        return registerNamespace(namespace, fingerprint, user);
    }

    public NamespaceImpl unregisterNamespace(long id, AuthenticatedUser user) throws RepositoryException {
        return unregisterNamespace(null, id, user);
    }

    public NamespaceImpl unregisterNamespace(String namespaceName, AuthenticatedUser user) throws RepositoryException {
        if (namespaceName == null)
            throw new IllegalArgumentException("namespaceName argument cannot be null.");

        return unregisterNamespace(namespaceName, -1, user);
    }

    public NamespaceImpl unregisterNamespace(String namespaceName, long namespaceId, AuthenticatedUser user) throws RepositoryException {
        if (!user.isInAdministratorRole())
            throw new RepositoryException("Only users in Administrator role can unregister a namespaces.");


        // Note: foreign key constraints on the database assure a namespace cannot be removed
        // if it is still referenced.
        Connection conn = null;
        PreparedStatement stmt = null;
        NamespaceImpl namespace;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            // load namespace to use in JMS event
            if (namespaceName != null)
                namespace = loadNamespaceByName(conn, namespaceName, true);
            else
                namespace = loadNamespaceById(conn, namespaceId, true);

            if (namespace.getName().equals(context.getRepositoryNamespace()))
                throw new RepositoryException("The repository's own namespace cannot be unregistered.");

            if (namespaceName != null) {
                stmt = conn.prepareStatement("delete from daisy_namespaces where name_ = ?");
                stmt.setString(1, namespaceName);
            } else {
                stmt = conn.prepareStatement("delete from daisy_namespaces where id = ?");
                stmt.setLong(1, namespaceId);
            }
            int updateCount = stmt.executeUpdate();

            if (updateCount != 1)
                throw new RepositoryException("Unexpected update count: " + updateCount);

            XmlObject eventDescription = createUnregisterNamespaceEvent(namespace, user.getId(), new Date());
            eventHelper.createEvent(eventDescription, "NamespaceUnregistered", conn);

            conn.commit();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            String identifier = namespaceName != null ? namespaceName : String.valueOf(namespaceId);
            throw new RepositoryRuntimeException("Error unregistering namespace \"" + identifier + "\".", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

        // notify creation of namespace through synchronous event
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.NAMESPACE_UNREGISTERED, new Long(namespace.getId()), 0);

        return namespace;
    }

    private NamespaceUnregisteredDocument createUnregisterNamespaceEvent(Namespace namespace, long unregisteredBy, Date unregisteredOn) {
        NamespaceUnregisteredDocument unregisteredDoc = NamespaceUnregisteredDocument.Factory.newInstance();
        NamespaceUnregisteredDocument.NamespaceUnregistered unregisteredXml = unregisteredDoc.addNewNamespaceUnregistered();

        unregisteredXml.addNewUnregisteredNamespace().setNamespace(namespace.getXml().getNamespace());
        unregisteredXml.setUnregistrarId(unregisteredBy);
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(unregisteredOn);
        unregisteredXml.setUnregisterTime(calendar);

        return unregisteredDoc;
    }
    
    private NamespaceManageDocument createManageNamespaceEvent(Namespace namespace, long manageBy, Date managedOn) {
        NamespaceManageDocument managedDoc = NamespaceManageDocument.Factory.newInstance();
        NamespaceManageDocument.NamespaceManage managedXml = managedDoc.addNewNamespaceManage();
        
        managedXml.addNewManageNamespace().setNamespace(namespace.getXml().getNamespace());
        managedXml.setManagerId(manageBy);
        Calendar cal = Calendar.getInstance();
        cal.setTime(managedOn);
        managedXml.setManageTime(cal);
        
        return managedDoc;
    }
    
    private NamespaceUnmanageDocument createUnmanageNamespaceEvent(Namespace namespace, long unmanageBy, Date unmanagedOn) {
        NamespaceUnmanageDocument unmanagedDoc = NamespaceUnmanageDocument.Factory.newInstance();
        NamespaceUnmanageDocument.NamespaceUnmanage unmanagedXml = unmanagedDoc.addNewNamespaceUnmanage();
        
        unmanagedXml.addNewUnmanageNamespace().setNamespace(namespace.getXml().getNamespace());
        unmanagedXml.setUnmanagerId(unmanageBy);
        Calendar cal = Calendar.getInstance();
        cal.setTime(unmanagedOn);
        unmanagedXml.setUnmanageTime(cal);
        
        return unmanagedDoc;
    }
    
    public Namespace updateNamespace (Namespace namespace, AuthenticatedUser user) throws RepositoryException {
        
        Connection conn = null;
        PreparedStatement stmt = null;
        NamespaceImpl newNs;
        NamespaceImpl oldNs;
        
        XmlObject eventDescription;
        String eventName;
        RepositoryEventType eventType;
        try {
            conn = context.getDataSource().getConnection();

            // load namespace to use in JMS event
            oldNs = loadNamespaceById(conn, namespace.getId(), true);
            
            // make sure 
            if (namespace.isManaged()) {
                if (oldNs.isManaged()) {
                    // don't do anything
                    return namespace;
                } else {
                    if (namespace.getDocumentCount() < oldNs.getDocumentCount()) {
                        throw new RepositoryRuntimeException ("The document count specified for " + namespace.getName() + " is too small. It should be larger or equal to " + oldNs.getDocumentCount());
                    }
                    stmt = conn.prepareStatement("insert into document_sequence(maxid, ns_id) values (?,?)");
                }
                stmt.setLong(1, namespace.getDocumentCount());
                stmt.setLong(2, oldNs.getId());
                
                eventName = "NamespaceManaged";
                eventDescription = createManageNamespaceEvent(namespace, user.getId(), new Date());
                eventType = RepositoryEventType.NAMESPACE_MANAGED;
            } else {
                if (oldNs.isManaged()) {
                    stmt = conn.prepareStatement("delete from document_sequence where ns_id = ?");
                    stmt.setLong(1, oldNs.getId());
                    
                    eventName = "NamespaceUnmanaged";
                    eventDescription = createUnmanageNamespaceEvent(namespace, user.getId(), new Date());
                    eventType = RepositoryEventType.NAMESPACE_UNMANAGED;
                } else {
                    // no need to do anything
                    return namespace;
                }
            }
            
            int updateCount = stmt.executeUpdate();

            if (updateCount != 1)
                throw new RepositoryException("Unexpected update count: " + updateCount);
            
            newNs = new NamespaceImpl(oldNs.getId(), oldNs.getName(), oldNs.getFingerprint(), oldNs.getRegisteredBy(),
                    oldNs.getRegisteredOn(), namespace.getDocumentCount(), namespace.isManaged());

            eventHelper.createEvent(eventDescription, eventName, conn);

        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            String identifier = namespace.getName() != null ? namespace.getName() : String.valueOf(namespace.getId());
            throw new RepositoryRuntimeException("Error managing namespace \"" + identifier + "\".", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

        // notify creation of namespace through synchronous event
        context.getCommonRepository().fireRepositoryEvent(eventType, new Long(namespace.getId()), 0);

        return newNs;
    }
    
    private NamespaceImpl loadNamespaceByName(Connection conn, String name, boolean lock) throws RepositoryException {
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            stmt = conn.prepareStatement("select ns.id, ns.name_, ns.fingerprint, ns.registered_by, ns.registered_on, ds.maxid, ds.maxid is not null as isManaged, max(d.id) " +
                        "from daisy_namespaces as ns left join document_sequence as ds on ns.id = ds.ns_id left join documents as d on ns.id = d.ns_id " + 
                        "where ns.name_ = ? " +
                        (lock?  jdbcHelper.getSharedLockClause() : ""));
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // namespace names are case sensitive, but SQL isn't, so do an additional check
                if (!name.equals(rs.getString("name_")))
                    throw new NamespaceNotFoundException(name);
                return getNamespaceFromResultSet(rs);
            } else {
                throw new NamespaceNotFoundException(name);
            }
        } catch (NamespaceNotFoundException e) {
            throw e;
        } catch (Throwable e) {
            throw new RepositoryException("Error loading namespace " + name, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private NamespaceImpl loadNamespaceById(Connection conn, long id, boolean lock) throws RepositoryException {
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            stmt = conn.prepareStatement("select ns.id, ns.name_, ns.fingerprint, ns.registered_by, ns.registered_on, ds.maxid, ds.maxid is not null as isManaged, max(d.id) " +
            		"from daisy_namespaces as ns left join document_sequence as ds on ns.id = ds.ns_id left join documents as d on ns.id = d.ns_id " + 
            		"where ns.id = ? " +
            		"group by ns.id order by null " +
                        (lock?  jdbcHelper.getSharedLockClause() : ""));
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return getNamespaceFromResultSet(rs);
            } else {
                throw new NamespaceNotFoundException(id);
            }
        } catch (NamespaceNotFoundException e) {
            throw e;
        } catch (Throwable e) {
            throw new RepositoryException("Error loading namespace " + id, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private NamespaceImpl getNamespaceFromResultSet(ResultSet rs) throws SQLException {
        long id = rs.getLong(1);
        String name = rs.getString(2);
        String fingerprint = rs.getString(3);
        long registeredBy = rs.getLong(4);
        Date registeredOn = rs.getTimestamp(5);
        long docSeq = rs.getLong(6);
        boolean isManaged = rs.getBoolean(7);
        long maxDocId = rs.getLong(8);
        
        long documentCount = isManaged ? docSeq : maxDocId;
        
        return new NamespaceImpl(id, name, fingerprint, registeredBy, registeredOn, documentCount, isManaged);
    }

    public Namespace[] getAllNamespaces(AuthenticatedUser user) {
        List<Namespace> namespaces = new ArrayList<Namespace>();
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            stmt = conn.prepareStatement("select ns.id, ns.name_, ns.fingerprint, ns.registered_by, ns.registered_on, ds.maxid, ds.maxid is not null as isManaged, max(d.id) " +
                    "from daisy_namespaces as ns left join document_sequence as ds on ns.id = ds.ns_id left join documents as d on ns.id = d.ns_id " + 
                    "group by ns.id");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                namespaces.add(getNamespaceFromResultSet(rs));
            }
        } catch (Throwable e) {
            throw new RepositoryRuntimeException("Error loading namespaces.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
        return namespaces.toArray(new Namespace[namespaces.size()]);
    }

    public Namespace getNamespaceByName(String namespaceName, AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        try {
            conn = context.getDataSource().getConnection();
            return loadNamespaceByName(conn, namespaceName, false);
        } catch (NamespaceNotFoundException e) {
            throw e;
        } catch (Throwable e) {
            throw new RepositoryException("Error loading namespace \"" + namespaceName + "\".", e);
        } finally {
            jdbcHelper.closeConnection(conn);
        }
    }

    public String getRepositoryNamespace(AuthenticatedUser user) {
        return context.getRepositoryNamespace();
    }
    
    public String[] getRepositoryNamespaces(AuthenticatedUser user) {
        return context.getRepositoryNamespaces();
    }
    
    public String getRepositoryNamespace(Document document, AuthenticatedUser user) throws RepositoryException {
        return context.getRepositoryNamespace(document);
    }
}
