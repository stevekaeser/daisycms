#!/bin/bash

mycp=${COPY_CMD:-"cp -r"}

function error() {
  echo 
  echo ERROR: $@
  echo
  echo "Usage:"
  echo " To show version information: $0 /path/to/app"
  echo " To upgrade between two versions: $0 /path/to/app oldpackage newpackage"
  echo ""
  exit
}

function show_packageinfo() {
  echo "Available packages: "
  for i in $app/packages/*; do
    [ ! -d $i ] && continue;
    read packVer < $i/package.version
    [ "$packVer" == "$currVer" ] && echo "*"`basename $i`" (version $packVer)"
    [ "$packVer" != "$currVer" ] && echo `basename $i`" (version $packVer)"
  done
  echo
}

[ -z "$1" ] && error "I need at least one argument"

app=$(cd $1 && pwd);

currVerFile=$app/package.version
read currVer < $currVerFile
[ -z "$currVer" ] && error "could not read $currVerFile"
[ ! -e "$app/package.version" ] && error "$app does not look like a daisy application directory"

[ -z "$2" ] && show_packageinfo && exit

oldpack=$app/packages/$2
newpack=$app/packages/$3

oldVerFile=$oldpack/package.version
newVerFile=$newpack/package.version

[ ! -e "$oldpack/package.version" ] && error "$oldpack does not look like a daisy application package"
[ ! -e "$newpack/package.version" ] && error "$newpack does not look like a daisy application package"

read oldVer < $oldVerFile
[ -z "$oldVer" ] && error "could not read $oldVerFile"
read newVer < $newVerFile
[ -z "$newVer" ] && error "could not read $newVerFile"

[ "$currVer" != "$oldVer" ] && error "The current package version ($newVer) does not match old package version ($oldVer)"

echo "Files to delete before copying new package:"
(cd $oldpack/repodata && find . -type f | while read f; do bash -c "test -f '${app}/repodata/$f' && echo rm \'${app}/repodata/$f\'"; done;)
(cd $oldpack/wikidata && find . -type f | while read f; do bash -c "test -f '${app}/wikidata/$f' && echo rm \'${app}/wikidata/$f\'"; done;)
echo "I will remove the files listed above before copying the new package"
echo "Is this okay? (type 'this is okay' to continue)"
read answer

[ "$answer" != "this is okay" ] && echo "aborted" && exit;

echo "Removing old repodata and wikidata files"
(cd $oldpack/repodata && find . -type f | while read f; do bash -c "test -f '${app}/repodata/$f' && echo rm \'${app}/repodata/$f\'"; done;)|sh
(cd $oldpack/wikidata && find . -type f | while read f; do bash -c "test -f '${app}/wikidata/$f' && echo rm \'${app}/wikidata/$f\'"; done;)|sh

echo "Copying new repodata and wikidata files"
$mycp $newpack/repodata $app
$mycp $DAISY_CP_OPT $newpack/wikidata $app
echo "repodata and wikidata now at package version $newVer"

echo "Replacing daisy..."
rm -rf $app/daisy
$mycp $DAISY_CP_OPT $newpack/daisy $app/daisy
echo "daisy now at package version $newVer"

echo "Copying new package.version"
$mycp $DAISY_CP_OPT $newVerFile $app

echo "Done. Before you leave, don't forget to:"
echo " * perform daisy upgrade steps"
echo " * import documents"
echo " * import workflows"
echo " * import the new acl"
echo " * check the repository schema"
echo " * check $newpack/other for other instructions"
echo " * check service scripts/environment properties"
echo " * check that backups are still working"
