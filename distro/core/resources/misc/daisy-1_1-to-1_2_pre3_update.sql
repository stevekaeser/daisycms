# script for updating from 1.1 to 1.2-pre3
#
# this is different from executing the different update scripts between
# the pre-releases since the name column merge and split is included from this

alter table comments add column visibility char(1) after private;
update comments set visibility = 'U';
update comments set visibility = 'P' where private = 1;
alter table comments drop column private;

alter table emailnotification_subscriptions add column comment_events char not null after acl_events;

alter table users add column confirmed char not null after updateable_by_user;
alter table users add column confirmkey varchar(50) after confirmed;
update users set confirmed = 1;
