# MySQL database script to upgrade a Daisy 1.0 repository to 1.1
# NOT needed when upgrading from 1.1-pre1
alter table documents add key(doctype_id);
alter table parts add key(parttype_id);
alter table thefields add key(fieldtype_id);
alter table doctypes_fieldtypes add key(field_id);
alter table doctype_contentmodel add key(part_id);

ALTER TABLE documents
  ADD FOREIGN KEY (doctype_id) REFERENCES document_types (id);
ALTER TABLE doctypes_fieldtypes
  ADD FOREIGN KEY (field_id) REFERENCES field_types (id);
ALTER TABLE doctype_contentmodel
  ADD FOREIGN KEY (part_id) REFERENCES part_types (part_id);
ALTER TABLE thefields
  ADD FOREIGN KEY (fieldtype_id) REFERENCES field_types (id);
ALTER TABLE parts
  ADD FOREIGN KEY (parttype_id) REFERENCES part_types (part_id);

alter table acl_entries add column perm_publish char(1) not null;
update acl_entries set perm_publish = perm_write;

alter table emailnotification_subscriptions drop column doc_filter;
alter table emailnotification_subscriptions drop column role_id;
alter table emailnotification_subscriptions add column all_doc_events BOOL NOT NULL;
CREATE table document_subscriptions
(
user_id    BIGINT NOT NULL,
doc_id     BIGINT NOT NULL,

PRIMARY KEY (user_id, doc_id),
KEY (doc_id)
)
TYPE = InnoDB;

CREATE table collection_subscriptions
(
user_id         BIGINT NOT NULL,
collection_id   BIGINT NOT NULL,

PRIMARY KEY (user_id, collection_id),
KEY (collection_id)
)
TYPE = InnoDB;

ALTER TABLE document_subscriptions
  ADD FOREIGN KEY (user_id) REFERENCES emailnotification_subscriptions (user_id);
ALTER TABLE collection_subscriptions
  ADD FOREIGN KEY (user_id) REFERENCES emailnotification_subscriptions (user_id);

update emailnotification_subscriptions set all_doc_events=1 where document_events=1;
