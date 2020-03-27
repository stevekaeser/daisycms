Transdiffer
===========

This little tool is used to diff a list of i18n files to see if they
have been updated between two svn revisions.

   >> Note: you'll probably need to make the tool executable using
            chmod u+x tools/i18ncheck/transdiffer

Usage : 

transdiffer <old-revision> <new-revision> <out-file>

The transdiffer expects to receive a list of files that it should check for
differences, the list is provided through the stdin. Such a list is
available in the file i18_list.txt. This file has been compiled in such a
manner that you need to run tool in the root of your daisy checkout.

This is the typical usage:

cat tools/i18ncheck/i18n-list.txt | tools/i18ncheck/transdiffer old new i18n-request-email.txt

After the transdiffer has checked for changes it will fill the out-file with
diffs of the changed files.

   >> Note: the process of checking for changes can take while
