alter table locks add column time_expires datetime after duration;
update locks set time_expires = date_add(time_acquired, interval duration/1000 second) where duration >= 0;

# the emailnotification_subscriptions table was renamed to something shorter to prepare for Oracle support
# (which has a limit of 30 characters on the table name)
alter table emailnotification_subscriptions rename emailntfy_subscriptions;

alter table part_types add column linkextractor varchar(50) after daisy_html;
update part_types set linkextractor = 'daisy-html' where daisy_html = 1;
update part_types set linkextractor = 'navigation' where name = 'NavigationDescription';
update part_types set linkextractor = 'book' where name = 'BookDefinitionDescription';
update part_types set linkextractor = 'xmlproperties' where name = 'BookMetadata';
update part_types set linkextractor = 'bookpubs' where name = 'BookPublicationsDefault';
