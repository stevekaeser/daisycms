This directory contains some schema definitions and data which can be imported into
a Daisy repository in order to test the field various field editors of the Wiki's
document editor.

It can be imported by executing something like:

sh ../../../../../importexport/target/daisy-import-dev -u testuser -r Administrator -i importdata/

After import, testing can be done using the "FieldTestDocType" document type.
