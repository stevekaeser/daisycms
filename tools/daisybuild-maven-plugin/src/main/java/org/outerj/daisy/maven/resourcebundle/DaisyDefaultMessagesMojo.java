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
package org.outerj.daisy.maven.resourcebundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * 
 * @author paul
 * 
 * @goal defaultmessages 
 * @phase package
 */
public class DaisyDefaultMessagesMojo extends AbstractMojo {
    
    /**
     * @parameter default-value="messages_en.properties"
     */
    private String defaultMessagesFilename;
    
    /**
     * @parameter default-value="${project.build.outputDirectory}"
     */
    private File targetDir;
    

    public void execute() throws MojoExecutionException, MojoFailureException {
        List<File> msgFiles = findMessageFiles(this.targetDir);
        
        for (File src : msgFiles ) {
            File target = new File(src.getParentFile(), "messages.properties");
            if (!target.exists()) {
                try {
                    copyFile(src, target);
                } catch (Exception e) {
                    throw new MojoExecutionException("Could not copy '" + src.getAbsolutePath() + "' to '" + target.getAbsolutePath() + "'", e);
                }
            }
        }
    }
    
    private List<File> findMessageFiles (File dir) {
        List<File> files = new ArrayList<File>();
        
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                files.addAll(findMessageFiles(f));
            } else if (f.isFile() && this.defaultMessagesFilename.equals(f.getName())) {
                files.add(f);
            } else {
                // skip
            }
        }
        
        return files;
    }
    
    private static void copyFile(File in, File out) throws Exception {
        FileInputStream fis  = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        try {
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        } 
        catch (Exception e) {
            throw e;
        }
        finally {
            if (fis != null) fis.close();
            if (fos != null) fos.close();
        }
    }
}