# new columns and indexes for thefields table for link field type support
alter table thefields add column link_docid bigint after booleanvalue;
alter table thefields add column link_branchid bigint after link_docid;
alter table thefields add column link_searchbranchid bigint after link_branchid;
alter table thefields add column link_langid bigint after link_searchbranchid;
alter table thefields add column link_searchlangid bigint after link_langid;
alter table thefields add column link_search varchar(100) after link_searchlangid;

alter table thefields add index (link_docid);
alter table thefields add index (link_searchbranchid);
alter table thefields add index (link_searchlangid);
alter table thefields add index (link_search);

# table to store the new link query selection lists
CREATE TABLE linkquerysellist
(
    fieldtype_id BIGINT NOT NULL,
    whereclause LONGTEXT NOT NULL,
    filtervariants CHAR NOT NULL,
    FOREIGN KEY (fieldtype_id) REFERENCES field_types (id),
    UNIQUE (fieldtype_id)
) Type=InnoDB;

# new columns for link field type in selectionlist_data
alter table selectionlist_data add column link_docid bigint after booleanvalue;
alter table selectionlist_data add column link_branchid bigint after link_docid;
alter table selectionlist_data add column link_langid bigint after link_branchid;

# new allowFreeEntry attribute for field types
alter table field_types add column selectlist_free_entry char not null after selectionlist_type;
update field_types set selectlist_free_entry = 0;

# table to store new query selection lists
CREATE TABLE querysellist
(
    fieldtype_id BIGINT NOT NULL,
    query LONGTEXT NOT NULL,
    filtervariants CHAR NOT NULL,
    sort_order VARCHAR (1) NOT NULL,
    FOREIGN KEY (fieldtype_id) REFERENCES field_types (id),
    UNIQUE (fieldtype_id)
) Type=InnoDB;

# New system information table with schema version
CREATE TABLE daisy_system
(
   propname VARCHAR (100) NOT NULL,
   propvalue VARCHAR (255) NOT NULL,
   PRIMARY KEY(propname)
) Type=InnoDB;
insert into daisy_system(propname, propvalue) values('schema_version', '1.5-M1');
