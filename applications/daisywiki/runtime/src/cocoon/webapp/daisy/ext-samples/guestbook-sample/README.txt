This sample illustrates how to extend the DaisyWiki with your
own stuff, in this case a form to add guestbook entries.

The guestbook form page will be styled by the same stylesheets
as other DaisyWiki pages, and include the hierarchical navigation
tree.

The guestbook entries are inserted as documents in the Daisy repository.

To try it out, do the following:
================================

* copy the cocoon subdirectory to a site directory, in other words
  a $WEBAPPS/daisy/sites/<yoursite>/ directory.

  If the <yoursite> directory already contains a cocoon subdirectory,
  but you didn't alter it, you can safely replace it with this one.

* edit the navigation tree for your site and add a link node with the
  following properties (as child of the root node):
   - url: /daisy/yoursite/ext/forms/guestbook
   - label: Guestbook
   - id: guestbook

  replace the "yoursite" in the url attribute by the name of your site.

  Note: you can also place this node in a non-root location, but then
  you need to modify the file cocoon/flow.js, search for the occurence
  of "/guestbook" and change it with the appropriate path to the navigation
  node. This is to make sure the guestbook node will be highlighted when
  you're on the guestbook page.

* edit the file cocoon/flow.js, at the top you'll find the following variables:
   var repositoryUser = "testuser";
   var repositoryPassword = "testuser";

  Change these to match a Daisy user that has the right to create documents.
  (for 'real' use, you might want to create a special 'guestbook' user for this).

* Now try surfing to:
   http://localhost:8888/daisy/coolsite/ext/forms/guestbook

  And you should see the guestbook form. Try to make an entry, afterwards you'll
  get a confirmation page.

* To find the created document, you can do a query like this on the querysearch page:
   select id, name where name like 'guestbook%'


If you have problems getting this to work, you can ask for help on the Daisy mailing
list.

Some further notes:
===================

* To use this for real, it is recommended to change the code so that the document
  is added to a 'guestbook' collection. Alternatively, you could create a
  special document type for guestbook entries.

* To have a page containing all guest book entries, you can create a document
  that uses the query-include feature.

