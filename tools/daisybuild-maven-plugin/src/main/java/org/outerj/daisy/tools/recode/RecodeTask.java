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
package org.outerj.daisy.tools.recode;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * recode ant task
 */
public class RecodeTask extends Task {
    
    private String file;
    private String encoding = "UTF-8";
    private String outputencoding = "UTF-8";

    @Override
    public void execute() throws BuildException {
        if (file == null) throw new BuildException("file attribute is required");

        try {
            RecodeUtil.recode(new File(file), encoding, outputencoding);
        } catch (Exception e) {
            throw new BuildException("recode failed", e);
        }
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getOutputencoding() {
        return outputencoding;
    }

    public void setOutputencoding(String outputencoding) {
        this.outputencoding = outputencoding;
    }
    
}
