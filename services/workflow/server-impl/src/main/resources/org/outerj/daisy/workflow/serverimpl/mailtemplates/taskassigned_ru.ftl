<#assign mailSubject = "Task " + task.definition.label>
Добрый день,

Вам будет распределено задание.

Задание:
${task.definition.label}

Процесс выполнения работы:
${getVariable(task, "daisy_description", "global").value}

<#if task.dueDate??>
Дата выполнения задания: ${task.dueDate?datetime}

</#if>
<#if taskURL??>
Решите задание:
${taskURL}

</#if>

Спасибо.