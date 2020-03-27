                                Daisy CMS
                        http://www.daisycms.org

           === BUILDING (ASSEMBLING) THE BINARY DISTRIBUTION ===

These are instructions for constructing a binary Daisy distribution, that is
the same thing as is provided from the Daisy download site.

    +---------------------------------------------------------------+
    |                          Quick summary                        |
    |                                                               |
    |     if you're simply building a non-official distribution     |
    +---------------------------------------------------------------+

>> Make sure you have built the repository server as explained in
   <root>/README.txt

>> Make sure you have built the Wiki as explained in
   <root>/applications/daisywiki/README.txt

>> Now you can build the dist:

     cd <root>
     cd distro/core
     mvn process-resources

     Note: at the end the javadocs are built, which might produce
           some error messages. These are harmless.

>> The result binary dist can than be found in:

     distro/core/target/daisy

     This Daisy can be installed using the normal installation instructions
     found in the Daisy documentation.


    +---------------------------------------------------------------+
    |         Detailed instructions/checklist for building          |
    |         official Daisy releases:                              |
    +---------------------------------------------------------------+

http://docs.outerthought.org/daisy-docs-2_5/293-daisy/651-daisy.html

