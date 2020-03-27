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
package org.outerj.daisy.sync.service;

import java.util.List;
import java.util.Map;

import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityNotFoundException;
import org.outerj.daisy.sync.SyncState;
import org.outerj.daisy.sync.SystemState;

public interface SyncService {
  Map<String, List<Entity>> getConflicts();

  Map<String, List<Entity>> getPermanentDaisyOverrules();

  Map<String, List<Entity>> getDaisyOnlys();

  Map<String, List<Entity>> getDaisyDeletes();

  Entity getDaisyEntity(String documentId, long branchId, long languageId) throws EntityNotFoundException;
  
  Entity getSyncEntity(String documentId, long branchId, long languageId) throws EntityNotFoundException;
  
  void resolveConflict(String documentId, long branchId, long languageId, SyncState resolution) throws Exception;
  
  void turnOffOverride(String documentId, long branchId, long languageId, SyncState resolution) throws Exception;
  
  void recreateDeletedDocument(String documentId, long branchId, long languageId) throws Exception;
  
  boolean startSynchronization ();
  
  SystemState getLockState();
}
