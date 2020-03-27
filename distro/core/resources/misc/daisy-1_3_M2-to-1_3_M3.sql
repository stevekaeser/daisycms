alter table acl_entries add column perm_read_live char(1) not null after subject_type;
update acl_entries set perm_read_live = 'D';
update acl_entries set perm_read_live = 'G' where perm_read = 'G';