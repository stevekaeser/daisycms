/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.books.publisher.impl.publicationprocess;

import org.apache.cocoon.components.flow.util.PipelineUtil;
import org.apache.cocoon.components.LifecycleHelper;

import java.util.Map;
import java.io.OutputStream;

public class PublicationProcessTaskUtil {
    public static String getFileName(String path) {
        int position = path.lastIndexOf('/');
        if (position == -1) {
            return path;
        } else {
            return path.substring(position + 1);
        }
    }

    public static void executePipeline(String pipe, Map viewData, OutputStream os, PublicationContext context) throws Exception {
        PipelineUtil pipelineUtil = new PipelineUtil();
        try {
            LifecycleHelper.setupComponent(pipelineUtil, null, context.getAvalonContext(), context.getServiceManager(), null, false);
            pipelineUtil.processToStream(pipe, viewData, os);
        } finally {
            LifecycleHelper.dispose(pipelineUtil);
        }
    }

    public static String getRequiredAttribute(Map attributes, String attrName, String processTaskName) throws Exception {
        String value = (String)attributes.get(attrName);
        if (value == null || value.trim().length() == 0) {
            throw new Exception("Missing attribute for process task " + processTaskName + ": " + attrName);
        }
        return value;
    }
}
