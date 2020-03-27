alter table users add column confirmed char not null after updateable_by_user;
alter table users add column confirmkey varchar(50) after confirmed;
update users set confirmed = 1;

alter table users add column name varchar(100) after last_name;
update users set name = concat(first_name ,' ',last_name);
alter table users drop column first_name;
alter table users drop column last_name;
