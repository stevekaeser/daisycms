SET FOREIGN_KEY_CHECKS=0;

#
# Schema changes for fine grained read access
#  This includes removal of the 'read live' permission
#

CREATE TABLE acl_accessdetail
(
   acl_id BIGINT NOT NULL,
   acl_object_id BIGINT NOT NULL,
   acl_entry_id BIGINT NOT NULL,
   ad_type VARCHAR (20) NOT NULL,
   ad_data VARCHAR (255),
   INDEX acl_accessdetail_I_1 (acl_id, acl_object_id, acl_entry_id)
) ENGINE=InnoDB;

alter table acl_entries
  add column read_detail CHAR NOT NULL after perm_delete;

update acl_entries set read_detail = 0;

# Add an explicit grant for non-live where read is grant
update acl_entries set read_detail = 1 where perm_read_live = 'G' and perm_read = 'G';

insert into acl_accessdetail(acl_id, acl_object_id, acl_entry_id, ad_type, ad_data)
  select acl_id, acl_object_id, id, 'non_live', 'grant' from acl_entries where perm_read_live = 'G' and perm_read = 'G';

# Move 'read live' to read with access details. This upgrade is not perfect in case
# the read permission is inherited from earlier rules.
update acl_entries set read_detail = 1 where perm_read_live = 'G' and perm_read = 'D';

insert into acl_accessdetail(acl_id, acl_object_id, acl_entry_id, ad_type, ad_data)
  select acl_id, acl_object_id, id, 'non_live', 'deny' from acl_entries where perm_read_live = 'G' and perm_read = 'D';

update acl_entries set perm_read = 'G' where perm_read_live = 'G' and perm_read = 'D';

# If 'read live' is denied, this implies that 'read' is also denied, make this explicit before dropping the read live
update acl_entries set perm_read = 'D' where perm_read_live = 'D';

alter table acl_entries
   drop column perm_read_live;


#
# Schema change to support new 'variants' identifier
#

alter table document_variants
  add column variant_search VARCHAR (100) NOT NULL after link_search;

update document_variants set variant_search = concat(branch_id, ':', lang_id);

alter table document_variants add INDEX variant_search (variant_search);

#
# Schema changes for translation management
#

# Reference language

alter table documents add column reference_lang_id BIGINT after private;
alter table documents add FOREIGN KEY (reference_lang_id) REFERENCES languages (id);

# Variants: add calculated last/live major change version columns

alter table document_variants add column last_major_change_version_id BIGINT after liveversion_id;
update document_variants set last_major_change_version_id = lastversion_id;
alter table document_variants add column live_major_change_version_id BIGINT after last_major_change_version_id;
update document_variants set live_major_change_version_id = liveversion_id where liveversion_id != -1;

# Versions

alter table document_versions change column state_last_modified last_modified DATETIME NOT NULL;

# rename of state_last_modifier fails because of FK -- do it the long way
alter table document_versions add column last_modifier BIGINT NOT NULL after state_last_modifier;
update document_versions set last_modifier = state_last_modifier;
# next statement relies on consistent auto-generated names by MySQL...
alter table document_versions drop foreign key document_versions_ibfk_1;
alter table document_versions drop column state_last_modifier;
alter table document_versions add FOREIGN KEY (last_modifier) REFERENCES `users` (id);

alter table document_versions add column synced_with_lang_id BIGINT after state;
alter table document_versions add column synced_with_version_id BIGINT after synced_with_lang_id;
alter table document_versions add column synced_with_search VARCHAR (100) after synced_with_version_id;
alter table document_versions add column change_type CHAR (1) NOT NULL after synced_with_search;
update document_versions set change_type = 'M';
alter table document_versions add column change_comment LONGTEXT after change_type;


#
# Locks table: fix bug: time_expires columns should not be 'not null'
#

alter table locks change time_expires time_expires datetime;


#
# Update schema version number
#

update daisy_system set propvalue = '2.2' where propname = 'schema_version';


SET FOREIGN_KEY_CHECKS=1;