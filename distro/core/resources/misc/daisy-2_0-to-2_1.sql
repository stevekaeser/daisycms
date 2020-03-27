SET FOREIGN_KEY_CHECKS=0;

CREATE TABLE parentlinkedsellist
(
  fieldtype_id BIGINT NOT NULL,
  whereclause LONGTEXT NOT NULL,
  filtervariants CHAR NOT NULL,
  linkfield VARCHAR (50) NOT NULL,
  FOREIGN KEY (fieldtype_id) REFERENCES field_types (id),
  UNIQUE (fieldtype_id)
) ENGINE=InnoDB;


# update schema version number
update daisy_system set propvalue = '2.1' where propname = 'schema_version';

SET FOREIGN_KEY_CHECKS=1;