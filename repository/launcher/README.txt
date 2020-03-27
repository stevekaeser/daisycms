Launcher
========

The launcher contains code to help launching the Daisy repository server
or the Daisy remote client.

The advantage of using the launcher is that it's the only jar one needs
to have in the classpath. So you don't need to find out which implementation
jars you need, and are independent of changes in those with Daisy upgrades.

For the remote client launcher and for embedded repository server
scenarios, you will need to put the API jars for the API's you
want to use in your own classloader (= the one containing the launcher,
or a parent thereof).
