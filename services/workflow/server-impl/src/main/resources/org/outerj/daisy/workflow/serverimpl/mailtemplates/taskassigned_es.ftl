<#assign mailSubject = "Task " + task.definition.label>
Saludos,

Le ha sido asignada una tarea.

Tarea:
${task.definition.label}

Proceso en el flujo de trabajo:
${getVariable(task, "daisy_description", "global").value}

<#if task.dueDate??>
Fecha de ejecución prevista: ${task.dueDate?datetime}

</#if>
<#if taskURL??>
Ejecute la tarea:
${taskURL}

</#if>

Gracias