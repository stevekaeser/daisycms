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
importPackage(org.outerj.daisy.sync);
importClass(Packages.org.springframework.context.support.FileSystemXmlApplicationContext);

cocoon.load("resource://org/outerj/daisy/frontend/util/daisy-util.js");

var daisy = new Daisy();
var context;
var syncService;

function getService() {
    if (context == null || syncService == null) {
      var appContextFile = new java.io.File(daisy.resolve("applicationContext.xml"));      
      context = new FileSystemXmlApplicationContext(appContextFile);
      syncService = context.getBean("syncService");
    }
    return syncService;
}

function conflicts () {
    var entities = getService().getConflicts();   
    var viewData = {
        "pageContext" : daisy.getPageContext(daisy.getRepository()),
        "baseUrl" : cocoon.request.getRequestURI() + "/../" ,
        "entityMap" : entities            
        };
    cocoon.sendPage("conflictOverviewPipe", viewData);      
}

function dsyonly () {
    var entities = getService().getDaisyOnlys();   
    var viewData = {
        "pageContext" : daisy.getPageContext(daisy.getRepository()),
        "baseUrl" : cocoon.request.getRequestURI() + "/../" ,
        "entityMap" : entities            
        };
    cocoon.sendPage("daisyOnlyOverviewPipe", viewData);      
}

function dsydel () {
    var entities = getService().getDaisyDeletes();   
    var viewData = {
        "pageContext" : daisy.getPageContext(daisy.getRepository()),
        "baseUrl" : cocoon.request.getRequestURI() + "/../" ,
        "entityMap" : entities            
        };
    cocoon.sendPage("daisyDeleteOverviewPipe", viewData);      
}

function dsyoverrule () {
    var entities = getService().getPermanentDaisyOverrules();   
    var viewData = {
        "pageContext" : daisy.getPageContext(daisy.getRepository()),
        "baseUrl" : cocoon.request.getRequestURI() + "/../" ,
        "entityMap" : entities            
        };
    cocoon.sendPage("daisyOverruleOverviewPipe", viewData);      
}

function conflictDetail () {
  var documentId = cocoon.parameters.documentId;
  var branchId = cocoon.parameters.branchId;
  var languageId = cocoon.parameters.languageId;
  if (cocoon.request.getMethod() == "GET")  {    
    var syncEntity = getService().getSyncEntity(documentId, branchId, languageId);
    if (syncEntity.getState() == SyncState.CONFLICT) {    
      var dsyEntity = getService().getDaisyEntity(documentId, branchId, languageId);
      var viewData = {
        "pageContext" : daisy.getPageContext(daisy.getRepository()),
        "baseUrl" : cocoon.request.getRequestURI() + "/../../" ,
        "syncEntity" : syncEntity,
        "daisyEntity" : dsyEntity
        };
      cocoon.sendPage("conflict-compare-DetailPipe", viewData);
    } else {
      cocoon.sendPage("NotConflictPipe");
    }
  } else {
    var resolution = SyncState.valueOf(cocoon.request.getParameter("resolution"));
    getService().resolveConflict(documentId, branchId, languageId, resolution);
    cocoon.redirectTo(daisy.getMountPoint() + "/" + daisy.getSiteConf().getName()  + "/ext/sync/conflicts", true);
  }
}

function extOverrideAll () {
    var entities = getService().getConflicts();
    var listIterator = entities.values().iterator();   
    while (listIterator.hasNext()) { 
      var list = listIterator.next();
      for (var i = 0; i < list.size(); i++) {
        var entity = list.get(i);
        var key = entity.getDaisyVariantKey();
        var resolution = SyncState.SYNC_EXT2DSY;
        getService().resolveConflict(key.getDocumentId(), key.getBranchId(), key.getLanguageId(),resolution);
      }
    }
    cocoon.redirectTo(daisy.getMountPoint() + "/" + daisy.getSiteConf().getName()  + "/ext/sync/conflicts", true);
}
function daisyOverrideDetail () {
  var documentId = cocoon.parameters.documentId;
  var branchId = cocoon.parameters.branchId;
  var languageId = cocoon.parameters.languageId;
  if (cocoon.request.getMethod() == "GET")  {    
    var syncEntity = getService().getSyncEntity(documentId, branchId, languageId);
    if (syncEntity.getState() == SyncState.CONFLICT_DSY_RULES) {    
      var dsyEntity = getService().getDaisyEntity(documentId, branchId, languageId);
      var viewData = {
        "pageContext" : daisy.getPageContext(daisy.getRepository()),
        "baseUrl" : cocoon.request.getRequestURI() + "/../../" ,
        "syncEntity" : syncEntity,
        "daisyEntity" : dsyEntity
        };
      cocoon.sendPage("override-compare-DetailPipe", viewData);
    } else {
      cocoon.sendPage("NotOverridePipe");
    }
  } else {
    var resolution = SyncState.valueOf(cocoon.request.getParameter("resolution"));
    getService().turnOffOverride(documentId, branchId, languageId, resolution);
    cocoon.redirectTo(daisy.getMountPoint() + "/" + daisy.getSiteConf().getName()  + "/ext/sync/dsyoverrule", true);
  }
}

function recreateDaisyDocument () {
  var documentId = cocoon.parameters.documentId;
  var branchId = cocoon.parameters.branchId;
  var languageId = cocoon.parameters.languageId;
  if (cocoon.request.getMethod() == "GET")  {
    var syncEntity = getService().getSyncEntity(documentId, branchId, languageId);
    if (syncEntity.isDaisyDeleted()) {
      var viewData = {
        "pageContext" : daisy.getPageContext(daisy.getRepository()),
        "baseUrl" : cocoon.request.getRequestURI() + "/../../" ,
        "entity" : syncEntity
        };
      cocoon.sendPage("recreateDocumentPipe", viewData);
    } else {
      cocoon.sendPage("NotDeletedPipe");
    }
  } else {  
    getService().recreateDeletedDocument(documentId, branchId, languageId);
    cocoon.redirectTo(daisy.getMountPoint() + "/" + daisy.getSiteConf().getName()  + "ext/sync/dsydel", true);
  } 
}

function daisyOnlyDetail () {
  var documentId = cocoon.parameters.documentId;
  var branchId = cocoon.parameters.branchId;
  var languageId = cocoon.parameters.languageId;
  if (cocoon.request.getMethod() == "GET")  {    
    var entity = getService().getSyncEntity(documentId, branchId, languageId);
    if (entity.getState() == SyncState.DSY_ONLY) {     
      var viewData = {
        "pageContext" : daisy.getPageContext(daisy.getRepository()),
        "baseUrl" : cocoon.request.getRequestURI() + "/../../" ,
        "entity" : entity
        };
      cocoon.sendPage("daisyonly-entity-DetailPipe", viewData);
    } else {
      cocoon.sendPage("NotDaisyOnlyPipe");
    }
  } 
}

function triggerSync () {  
  if (cocoon.request.getMethod() == "GET")  {
    var status = getService().getLockState();
    var viewData = {
      "status" : status,
      "baseUrl" : cocoon.request.getRequestURI() + "/../" ,
      "pageContext" : daisy.getPageContext(daisy.getRepository())        
    };
    cocoon.sendPage("triggerSyncPipe", viewData);
  }else{
    var result = getService().startSynchronization();
    java.lang.Thread.sleep(1000);
    var status = getService().getLockState();
    var viewData = {
      "status": status,
      "baseUrl" : cocoon.request.getRequestURI() + "/../" ,
      "pageContext" : daisy.getPageContext(daisy.getRepository()),
      "commandResult" : result        
    };
    cocoon.sendPage("triggerSyncPipe", viewData);
  }
}

