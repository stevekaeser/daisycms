alter table comments add column visibility char(1) after private;
update comments set visibility = 'U';
update comments set visibility = 'P' where private = 1;
alter table comments drop column private;

alter table emailnotification_subscriptions add column comment_events char not null after acl_events;
