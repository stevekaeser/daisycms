/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.outerj.daisy.workflow.serverimpl;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.cache.TemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.core.Environment;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.workflow.*;
import org.outerj.daisy.workflow.jbpm_util.Mailer;
import org.outerj.daisy.emailer.Emailer;
import org.outerj.daisy.configutil.PropertyResolver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.io.StringWriter;
import java.io.File;
import java.io.IOException;

/**
 * Workflow mailer component. Supports template-based formatting and sending of emails.
 */
public class WfMailer implements Mailer {
    private Configuration configuration;
    private Emailer emailer;
    private String taskURL;
    private Log log = LogFactory.getLog(getClass());

    public WfMailer(String[] locations, String taskURL, Repository repository) throws IOException {
        configuration = new Configuration();

        List<TemplateLoader> loaders = new ArrayList<TemplateLoader>(locations.length);
        for (String location : locations) {
            if (location.startsWith("resource:")) {
                String basePath = location.substring("resource:".length());
                loaders.add(new ClassTemplateLoader(WfMailer.class, basePath));
            } else {
                File file = new File(location);
                if (!file.exists()) {
                    log.warn("Workflow mail template location does not exist, will skip it: " + location);
                } else {
                    loaders.add(new FileTemplateLoader(file, true));
                }
            }
        }

        configuration.setTemplateLoader(new MultiTemplateLoader(loaders.toArray(new TemplateLoader[0])));

        this.emailer = (Emailer)repository.getExtension("Emailer");
        this.taskURL = taskURL;
    }

    public void sendMail(String templateName, Locale locale, String destinationEmail, Map<String, Object> mailData) throws WorkflowException {
        try {
            Template template = configuration.getTemplate(templateName, locale, "UTF-8", true);

            if (taskURL != null && mailData.containsKey("task")) {
                WfTask task = (WfTask)mailData.get("task");
                Properties props = new Properties();
                props.put("taskId", task.getId());

                // Try to get the site name for the daisy_site_hint variable.
                // This variable is usually only declared on the start state task, so
                // search for the start state.
                if (mailData.containsKey("process")) {
                    WfProcessInstance process = (WfProcessInstance)mailData.get("process");
                    for (WfTask aTask : process.getTasks()) {
                        if (aTask.getDefinition().getNode().getNodeType().equals("StartState")) {
                            WfVariable variable = aTask.getVariable("daisy_site_hint", VariableScope.GLOBAL);
                            if (variable != null)
                                props.put("site", variable.getValue());
                            break;
                        }
                    }
                }

                String result = PropertyResolver.resolveProperties(taskURL, props);
                mailData.put("taskURL", result);
            }

            StringWriter writer = new StringWriter();
            Environment environment = template.createProcessingEnvironment(mailData, writer);
            environment.setLocale(locale);
            environment.process();

            // Template can define the subject by setting a variable "mailSubject"
            Object subject = environment.getVariable("mailSubject");
            if (subject == null)
                subject = "workflow mail without subject";

            emailer.send(destinationEmail, subject.toString(), writer.toString());
        } catch (Exception e) {
            throw new WorkflowException("Error sending email.", e);
        }
    }
}
