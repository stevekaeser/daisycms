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

import org.apache.maven.plugin.logging.Log;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaLoadListener;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaLoadResult;

public class LoggingSchemaLoadListener implements SchemaLoadListener {
    private Log log;
    private boolean interrupted = false;

    public LoggingSchemaLoadListener(Log log) {
        this.log = log;
    }

    public void done() {
        log.info("schema imported");
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void interrupt() {
        this.interrupted = true;
    }

	public void conflictingFieldType(String fieldTypeName, ValueType requiredType, ValueType foundType) throws Exception {
		log.error("Conflicting field type: " + fieldTypeName + " required " + requiredType.toString() + " found " + foundType.toString());
	}

	public void conflictingHierarchical(String fieldTypeName, boolean needHierarchical, boolean foundHierarchical) throws Exception {
		log.error("Conflicting hierarchical field: " + fieldTypeName + " required " + needHierarchical + " found " + foundHierarchical);		
	}

	public void conflictingMultiValue(String fieldTypeName, boolean needMultivalue, boolean foundMultivalue) throws Exception {
		log.error("Conflicting multivalue field: " + fieldTypeName + " required " + needMultivalue + " found " + foundMultivalue);
	}

	public void documentTypeLoaded(String documentTypeName, SchemaLoadResult result) {
		log.debug("Document type loaded: " + documentTypeName.toString() + " " + result.toString());
	}

	public void fieldTypeLoaded(String fieldTypeName, SchemaLoadResult result) {
		log.debug("Field type loaded: " + fieldTypeName.toString() + " " + result.toString());
	}

	public void partTypeLoaded(String partTypeName, SchemaLoadResult result) {
		log.debug("Part type loaded: " + partTypeName.toString() + " " + result.toString());
	}
}
