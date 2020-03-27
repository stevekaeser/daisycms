#
# To create/use a custom language when migrating the repository, adjust
# the variables below as follows:
#  - set @daisylangid to 2
#  - set @daisylang to the language name you want to use (no spaces allowed, should
#                    validate against the regexp [a-zA-Z][a-zA-Z\-_0-9]*, see the following
#                    web page for a good list of language codes:
#                    http://ftp.ics.uci.edu/pub/ietf/http/related/iso639.txt)
#
set @daisylangid = 1;
set @daisylang = 'default';

SET FOREIGN_KEY_CHECKS = 0;

#
# Create the new branches and languages tables, including initial content.
#

CREATE TABLE branches
(
		            id BIGINT NOT NULL,
		            name VARCHAR (50) NOT NULL,
		            description VARCHAR (255),
		            last_modified DATETIME NOT NULL,
		            last_modifier BIGINT NOT NULL,
		            updatecount BIGINT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (last_modifier) REFERENCES users (id)
    ,
    UNIQUE (name),
    INDEX (last_modifier)
) Type=InnoDB;

CREATE TABLE branch_sequence
(
		            maxid INTEGER
) Type=InnoDB;

CREATE TABLE languages
(
		            id BIGINT NOT NULL,
		            name VARCHAR (50) NOT NULL,
		            description VARCHAR (255),
		            last_modified DATETIME NOT NULL,
		            last_modifier BIGINT NOT NULL,
		            updatecount BIGINT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (last_modifier) REFERENCES users (id)
    ,
    UNIQUE (name),
    INDEX (last_modifier)
) Type=InnoDB;

CREATE TABLE language_sequence
(
		            maxid INTEGER
) Type=InnoDB;

INSERT INTO branch_sequence (maxid)
    VALUES (1);

INSERT INTO language_sequence (maxid)
    VALUES (@daisylangid);

INSERT INTO branches (id,name,last_modified,last_modifier,updatecount)
    VALUES (1,'main',NOW(),1,1);

INSERT INTO languages (id,name,last_modified,last_modifier,updatecount)
    VALUES (1,'default',NOW(),1,1);

# Note: if @daisylang is default, this will fail with a warning, which is OK.
INSERT IGNORE INTO languages (id,name,last_modified,last_modifier,updatecount)
    VALUES (@daisylangid,@daisylang,NOW(),1,1);

#
# document_variants table
#

CREATE TABLE document_variants
(
		            doc_id BIGINT NOT NULL,
		            branch_id BIGINT NOT NULL,
		            lang_id BIGINT NOT NULL,
		            doctype_id BIGINT NOT NULL,
		            retired CHAR NOT NULL,
		            lastversion_id BIGINT NOT NULL,
		            liveversion_id BIGINT NOT NULL,
		            last_modified DATETIME NOT NULL,
		            last_modifier BIGINT NOT NULL,
		            updatecount BIGINT NOT NULL,
		            created_from_branch_id BIGINT NOT NULL,
		            created_from_lang_id BIGINT NOT NULL,
		            created_from_version_id BIGINT NOT NULL,
    PRIMARY KEY(doc_id,branch_id,lang_id),
    FOREIGN KEY (last_modifier) REFERENCES users (id)
    ,
    FOREIGN KEY (doctype_id) REFERENCES document_types (id)
    ,
    FOREIGN KEY (branch_id) REFERENCES branches (id)
    ,
    FOREIGN KEY (lang_id) REFERENCES languages (id)
    ,
    INDEX (doctype_id),
    INDEX (last_modifier),
    INDEX (branch_id),
    INDEX (lang_id)
) Type=InnoDB;

insert into document_variants(doc_id, branch_id, lang_id, doctype_id, retired,
    lastversion_id, liveversion_id, last_modified, last_modifier, updatecount,
    created_from_branch_id, created_from_lang_id, created_from_version_id)
    select
      id,
      1,
      @daisylangid,
      doctype_id,
      retired,
      lastversion_id,
      liveversion_id,
      last_modified,
      last_modifier,
      1,
      -1,
      -1,
      -1
    from documents;

#
# documents table
#

alter table documents rename to documents_old;

CREATE TABLE documents
(
		            id BIGINT NOT NULL,
		            created DATETIME NOT NULL,
		            owner BIGINT NOT NULL,
		            private CHAR NOT NULL,
		            last_modified DATETIME NOT NULL,
		            last_modifier BIGINT NOT NULL,
		            updatecount BIGINT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (last_modifier) REFERENCES users (id)
    ,
    FOREIGN KEY (owner) REFERENCES users (id)
    ,
    INDEX (owner),
    INDEX (last_modifier)
) Type=InnoDB;

insert into documents(id,created,owner,private,last_modified,last_modifier,updatecount)
select id,created,owner,private,last_modified,last_modifier,updatecount from documents_old;

drop table documents_old;


#
# versions table
#

alter table document_versions rename to document_versions_old;

CREATE TABLE document_versions
(
		            doc_id BIGINT NOT NULL,
		            branch_id BIGINT NOT NULL,
		            lang_id BIGINT NOT NULL,
		            id BIGINT NOT NULL,
		            name VARCHAR (255) NOT NULL,
		            created_on DATETIME NOT NULL,
		            created_by BIGINT NOT NULL,
		            state CHAR (1) NOT NULL,
		            state_last_modified DATETIME NOT NULL,
		            state_last_modifier BIGINT NOT NULL,
		            total_size_of_parts BIGINT NOT NULL,
    PRIMARY KEY(doc_id,branch_id,lang_id,id),
    FOREIGN KEY (state_last_modifier) REFERENCES users (id)
    ,
    FOREIGN KEY (created_by) REFERENCES users (id)
    ,
    INDEX (state_last_modifier),
    INDEX (created_by)
) Type=InnoDB;

insert into document_versions(doc_id,branch_id,lang_id,id,name,created_on,created_by,
      state,state_last_modified,state_last_modifier,total_size_of_parts)
   select doc_id,1,@daisylangid,id,name,created_on,created_by,state,state_last_modified,
      state_last_modifier,total_size_of_parts from document_versions_old;

drop table document_versions_old;

#
# parts table
#

alter table parts rename to parts_old;

CREATE TABLE parts
(
		            doc_id BIGINT NOT NULL,
		            branch_id BIGINT NOT NULL,
		            lang_id BIGINT NOT NULL,
		            version_id BIGINT NOT NULL,
		            parttype_id BIGINT NOT NULL,
		            blob_id VARCHAR (255) NOT NULL,
		            mimetype VARCHAR (255) NOT NULL,
		            blob_size BIGINT NOT NULL,
    PRIMARY KEY(doc_id,branch_id,lang_id,version_id,parttype_id),
    FOREIGN KEY (parttype_id) REFERENCES part_types (part_id)
    ,
    INDEX (parttype_id),
    INDEX (blob_id)
) Type=InnoDB;

insert into parts(doc_id,branch_id,lang_id,version_id,parttype_id,blob_id,mimetype,blob_size)
   select doc_id,1,@daisylangid,version_id,parttype_id,blob_id,mimetype,blob_size from parts_old;

drop table parts_old;

#
# the_fields table
#

alter table thefields rename to thefields_old;

CREATE TABLE thefields
(
		            doc_id BIGINT NOT NULL,
		            branch_id BIGINT NOT NULL,
		            lang_id BIGINT NOT NULL,
		            version_id BIGINT NOT NULL,
		            fieldtype_id BIGINT NOT NULL,
		            stringvalue VARCHAR (255),
		            datevalue DATETIME,
		            datetimevalue DATETIME,
		            integervalue BIGINT,
		            floatvalue DOUBLE,
		            decimalvalue DECIMAL (10, 5),
		            booleanvalue CHAR,
    PRIMARY KEY(doc_id,branch_id,lang_id,version_id,fieldtype_id),
    FOREIGN KEY (fieldtype_id) REFERENCES field_types (id)
    ,
    INDEX (stringvalue),
    INDEX (datevalue),
    INDEX (datetimevalue),
    INDEX (integervalue),
    INDEX (floatvalue),
    INDEX (decimalvalue),
    INDEX (booleanvalue),
    INDEX (fieldtype_id)
) Type=InnoDB;

insert into thefields(doc_id,branch_id,lang_id,version_id,fieldtype_id,stringvalue,datevalue,
        datetimevalue,integervalue,floatvalue,decimalvalue,booleanvalue)
    select doc_id,1,@daisylangid,version_id,fieldtype_id,stringvalue,datevalue,datetimevalue,integervalue,
        floatvalue,decimalvalue,booleanvalue from thefields_old;

drop table thefields_old;


#
# links table
#

alter table links rename to links_old;

CREATE TABLE links
(
		            doc_id BIGINT NOT NULL,
		            branch_id BIGINT NOT NULL,
		            lang_id BIGINT NOT NULL,
		            version_id BIGINT NOT NULL,
		            id BIGINT NOT NULL,
		            title LONGTEXT NOT NULL,
		            target LONGTEXT NOT NULL,
    PRIMARY KEY(doc_id,branch_id,lang_id,version_id,id)
) Type=InnoDB;

insert into links(doc_id,branch_id,lang_id,version_id,id,title,target)
    select doc_id,1,@daisylangid,version_id,id,title,target from links_old;

drop table links_old;


#
# summaries table
#

alter table summaries rename to summaries_old;

CREATE TABLE summaries
(
		            doc_id BIGINT NOT NULL,
		            branch_id BIGINT NOT NULL,
		            lang_id BIGINT NOT NULL,
		            summary LONGTEXT NOT NULL,
    PRIMARY KEY(doc_id,branch_id,lang_id)
) Type=InnoDB;

insert into summaries(doc_id,branch_id,lang_id,summary)
   select doc_id,1,@daisylangid,summary from summaries_old;

drop table summaries_old;


#
# comments table
#

alter table comments rename to comments_old;

CREATE TABLE comments
(
		            id BIGINT NOT NULL,
		            branch_id BIGINT NOT NULL,
		            lang_id BIGINT NOT NULL,
		            doc_id BIGINT NOT NULL,
		            created_by BIGINT NOT NULL,
		            created_on DATETIME NOT NULL,
		            visibility CHAR (1) NOT NULL,
		            comment_text LONGTEXT NOT NULL,
    PRIMARY KEY(id,branch_id,lang_id),
    FOREIGN KEY (created_by) REFERENCES users (id)
    ,
    INDEX comments_I_1 (doc_id),
    INDEX comments_I_2 (created_by)
) Type=InnoDB;

insert into comments(id,branch_id,lang_id,doc_id,created_by,created_on,visibility,comment_text)
    select id,1,@daisylangid,doc_id,created_by,created_on,visibility,comment_text from comments_old;

drop table comments_old;

#
# customfields table
#

alter table customfields rename to customfields_old;

CREATE TABLE customfields
(
		            doc_id BIGINT NOT NULL,
		            branch_id BIGINT NOT NULL,
		            lang_id BIGINT NOT NULL,
		            name VARCHAR (255) NOT NULL,
		            value VARCHAR (255) NOT NULL,
    PRIMARY KEY(doc_id,branch_id,lang_id,name)
) Type=InnoDB;

insert into customfields(doc_id,branch_id,lang_id,name,value)
   select doc_id,1,@daisylangid,name,value from customfields_old;

drop table customfields_old;

#
# locks table
#

alter table locks rename to locks_old;

CREATE TABLE locks
(
		            doc_id BIGINT NOT NULL,
		            branch_id BIGINT NOT NULL,
		            lang_id BIGINT NOT NULL,
		            user_id BIGINT NOT NULL,
		            locktype CHAR (1) NOT NULL,
		            time_acquired DATETIME NOT NULL,
		            duration BIGINT NOT NULL,
    PRIMARY KEY(doc_id,branch_id,lang_id),
    FOREIGN KEY (user_id) REFERENCES users (id)
    ,
    INDEX (user_id)
) Type=InnoDB;

insert into locks(doc_id,branch_id,lang_id,user_id,locktype,time_acquired,duration)
    select doc_id,1,@daisylangid,user_id,locktype,time_acquired,duration from locks_old;

drop table locks_old;

#
# extracted_links table
#

alter table extracted_links rename to extracted_links_old;

CREATE TABLE extracted_links
(
		            source_doc_id BIGINT NOT NULL,
		            source_branch_id BIGINT NOT NULL,
		            source_lang_id BIGINT NOT NULL,
		            source_parttype_id BIGINT NOT NULL,
		            target_doc_id BIGINT NOT NULL,
		            target_branch_id BIGINT NOT NULL,
		            target_lang_id BIGINT NOT NULL,
		            target_version_id BIGINT NOT NULL,
		            linktype CHAR (1) NOT NULL,
		            in_last_version CHAR NOT NULL,
		            in_live_version CHAR NOT NULL,
    INDEX (source_doc_id),
    INDEX (target_doc_id)
) Type=InnoDB;

insert into extracted_links(source_doc_id,source_branch_id,source_lang_id,source_parttype_id,
        target_doc_id,target_branch_id,target_lang_id,target_version_id,linktype,in_last_version,
        in_live_version)
    select source_doc_id,1,@daisylangid,source_parttype_id,target_doc_id,1,@daisylangid,target_version_id,
        linktype,in_last_version,in_live_version from extracted_links_old;

drop table extracted_links_old;


#
# document_collections table
#

alter table document_collections rename to document_collections_old;

CREATE TABLE document_collections
(
		            document_id BIGINT NOT NULL,
		            branch_id BIGINT NOT NULL,
		            lang_id BIGINT NOT NULL,
		            collection_id BIGINT NOT NULL,
    PRIMARY KEY(document_id,branch_id,lang_id,collection_id),
    FOREIGN KEY (collection_id) REFERENCES collections (id)
    ,
    INDEX (collection_id)
) Type=InnoDB;

insert into document_collections(document_id,branch_id,lang_id,collection_id)
    select document_id,1,@daisylangid,collection_id from document_collections_old;

drop table document_collections_old;

#
# document_subscriptions_table
#

alter table document_subscriptions rename to document_subscriptions_old;

CREATE TABLE document_subscriptions
(
		            user_id BIGINT NOT NULL,
		            doc_id BIGINT NOT NULL,
		            branch_id BIGINT NOT NULL,
		            lang_id BIGINT NOT NULL,
    PRIMARY KEY(user_id,doc_id,branch_id,lang_id),
    FOREIGN KEY (user_id) REFERENCES emailnotification_subscriptions (user_id)
    ,
    INDEX (doc_id)
) Type=InnoDB;

insert into document_subscriptions(user_id,doc_id,branch_id,lang_id)
    select user_id,doc_id,1,@daisylangid from document_subscriptions;

drop table document_subscriptions_old;

#
# collection_subscriptions table
#

alter table collection_subscriptions rename to collection_subscriptions_old;

CREATE TABLE collection_subscriptions
(
		            user_id BIGINT NOT NULL,
		            collection_id BIGINT NOT NULL,
		            branch_id BIGINT NOT NULL,
		            lang_id BIGINT NOT NULL,
    PRIMARY KEY(user_id,collection_id,branch_id,lang_id),
    FOREIGN KEY (user_id) REFERENCES emailnotification_subscriptions (user_id)
    ,
    INDEX (collection_id)
) Type=InnoDB;

insert into collection_subscriptions(user_id,collection_id,branch_id,lang_id)
    select user_id,collection_id,-1,@daisylangid from collection_subscriptions;

drop table collection_subscriptions_old;

#
# emailnotification_subscriptions table
#

insert into document_subscriptions(user_id,doc_id,branch_id,lang_id)
    select user_id,-1,-1,-1 from emailnotification_subscriptions where all_doc_events = 1;

alter table emailnotification_subscriptions drop column all_doc_events;

#
# Document Tasks tables
#

CREATE TABLE task_sequence
(
		            maxid INTEGER
) Type=InnoDB;

CREATE TABLE document_tasks
(
		            id BIGINT NOT NULL,
		            scriptlanguage VARCHAR (100) NOT NULL,
		            owner BIGINT NOT NULL,
		            started_at DATETIME NOT NULL,
		            finished_at DATETIME,
		            state VARCHAR (1) NOT NULL,
		            progress VARCHAR (255) NOT NULL,
		            description LONGTEXT NOT NULL,
		            script LONGTEXT NOT NULL,
		            details LONGTEXT,
    PRIMARY KEY(id),
    INDEX (started_at),
    INDEX (owner),
    INDEX (state)
) Type=InnoDB;


CREATE TABLE task_doc_details
(
		            task_id BIGINT NOT NULL,
		            doc_id BIGINT NOT NULL,
		            branch_id BIGINT NOT NULL,
		            lang_id BIGINT NOT NULL,
		            seqnr BIGINT NOT NULL,
		            state VARCHAR (1) NOT NULL,
		            details LONGTEXT,
    PRIMARY KEY(task_id,doc_id,branch_id,lang_id),
    FOREIGN KEY (task_id) REFERENCES document_tasks (id)

) Type=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;

#
# Hash passwords in user table
#

update users set password = sha1(password) where id != 1;

# slightly better would be (but doesn't work on MySQL 4.0):
# update users set password = sha1(convert(password using utf8)) where id != 1;