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
import org.outerj.daisy.linkextraction.LinkType;
import org.outerj.daisy.tools.importexport.export.ExportListener;
import org.outerj.daisy.tools.importexport.model.ImpExpVariantKey;

/**
 * Implementation of the {@link ExportListener} to route all output to the Maven
 * Logger.
 * 
 * @author Jan Hoskens
 *
 */
public class LoggingExportListener implements ExportListener {

	private final Log log;
	
	public LoggingExportListener(Log log) {
		this.log = log;
	}

	public void endDocumentProgress() {
	}

	public void info(String info) {
		log.info(info);
	}

	public boolean isInterrupted() {
		return false;
	}

	public void startDocumentProgress(int arg0) {
		
	}

	public void updateDocumentProgress(int arg0) {
		
	}

	public void failed(ImpExpVariantKey impExpVariantKey, Throwable throwable) throws Exception {
		log.error("Export failed: \t" + impExpVariantKey.toString() + " \t" + throwable.toString());
	}

	public void failedItem(String itemType, String itemName, Throwable throwable) {
		log.error("Export error: \t" + itemType + " " + itemName + " \t" + throwable);
	}

	public void hasLink(ImpExpVariantKey arg0, ImpExpVariantKey arg1, LinkType arg2) {
		
	}

	public void skippedBecauseNoLiveVersion(ImpExpVariantKey impExpVariantKey) {
		log.info("Export skipped - no live version: " + impExpVariantKey.toString() + " ");		
	}

	public void skippedBecauseRetired(ImpExpVariantKey impExpVariantKey) {
		log.info("Export skipped - retired: " + impExpVariantKey.toString() + " ");
	}

	public void success(ImpExpVariantKey impExpVariantKey) {
		log.debug("Export: " + impExpVariantKey.toString());
	} 
}
