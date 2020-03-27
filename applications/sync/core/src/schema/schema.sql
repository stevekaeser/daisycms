create table entity (
  id varchar(255) not null,
  ext_id bigint,
  entity_name varchar(200) not null,
  last_modified timestamp,
  primary key(id),
  index(entity_name(200)),
  constraint unique (ext_id, entity_name)
)ENGINE=InnoDB CHARSET=utf8;

create table entity_attribute (
  entity_id varchar(255) not null,
  attribute_name varchar(255) not null,
  value varchar(255),
  constraint foreign key (entity_id) references entity (id),
  constraint unique (entity_id, attribute_name)
)ENGINE=InnoDB CHARSET=utf8;

create table sync_ext (
  id bigint auto_increment,
  ext_id bigint,
  entity_name varchar(150),
  language varchar(50),
  ext_last_modified timestamp null,
  ext_deleted bit default 0,
  update_ts timestamp default CURRENT_TIMESTAMP,
  state varchar(255),
  primary key(id),
  index (ext_id, entity_name(150), language(50))
) ENGINE=InnoDB CHARSET=utf8;

create table sync_dsy (
  sync_id bigint not null,
  dsy_var_key varchar(255) not null,
  dsy_version bigint not null,
  dsy_deleted bit default 0,  
  primary key(dsy_var_key),
  constraint foreign key (sync_id) references sync_ext(id)
) ENGINE=InnoDB CHARSET=utf8;

create table sync_value (
  dsy_var_key varchar(255) not null,
  dsy_field_name varchar(255),
  dsy_attribute_type varchar(255),
  i int, 
  value varchar(255),
  primary key(dsy_var_key, dsy_field_name, dsy_attribute_type, i),
  constraint foreign key (dsy_var_key) references sync_dsy(dsy_var_key)
) ENGINE=InnoDB CHARSET=utf8;

create table sync_system (
  propname varchar(100) NOT NULL,
  propvalue varchar(255) NOT NULL,
  PRIMARY KEY  (propname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
