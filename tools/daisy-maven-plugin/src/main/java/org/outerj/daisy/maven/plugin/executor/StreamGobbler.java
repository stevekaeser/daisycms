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
package org.outerj.daisy.maven.plugin.executor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Saves the input to a string for later use.
 *
 * @author Jan Hoskens
 */
public class StreamGobbler extends Thread {
	InputStream is;

	ExecutionResult executionResult;

	boolean errorStream;

	public StreamGobbler(InputStream is, ExecutionResult executionResult, boolean errorStream) {
		this.is = is;
		this.executionResult = executionResult;
		this.errorStream = errorStream;
	}

	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null) {
				if (errorStream)
					executionResult.appendError(line);
				else
					executionResult.appendOutput(line);
			}
		} catch (IOException ioe) {
			executionResult.appendError(ioe.toString());
		}
	}

}