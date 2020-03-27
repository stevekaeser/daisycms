SET FOREIGN_KEY_CHECKS=0;

#
# Schema changes for document tasks
#

alter table `document_tasks` change column `scriptlanguage` `action_type` varchar(100) NOT NULL;
alter table `document_tasks` change column `script` `action_parameters` longtext;

#
# Schema changes for manageable namespaces
#
alter table `document_sequence` add column `ns_id` bigint(20);
alter table `document_sequence` add unique (`ns_id`);
alter table `document_sequence` add foreign key (`ns_id`) references `daisy_namespaces` (`id`);
alter table `daisy_namespaces` add unique (`fingerprint`);

#
# Schema changes for write permission details
#

alter table `acl_entries` add column `write_detail` CHAR NOT NULL after `read_detail`;
update `acl_entries` set `write_detail` = 0;
alter table `acl_accessdetail` add column `acl_permission` CHAR (1) NOT NULL after `acl_entry_id`;
update `acl_accessdetail` set `acl_permission` = 'R';


# Add ACL rules to make "conceptual document" behavior and
# owner behaviour backwards compatible.
# These statements assume that no one has more than 100000 ACL objects,
# which should be about 99890 more than most users have

insert into acl_objects(acl_id,id,objectspec)
            values
            (1, 100000, "conceptual = 'true'"),
            (2, 100000, "conceptual = 'true'"),
            (1, 100001, "true"),
            (2, 100001, "true");

insert into acl_entries(acl_id,acl_object_id,id,subject_user_id,subject_role_id,subject_type,perm_read,perm_write,
                       perm_publish,perm_delete,read_detail,write_detail)
            values
            (1,100000,0,NULL,NULL,'E','G','G','G','G',0,0),
            (1,100000,1,NULL,(select id from roles where name = 'guest'),'R','D','D','D','D',0,0),
            (2,100000,0,NULL,NULL,'E','G','G','G','G',0,0),
            (2,100000,1,NULL,(select id from roles where name = 'guest'),'R','D','D','D','D',0,0),
            (1,100001,0,NULL,NULL,'O','G','G','N','G',0,0),
            (2,100001,0,NULL,NULL,'O','G','G','N','G',0,0);

#
# For consistency, these columns are now nullable.  -1 values are replaced with NULLs
#

alter table `document_variants` change column `liveversion_id` `liveversion_id` bigint(20) default NULL;
alter table `document_variants` change column `created_from_branch_id` `created_from_branch_id` bigint(20) default NULL;
alter table `document_variants` change column `created_from_lang_id` `created_from_lang_id` bigint(20) default NULL;
alter table `document_variants` change column `created_from_version_id` `created_from_version_id` bigint(20) default NULL;
update document_variants set `liveversion_id` = NULL where `liveversion_id` = -1;
update document_variants set `created_from_branch_id` = NULL where `created_from_branch_id` = -1;
update document_variants set `created_from_lang_id` = NULL where `created_from_lang_id` = -1;
update document_variants set `created_from_version_id` = NULL where `created_from_version_id` = -1;

#
# Update schema version number
#

update daisy_system set propvalue = '2.3' where propname = 'schema_version';

# Set the document sequence for the default namespace
update document_sequence set ns_id = 1;

SET FOREIGN_KEY_CHECKS=1;
