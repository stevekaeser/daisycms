Daisy Windows Installer
=======================

Intro
-----

The Daisy Windows installer starts from a binary Daisy distribution
and creates a Windows installer for it.
To build a binary Daisy distribution, see ../core/README.txt
(alternatively, download an existing one)

Building instructions
---------------------

*  Download and install the latest NSIS release from
   http://nsis.sourceforge.net/Download

   Add the installation main directory your Windows PATH variable

*  Copy all dll-files inside the directory 'plugins' to the directory
   'Plugins' of your NSIS installation.

*  Copy the file installer/settings.nsh.template to installer/settings.nsh
   and open it in an editor of your choice.
   - Look for the Section 'File locations' at the very top and adjust
   the variables defined there.
   - Make sure the values for APP_VERSION and APP_SERIES_KEY are correct
   
   If you want to produce the bundled version:
   Download the latest binaries of JRE, JAI and MySQL to a directory of
   your choice. Adapt the variable FILES_BINARIES_BUNDLE so that it
   points to that directory.

*  Open a windows command prompt and change to the directory 'installer'
   Issue the following command:

   makensis daisy.nsi
   
   This compiler run will produce the installer executable (in the
   current directory).

   If you want to produce the bundled version, issue:   

   makensis /DSETUPTYPE_BUNDLE="Bundled installer" daisy.nsi
   
   Alternatively, you can use the Compiler GUI MakeNSISW, which may
   be invoked by right-clicking on the daisy.nsi.
