importPackage(Packages.org.outerj.daisy.repository);
importClass(Packages.org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager);
importClass(Packages.org.outerx.daisy.x10.AclDocument);

/*

Exports and Imports ACLs from a repository. Creates roles and users when these
are not present on the system. Users will be initialized with a password that
equals the login.
Accepts arguments :
1. Command(mandatory)
	Possible commands :
	get : gets the live acl 
	put : puts the acl as staging
2. ACL file
3. Repository Url (optional, defaults to 'http://localhost:9263')
4. Repository user (optional, defaults to 'testuser')
5. Repository password (optional, defaults to 'testuser')

*/

var d = new Namespace("http://outerx.org/daisy/1.0");

var repository;

if (arguments.length < 2) {
	showUsage();
	throw new Error("Please specify 2 arguments : 1. command, 2. file")
}

var repoUrl = arguments[2] != null ? arguments[2] : "http://localhost:9263";
var repoUser = arguments[3] != null ? arguments[3] : "testuser";
var repoPassword = arguments[4] != null ? arguments[4] : "testuser";

switch(arguments[0]) {
	case 'get':
		getAcl(arguments[1]);
		break;
	case 'put':
		putAcl(arguments[1]);
		break;
	default:
		showUsage();
		break;
}

function getRepository () {
	if (repository == null) {	
		var credentials = new Credentials(repoUser, repoPassword);
		var repositoryManager = new RemoteRepositoryManager(repoUrl, credentials);
		repository = repositoryManager.getRepository(credentials);
    repository.switchRole(1);
	}

	return repository;
}

function getAcl(filename) {
  var repository = getRepository();
  var acl = repository.getAccessManager().getLiveAcl();
  var exportXml = createExport(acl);
  var writer = new java.io.FileWriter(filename);
  writer.write(exportXml);
  writer.flush();
  writer.close();
}

function putAcl(filename) {
  var repository = getRepository();
  var targetAcl = repository.getAccessManager().getStagingAcl(); 
  var stringWriter = new java.io.StringWriter();
  var reader = new java.io.FileReader(filename);
  var buff = java.lang.reflect.Array.newInstance(java.lang.Character.TYPE, 256);
  var count = 0;
  while ((count = reader.read(buff)) >= 0){
    stringWriter.write(buff, 0, count);
  }
  
  var aclExport = new XML(stringWriter.toString());
  stringWriter.flush();
  stringWriter.close();
  reader.close();

  doImport(aclExport, targetAcl);
}

function createExport(acl, writer) {
  var aclXml = new XML(new String(acl.getXml()));
  var exportXml = <aclExport><users/><roles/><acl/></aclExport>;
  
  var roles = new Object();
  var users = new Object();

  var userManager = getRepository().getUserManager();

  var roleEntries = aclXml.d::aclObject.d::aclEntry.(@subjectType=="role");
  for each (var e in roleEntries) { 
    roles[e.@subjectValue] = e.@subjectValue;
  }
  for each (var o in roles) {
    var role = userManager.getRole(java.lang.Long.valueOf(o).longValue() , false);
    exportXml.roles.appendChild(role.getXml());
    var roleEntries = aclXml.d::aclObject.d::aclEntry.(@subjectType=="role" && @subjectValue==o);
    for each (var e in roleEntries) { 
      e.@subjectValue = role.getName();

    }
  }

  var userEntries = aclXml.d::aclObject.d::aclEntry.(@subjectType=="user");
  for each (var e in userEntries) {
    users[e.@subjectValue] = e.@subjectValue;
  }
  for each (var o in users) {
    var user = userManager.getUser(java.lang.Long.valueOf(o).longValue() , false);
    exportXml.users.appendChild(user.getXml());
    var userEntries = aclXml.d::aclObject.d::aclEntry.(@subjectType=="user");
    for each (var e in userEntries) {
      e.@subjectValue = user.getLogin();
    }
  }

  exportXml.acl.appendChild(aclXml);

  return exportXml;
}

function doImport(aclExport, targetAcl) {
  var roles = aclExport.roles;
  var users = aclExport.users;
  var acl = aclExport.acl.*;

  var roleEntries = acl.d::aclObject.d::aclEntry.(@subjectType=="role");
  for each (var e in roleEntries) { 
    var roleName = e.@subjectValue;
    var role = createRole(roles.d::role.(@name==roleName));    
    e.@subjectValue = role.getId();
  }

  var userEntries = acl.d::aclObject.d::aclEntry.(@subjectType=="user");
  for each (var e in userEntries) { 
    var login = e.@subjectValue;
    var user = createUser(users.d::user.(@login==login));    
    e.@subjectValue = user.getId();
  }

  targetAcl.setFromXml(new AclDocument.Factory.parse(acl).getAcl());
  targetAcl.save();
}

function createRole(roleXml) {
  if (roleXml != null) {
    var userManager = getRepository().getUserManager();
    var role;
    try {
      role = userManager.getRole(roleXml.@name, false);
    } catch (e) {}
    
    if (role == null) {
      role = userManager.createRole(roleXml.@name);
      role.setDescription(roleXml.@description);
      role.save();
    }

    return role;
  }
}

function createUser(userXml) {
  if (userXml != null) {
    var userManager = getRepository().getUserManager();
    var user;
    try {
      user = userManager.getUser(userXml.@login, false);
    } catch (e) {}
    
    if (user == null) {
      user = userManager.createUser(userXml.@login);
      user.setAuthenticationScheme(userXml.@authenticationScheme);
      user.setConfirmed(userXml.@confirmed == "true");
      user.setConfirmKey(userXml.@confirmKey);
      user.setDefaultRole(createRole(userXml.d::role));
      user.setEmail(userXml.@email);
      user.setFirstName(userXml.@firstName);
      user.setLastName(userXml.@lastName);
      user.setUpdateableByUser(userXml.@updateableByUser == "true");
      user.setPassword(userXml.@login);

      for each (var role in userXml.d::roles.d::role) {
        user.addToRole(createRole(role));
      }

      user.save();
      print("Created user " + user.getLogin() + " with password " + user.getLogin());
    }

    return user;
  }
}

function print(s) {
  java.lang.System.out.println(s);
}

function showUsage() {
	print("Gets and puts an ACL xml document out of / into a repository.\n\n" +
		"Usage : daisy-js <acl script> command file-name [repo-url] [repo-user] [repo-passwd]\n" + 
		"Possible commands:\n" + 
		"  get   : get LIVE ACL \n" + 
		"  put   : imports the specified ACL as a STAGING ACL\n" +
		"  help  : shows this message");
}

