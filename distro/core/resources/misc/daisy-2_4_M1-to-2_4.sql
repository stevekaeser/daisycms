SET FOREIGN_KEY_CHECKS=0;

#
# Schema changes for replication
#

CREATE TABLE  `replication` (
  `id` bigint(20) NOT NULL,
  `document_id` varchar(255) NOT NULL,
  `branch_id` bigint(20) NOT NULL,
  `lang_id` bigint(20) NOT NULL,
  `target` varchar(255) NOT NULL,
  `state` char(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE  `replication_sequence` (
  `maxid` bigint(20) NOT NULL
) ENGINE=InnoDB;

#
# Update schema version number
#

update daisy_system set propvalue = '2.4' where propname = 'schema_version';

SET FOREIGN_KEY_CHECKS=1;

