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
package org.outerj.daisy.doctaskrunner;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;

/**
 * Every time a document task is started, a DocumentAction instance is created and 
 * <ol>
 *   <li>setup is called (only once)</li>
 *   <li>execute is called (once per document)</li>
 *   <li>tearDown is called (only once)</li>
 * </ol>
 * 
 * Note that tearDown is *guaranteed* to be called, even if exceptions are thrown by the 
 * setup or execute methods. (guaranteed, unless you count System.exit calls or other 
 * mayhem like fire in the server room ;-)
 */
public interface DocumentAction {
    /**
     * Called before starting document actions to allow configuration and one-time setup
     * @param keys
     * @param taskSpecifiation
     * @param repository
     */
    public void setup(VariantKey[] keys, TaskSpecification taskSpecifiation, TaskContext taskContext, Repository repository) throws Exception;
    
    /**
     * Called once for each document.
     * @param variantKey
     * @return
     */
    public void execute(VariantKey variantKey, DocumentExecutionResult documentExecutionResult) throws Exception;
    
    /**
     * Called once to allow cleanup when task is stopped or interrupted (either by user action or because of exceptions) 
     */
    public void tearDown() throws Exception;

}
