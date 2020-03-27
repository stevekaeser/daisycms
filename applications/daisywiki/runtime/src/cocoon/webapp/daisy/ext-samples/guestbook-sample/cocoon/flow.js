cocoon.load("resource://org/apache/cocoon/forms/flow/javascript/Form.js");
cocoon.load("resource://org/outerj/daisy/frontend/util/daisy-util.js");

/**
 * The guestbook code. Note that this code is designed to be stateless, it doesn't
 * use continuations or the session.
 */
function guestbook() {
    var daisy = getDaisy();
    var repositoryUser = "testuser";
    var repositoryPassword = "testuser";

    //
    // Get the Daisy Repository client for the current user (or guest user if not logged in)
    //
    var repository = daisy.getRepository();

    //
    // Get some other contextual data
    //

    // siteConf is an object holding the configuration information for the current DaisyWiki site
    var siteConf = daisy.getSiteConf();
    // mountPoint is where in the URI space the daisy sitemap is mounted (usually /daisy)
    var mountPoint = daisy.getMountPoint();
    var locale = daisy.getLocale();

    //
    // Get the hierarchical navigation tree
    //
    var navigationData = new Packages.org.apache.cocoon.xml.SaxBuffer();
    var navigationManager = repository.getExtension("NavigationManager");
    var navigationParams = new Packages.org.outerj.daisy.navigation.NavigationParams(siteConf.getNavigationDoc(),
                                                                                     "/guestbook",
                                                                                     siteConf.contextualizedTree());
    navigationManager.generateNavigationTree(navigationData, navigationParams, null, true);


    // create form
    var form = new Form("guestbook_form_definition.xml");
    var finished = false;

    // POST = form was submitted, so process it
    if (cocoon.request.getMethod() == "POST") {
        var formContext = new Packages.org.apache.cocoon.forms.FormContext(cocoon.request, locale);
        finished = form.form.process(formContext);
    }

    if (finished) {
        // The form has been correctly completed, now we can do whatever we like
        // to do with the entered data: send it as an email to someone, insert
        // it in a database, or as illustrated here, create a document in the Daisy
        // repository
        var name = form.getChild("name").getValue();
        var age = form.getChild("age").getValue();
        var message = form.getChild("message").getValue();

        // Retrieve the repository client object for the user that is authorized to create these guest book entries
        // (assuming other users, especially guest users, aren't allowed to do this)
        var repository2 = daisy.getRepository(repositoryUser, repositoryPassword);
        var document = repository2.createDocument("Guestbook entry by " + name, "SimpleDocument");
        document.setPart("SimpleDocumentContent", "text/xml", buildContent(name, age, message));
        // possible other things to do before saving:
        // Add the document to a collection:
        //   var collection = repository2.getCollectionManager().getCollectionByName("mycollection", false);
        //   document.addToCollection(collection);
        // Change the new version state to DRAFT so the entry doesn't get immediately published:
        //   document.setNewVersionState(Packages.org.outerj.daisy.repository.VersionState.DRAFT);
        document.save();

        // show confirmation page
        var viewData = new Object();
        viewData["name"] = name;
        viewData["pageContext"] = daisy.getPageContext();
        viewData["locale"] = locale;
        viewData["navigationTree"] = navigationData;
        viewData["template"] = daisy.resolve("guestbook_confirm.xml");
        cocoon.sendPage(daisy.getDaisyCocoonPath() + "/GenericMessagePipe", viewData);

        // Note about the above: /daisy/GenericMessagePipe is a pipeline we reuse from the main
        // Daisy sitemap. The template read by the JX generator is specified using the 'template'
        // key in the viewData. The resolve() funtion is used to resolve the path relative to
        // the current extension sitemap.
    } else {
        // show form
        var viewData = new Object();
        viewData["CocoonFormsInstance"] = form.form;
        viewData["pageContext"] = daisy.getPageContext();
        viewData["submitPath"] = mountPoint + "/" + siteConf.getName() + "/ext/forms/guestbook";
        viewData["locale"] = locale;
        viewData["navigationTree"] = navigationData;
        viewData["formTemplate"] = daisy.resolve("guestbook_form_template.xml");
        cocoon.sendPage(daisy.getDaisyCocoonPath() + "/GenericFormPipe", viewData);
    }
}

/**
 * Builds the HTML content to be stored, and returns the result as a byte array.
 */
function buildContent(name, age, message) {
    name = encodeToHTML(name);
    message = encodeToHTML(message);

    var content = "<html><body><p><strong>" + name + " (" + age + " years old)</strong> left us the following message:</p><p>";
    content = content + message + "</p></body></html>";

    // pull it through the htmlcleaner to have it nicely formatted (this is optional)
    var htmlcleaner = getDaisy().getHTMLCleaner();
    return htmlcleaner.cleanToByteArray(content);
}

/**
 * Escapes characters disallowed in XML, change newlines to <br> tags.
 */
function encodeToHTML(text) {
    // the first statement is to be sure the text is a javascript string, it might be a java string
    // if the value was obtained from a form.
    text = "" + text;
    text = text.replace(/</g, "&lt;");
    text = text.replace(/>/g, "&gt;");
    text = text.replace(/'/g, "&apos;");
    text = text.replace(/"/g, "&quot;");
    text = text.replace(/\n/g, "<br/>");
    return text;
}
