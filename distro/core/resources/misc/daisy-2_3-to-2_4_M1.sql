SET FOREIGN_KEY_CHECKS=0;

#
# Schema changes for document tasks
#

alter table `document_tasks` modify column action_type varchar(100) NOT NULL after `description`;
alter table `document_tasks` modify column state varchar(1) NOT NULL after `owner`;

alter table `document_tasks` add column `max_tries` smallint(6) NOT NULL after `state`;
alter table `document_tasks` add column `try_count` smallint(6) NOT NULL DEFAULT 0 after `started_at`;
alter table `document_tasks` add column `retry_interval` bigint(20) DEFAULT NULL after `try_count`;

alter table `document_tasks` modify column finished_at datetime DEFAULT NULL after `details`;
alter table `document_tasks` modify column progress varchar(255) NOT NULL after `finished_at`;

alter table `task_doc_details` add column `try_count` smallint(6) NOT NULL DEFAULT 0 after `state`;

update `document_versions` set `name` = '' where name is null;
alter table `document_versions` modify column `name` varchar(1023) NOT NULL;

CREATE TABLE  `live_history` (
  `id` bigint(20) NOT NULL,
  `doc_id` bigint(20) NOT NULL,
  `ns_id` bigint(20) NOT NULL,
  `branch_id` bigint(20) NOT NULL,
  `lang_id` bigint(20) NOT NULL,
  `version_id` bigint(20) NOT NULL,
  `begin_date` datetime NOT NULL,
  `end_date` datetime DEFAULT NULL,
  `created_on` datetime NOT NULL,
  `created_by` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `created_by` (`created_by`),
  KEY `live_history_I_2` (`doc_id`),
  CONSTRAINT `live_history_ibfk_1` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

#
# Create a 'dummy' live history: for every document, assume the current live version has been live since the document was created
#
set @dummy = 0;
insert into live_history (id, doc_id, ns_id, branch_id, lang_id, version_id, begin_date, end_date, created_on, created_by) select ( @dummy := @dummy + 1 ) as d, var.doc_id, var.ns_id, var.branch_id, var.lang_id, var.liveversion_id, ver.created_on, null, ver.created_on, ver.created_by from document_variants var left join document_versions ver on var.doc_id = ver.doc_id and var.ns_id = ver.ns_id and var.branch_id = ver.branch_id and var.lang_id = ver.lang_id where ver.id = 1 and var.liveversion_id is not null;

CREATE TABLE  `live_history_sequence` (
  `maxid` bigint(20) DEFAULT NULL
) ENGINE=InnoDB;

insert into live_history_sequence(maxid) select max(id) from live_history;

#
# Add version_id column and add it to the primary key
# Then create a summary for every version (summaries should be recalculated but we can't do that using just sql)
# Then delete the old summaries
# 
alter table `summaries` add column `version_id` int NOT NULL after `lang_id`;
alter table `summaries` drop primary key;
alter table `summaries` ADD PRIMARY KEY (`doc_id`, `ns_id`, `branch_id`, `lang_id`, `version_id`);
insert into summaries(doc_id, ns_id, branch_id, lang_id, version_id, summary) select v.doc_id, v.ns_id, v.branch_id, v.lang_id, v.id, summary from document_versions v left join summaries s on v.doc_id = s.doc_id and v.ns_id = s.ns_id and v.branch_id = s.branch_id and v.lang_id = s.lang_id where s.summary is not null;
delete from summaries where version_id = 0;

alter table `acl_entries` add column `publish_detail` char(1) NOT NULL after `write_detail`;
update `acl_entries` set `publish_detail`=0 where 1;

# 
# extracted links are stored for all versions, not just live and last
# existing records are deleted and should be recreated as per the daisy 2.4 upgrade instructions
#
delete from extracted_links where 1;

ALTER TABLE `extracted_links`
  DROP COLUMN `in_last_version`,  
  DROP COLUMN `in_live_version`,  
  ADD COLUMN `source_version_id` BIGINT(20)  NOT NULL AFTER `source_lang_id`;

#
# Repository revisions
#
CREATE TABLE `repository_revisions` (
 `ns_id` bigint(20) NOT NULL,
 `doc_id` bigint(20) NOT NULL,
 `revision_date` datetime DEFAULT NULL
) ENGINE=InnoDB;

# Migrate revisions
insert into repository_revisions(ns_id, doc_id, revision_date) select ns_id, doc_id, begin_date from live_history;
insert into repository_revisions(ns_id, doc_id, revision_date) select ns_id, doc_id, end_date from live_history;

#
# Update schema version number
#

update daisy_system set propvalue = '2.4-M1' where propname = 'schema_version';

SET FOREIGN_KEY_CHECKS=1;

