                                Daisy CMS
                        http://www.daisycms.org

                 === DAISY WIKI BUILD PROFILES ===

Like for the repository server, there are two scenarios:

 1. Only build the wiki (e.g. when you're building a binary dist)

 2. Set up a development environment for running/building the wiki

In both cases, first build the repository server as outlined in
../../README.txt

>> Maven used 'profiles' to influence the build process.
   You can activate build profiles using -P<profileId>.
   Below are the profiles defined in the daisywiki buildfiles (pom.xml) and their function.
   
     -Pcocoon.download : This profile makes sure the cocoon source code is present by downloading it
                         You only  need to do this once (and if you deleted the directory <daisy-src>/../daisy-deps/cocoon.version)
     -Pcocoon.get      : This profile-Pwebapp -Pdev (you can also do this from <daisy-src>)
                         You need to do this only, or if you deleted the 'target' directory under
                         <daisy-src>/applications/daisywiki/runtime/target
     -Pwebapp          : Prepares the cocoon environment built with -Pcocoon.get to run the Daisy wiki
     -PdeployResources : Copies the resources needed to run the Daisy wiki
     -Pdev             : Signs the parteditor-applet artifact.  This flag is always needed
                         when building the parteditor-applet module.  If you omit it, it implies
                         that you are performing an official build, and that you will provide a keystore password
                         using -Dmystorepass=....

If you are behind a proxy you will need to download cocoon manually.  The location to save cocoon can be found in the runtime/project.properties file.

    +---------------------------------------------------------------+
    |       If you want to use a different Cocoon version           |
    |                                                               |
    |        This replaces using "-Pcocoon.download"                |
    |                                                               |
    |           (Skip this unless you want to do so)                |
    +---------------------------------------------------------------+

>> Download Cocoon 2.1 and extract it somewhere (no need to build it)

Note: - the exact Cocoon version required for the Daisy Wiki varies
        from time to time, as of now at least a 2.1.11-dev is required
      - newer Cocoon versions from the 2.1 series might work, older
        ones (such as 2.1.10) won't
      - Cocoon 2.2 is a major new release, and won't work with Daisy

>> Create an empty text file named:

     <daisy-src>/applications/daisywiki/runtime/build.properties

and define a property named cocoon.dist.home in it pointing to the
location where you extracted Cocoon. On Windows, you need to escape
backslashes by entering them double. Examples:

cocoon.dist.home=/path/to/cocoon-<version>
cocoon.dist.home=e:\\path\\to\\cocoon-<version>

>> Continue building with mvn -Pcocoon.get -Pwebapp

    +---------------------------------------------------------------+
    |    Seeding the repository server with required wiki things    |
    |                [skip when building dist only]                 |
    +---------------------------------------------------------------+

>> The repository server should be running

>> Go to the <daisy-src>/install/target directory and execute:

     daisy-wiki-init-dev
     (linux: sh daisy-wiki-init-dev if not executable)

   Note the "-dev" suffix !!!

>> You will be asked for the address of the repository server,
   just press enter.

>> Next you're asked for a user, enter "testuser", password "testuser".

The install program continues its work and finishes.


    +---------------------------------------------------------------+
    |               Creating a wiki data directory                  |
    |                [skip when building dist only]                 |
    +---------------------------------------------------------------+

Now we'll create a "wikidata directory". This is a directory where
configuration (skins etc.) and data (e.g. the bookstore) for the wiki
are stored.

>> Go to the the <daisy-src>/install/target directory and execute:

     daisy-wikidata-init-dev
     (linux: sh daisy-wikidata-init-dev)

   Note the "-dev" suffix !!!

The script will ask where to create the wikidata directory. As default
it will suggest to use "devwikidata" as sibling of <daisy-src>.

   If you opt for another location you'll need to add that path as a
   property 'daisywiki.data' in a file called build.properties in
   <daisy-src>/applications/daisywiki/runtime   
   For this change to have effect, you need to re-execute "mvn -Pcocoon.get"
   in <daisy-src>/applications/daisywiki/runtime

When the script asks for a user, enter "testuser", password "testuser".

The script finishes.


    +---------------------------------------------------------------+
    |                        Creating a site                        |
    |                [skip when building dist only]                 |
    +---------------------------------------------------------------+

>> Now again in <daisy-src>/install/target, execute:

daisy-wiki-add-site-dev <daisy-src>/../devwikidata

(thus, the location of the wikidata directory should be specified
as an argument)

Use again testuser/testuser to login, and enter when asked for the address of
the repository server.

Then you're asked for the name of the site to create. Choose something,
for example "testsite".



    +---------------------------------------------------------------+
    |                    Running the Daisy Wiki                     |
    |                [skip when building dist only]                 |
    +---------------------------------------------------------------+

To run the Daisy Wiki, execute:

cd <daisy-src>/applications/daisywiki/runtime

(On linux/unix)
sh target/cocoon/runc2.sh

(On Windows)
target\cocoon\runc2

You can view the app by surfing to:

http://localhost:8888/daisy/

(hint: you do need the slash on the end!)
(hint: the repository server needs to be up and running)

To view your created site, go to:

http://localhost:8888/daisy/testsite/

By default, you're guest user, so you won't be able to edit or create
anything unless you change the login. You can login with testuser/testuser.
