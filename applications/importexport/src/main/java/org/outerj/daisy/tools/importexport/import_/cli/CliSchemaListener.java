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
package org.outerj.daisy.tools.importexport.import_.cli;

import org.outerj.daisy.tools.importexport.import_.schema.BaseSchemaLoadListener;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaLoadResult;

import java.io.PrintStream;
import java.util.List;

class CliSchemaListener extends BaseSchemaLoadListener {
    private PrintStream out;
    private boolean interrupted = false;

    public CliSchemaListener(PrintStream out) {
        this.out = out;
    }

    public void done() {
        out.println();
        out.println("Schema import summary");
        out.println("---------------------");
        out.println(" Type      Total   Created  Updated  No update needed  Update skipped");
        printSummaryLine("Field", getLoadedFieldTypes());
        printSummaryLine("Part", getLoadedPartTypes());
        printSummaryLine("Document", getLoadedDocumentTypes());
    }

    private void printSummaryLine(String title, List<LoadInfo> types) {
        out.printf(" %1$-9s %2$-7d %3$-8d %4$-8d %5$-17d %6$-10d",
                title,
                types.size(),
                count(types, SchemaLoadResult.CREATED),
                count(types, SchemaLoadResult.UPDATED),
                count(types, SchemaLoadResult.NO_UPDATE_NEEDED),
                count(types, SchemaLoadResult.UPDATE_SKIPPED));
        out.println();
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void interrupt() {
        this.interrupted = true;
    }
}
