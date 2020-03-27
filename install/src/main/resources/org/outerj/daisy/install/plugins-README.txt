Plugins directory
=================

Here you can add jars which should be loaded by the Daisy Runtime
during the startup of the repository server.

These jars need to follow a certain structure. For complete information,
see the Daisy documentation.

The jars need to be in one of two subdirectories:

  * load-before-repository

       These jars will be loaded before the core repository.
       Use this for things like pre-save hooks, authentication
       schemes, text extractors, etc. which should be available
       immediately when the repository starts.

  * load-after-repository

      These jars will be loaded after the core repository.
      Use this for components which need access to the repository
      server.

