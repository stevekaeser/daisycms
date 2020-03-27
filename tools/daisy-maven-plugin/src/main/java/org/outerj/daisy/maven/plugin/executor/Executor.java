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


import org.apache.maven.plugin.logging.Log;
import org.outerj.daisy.maven.plugin.os.OsUtils;

public class Executor {

    public static String[] env;

    public static boolean executeCommand(String cmd) throws Exception {
        return executeCommand(new String[] {cmd});
    }

    public static boolean executeCommand(String cmd, Log log) throws Exception {
        return executeCommand(new String[] {cmd}, log);
    }

    public static boolean executeCommand(String cmd, ExecutionResult executionResult) throws Exception {
        return executeCommand(new String[] {cmd}, executionResult);
    }

    public static boolean executeCommand(String[] cmd) throws Exception {
        return executeCommand(cmd, new NullExecutionResult());
    }

    public static boolean executeCommand(String[] cmd, Log log) throws Exception {
        return executeCommand(cmd, new LoggingExecutionResult(log));
    }

    public static boolean executeCommand(String[] cmd,
            ExecutionResult executionResult) throws Exception {
        if (OsUtils.isWindows()) {
            String[] winCommands = new String[cmd.length + 2];
            System.arraycopy(cmd, 0, winCommands, 2, cmd.length);
            winCommands[0] = "cmd.exe";
            winCommands[1] = "/C";
            cmd = winCommands;
        }
//		executionResult.appendOutput("execute: " + ArrayUtils.toString(cmd));
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(cmd, env);
        StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(),
                executionResult, true);
        StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(),
                executionResult, false);

        errorGobbler.start();
        outputGobbler.start();

        // make sure ALL threads are closed before returning a result. Not waiting for the gobblers to
        // finish may result in fetching their result too early and returning empty strings instead.
        int exitValue = proc.waitFor();
        errorGobbler.join();
        outputGobbler.join();

        executionResult.setExitValue(exitValue);
        return exitValue == 0;
    }

}
