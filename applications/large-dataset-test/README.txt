A test utility for testing the repository with large(r) amounts of data.

It can do three things:
  - create a lot of documents
  - create a lot of documents and perform some updates on them
  - perform some searches on these documents

and in all cases outputs some timings of these actions.

The tool creates its own document/part/field type(s), see the file
largedataset-schema.xml in the source tree.

For filling up string fields and Daisy-HTML parts, it makes use of
wordlist files from http://wordlist.sourceforge.net/.

Usage instructions:
===================

Let's assume we're in a development setup (Daisy source tree):

Step 1: compile
---------------

Execute

cd applications/large-dataset-test
mvn install

Step 2: load data
-----------------

Execute

cd target
sh large-dataset-test-dev -m load -x 100 -y 300 -u testuser -p testuser

The -m parameter indicates the mode, here "load". Alternatively, use load_update
to also perfrom an update to the created documents.

The -x parameter indicates the size of a run, the -y parameter indicates
the number of runs. So here we will create 300 times 100 documents.
Statistics will be written for each run, thus each 100 documents.


Step 3: search data
-------------------

Execute

cd target
sh large-dataset-test-dev -m search -x 5 -y 3

The -m parameter indicates that we want the tool to perform the search tests.

The search test performs a number of different searches.

The -x parameter indicates the number of times to run the search test as a whole.
The -y parameter indicates who many times to perform each query (= literally the same query) within a run.

Note: after doing the load, you might want to wait until the fulltext indexer finished its job.

Step 4: look at the data
------------------------

Both load and search generate files with timing data. These are written
as tab-delimited data, and can be opened in any spreadsheet application.

Note: for queries the ACL evaluation time includes the time for loading the
document (if it could not be retrieved from cache). Similarly, the result
build time include the time for retrieving the live version of the document
if not yet cached.

Since this tool currently automatically switches to the administrator role,
the ACL rules are actually not evaluated.

For the total timings, don't forget that these always include the
overhead of the remote communication.

Other ideas
-----------

Run both load and search at the same time, and possibly multiple copies of them,
in order to spot concurrency-issues (there shouldn't be any, of course).

When working with large amounts of data, an important factor is the size
of the document cache of Daisy, once the whole repository doesn't fit inside
it anymore, performance will (should) start degrading. So you might want to
augment it (if you do, you'll probably will have to augment the server VM
memory too -- as a broad mesure 100 MB per 10000 docs should be more than enough).

Of course, tons of other performance tweakings could be applied.
