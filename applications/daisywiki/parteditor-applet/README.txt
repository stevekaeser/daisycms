The parteditor applet is signed to avoid security warnings in the browser:

Development mode
================
Use the 'dev' profile:

mvn -Pdev install

This will cause the applet jar to be signed using a dummy (self-signed) certificate.

Binary build mode
=================
Specify a keystore password and a certificate alias like this:

mvn -Djarsigner.storepass=... -Djarsigner.alias=...

You can also specify the keystore using -Djarsigner.keystore=... - the default value is ${user.home}/.keystore

