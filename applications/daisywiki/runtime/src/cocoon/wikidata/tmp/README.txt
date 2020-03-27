tmp directory
=============

This directory is used as Java temporary directory (java.io.tmpdir) by the Daisy Wiki.

This is prefered over the system's default temp directory as on some systems
this directory is wiped automatically, which can cause things to go broken
(see DSY-379).