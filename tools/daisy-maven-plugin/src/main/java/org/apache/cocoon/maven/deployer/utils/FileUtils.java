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
package org.apache.cocoon.maven.deployer.utils;

import java.io.File;

/**
 * Utitily class to handle ZIP archives.
 * 
 * @version $Id: FileUtils.java 588009 2007-10-24 20:39:12Z vgritsenko $
 */
public class FileUtils {
	/**
	 * Prepare directory structure for non-existing file
	 */
	public static File createPath(File file) {
        if ( file.getParentFile() != null && !file.getParentFile().exists())
            file.getParentFile().mkdirs();
        return file;
	}


	
}
