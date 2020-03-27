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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
/**
 * Create a variant of an existing Daisy document.
 *
 * @author Jan Hoskens
 * @goal create-variant
 * @aggregator
 * @description Create daisy languages and/or branches.
 */
public class DaisyCreateVariant extends AbstractDaisyMojo {

    /**
     * Create a new language variant.
     *
     * @parameter expression="{language}"
     */
    private String language;

    /**
     * Create multiple variants in each given language.
     *
     * @parameter
     */
    private String[] languages;

    /**
     * Create a new branch variant.
     *
     * @parameter expression="{branch}"
     */
    private String branch;

    /**
     * Create multiple variants in each given branch.
     *
     * @parameter
     */
    private String[] branches;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (languages != null) {
                for (String daisyLanguage : languages) {
                    createLanguage(daisyLanguage);
                }
            }

            if (language != null) {
                createLanguage(language);
            }

            if (branches != null) {
                for (String daisyBranch : branches) {
                    createBranch(daisyBranch);
                }
            }

            if (branch != null) {
                createBranch(branch);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Exception while creating variant.", e);
        }
    }

    private void createLanguage(String daisyLanguage) throws Exception {
        getVariantManager().createLanguage(daisyLanguage);
    }

    private void createBranch(String daisyBranch) throws Exception {
        getVariantManager().createBranch(daisyBranch);
    }
}