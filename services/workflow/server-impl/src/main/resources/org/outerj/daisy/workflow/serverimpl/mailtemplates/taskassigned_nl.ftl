<#assign mailSubject = "Taak " + task.definition.label>
Hallo,

Er is een taak aan je toegekend.

Taak:
${task.definition.label}

Workflow proces:
${getVariable(task, "daisy_description", "global").value}

<#if task.dueDate??>
Vervaldatum: ${task.dueDate?datetime}

</#if>
<#if taskURL??>
Hier vind je de taak:
${taskURL}

</#if>

Alvast bedankt.