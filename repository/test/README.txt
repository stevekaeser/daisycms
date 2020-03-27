                       Welcome to the Daisy CMS!
                        http://www.daisycms.org

                     === RUNNING THE TESTCASES ===

These testcases are more integration tests than unit tests: they
initialize and start a complete daisy repository server, and
then run tests via the public repository APIs.

To run the testcases, you first need to build the repository
server as outlined in ../../README.txt

    +---------------------------------------------------------------+
    |                     Creating a database                       |
    +---------------------------------------------------------------+

The testscripts make use of a separate database because it is emptied
between each test case run. Therefore create a database:

>> Log in to MySQL:

      mysql -uroot -pYourRootPassword

>> On the MySQL prompt, execute the following:

      CREATE DATABASE daisytestrepository;
      GRANT ALL ON daisytestrepository.* TO daisy@"%" IDENTIFIED BY "daisy";
      GRANT ALL ON daisytestrepository.* TO daisy@localhost IDENTIFIED BY "daisy";


    +---------------------------------------------------------------+
    |                        ActiveMQ note                          |
    +---------------------------------------------------------------+

For the testcases, ActiveMQ is configured without persistency nor
access control, so requires no special setup.


    +---------------------------------------------------------------+
    |                        Configuration                          |
    +---------------------------------------------------------------+

Copy testsupport.properties to local.testsupport.properties, and
adjust as needed, minimally the following properties:

testsupport.blobstore
testsupport.fulltextindexstore
testsupport.smtpHost
testsupport.fromAddress
testsupport.driverClasspath

Very important note:

  Since the blobstore and indexstore are emptied between each testcase,
  and since they should not be shared with other installations, make sure
  those directories point to other locations then your normal daisy
  installation!


    +---------------------------------------------------------------+
    |                    Running the tests                          |
    +---------------------------------------------------------------+

>> To run all tests, simply execute:

mvn test

Note: you should not have the repository server running, otherwise
      the testcases will fail because of the http ports which are
      already in use.

>> To run a single test:

mvn test -Dtest=LocalTMTest

You can also use wildcards to run multiple tests at the same time:

mvn test -Dtest=*TMTest #runs both LocalTMTest and RemoteTMTest
mvn test -Dtest=Local*  #runs all local tests

>> To debug a test, add -Pdebug.  This will start the tests with a debugging port 5005.
You can also manually provide the jvm options using -Dmaven.surefire.debug=...  See the pom
to see the minimal set of options required to run the tests.

