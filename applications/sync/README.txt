The sync service is not included with Daisy,
but is maintained as a standalone application which you must package yourself.
(we simply did not implement the packaging step).

Building
--------
Build the sync service using mvn install

Packaging
---------
*** TODO *** (solutions should involve generating tanuki wrapper conf, creating a repo dir, ...)

Installing Sync
---------------

Create the database
mysql -u root

In the mysql console create the database
CREATE DATABASE syncstore CHARACTER SET 'utf8';
GRANT ALL ON syncstore.* TO syncuser@'%' IDENTIFIED BY 'syncuser';
GRANT ALL ON syncstore.* TO syncuser@localhost IDENTIFIED BY 'syncuser';

Create the tables
mysql -u root syncstore < $DAISY_HOME/extras/sync/misc/schema.sql

Load initial data
mysql -u root syncstore < $DAISY_HOME/extras/sync/misc/init-data.sql

Create a sync.properties file and a mapping file. Put them in a directory, and point SYNC_CONF to that directory.
(See the sample files under core/src/main/conf)

(Optional) Installing the UI
----------------------------
Copy the jar into daisy wiki :
cp ui/target/daisy-sync-ui-2.0-dev.jar $DAISY_HOME/daisywiki/webapp/WEB-INF/lib

Copy the wiki extension:
cp $DAISY_HOME/extras/sync/wiki-ext <wikidata.dir>/sites/<site>/cocoon/sync

Running Sync
------------
set DAISY_JAVA_OPTIONS="-Dsync.conf=$SYNC_CONF"
core/target/daisy-sync

(Normally you would want to run sync as a service -- service scripts should be generated
when building Daisy (so the classpath corresponds to the pom.xml)
