# Set the following variable to the namespace you have selected
# for your repository
set @daisynamespace = 'DSY';

SET FOREIGN_KEY_CHECKS=0;

#
# The sequence tables where incorrectly using the INTEGER datatype,
# instead of the BIGINT datatype. The datatype should be something
# large enough to store a Java long value (which BIGINT is, but
# INTEGER isn't).
#
alter table branch_sequence change column maxid maxid BIGINT;
alter table language_sequence change column maxid maxid BIGINT;
alter table document_sequence change column maxid maxid BIGINT;
alter table comment_sequence change column maxid maxid BIGINT;
alter table event_sequence change column maxid maxid BIGINT;
alter table parttype_sequence change column maxid maxid BIGINT;
alter table localizedstring_sequence change column maxid maxid BIGINT;
alter table fieldtype_sequence change column maxid maxid BIGINT;
alter table documenttype_sequence change column maxid maxid BIGINT;
alter table collection_sequence change column maxid maxid BIGINT;
alter table user_sequence change column maxid maxid BIGINT;
alter table role_sequence change column maxid maxid BIGINT;
alter table task_sequence change column maxid maxid BIGINT;

#
# Create tables for namespaces
#

CREATE TABLE daisy_namespaces
(
    id BIGINT NOT NULL,
    name_ VARCHAR (200) NOT NULL,
    fingerprint VARCHAR (255) NOT NULL,
    registered_by BIGINT NOT NULL,
    registered_on DATETIME NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (registered_by) REFERENCES users (id),
    UNIQUE (name_)
) Engine=InnoDB;
show warnings;

CREATE TABLE namespace_sequence
(
  maxid BIGINT
) Engine=InnoDB;
show warnings;

#
# Adjust current repository tables to include document namespace
#

alter table document_variants
   drop primary key,
   add column ns_id BIGINT not null after doc_id,
   add primary key (doc_id, ns_id, branch_id, lang_id);
show warnings;

update document_variants set ns_id = 1;
show warnings;

alter table documents
   drop primary key,
   add column ns_id BIGINT not null after id,
   add column id_search VARCHAR(50) not null after ns_id,
   add primary key (id, ns_id),
   add index (id_search),
   add foreign key(ns_id) references daisy_namespaces(id);
show warnings;

update documents set ns_id = 1;
show warnings;
update documents set id_search = concat(id, '-', ns_id);
show warnings;

alter table document_collections
   drop primary key,
   add column ns_id BIGINT not null after document_id,
   add primary key (document_id, ns_id, branch_id, lang_id, collection_id);
show warnings;

update document_collections set ns_id = 1;
show warnings;

alter table summaries
   drop primary key,
   add column ns_id BIGINT not null after doc_id,
   add primary key (doc_id, ns_id, branch_id, lang_id);
show warnings;

update summaries set ns_id = 1;
show warnings;

alter table customfields
   drop primary key,
   add column ns_id BIGINT not null after doc_id,
   add primary key (doc_id, ns_id, branch_id, lang_id, name);
show warnings;

update customfields set ns_id = 1;
show warnings;

alter table locks
   drop primary key,
   add column ns_id BIGINT not null after doc_id,
   add primary key (doc_id, ns_id, branch_id, lang_id);
show warnings;

update locks set ns_id = 1;
show warnings;


# For extracted links, we need to modify index, but we don't have
# the name of the indexes, therefore recreate the table

alter table extracted_links rename to extracted_links_old;
show warnings;

CREATE TABLE extracted_links
(
    source_doc_id BIGINT NOT NULL,
    source_ns_id BIGINT NOT NULL,
    source_branch_id BIGINT NOT NULL,
    source_lang_id BIGINT NOT NULL,
    source_parttype_id BIGINT NOT NULL,
    target_doc_id BIGINT NOT NULL,
    target_ns_id BIGINT NOT NULL,
    target_branch_id BIGINT NOT NULL,
    target_lang_id BIGINT NOT NULL,
    target_version_id BIGINT NOT NULL,
    linktype CHAR (1) NOT NULL,
    in_last_version CHAR NOT NULL,
    in_live_version CHAR NOT NULL,
    INDEX extracted_links_I_1 (source_doc_id, source_ns_id),
    INDEX extracted_links_I_2 (target_doc_id, target_ns_id),
    foreign key (target_ns_id) references daisy_namespaces(id)
) Engine=InnoDB;
show warnings;


insert into extracted_links(source_doc_id,source_ns_id,source_branch_id,source_lang_id,source_parttype_id,
        target_doc_id,target_ns_id,target_branch_id,target_lang_id,target_version_id,linktype,in_last_version,
        in_live_version)
    select source_doc_id,1,source_branch_id,source_lang_id,source_parttype_id,
           target_doc_id,1,target_branch_id,target_lang_id,target_version_id,
           linktype,in_last_version,in_live_version from extracted_links_old;
show warnings;

drop table extracted_links_old;
show warnings;

alter table comments
   drop primary key,
   add column ns_id BIGINT not null after doc_id,
   add primary key (id);
show warnings;

update comments set ns_id = 1;
show warnings;

alter table document_versions
   drop primary key,
   add column ns_id BIGINT not null after doc_id,
   add primary key (doc_id,ns_id,branch_id,lang_id,id);
show warnings;

update document_versions set ns_id = 1;
show warnings;

alter table links
   drop primary key,
   add column ns_id BIGINT not null after doc_id,
   add primary key (doc_id,ns_id,branch_id,lang_id,version_id,id);
show warnings;

update links set ns_id = 1;
show warnings;

alter table parts
   drop primary key,
   add column ns_id BIGINT not null after doc_id,
   add primary key (doc_id,ns_id,branch_id,lang_id,version_id,parttype_id);
show warnings;

update parts set ns_id = 1;
show warnings;

# We create the thefields table newly and copy over the data, since that seems to
# be the only easy way to drop the index on link_docid when you don't know its name.

alter table thefields rename to thefields_old;
show warnings;

CREATE TABLE thefields
(
    doc_id BIGINT NOT NULL,
    ns_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    lang_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    fieldtype_id BIGINT NOT NULL,
    value_seq BIGINT NOT NULL,
    value_count BIGINT NOT NULL,
    stringvalue VARCHAR (255),
    datevalue DATETIME,
    datetimevalue DATETIME,
    integervalue BIGINT,
    floatvalue DOUBLE,
    decimalvalue DECIMAL (10, 5),
    booleanvalue CHAR,
    link_docid BIGINT,
    link_nsid BIGINT,
    link_searchdocid VARCHAR (50),
    link_branchid BIGINT,
    link_searchbranchid BIGINT,
    link_langid BIGINT,
    link_searchlangid BIGINT,
    link_search VARCHAR (100),
    PRIMARY KEY(doc_id,ns_id,branch_id,lang_id,version_id,fieldtype_id,value_seq),
    FOREIGN KEY (fieldtype_id) REFERENCES field_types (id),
    FOREIGN KEY (link_nsid) REFERENCES daisy_namespaces (id),
    INDEX thefields_I_1 (stringvalue),
    INDEX thefields_I_2 (datevalue),
    INDEX thefields_I_3 (datetimevalue),
    INDEX thefields_I_4 (integervalue),
    INDEX thefields_I_5 (floatvalue),
    INDEX thefields_I_6 (decimalvalue),
    INDEX thefields_I_7 (booleanvalue),
    INDEX thefields_I_8 (link_searchdocid),
    INDEX thefields_I_9 (link_searchbranchid),
    INDEX thefields_I_10 (link_searchlangid),
    INDEX thefields_I_11 (link_search),
    INDEX thefields_I_12 (fieldtype_id)
) Engine=InnoDB;
show warnings;

insert into thefields(doc_id, ns_id, branch_id, lang_id, version_id, fieldtype_id, value_seq,
    value_count, stringvalue, datevalue, datetimevalue, integervalue, floatvalue, decimalvalue,
    booleanvalue, link_docid, link_nsid, link_searchdocid, link_branchid, link_searchbranchid,
    link_langid, link_searchlangid, link_search)

    select doc_id, 1, branch_id, lang_id, version_id, fieldtype_id, value_seq,
    value_count, stringvalue, datevalue, datetimevalue, integervalue, floatvalue, decimalvalue,
    booleanvalue, link_docid, NULL, NULL, link_branchid, link_searchbranchid,
    link_langid, link_searchlangid, link_search from thefields_old;

show warnings;

update thefields set link_nsid = 1 where link_docid is not null;
show warnings;
update thefields set link_search = concat(left(link_search, locate('@', link_search) - 1), '-1', substring(link_search, locate('@', link_search))) where link_docid is not null;
show warnings;
update thefields set link_searchdocid = concat(link_docid, '-', link_nsid) where link_docid is not null;
show warnings;

drop table thefields_old;
show warnings;

alter table selectionlist_data
   add column link_nsid BIGINT after link_docid,
   add foreign key (link_nsid) references daisy_namespaces(id);
show warnings;

update selectionlist_data set link_nsid = 1 where link_docid is not null;
show warnings;

alter table task_doc_details
    modify column doc_id VARCHAR(255) NOT NULL;
show warnings;

alter table document_subscriptions
    modify column doc_id VARCHAR(255) NOT NULL;
show warnings;

# '*' is the new wildcard for indicating a subscription to all documents
update document_subscriptions set doc_id = '*' where doc_id = '-1';
show warnings;
update document_subscriptions set doc_id = concat(doc_id, '-', @daisynamespace) where doc_id != '*';
show warnings;


# Add a column to the parts table to keep track of the last
# version in which a part changed.
alter table parts
    add column changed_in_version BIGINT NOT NULL;
show warnings;

# Ideally we would set the changed_in_version column to the last version in which the blobkey changed,
# though just doing it like this for now
update parts set changed_in_version = version_id;
show warnings;

#
# Changes for 'editable' property
#
alter table doctypes_fieldtypes
    add column editable CHAR NOT NULL after required;
show warnings;
update doctypes_fieldtypes set editable = 1;
show warnings;
alter table doctype_contentmodel
    add column editable CHAR NOT NULL after required;
show warnings;
update doctype_contentmodel set editable = 1;
show warnings;

#
# Changes for hierarchical fields
#

# hierarchical property on field types
alter table field_types
    add column hierarchical CHAR NOT NULL after multivalue;
show warnings;
update field_types set hierarchical = 0;
show warnings;

# add columns to thefields table
alter table thefields
    drop primary key,
    add column hier_seq BIGINT NOT NULL after value_seq,
    add column hier_count BIGINT NOT NULL after value_count,
    add primary key(doc_id,ns_id,branch_id,lang_id,version_id,fieldtype_id,value_seq,hier_seq);
show warnings;

# changes to selectionlist_data to support hierarchical data
alter table selectionlist_data
    add column depth SMALLINT NOT NULL after sequencenr;
show warnings;
update selectionlist_data set depth = 0;
show warnings;

# new tables for hierarchical query selection list
CREATE TABLE hierquerysellist
(
    fieldtype_id BIGINT NOT NULL,
    whereclause LONGTEXT NOT NULL,
    filtervariants CHAR NOT NULL,
    FOREIGN KEY (fieldtype_id) REFERENCES field_types (id),
    UNIQUE (fieldtype_id)
) Engine=InnoDB;
show warnings;

CREATE TABLE hierquerysellist_fields
(
    fieldtype_id BIGINT NOT NULL,
    sequencenr SMALLINT NOT NULL,
    fieldname VARCHAR (50) NOT NULL,
    FOREIGN KEY (fieldtype_id) REFERENCES hierquerysellist (fieldtype_id),
    UNIQUE (fieldtype_id, sequencenr)
) Engine=InnoDB;
show warnings;


#
# Selection list async loading flag
#
alter table field_types
    add column selectlist_load_async CHAR NOT NULL after selectlist_free_entry;
show warnings;
update field_types set selectlist_load_async = 0;
show warnings;

#
# Workflow pool tables
#
CREATE TABLE wfpool_sequence
(
    maxid BIGINT
) Engine=InnoDB;

CREATE TABLE wf_pools
(
    id BIGINT NOT NULL,
    name_ VARCHAR (100) NOT NULL,
    description VARCHAR (255),
    last_modified DATETIME NOT NULL,
    last_modifier BIGINT NOT NULL,
    updatecount BIGINT NOT NULL,
    PRIMARY KEY(id),
    UNIQUE (name_)
) Engine=InnoDB;

CREATE TABLE wf_pool_members
(
    pool_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    added DATETIME NOT NULL,
    adder BIGINT NOT NULL,
    PRIMARY KEY(pool_id,user_id),
    FOREIGN KEY (pool_id) REFERENCES wf_pools (id),
    INDEX wf_pool_members_I_1 (user_id)
) Engine=InnoDB;


#
# Addition of link_search column to document_variants
# (to support link dereference mechanism in query language)
#

alter table document_variants
   add column link_search VARCHAR(100) not null after lang_id,
   add index (link_search)
;
show warnings;

update document_variants set link_search = concat(doc_id, '-', ns_id, '@', branch_id, ':', lang_id);
show warnings;


# update schema version number
update daisy_system set propvalue = '2.0-M1' where propname = 'schema_version';
show warnings;

SET FOREIGN_KEY_CHECKS=1;
