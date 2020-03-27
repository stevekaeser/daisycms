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

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * General utility to copy or link files to a particular destination.
 *
 * @author Jan Hoskens
 * @goal copy-or-link
 * @description General utility to copy or link files to a particular
 *              destination.
 */
public class DaisyCopyOrLinkMojo extends AbstractDaisyMojo {

    /**
     * Source directory.
     *
     * @parameter expression="${src}"
     * @required
     */
    private File src;

    /**
     * Destination directory.
     *
     * @parameter expression="${dest}"
     * @required
     */
    private File dest;

    /**
     * Copy the parent into the destination instead of copying its children.
     *
     * @parameter expression="${copyParent}" default-value="false"
     */
    private boolean copyParent;

    /**
     * This parameter won't allow the creation of links instead of making
     * copies. As a default, a Linux machine will get symbolic links to ease the
     * development setup.
     *
     * @parameter expression="${noLink}" default-value="false"
     */
    private boolean noLink;

    
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (copyParent)
                copyOrLink(src, new File(dest, src.getName()), FileFilterUtils.makeSVNAware(null), noLink);
            else
                copyOrLink(src, dest, noLink);
        } catch (IOException e) {
            throw new MojoExecutionException("Exception while copying or linking dir: " + src + " to " + dest, e);
        }
    }
}