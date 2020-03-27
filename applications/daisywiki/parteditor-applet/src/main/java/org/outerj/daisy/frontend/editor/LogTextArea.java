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
package org.outerj.daisy.frontend.editor;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JTextArea;

public class LogTextArea extends JTextArea {
    
    public LogTextArea() {
    }
    
    protected void log(Object msg, Throwable t) {
        append("" + msg + "\n");
        if (t != null) {
            StringWriter stringWriter = new StringWriter();
            t.printStackTrace(new PrintWriter(stringWriter));
            append(stringWriter.getBuffer().toString());
        }
    }
    protected void log(Object msg) {
        append("" + msg);
        append("\n");
        setAutoscrolls(true);
        setCaretPosition(getText().length());
    }


}
