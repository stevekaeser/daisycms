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
package org.outerj.daisy.tools.recode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

// The 1-minute hack to recode a file
public class RecodeUtil {
    
    private RecodeUtil() {
        //utility class
    }
    
    public static void recode(File file, String inputEncoding, String outputEncoding) throws Exception {
        ArrayList lines = new ArrayList();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), inputEncoding));
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        reader.close();

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), outputEncoding));
        Iterator linesIt = lines.iterator();
        while (linesIt.hasNext()) {
            writer.println((String)linesIt.next());
        }
        writer.close();
    }

}
