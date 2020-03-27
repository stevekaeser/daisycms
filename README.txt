                       Welcome to the Daisy CMS!
                        http://www.daisycms.org

                     === BUILDING DAISY FROM SOURCE ===

There are two scenarios:

   1. You only want to build a Daisy binary distribution
        (= the same as can be downloaded)

   2. You want to set up a development environment
        (= for incremental building and testing)

The second scenario is a bit more work the first time (you need
to create databases, set up configuration, ...) but once that's done
you can make changes to the code, quickly rebuild and test. So this
scenario is intended for people who want to work on Daisy itself.

>>> If you (only) want to build a binary distribution:

 - first build the repository server by following the instructions in
   this file, skip all sections marked "[skip when building dist only]"

 - then assemble the distribution by following the instructions in
   distro/core/README.txt


>>> If you run into problems during the source build:

 - You are welcome to ask for help on the Daisy mailing list.
    (please mention applicable details such as Daisy version,
     operating system, ...)

 - General feedback on confusing instructions (even if you found out how
   to do it in the end) is also welcome.


    +---------------------------------------------------------------+
    |                          Conventions                          |
    +---------------------------------------------------------------+

Throughout these instructions we use <daisy-src> to refer to the root
of the source tree.    


    +---------------------------------------------------------------+
    |                      Maven installation                       |
    +---------------------------------------------------------------+

Maven 2 is the tool used to build Daisy.

>> download version 2.2.1 or newer from http://maven.apache.org/

Note: - Maven 2.1.0 or older will not work.
      - Other version prior to 2.2.1 have not been tested
      - Maven 1 will not work.
      - to install Maven, just extract the download somewhere
        and add its bin directory to the PATH environment variable


    +---------------------------------------------------------------+
    |                           Compiling                           |
    +---------------------------------------------------------------+

>> Go to <daisy-src>

>> Set the MAVEN_OPTS environment variable, it should at least contain -Xmx512M,
or the build may fail with a java.lang.OutOfMemoryError. e.g.

export MAVEN_OPTS=-Xmx512m

or

set MAVEN_OPTS=-Xmx512m on windows

>> Execute:

mvn install -Pcocoon.download,cocoon.get,webapp,dev

Notes: - If downloading of dependencies is slow, consider using a
         Maven repository mirror (instead of repo1.maven.org),
         see the instructions here on how to do this:
         http://maven.apache.org/settings.html#Repositories
         
       - Background info: Daisy is separated in a number of different
         subprojects, executing the above command builds them all and put the result
         of them (an "artifact" jar) in your local maven
         repository (~/.m2/repository/)

       - The flags -Pcocoon.get and -Pwebapp are only
         the first time and after running mvn clean.
         They make sure that the daisywiki application is built completely

       - See <daisy-src>/applications/daisywiki/README.txt for more detailed
         information on building the daisywiki application.

       - If the build fails with "taskdef class org.apache.torque.task.TorqueDataModelTask cannot be found', cd to the <daisy-src>/install directory and execute 'mvn install'.  This is due to a known bug in maven.

    +---------------------------------------------------------------+
    |                 Creating the MySQL database                   |
    |                [skip when building dist only]                 |
    +---------------------------------------------------------------+

Required MySQL version: 4.1.7 or higher, or 5 final or higher
MySQL version 4.0 and earlier will not work correctly!

>> login as root user to MySQL:

mysql -uroot -pYourSecretPassword

(drop the -p if the root user has no password)

>> on the MySQL prompt, execute:

DROP DATABASE daisydev_repository;
CREATE DATABASE daisydev_repository charset utf8;
GRANT ALL ON daisydev_repository.* TO daisy@"%" IDENTIFIED BY "daisy";
GRANT ALL ON daisydev_repository.* TO daisy@localhost IDENTIFIED BY "daisy";

DROP DATABASE daisydev_activemq;
CREATE DATABASE daisydev_activemq charset utf8;
GRANT ALL ON daisydev_activemq.* TO activemq@"%" IDENTIFIED BY "activemq";
GRANT ALL ON daisydev_activemq.* TO activemq@localhost IDENTIFIED BY "activemq";

(The localhost entries are necessary because otherwise the default access
rights for anonymous users @localhost will take precedence.)

    +---------------------------------------------------------------+
    |      Configuring and initializing the repository server       |
    |                [skip when building dist only]                 |
    +---------------------------------------------------------------+

>> Go to <daisy-src>/install/target

>> Execute:

     daisy-repository-init-dev -d install.properties

have a look at the install.properties file to see if you want to change
something but usually the defaults are just fine for development.

     daisy-repository-init-dev -i install.properties

     (linux: ./daisy-repository-init-dev)

   Note the "-dev" suffix !!!

    +---------------------------------------------------------------+
    |                Running the Repository server                  |
    |                [skip when building dist only]                 |
    +---------------------------------------------------------------+

>> Change to the directory <daisy-src>/repository/server

>> Execute:

On Linux:
./start-repository

On Windows:
start-repository.bat

Note: on Linux, if your Maven repository directory is in a non-standard
      location, you will have to edit the start-repository script and
      modify the value of the -r argument.

Note: this script:
       - opens a debug port on port 8001
       - allows JMX access using jconsole


    +---------------------------------------------------------------+
    |                        Running testcases                      |
    |           >  optional, requires dev environment scenario      |
    |           >  just FYI, not needed to do this right now        |
    +---------------------------------------------------------------+

There is a set of automated testcasing verifying various parts of the
repository functionality. For information on running these, see the file
repository/test/README.txt


    +---------------------------------------------------------------+
    |                     More build hints                          |
    +---------------------------------------------------------------+

Performing a clean build
------------------------

If things don't work even when starting over (e.g. errors about methods
not being found etc.) doing a clean build is the solution.

To do a clean build:

Go to <daisy-src> and execute

mvn clean

What this actually does is removing all 'target' subdirectories. After doing
"mvn clean", perform the build in the usual way.

Generating IDE project files
----------------------------

 [ not perfect yet ]
 
First build Daisy (including the Wiki), then run:

For eclipse:
mvn eclipse:eclipse -Pdev -Dmaven.eclipse.src.download=false -Dmaven.eclipse.javadoc.download=false -Dmaven.compile.target=1.5 -Dmaven.compile.source=1.5

For Intellij IDEA (>= 9.0):
  No need to generate project files, just go to
  File, Open Project and select the pom.xml file

If you are working on a subversion checkout, you can avoid the .project etc.
files showing up when doing "svn status" by editing ~/.subversion/conf and
adding the following to the global-ignores property:
*.iml *.ipr *.iws .classpath .project .settings

What to rebuild/restart when making changes
-------------------------------------------

If you make changes to code used in the repository server:

 * execute 'mvn install' in <daisy-src>/repository and in <daisy-src>/services
 * restart the repository server

If you are making changes only in one specific subproject, you
can also execute 'mvn install' in that specific subproject.

If you make Java-code changes to the Wiki:

  * execute 'mvn install' in <daisy-src>/applications/daisywiki/frontend
  * execute 'mvn install -PdeployResources' in <daisy-src>/applications/daisywiki/runtime
  * restart the wiki

If you make resource-only (= XSLT, CSS, images, ...) changes to the Wiki:

  * execute 'mvn install -PdeployResources' in
       <daisy-src>/applications/daisywiki/runtime
  * restart the wiki (not always necessary, depends on caching)


