Congratulations, you have succesfully generated a sample daisy application.
To get started, follow these instructions:

# 1. Download a Daisy distribution
mvn daisy:get-runtime

# 2. Install the Daisy distribution
mvn daisy:init-runtime

# 3. Create all the maven artifacts and deploy various components (workflows, plugins, wiki extensions)
mvn -Pfull install

# 4. Create your first two documents. You can use whatever collection name you want
# ATTENTION: steps 4-8 are only needed the very first time!
mvn daisy:create-doc -Did=1-${project-namespace} -Dtype=SimpleDocument -Dname=home -Dcollection=project
mvn daisy:create-doc -Did=1-${project-namespace} -Dtype=Navigation -Dname=nav -Dcollection=project

# 5. Edit the documents (Go to http://localhost:8888)

# 6. Export the documents and acl
cd daisy-main
mvn daisy:export
mvn daisy:export-acl
cd ..

# 7. Uncomment the <goal>daisy:import-acl</goal>

# 8. Remove steps 4-8 from this file and check everything (except the runtime and target directory) into your version control system.

# 4. Import documents and acl: cd 'full' profile in the pom.
