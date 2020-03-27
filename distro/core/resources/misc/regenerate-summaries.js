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

importClass(Packages.org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager);
importClass(Packages.org.outerj.daisy.repository.Credentials);
importClass(Packages.org.outerj.daisy.summary.DocumentSummarizerImpl);
importClass(Packages.java.sql.DriverManager);

var repoUrl = arguments[0];
var repoUser = arguments[1];
var repoPassword = arguments[2];

var dbUrl = arguments[3];
var dbUser = arguments[4];
var dbPassword = arguments[5] || "";

var credentials = new Credentials(repoUser, repoPassword);
var repo = new RemoteRepositoryManager(repoUrl, credentials)
    .getRepository(credentials);

java.lang.Class.forName("com.mysql.jdbc.Driver");
var conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

function run() {
  var keys = repo.getQueryManager().performQueryReturnKeys("select name where true", Packages.java.util.Locale.US);

  var summarizer = new DocumentSummarizerImpl();

  var schema = repo.getRepositorySchema();

  deleteSummaries();

  try {
    for (var i = 0; i < keys.length; i++) {
      var doc = repo.getDocument(keys[i], false);
      
      for (var version = 1; version <= doc.getLastVersionId(); version++) {
        var summary = null;
        try {
          summary = summarizer.getSummary(doc, version, schema);
        } catch (e) {
          // when summarizer fails, no summary will be stored
        }

        var ns = repo.getNamespaceManager().getNamespace(doc.getNamespace()).getId();
        if (summary != null) {
          storeSummary(doc, version, summary, ns);
        }
      }
    }
  } finally {
    conn.close();
  }
}

function deleteSummaries() {
  var stmt;
  try {
    java.lang.System.out.println("Deleting existing summaries");
    stmt = conn.prepareStatement("delete from summaries where 1"); 
    stmt.executeUpdate();
  } finally {
    try { stmt.close(); } catch (e) {}
  }
}

function storeSummary(doc, version, summary, ns) {
  var stmt;
  try {
    java.lang.System.out.println("Storing summary for "+doc.getVariantKey()+" version "+version);
    stmt = conn.prepareStatement("insert into summaries(doc_id, ns_id, branch_id, lang_id, version_id, summary) values (?, ?, ?, ?, ?, ?)");
    stmt.setLong(1, doc.getSeqId());
    stmt.setLong(2, ns);
    stmt.setLong(3, doc.getBranchId());
    stmt.setLong(4, doc.getLanguageId());
    stmt.setLong(5, version);
    stmt.setString(6, summary);
    stmt.executeUpdate();
  } finally {
    try { stmt.close(); } catch (e) {}
  }
}


run();
