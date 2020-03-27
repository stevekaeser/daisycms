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
package org.outerj.daisy.tools.importexport.util;

import java.io.PrintStream;

/**
 * CLI-based progress indicator with support for indicating failures.
 */
public class ProgressIndicator {
    private double total;
    private int currentPercentage;
    private boolean failure = false;
    private PrintStream out;

    public ProgressIndicator() {
        out = System.out;
    }

    public ProgressIndicator(PrintStream stream) {
        out = stream;
    }

    public void failureOccured() {
        failure = true;
    }

    public void startProgress(int total) {
        this.total = total;
        currentPercentage = 0;
        out.println();
        out.println("If a '!' symbol is printed in the progress bar, it means at least");
        out.println("one failure occured in that interval.");
        out.println();
        out.println("0% -------------------------------------------------- 100%");
        out.print("   ");
    }

    public void updateProgress(int current) {
        int percentage = (int)Math.round( ( ((double)current) / total) * 50 );
        if (percentage > currentPercentage) {
            String symbol = failure ? "!" : ">";
            failure = false;
            while (currentPercentage < percentage) {
                out.print(symbol);
                currentPercentage++;
            }
        }
    }

    public void endProgress() {
        updateProgress((int)total);
        System.out.println();
    }

}
