Workflow functionality for Daisy.

Internally based on jBPM.

IN CASE OF UPGRADING JBPM:
 - Database schema installation and version check is handled in CommonWorkflowManager

 - Look at the following files in our source to make them in sync
   with the versions from the new jBPM sources:
      - jbpm.converter.properties
      - jbpm.varmapping.xml
      - (possibly: jbpm.cfg.xml)
      - DaisyJobExecutor and DaisyJobExecutorThread: update overriden methods with up-to-date version