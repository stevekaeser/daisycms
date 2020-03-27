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
package org.outerj.daisy.maven.plugin.os;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.outerj.daisy.maven.plugin.executor.ExecutionResultBuffer;
import org.outerj.daisy.maven.plugin.executor.Executor;

public class OsUtils {

	/**
	 * Check if the current platform is a Linux machine.
	 *
	 * @return <code>true</code> if the current platform is a Linux machine.
	 */
	public static boolean isLinux() {
		return System.getProperty("os.name").equals("Linux");
	}

	/**
	 * Check if the current platform is a Windows machine.
	 *
	 * @return <code>true</code> if the current platform is a Windows machine.
	 */
	public static boolean isWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}

	/**
	 * Checks whether or not the given file is a symbolic link. This type of
	 * file may be handled differently than normal files. Note that this is
	 * completely experimental as there is no decent way to figure out if a file
	 * is a symbolic link. We therefore resort to a native command execution and
	 * hope to be able to predict the output of "file -v path_to_file".
	 *
	 * @param file
	 *            the file to examine.
	 * @return <code>true</code> if it is a symbolic link.
	 */
	public static boolean isLink(File file) {
		if (isLinux()) {
			ExecutionResultBuffer executionResult = new ExecutionResultBuffer();
			try {
				Executor.executeCommand(new String[] { "file", "-b", file.getAbsolutePath() }, executionResult);
				String result = executionResult.getOutput();
				if ((result != null) && result.startsWith("symbolic link to"))
					return true;
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}

	public static Collection<File> deleteFile(File file) throws IOException {
		if (!file.exists())
			return null;
		
		if (file.isDirectory() && !isLink(file)) 
			return FileDeleteWalker.cleanFile(file);
		else if (!file.delete())
			throw new IOException("Could not delete " + file.getAbsolutePath());
		
		return null;
	}
	
	public static boolean windowsDeleteService(String serviceName) throws Exception {
		ExecutionResultBuffer executionResultBuffer = new ExecutionResultBuffer();
		Executor.executeCommand("sc delete \"" + serviceName + "\"", executionResultBuffer);
		return !executionResultBuffer.getOutput().contains("FAILED 1060");
	}

	public static boolean windowsServiceInstalled(String serviceName) throws Exception {
		ExecutionResultBuffer executionResultBuffer = new ExecutionResultBuffer();
		Executor.executeCommand("sc query \"" + serviceName + "\"", executionResultBuffer);
		return !executionResultBuffer.getOutput().contains("FAILED 1060");
	}

	public static boolean windowsServiceRunning(String serviceName) throws Exception {
		ExecutionResultBuffer executionResultBuffer = new ExecutionResultBuffer();
		Executor.executeCommand("sc query \"" + serviceName + "\"", executionResultBuffer);
		if (executionResultBuffer.getOutput().contains("RUNNING"))
			return true;

		return false;
	}

	public static String windowsServiceStatus(String serviceName) throws Exception {
		ExecutionResultBuffer executionResultBuffer = new ExecutionResultBuffer();
		Executor.executeCommand("sc query \"" + serviceName + "\"", executionResultBuffer);
		String output = executionResultBuffer.getOutput();
		int index = output.indexOf("STATE");
		if (index == -1)
			return "No service found";

		output = output.substring(index);
		output = output.substring(0, output.indexOf('\n'));
		return output;
	}

	public static void main(String[] args) throws Exception {
	}
}
