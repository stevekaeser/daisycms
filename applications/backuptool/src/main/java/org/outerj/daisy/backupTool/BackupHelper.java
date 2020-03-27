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
package org.outerj.daisy.backupTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.binary.Base64;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class BackupHelper {
    private static int BUFFER_SIZE = 1024;

    public static Document parseFile(File file) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(file);
    }

    public static void streamCopy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = is.read(buffer)) > 0) {
            os.write(buffer, 0, len);
        }
    }

    public static void copyFile(File source, File destination) throws Exception {
        if (source.isDirectory()) {
            destination.mkdirs();
            File[] files = source.listFiles();
            for (int i = 0; i < files.length; i++) {
                copyFile(files[i], new File(destination, files[i].getName()));
            }
        } else {
            destination.getParentFile().mkdirs();
            destination.createNewFile();
            copyFileFile(source, destination);
        }
    }

    private static void copyFileFile(File source, File destination) throws Exception {
        FileChannel srcChannel = new FileInputStream(source).getChannel();
        FileChannel dstChannel = new FileOutputStream(destination).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
    }

    public static void deleteFile(File file) throws Exception {
        deleteFile(file, false);
    }

    public static void deleteFile(File file, boolean quiet) throws Exception {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++)
                deleteFile(files[i], quiet);
        }

        boolean success = file.delete();
        if (!success && !quiet)
            throw new Exception("Could not delete file " + file.getAbsolutePath());
    }

    public static class RunnableStreamCopy implements Runnable {
        private InputStream is;

        private OutputStream os;

        public RunnableStreamCopy(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
        }

        public void run() {
            try {
                streamCopy(is, os);
                os.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public static void unzipToDirectory(File zippedFile, File file) throws Exception{
        file.mkdirs();
        unzipToFile(zippedFile, file);
    }

    public static void unzipToFile(File zippedFile, File file) throws Exception {
        if (!zippedFile.exists())
            throw new FileNotFoundException("The zipfile " + zippedFile.getPath() + " could not be found");
        ZipFile zipFile = new ZipFile(zippedFile);
        Enumeration entries = zipFile.entries();
        File baseDir = null;
        if (file.isDirectory()) {
            baseDir = file;
        } else {
            file.createNewFile();
            baseDir = file.getParentFile();
        }
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            BackupHelper.unzipToFile(zipFile, entry, baseDir);
        }
    }

    private static void unzipToFile(ZipFile zipFile, ZipEntry entry, File basedir) throws Exception {
        File entryFile = new File(basedir, entry.getName());
        if (entry.isDirectory()) {
            entryFile.mkdir();
        } else {
            InputStream is = zipFile.getInputStream(entry);
            OutputStream os = new FileOutputStream(entryFile);
            try {
                streamCopy(is, os);
            } finally {
                is.close();
                os.close();
            }
        }
    }

    public static String prompt(String message) throws Exception {
        System.out.println(message);
        System.out.flush();
        String input = null;
        while (input == null || input.trim().equals("")) {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            try {
                input = in.readLine();
            } catch (IOException e) {
                throw new Exception("Error reading input from console.", e);
            }
        }
        return input;
    }

    public static String prompt(String message, String defaultInput) throws Exception {
        System.out.println(message);
        System.out.flush();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String input;
        try {
            input = in.readLine();
        } catch (IOException e) {
            throw new Exception("Error reading input from console.", e);
        }
        if (input == null || input.trim().equals(""))
            input = defaultInput;
        return input;
    }

    public static boolean promptYesNo(String message, boolean defaultInput) throws Exception {
        String input = "";
        while (!input.equals("yes") && !input.equals("no")) {
            input = prompt(message, defaultInput ? "yes" : "no");
            input = input.toLowerCase();
        }
        return input.equals("yes");
    }

    public static void saveDocument(File file, Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(file);
        transformer.setOutputProperty("encoding", "UTF-8");
        transformer.setOutputProperty("indent", "yes");
        transformer.transform(source, result);
    }

    public static String generateMD5Hash(File file) throws Exception {
        byte[] buffer = new byte[1024];
        int len;
        InputStream fis = new FileInputStream(file);
        MessageDigest hash = MessageDigest.getInstance("MD5");

        try {
            while ((len = fis.read(buffer)) > 0)
                hash.update(buffer, 0, len);
        } finally {
            fis.close();
        }

        return new String(Base64.encodeBase64(hash.digest()));
    }

    public static Element getElementFromDom(Node domNode, String xpath) throws Exception {
        return getElementFromDomNS(domNode, xpath, null, null);
    }

    public static Element getElementFromDom(Node domNode, String xpath, boolean fail) throws Exception {
        return getElementFromDomNS(domNode, xpath, null, null, fail);
    }

    public static Element getElementFromDomNS(Node domNode, String xpath, String prefix, String namespace, boolean fail) throws Exception {
        DOMXPath path = new DOMXPath(xpath);
        if (namespace != null)
            path.addNamespace(prefix, namespace);

        Element element = (Element) path.selectSingleNode(domNode);
        if (element == null && fail) {
            throw new Exception("Node with XPath '" + xpath + "' could not be found.");
        }
        return element;
    }

    public static Element getElementFromDomNS(Node domNode, String xpath, String prefix, String namespace) throws Exception {
        return getElementFromDomNS(domNode, xpath, prefix, namespace, true);
    }

    public static String getStringFromDom(Node domNode, String xpath) throws Exception {
        return getStringFromDomNS(domNode, xpath, null, null);
    }

    public static String getStringFromDomNS(Node domNode, String xpath, String prefix, String namespace, String defaultValue) throws Exception {
        DOMXPath path = new DOMXPath(xpath);
        if (namespace != null)
            path.addNamespace(prefix, namespace);

        String value = path.stringValueOf(domNode);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public static String getStringFromDomNS(Node domNode, String xpath, String prefix, String namespace) throws Exception {
        DOMXPath path = new DOMXPath(xpath);
        if (namespace != null)
            path.addNamespace(prefix, namespace);

        String value = path.stringValueOf(domNode);
        if (value == null) {
            throw new Exception("Node with XPath '" + xpath + "' could not be found.");
        }
        return value;
    }
}
