Debian package builder
======================

Intro
-----

* Note: The daisy debian distribution build has not been converted to maven 2 yet.

* Please note that since Daisy version 2.1, the source build has to be done with Maven 1.1,
  Maven 1.0.2 will not work any more.
  However, for installing the maven-deb-plugin and building the debian distribution,
  Maven 1.1 does not work, you still have to use Maven 1.0.2 for that purpose.

* note that the VERSION variable in src/resources/postinst matches the version of the daisy distribution

Building
--------

* Do the build on a Linux system (preferably Debian/Ubuntu)

* Make sure you have 'fakeroot' and 'dpkg' installed
  (can be installed via your system's package manager)

* Build the core distribution following the instructions in
  ../core/README.txt

* Install the maven-deb-plugin, using maven 1.0.2:
  maven plugin:download -DartifactId=maven-deb-plugin -DgroupId=maven-plugins -Dversion=0.5 -Dmaven.repo.remote=http://repo1.maven.org/maven

Note: the next step will fail if the version number suffix doesn't contain any numbers, e.g. if the version number ends with -dev.
Edit the <currentVersion> element in project.xml to match the Daisy release number (without -dev)

* Execute (also using maven 1.0.2):
  maven build-debian-dist
