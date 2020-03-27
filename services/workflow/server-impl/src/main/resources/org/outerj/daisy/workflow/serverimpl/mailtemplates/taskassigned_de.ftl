<#assign mailSubject = "Task " + task.definition.label>
Guten Tag,

Ihnen wurde eine Aufgabe zugewiesen.

Aufgabe:
${task.definition.label}

Prozess im Arbeitsablauf:
${getVariable(task, "daisy_description", "global").value}

<#if task.dueDate??>
Fälligkeitsdatum: ${task.dueDate?datetime}

</#if>
<#if taskURL??>
Erledigen Sie die Aufgabe:
${taskURL}

</#if>

Danke.