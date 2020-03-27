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
import org.outerj.daisy.repository.AccessException;
import org.outerj.daisy.repository.DocumentLockedException;
import org.outerj.daisy.tools.importexport.import_.ImportListener;
import org.outerj.daisy.tools.importexport.import_.documents.DocumentImportResult;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaLoadListener;
import org.outerj.daisy.tools.importexport.model.ImpExpVariantKey;

/**
 * Implementation of the {@link ImportListener} to route all output to the Maven
 * Logger.
 * 
 * @author Jan Hoskens
 * 
 */
public class LoggingImportListener implements ImportListener {

	private final Log log;

	private final SchemaLoadListener schemaListener;

	public LoggingImportListener(Log log) {
		this.log = log;
		this.schemaListener = new LoggingSchemaLoadListener(log);
	}

	public void debug(String debug) {
		log.debug(debug);
	}

	public void endDocumentProgress() {
	}

	public SchemaLoadListener getSchemaListener() {
		return schemaListener;
	}

	public void info(String info) {
		log.info(info);
	}

	public boolean isInterrupted() {
		return false;
	}

	public void startActivity(String arg0) {
		log.info(arg0);
	}

	public void startDocumentProgress(int arg0) {
	}

	public void updateDocumentProgress(int arg0) {
	}

	public void failed(ImpExpVariantKey impExpVariantKey, Throwable throwable) throws Exception {
		log.error("Import failed: \t" + impExpVariantKey.toString() + " \t" + throwable.toString());
	}

	public void lockedDocument(ImpExpVariantKey impExpVariantKey,
			DocumentLockedException documentLockedException) throws Exception {
		log
				.warn(" Import warning: " + impExpVariantKey.toString() + " \t"
						+ documentLockedException.toString());
	}

	public void permissionDenied(ImpExpVariantKey impExpVariantKey, AccessException accessException)
			throws Exception {
		log.warn(" Import warning: \t" + impExpVariantKey.toString() + " \t" + accessException.toString());
	}

	public void success(ImpExpVariantKey impExpVariantKey, DocumentImportResult documentImportResult) {
		log.debug("Import success: \t" + impExpVariantKey.toString() + " " + documentImportResult.toString());
	}

}
