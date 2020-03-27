alter table users add column first_name varchar(50) after name;
alter table users add column last_name varchar(50) after name;
update users set first_name = left(name, instr(name, ' '));
update users set last_name = right(name, length(name) - instr(name, ' '));
alter table users drop column name;