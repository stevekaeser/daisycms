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
package org.outerj.daisy.sync.mapping;

import java.util.logging.Logger;

import org.outerj.daisy.sync.dao.ExternalEntityDao;
import org.outerj.daisy.sync.dao.SyncEntityDao;

public abstract class AbstractAssociatedMapping implements AssociatedMapping{
  protected Logger logger;
  protected ExternalEntityDao externalEntityDao;
  //protected InternalEntityDao internalEntityDao;
  protected SyncEntityDao syncEntityDao; 
  
  protected MappingConfiguration configuration;
  
  public AbstractAssociatedMapping(ExternalEntityDao externalEntityDao, SyncEntityDao syncEntityDao, MappingConfiguration configuration) {
      this.externalEntityDao = externalEntityDao;
      this.syncEntityDao = syncEntityDao;
      this.configuration = configuration;
      this.logger = Logger.getLogger("org.outerj.daisy.sync.mapping");
  }
  
  public ExternalEntityDao getExternalEntityDao() {
    return externalEntityDao;
  }
  public void setExternalEntityDao(ExternalEntityDao externalEntityDao) {
    this.externalEntityDao = externalEntityDao;
  }
  /*
  public InternalEntityDao getInternalEntityDao() {
    return internalEntityDao;
  }
  public void setInternalEntityDao(InternalEntityDao internalEntityDao) {
    this.internalEntityDao = internalEntityDao;
  }
  */
  public SyncEntityDao getSyncEntityDao() {
    return syncEntityDao;
  }
  public void setSyncEntityDao(SyncEntityDao syncEntityDao) {
    this.syncEntityDao = syncEntityDao;
  }
  
  
}
