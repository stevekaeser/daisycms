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
package org.outerj.daisy.maven.plugin;

import java.io.File;
import java.io.IOException;

import javax.xml.xpath.XPathException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 *
 * @author Karel Vervaeke
 * @goal xpatch
 * @description patching xml-files (based on the XPatch stuff from Cocoon)
 */
public class XPatchMojo extends AbstractDaisyMojo {

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${patchDir}" default-value="${basedir}/src/main/xpatch"
     */
    private File patchDir;
    
    /**
     * @parameter expression="${target}"
     * @required
     */
    private Patch[] patches;

    public void execute() throws MojoExecutionException, MojoFailureException {
        for (Patch patch: patches) {
            applyPatchFiles(patch);
        }
    }

    private void applyPatchFiles(Patch patch) throws MojoExecutionException {
        if (patch.getTarget().exists()) {
            File patchFile = patch.getPatchFile();
            try {
                if (patchFile == null) {
                    patchFile = new File(patchDir, patch.getTarget().getName() + ".xpatch");
                }
                patchFile(patch.getTarget(), patchFile);
            } catch (XPathException e) {
                throw new MojoExecutionException("Failed to patch "
                        + patch.getTarget() + " with " + patchFile, e);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to patch "
                        + patch.getTarget() + " with " + patchFile, e);
            }
        }
        else {
            throw new MojoExecutionException("Patch target does not exist: " + patch.getTarget().getAbsolutePath());
        }

    }

}
