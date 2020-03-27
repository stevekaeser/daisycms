This directory contains the sources of the Daisy import/export tools.
See the Daisy documentation for more information on these.



                              - o -


Information in case the tools need to be updated because new schema or
document attributes have been introduced.
======================================================================

First of all you need to know that the import and export tools don't
read/write directly from the repository objects to the export data (XML),
but there is an extra object model in between, which we'll call
the intermediary object model. This intermediary object model allows
manipulation of the entities before import or export.

There are various things which need adjusting when a the structure
of the schema or documents is changed:

 - the intermediary object model, found in the package
    org.outerj.daisy.tools.importexport.model

 - the Xmlizer classes which convert from the intermediary object
   model to XML

 - the Dexmlizer classes which convert from XML to the intermediary
   object model

 - The Factory classes which convert from repository objects to
   the intermediary object model.

 - The SchemaLoader or DocumentLoader which copy from the intermediary
   object model to the actual repository objects.

After updating, an interesting exercise is to do an export of a
repository, and then import it again in the same repository.
Then check the export summary, it should say everywhere
"no-update-needed". Likewise, do an import twice to an empty
target repository, the second time it should also say
"no-update-needed" for everything.

In case schema objects were or were not updated, contrary to what
was expected, make sure the "equals" methods of FieldTypeImpl,
PartTypeImpl, etc. (including contained objects such as
selection lists) are correctly implemented.
