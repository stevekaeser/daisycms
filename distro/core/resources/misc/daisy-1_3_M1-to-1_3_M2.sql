# changes for authentication scheme support
alter table users add column auth_scheme VARCHAR (50) NOT NULL after confirmkey;
update users set auth_scheme = 'daisy';
alter table users change column password password VARCHAR (50);

# changes for multi-value field support
alter table thefields add column value_seq bigint not null after fieldtype_id;
update thefields set value_seq = 0;
alter table thefields add column value_count bigint not null after value_seq;
update thefields set value_count = 1;
alter table thefields drop primary key;
alter table thefields add primary key (doc_id, branch_id, lang_id, version_id, fieldtype_id, value_seq);

alter table field_types add column multivalue char not null after acl_allowed;
update field_types set multivalue = 0;

# changes for multiple active roles
# foreign key constraint for default role: default role is now optional,
# and this check wasn't really necessary either since the role existance
# is also tested via the user-role association table
alter table users drop foreign key users_ibfk_1;
alter table users drop index default_role;

# new column for delete permission, initialiase to grant for entries that have write access
alter table acl_entries add column perm_delete char(1) not null after perm_publish;
update acl_entries set perm_delete = 'D';
update acl_entries set perm_delete = 'G' where perm_write = 'G';

# add filename for parts
alter table parts add column filename varchar(255) after mimetype;