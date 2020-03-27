if [ -z "$DAISY_HOME" ] ; then
  echo "DAISY_HOME not set!"
  exit 1;
fi;


[ -z $5 ] && echo "Usage:
$0 <repoUrl> <adminUser> <adminPassword> <dbUrl> <dbUser> <dbPassword>

Example:

$0 http://localhost:9263 testuser testuser jdbc:mysql://localhost:3306/daisyrepository root \"\"" && exit

export DAISY_CLI_CLASSPATH=$DAISY_HOME/lib/daisy/daisy-repository-server-impl/@daisy.version@/daisy-repository-server-impl-@daisy.version@.jar:$DAISY_HOME/lib/mysql/mysql-connector-java/3.1.12/mysql-connector-java-3.1.12.jar:$DAISY_HOME/lib/xpp3/xpp3_min/1.1.3.4-RC8/xpp3_min-1.1.3.4-RC8.jar


abspath="$(cd "${0%/*}" 2>/dev/null; echo "$PWD")"
$DAISY_HOME/bin/daisy-js $abspath/regenerate-summaries.js $@
