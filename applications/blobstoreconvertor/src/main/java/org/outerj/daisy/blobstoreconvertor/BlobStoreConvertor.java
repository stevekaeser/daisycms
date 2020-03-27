/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.blobstoreconvertor;

import java.io.File;
import java.io.FileFilter;

public class BlobStoreConvertor {
    private File blobDir;

    private static final int DIRECTORY_DEPTH = 4;

    private static final int DIRECTORY_NAME_LENGTH = 2;

    private static final int KEYLENGTH = 20;

    public static void main(String[] args) {
        if (args.length == 1) {
            try {
                new BlobStoreConvertor(args[0]).flatToHierarchical();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    public BlobStoreConvertor(String blobStorePath) throws Exception {
        blobDir = new File(blobStorePath);
        if (!blobDir.exists() || !blobDir.isDirectory())
            throw new Exception(blobStorePath + " does not exist or is not a directory");
    }

    public void flatToHierarchical() throws Exception {
        File[] files = blobDir.listFiles(new FlatBlobFilter());
        for (int i = 0; i < files.length; i++) {
            String id = files[i].getName();
            System.out.println("Processing " + id);
            String subdirName = "";
            int position = 0;
            for (int j = 0; j < DIRECTORY_DEPTH; j++)
                subdirName += id.substring(position, position += DIRECTORY_NAME_LENGTH) + File.separator;

            File subdir = new File(blobDir, subdirName);
            subdir.mkdirs();

            File newFile = new File(subdir, id.substring(position));
            if (!files[i].renameTo(newFile)) {
                throw new Exception("Could not rename file " + files[i].getName());
            }
        }
        System.out.println("Finished successfully.");
    }

    private class FlatBlobFilter implements FileFilter {
        public boolean accept(File pathname) {
            return pathname.isFile() && pathname.getName().length() == KEYLENGTH * 2;
        }
    }
}
