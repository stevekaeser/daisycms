importPackage(Packages.org.outerj.daisy.repository);
importClass(Packages.org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager);

var repository;

if (arguments.length < 3) {
	showUsage();
	throw new Error("Insufficient arguments");
}

var repoUrl = arguments[0];
var repoUser = arguments[1];
var repoPassword = arguments[2];

var wfFiles = arguments.splice(3, arguments.length - 3);

function getRepository () {
	if (repository == null) {	
		var credentials = new Credentials(repoUser, repoPassword);
		var repositoryManager = new RemoteRepositoryManager(repoUrl, credentials);
    repositoryManager.registerExtension("WorkflowManager",
        new Packages.org.outerj.daisy.workflow.clientimpl.RemoteWorkflowManagerProvider());

		repository = repositoryManager.getRepository(credentials);
    repository.switchRole(1);
	}

	return repository;
}

function wfUpload() {
  wf = getRepository().getExtension("WorkflowManager");
  if (wfFiles.length == 0) {
    print ("No files specified - not uploading any workflows");
  }
  for (var i = 0; i < wfFiles.length; i++) {
    var f = new Packages.java.io.File(wfFiles[i]);
    var def;
    print("Deploying " + f);
    if (f.name.endsWith(".zip")) {
      def = wf.deployProcessDefinition(new Packages.java.io.FileInputStream(f), "application/zip", Packages.java.util.Locale.getDefault());
    } else {
      def = wf.deployProcessDefinition(new Packages.java.io.FileInputStream(f), "text/xml", Packages.java.util.Locale.getDefault());
    }
    print("Workflow \"" + def.getName() + "\" deployed with version " + def.getVersion());
  }
}

function print(s) {
  java.lang.System.out.println(s);
}

function showUsage() {
	print("Uploads one or more workflow files to a repository.\n\n" +
		"Usage : daisy-js wf-upload.js [repo-url] [repo-user] [repo-passwd] [wf-file...] \n");
}

wfUpload();
