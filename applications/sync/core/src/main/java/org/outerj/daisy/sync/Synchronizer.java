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
package org.outerj.daisy.sync;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.sync.dao.ExternalEntityDao;
import org.outerj.daisy.sync.dao.InternalEntityDao;
import org.outerj.daisy.sync.dao.Locker;
import org.outerj.daisy.sync.dao.SyncEntityDao;
import org.outerj.daisy.sync.mapping.EntityMapping;
import org.outerj.daisy.sync.mapping.MappingConfiguration;

public class Synchronizer {
    private MappingConfiguration mappingConfiguration;

    private InternalEntityDao daisyDao;

    private SyncEntityDao syncDao;

    private ExternalEntityDao externalDao;

    private Locker locker;
    
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public Synchronizer(MappingConfiguration mappingConfiguration, ExternalEntityDao externalDao, SyncEntityDao syncDao, InternalEntityDao daisyDao,
            Locker locker) {
        logger.fine("Starting synchronizer");
        this.mappingConfiguration = mappingConfiguration;
        this.externalDao = externalDao;
        this.syncDao = syncDao;
        this.daisyDao = daisyDao;
        this.locker = locker;
    }

    public boolean startSync() {
        return startSync(false);
    }

    public boolean startSync(boolean manualStart) {
        boolean hasStarted = false;        
        SystemState systemState = locker.getLockState();
        logger.fine("Trying to start synchronization " + (manualStart ? "manually" : "automatically") + ". The system is currently in state : " + systemState);
        if (systemState == SystemState.AWAITING_SYNC || (manualStart && systemState == SystemState.IDLE)) {
            SynchronizationTask task = new SynchronizationTask(this.mappingConfiguration, this.externalDao, this.syncDao, this.daisyDao, this.locker);
            task.setStartState(systemState);
            new Thread(task).start();
            hasStarted = true;
            logger.info("Started synchronization " + (manualStart ? "manually" : "automatically"));
        }

        return hasStarted;
    }

    public ExternalEntityDao getExternalDao() {
        return externalDao;
    }

    public InternalEntityDao getDaisyDao() {
        return daisyDao;
    }

    public SyncEntityDao getSyncDao() {
        return syncDao;
    }

    public Locker getLocker() {
        return locker;
    }

    public void setLocker(Locker locker) {
        this.locker = locker;
    }

    private class SynchronizationTask implements Runnable {

        private EntityValueComparator valueComparator = new EntityValueComparator();

        private MappingConfiguration mappingConfiguration;

        private InternalEntityDao daisyDao;

        private SyncEntityDao syncDao;

        private ExternalEntityDao externalDao;

        private Locker locker;

        private SystemState startState;

        public SystemState getStartState() {
            return startState;
        }

        public void setStartState(SystemState startState) {
            this.startState = startState;
        }

        public SynchronizationTask(MappingConfiguration mappingConfiguration, ExternalEntityDao externalDao, SyncEntityDao syncDao, InternalEntityDao daisyDao,
                Locker locker) {
            this.mappingConfiguration = mappingConfiguration;
            this.externalDao = externalDao;
            this.syncDao = syncDao;
            this.daisyDao = daisyDao;
            this.locker = locker;
        }

        public void run() {
            try {
                boolean success = this.locker.changeLockState(startState, SystemState.SYNCING);
                if (!success)
                    throw new Exception("Could not change lock state from " + startState.toString() + " to " + SystemState.SYNCING.toString());
                
                
                for (EntityMapping entityMapping : this.mappingConfiguration.getEntityMappings()) {
                    logger.fine("Handling entities with name " + entityMapping.getEntityName());
                    externalSync(entityMapping);
                    internalSync(entityMapping);
                }
                success = this.locker.changeLockState(SystemState.SYNCING, SystemState.IDLE);
                if (!success)
                    throw new Exception("Could not change lock state from " + SystemState.SYNCING.toString() + " to " + SystemState.IDLE.toString());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Could not start synchronization", e);
            } finally {
                if (this.locker.getLockState() != SystemState.IDLE) {
                  boolean success = this.locker.changeLockState(SystemState.SYNCING, SystemState.IDLE);
                  if (!success) {
                      logger.logp(Level.WARNING, "SynchronizationTask", "run", "Could not change lock state from {0} to {1}", new Object[]{SystemState.SYNCING, SystemState.IDLE});
                  }
                }
            }

        }

        private void externalSync(EntityMapping entityMapping) throws Exception {
            logger.fine("Starting sync with external data source.");
            String entityName = entityMapping.getEntityName();
            String internalName = entityMapping.getDaisyDocumentTypeName();
            List<Long> ids = this.externalDao.getEntityIds(entityName);
            for (Long extId : ids) {
                logger.fine("Handling external entity with id " + extId);
                List<Entity> externalEntities = this.externalDao.getEntity(entityName, extId);

                for (Entity externalEntity : externalEntities) {
                    String language = externalEntity.getLanguage();                    
                    Entity syncEntity = this.syncDao.getEntity(internalName, extId, language);
                    Entity daisyEntity = this.daisyDao.getEntity(internalName, extId, language);
                    if (syncEntity != null) {                        
                        if (daisyEntity != null) {                            
                            handleEntity(externalEntity, syncEntity, daisyEntity);
                        } else {
                            if (syncEntity.isExternalDeleted()) {
                                // this is a resurrection
                                // try to find daisy document (if it was retired)
                                try {
                                    daisyEntity = daisyDao.getEntity(syncEntity.getDaisyVariantKey());                                    
                                    // found. now try to unretire the document if possible
                                    handleEntity(externalEntity, syncEntity, daisyEntity);
                                } catch (EntityNotFoundException e) {
                                    // document was 'hard' deleted. Trying to recreate it
                                    logger.warning("Daisy entity (" + syncEntity.getDaisyVariantKey() + ") seems to have been hard deleted. External entity (" + externalEntity.getName() + " - " + externalEntity.getExternalId() + ") has been resurrected so we will try to recreate the daisy document");
                                    externalEntity.setDaisyDeleted(false);
                                    externalEntity.setState(SyncState.SYNC_EXT2DSY);
                                    daisyDao.storeEntity(externalEntity);
                                    syncDao.replaceEntity(syncEntity.getDaisyVariantKey(), externalEntity);
                                }
                                
                            } else {
                                //CASE 12 : deleted in daisy only
                                logger.fine("Daisy entity seems to have been deleted. Marking this in the sync entity");
                                
                                syncEntity.setDaisyDeleted(true);
                                syncEntity.setState(SyncState.DSY_OVERWRITE);
                                this.syncDao.storeEntity(syncEntity);
                            }
                        }
                    } else {
                        if (daisyEntity != null) {
                            logger.logp(Level.WARNING, this.getClass().getName(), "externalSync", "An entity exists in daisy (variantKey : {0} - extid : {1}) but not in sync. The external id might be faked", new  Object[]{daisyEntity.getDaisyVariantKey().toString(), daisyEntity.getExternalId()} );
                            // CASE 14 : entity exists in daisy but not in sync
                            // (sync is not in sync)
                        } else {
                            // CASE 1 : new entity instance
                            logger.logp(Level.FINE, this.getClass().getName(), "externalSync", "New entity found (extid : {0}), will be created in daisy & sync", new Object[]{externalEntity.getExternalId()});
                            externalEntity.setState(SyncState.SYNC_EXT2DSY);
                            this.daisyDao.storeEntity(externalEntity);
                            this.syncDao.storeEntity(externalEntity);
                        }
                    }
                }
            }
        }

        private void internalSync(EntityMapping entityMapping) throws Exception {
            // checks if there are new entities in daisy
            logger.fine("Starting internal synchronization. Which looks for daisy only entities");
            List<VariantKey> keys = this.daisyDao.getEntityIds(entityMapping.getDaisyDocumentTypeName());
            for (VariantKey key : keys) {
                Entity daisyEntity = daisyDao.getEntity(key);
                if (daisyEntity.getExternalId() > 0) {
                    // externalId has been filled in                    
                    /*
                    try {
                        Entity syncEntity = syncDao.getEntity(key);
                        // Found the entity
                    } catch (EntityNotFoundException e) {
                        // Did not find the entity.  Don't do anything
                    }
                    */
                  
                  List<Entity> extEntities = externalDao.getEntity(daisyEntity.getName(), daisyEntity.getExternalId());
                  if (extEntities == null || extEntities.size() == 0 ) {
                  // if not found check if it has ever existed                  
                    try {
                      Entity syncEntity = syncDao.getEntity(key);
                      // Found the entity, so it existed. Now delete(retire) it in daisy
                      daisyEntity.setExternalDeleted(true);
                      daisyEntity.setDaisyDeleted(true);
                      daisyDao.storeEntity(daisyEntity);
                      
                      syncEntity.setExternalDeleted(true);
                      syncEntity.setDaisyDeleted(true);
                      syncDao.storeEntity(syncEntity);
                      
                      logger.fine("External entity (" + syncEntity.getName() + " ," + syncEntity.getExternalId() + ") has been deleted. Retiring daisy document with variant key " + daisyEntity.getDaisyVariantKey());
                    } catch (EntityNotFoundException es) {
                      logger.warning("Found daisy only entity with variant key " + daisyEntity.getDaisyVariantKey() + ". The document contains an external id  (" + daisyEntity.getName() + " ," + daisyEntity.getExternalId() + ") but doesn't exist on the system.");
                      // can't do anything
                    }
                  }
                  
                } else {
                    // externalId has not been filled in so this is a new entity
                    // instance
                    logger.fine("Found daisy only entity with variant key " + daisyEntity.getDaisyVariantKey());
                    daisyEntity.setState(SyncState.DSY_ONLY);
                    this.syncDao.storeEntity(daisyEntity);
                }
            }
        }
        
        // call from externalSync
        private void handleEntity(Entity externalEntity, Entity syncEntity, Entity daisyEntity) {
            if (syncEntity.getDaisyVersion() > daisyEntity.getDaisyVersion()) {
                logger.logp(Level.WARNING, "SynchronizationTask", "externalSync", "There is an entity (extid: {0}) has a greater daisy version ({1}) then the daisy entity ({2}). Has daisy had a database recovery?", new Object[]{syncEntity.getExternalId(), syncEntity.getDaisyVersion(), daisyEntity.getDaisyVersion()});
                // CASE 13 : a restore of the sync_store must
                // have been done
                // TODO catch database restores
            } else {
                if (this.valueComparator.compare(externalEntity, syncEntity) == 0 && !syncEntity.isExternalDeleted()) {
                    logger.fine("The external entity has not been updated");
                    // ext not updated -> check if daisy is in sync
                    if (syncEntity.getState() != SyncState.CONFLICT && syncEntity.getState() != SyncState.CONFLICT_DSY_RULES ) {
                        if (this.valueComparator.compare(daisyEntity, syncEntity) != 0) {
                            logger.fine("Daisy entity != sync entity. Setting entity state to " + SyncState.DSY_OVERWRITE);
                            // CASE 4 / 11
                            syncEntity.setState(SyncState.DSY_OVERWRITE);
                            //syncEntity.setAttributes(externalEntity.getAttributes());
                        } else {
                            logger.fine("Entity is completely in sync. Setting status to " + SyncState.SYNC_EXT2DSY);
                            // ext == sync == dsy -> ext2dsy
                            syncEntity.setState(SyncState.SYNC_EXT2DSY);
                        }
                        if (daisyEntity.getDaisyVersion() > syncEntity.getDaisyVersion()) {
                            // CASE 3, 4 & 11
                            syncEntity.setDaisyVersion(daisyEntity.getDaisyVersion());
                            this.syncDao.storeEntity(syncEntity);
                        }
                    } else {
                        logger.logp(Level.INFO, "SynchronizationTask", "externalSync", "Found old conflicting entity with external id {0}. Will not update until conflict has been resolved.", externalEntity.getExternalId());
                        // we wait until the conflict has been resolved
                    }
                } else {
                    logger.fine("The external entity has been updated or resurrected");
                    //  ext has been updated -> always update sync, check if daisy should be updated                                 
                    if (this.valueComparator.compare(daisyEntity, syncEntity) != 0 || syncEntity.isDaisyDeleted() != daisyEntity.isDaisyDeleted()) {
                        logger.fine("Daisy entity not in sync with sync entity");
                        // daisy not in sync with sync store
                        // these two lines keep the ext store in sync
                        syncEntity.setAttributes(externalEntity.getAttributes());
                        syncEntity.setExternalLastModified(externalEntity.getExternalLastModified());
                        
                        if (syncEntity.getState() != SyncState.CONFLICT_DSY_RULES) {
                            if (this.valueComparator.compare(externalEntity, daisyEntity) != 0 || daisyEntity.isDaisyDeleted()) {
                                // Do an extra check to see if external entity matches daisy. 
                                // this situation may occur if the mapping changes.
                                logger.logp(Level.INFO, this.getClass().getName(), "externalSync", "New conflict found. Entity with external id {0} and daisy variant key {1}", new Object[]{externalEntity.getExternalId(), daisyEntity.getDaisyVariantKey()});
                                syncEntity.setState(SyncState.CONFLICT);
                                //TODO notify administration !
                                // only update sync & leave daisy alone
                                
                            } else {
                                logger.fine("Ext entity not in sync entity but daisy entity is in sync with the external entity. Just updating sync entity values and continuing");
                                // daisy equals external store so no conflict here.  only update sync data
                                syncEntity.setDaisyDeleted(daisyEntity.isDaisyDeleted());
                                syncEntity.setState(SyncState.SYNC_EXT2DSY);
                            }
                        }
                    } else {
                        logger.fine("Daisy entity in sync with sync entity. Updating daisy/sync entity values");
                        syncEntity.setAttributes(externalEntity.getAttributes());
                        syncEntity.setExternalLastModified(externalEntity.getExternalLastModified());
                        syncEntity.setDaisyDeleted(false);
                        // ext data changed and daisy is in sync with sync store -> update dsy & sync
                        syncEntity.setState(SyncState.SYNC_EXT2DSY);
                        this.daisyDao.storeEntity(syncEntity);
                    }
                    
                    syncEntity.setExternalDeleted(false);
                    this.syncDao.storeEntity(syncEntity);                                    
                }
            }
        }
    }
}
