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
package org.outerj.daisy.books.publisher.impl.publicationprocess;

import org.outerj.daisy.books.publisher.impl.util.PublicationLog;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlCursor;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * A book publication task for executing an external application.
 * Currently in the form of a custom task and not documented, only
 * committed to have it out of my sandbox and because it's too useful
 * to throw away.
 */
public class ExecuteCommandTask implements PublicationProcessTask {
    private List<String> command;
    private String workingDirPath;
    private int exitValue;

    public ExecuteCommandTask(XmlObject xmlObject) {
        String program = getAttribute(xmlObject, "program");
        workingDirPath = getAttribute(xmlObject, "workingDir");

        String exitValueString = getAttribute(xmlObject, "exitValue");
        exitValue = exitValueString != null ? Integer.parseInt(exitValueString) : -1;

        command = new ArrayList<String>();
        command.add(program);

        XmlObject[] args = xmlObject.selectPath("args/arg");
        for (XmlObject arg : args) {
            command.add(getTextValue(arg));
        }
    }

    private String getAttribute(XmlObject xmlObject, String name) {
        XmlObject attr = xmlObject.selectAttribute(new QName(name));
        if (attr == null)
            return null;
        return getTextValue(attr);
    }

    private String getTextValue(XmlObject xmlObject) {
        XmlCursor cursor = xmlObject.newCursor();
        String text = cursor.getTextValue();
        cursor.dispose();
        return text;
    }

    public void run(PublicationContext context) throws Exception {
        context.getPublicationLog().info("Running execute command task.");
        File workingDir = null;

        if (workingDirPath != null) {
            workingDir = new File(workingDirPath);
            if (!workingDir.exists()) {
                throw new Exception("Specified working directory does not exist: " + workingDir.getAbsolutePath());
            }
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        if (workingDir != null)
            processBuilder.directory(workingDir);

        context.getPublicationLog().info("Will start process " + command);
        Process process = processBuilder.start();

        OutputCollector outputCollector = new OutputCollector(process.getInputStream(), context.getPublicationLog());
        Thread outputCollectionThread = new Thread(outputCollector);
        outputCollectionThread.start();
        process.waitFor();
        outputCollectionThread.join();

        context.getPublicationLog().info("Process ended with exit value " + process.exitValue());
        String output = outputCollector.getOutput();
        if (output.length() > 0) {
            context.getPublicationLog().info("Process output:");
            context.getPublicationLog().info(output);
        }
        
        if (exitValue != -1 && exitValue != process.exitValue()) {
            throw new Exception("Expected process to end with exit value " + exitValue + " but got " + process.exitValue());
        }
    }

    private static class OutputCollector implements Runnable {
        private InputStream is;
        private PublicationLog publicationLog;
        private StringWriter writer = new StringWriter();

        public OutputCollector(InputStream is, PublicationLog publicationLog) {
            this.is = is;
            this.publicationLog = publicationLog;
        }

        public void run() {
            try {
                byte[] buffer = new byte[2000];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    writer.write(new String(buffer, 0, len));
                }
            } catch (Throwable e) {
                publicationLog.error("Unexpected error reading process output.", e);
            }
        }

        public String getOutput() {
            return writer.toString();
        }
    }
}
