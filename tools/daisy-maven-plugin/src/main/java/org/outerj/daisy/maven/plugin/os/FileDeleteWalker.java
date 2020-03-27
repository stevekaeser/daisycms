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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;

/**
 * This implementation of a {@link DirectoryWalker} will delete the given
 * {@link File} and its contents if it is a directory. Main difference between a
 * the {@link FileUtils#deleteDirectory(File)} method is that we check for link
 * files to avoid entering them and deleting a directory/file on a different
 * location. We opt to only remove the link instead.
 * 
 * @author Jan Hoskens
 * 
 */
public class FileDeleteWalker extends DirectoryWalker {

	public FileDeleteWalker() {
		super();
	}

	public static List cleanFile(File file) throws IOException {
		return new FileDeleteWalker().clean(file);
	}

	public List clean(File startDirectory) throws IOException {
		List results = new ArrayList();
		walk(startDirectory, results);
		if (startDirectory.exists())
			throw new IOException("Could not delete " + startDirectory.getAbsolutePath());
		return results;
	}

	protected boolean handleDirectory(File directory, int depth, Collection results) {
		if (OsUtils.isLink(directory)) {
			directory.delete();
			results.add(directory);
			return false;
		} else {
			return true;
		}
	}

	@Override
	protected void handleDirectoryEnd(File directory, int depth, Collection results) throws IOException {
		directory.delete();
		results.add(directory);
	}

	protected void handleFile(File file, int depth, Collection results) {
		file.delete();
		results.add(file);
	}

	/**
	 * A simple main method that deletes whatever you pass as argument(s).
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (args.length > 0) {
			File deleteFile;
			FileDeleteWalker fileDeleteWalker = new FileDeleteWalker();
			for (int i = 0; i < args.length; ++i) {
				deleteFile = new File(args[i]);
				Collection results = fileDeleteWalker.clean(deleteFile);
				
				for (Iterator iterator = results.iterator(); iterator.hasNext();) {
					File deletedFile = (File) iterator.next();
					System.out.println("Deleted: " + deletedFile.getAbsolutePath());
				}
			}
		} else {
			System.out.println("Usage: FileDeleteWalker <file*>");
		}
	}
}
