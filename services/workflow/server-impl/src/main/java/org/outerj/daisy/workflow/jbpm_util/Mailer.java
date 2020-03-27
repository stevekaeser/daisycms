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
package org.outerj.daisy.workflow.jbpm_util;

import org.outerj.daisy.workflow.WorkflowException;

import java.util.Locale;
import java.util.Map;

/**
 * Interface for the mailer component made available to workflow actions.
 */
public interface Mailer {
    void sendMail(String templateName, Locale locale, String destinationEmail, Map<String, Object> mailData) throws WorkflowException;
}
