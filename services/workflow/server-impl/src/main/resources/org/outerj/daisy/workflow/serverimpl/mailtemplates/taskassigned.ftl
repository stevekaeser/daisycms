<#assign mailSubject = "Task " + task.definition.label>
Hi,

A task has been assigned to you.

Task:
${task.definition.label}

Workflow process:
${getVariable(task, "daisy_description", "global").value}

<#if task.dueDate??>
Due date: ${task.dueDate?datetime}

</#if>
<#if taskURL??>
Go for it:
${taskURL}

</#if>

Thanks.