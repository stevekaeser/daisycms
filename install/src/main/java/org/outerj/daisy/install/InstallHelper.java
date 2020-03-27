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
package org.outerj.daisy.install;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.repository.AuthenticationFailedException;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.util.CliUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class InstallHelper {
    private static final Log log = LogFactory.getLog(InstallHelper.class);
    
    private static SecureRandom secureRandom;
    static {
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static DatabaseParams collectDatabaseParams(DatabaseInfo dbInfo, String user, String password, String dbName) throws Exception {
        String dbUrl = dbInfo.getDriverUrl(dbName);
        dbUrl = InstallHelper.prompt("Enter database URL [default = " + dbUrl + "] : ", dbUrl);
        String dbUser = InstallHelper.prompt("Enter database user [default = " + user + "] : ", user);
        String dbPassword = CliUtil.promptPassword("Enter database password [default = " + password + "] : ", password);
        System.out.println();

        String dbDriverClassName = dbInfo.getDriverClass();
        dbDriverClassName = InstallHelper.prompt("Enter database driver class [default = " + dbDriverClassName + "] : ", dbDriverClassName);

        String dbDriverClasspath = getRepoLocation() + "/" + dbInfo.getDriverPath();
        dbDriverClasspath = InstallHelper.prompt("Enter database driver jar location [default = " + dbDriverClasspath + "] : ", dbDriverClasspath);

        DatabaseParams dbParams = new DatabaseParams(dbName, dbUrl, dbUser, dbPassword, dbDriverClassName, dbDriverClasspath,
                dbInfo.getHibernateDialect(), dbInfo.getValidator());
        System.out.println("Registering driver...");
        dbParams.loadDriver();
        System.out.println("Successful.");

        return dbParams;
    }

    public static String getRepoLocation() {
        String daisyHome = System.getProperty("daisy.home");
        String repoLocation;
        if (daisyHome != null) {
            repoLocation = "${daisy.home}" + File.separator + "lib";
        } else {
            repoLocation = getMavenRepoLocation();
        }
        return repoLocation;
    }

    public static String getMavenRepoLocation() {
        File mavenPropFile = new File(System.getProperty("user.home"), "build.properties");
        if (mavenPropFile.exists()) {
            Properties properties = new Properties();
            InputStream is = null;
            try {
                is = new FileInputStream(mavenPropFile);
                properties.load(is);
            } catch (Exception e) {
                throw new RuntimeException("Error reading " + mavenPropFile.getAbsolutePath(), e);
            } finally {
                if (is != null)
                    try { is.close(); } catch (Exception e) { /* ignore */ }
            }
            if (properties.containsKey("maven.home.local")) {
                return properties.getProperty("maven.home.local") + "/repository";
            }
        }

        return System.getProperty("user.home") + "/.m2/repository";
    }

    public static File getDaisyHome() {
        String daisyHomeProp = System.getProperty("daisy.home");

        if (daisyHomeProp == null)
            throw new RuntimeException("System property daisy.home missing.");

        File daisyHome = new File(daisyHomeProp);
        if (!daisyHome.exists() || !daisyHome.isDirectory())
            throw new RuntimeException("daisy.home does not point to an existing directory");

        return daisyHome;
    }

    public static File getDaisySourceHome() {
        String daisySourceHomeProp = System.getProperty("daisy.sourcehome");

        if (daisySourceHomeProp == null)
            throw new RuntimeException("System property daisy.sourcehome missing.");

        File daisySourceHome = new File(daisySourceHomeProp);
        if (!daisySourceHome.exists() || !daisySourceHome.isDirectory())
            throw new RuntimeException("daisy.sourcehome does not point to an existing directory");

        return daisySourceHome;
    }

    public static boolean isDevelopmentSetup() {
        return System.getProperty("daisy.home") == null;
    }

    public static boolean isDistroDirectory(File daisyHome) {
        // if there is a directory called 'repository-server', assume we're in the distro home
        File file = new File(daisyHome, "repository-server");
        return file.exists();
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

    public static String prompt(String message, String defaultInput) {
        System.out.println(message);
        System.out.flush();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String input;
        try {
            input = in.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Error reading input from console.", e);
        }
        if (input == null || input.trim().equals(""))
            input = defaultInput;
        return input;
    }

    public static boolean promptYesNo(String message, boolean defaultInput) {
        String input = "";
        while (!input.equals("yes") && !input.equals("no") && !input.equals("y") && !input.equals("n")) {
            input = prompt(message, defaultInput ? "yes" : "no");
            input = input.toLowerCase();
        }
        return (input.equals("yes") || input.equals("y"));
    }

    public static void waitPrompt() throws Exception {
        promptYesNo("Press enter to continue.", true);
    }

    public static void printTitle(String title) {
        int width = 74;

        if (title.length() > width)
            title = title.substring(0, width - 4) + "...";

        int spaceBefore = (int)Math.floor(((double)(width - title.length())) / 2d);
        int spaceAfter = width - title.length() - spaceBefore;

        System.out.println("  +" + repeat('-', width) + "+");
        System.out.println("  |" + repeat(' ', spaceBefore) + title + repeat(' ', spaceAfter) + "|");
        System.out.println("  +" + repeat('-', width) + "+");
    }

    public static String repeat(char c, int times) {
        StringBuilder builder = new StringBuilder(times);
        for (int i = 0; i < times; i++)
            builder.append(c);
        return builder.toString();
    }

    public static void printSubTitle(String title) {
        int width = 80;
        if (title.length() > width)
            title = title.substring(0, width - 4) + "...";

        int spaceBefore = (int)Math.floor(((double)(width - title.length())) / 2d);
        int spaceAfter = width - title.length() - spaceBefore;

        System.out.println(repeat(' ', spaceBefore) + title + repeat(' ', spaceAfter));
        System.out.println(repeat(' ', spaceBefore) + repeat('=', title.length()));
    }

    public static void verticalSpacing(int amount) {
        for (int i = 0; i < amount; i++)
            System.out.println();
    }

    public static String generatePassword() throws Exception {
        byte[] bytes = new byte[15];
        secureRandom.nextBytes(bytes);
        return toHexString(bytes);
    }

    public static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(hexChar[(b[i] & 0xf0) >>> 4]);
            sb.append(hexChar[b[i] & 0x0f]);
        }
        return sb.toString();
    }

    static char[] hexChar = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static Properties loadDistroProperties(File daisyHome) throws Exception {
        Properties properties = new Properties();
        File config = new File(daisyHome, "config.properties");
        if (config.exists()) {
            properties.load(new FileInputStream(config));
        }
        return properties;
    }

    public static void storeDistroProperties(Properties properties, File daisyHome) throws Exception {
        File config = new File(daisyHome, "config.properties");
        properties.store(new FileOutputStream(config), null);
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

    public static void setExecutable(File file) throws Exception {
        // rough check to see if it's a unix-type system
        if (System.getProperty("path.separator").equals(":")) {
            Process process = Runtime.getRuntime().exec(new String[] {"chmod", "u+x", file.getAbsolutePath()});
            process.waitFor();
            int exitValue = process.exitValue();
            if (exitValue != 0) {
                verticalSpacing(2);
                InstallHelper.printSubTitle("Warning");
                System.out.println("Could not make the following file executable, you might want to do it yourself:");
                System.out.println(file.getAbsolutePath());
                verticalSpacing(2);
            }
        }
    }

    public static void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++)
                deleteFile(files[i]);
        }
        file.delete();
    }

    public static void copyStream(InputStream source, File destination) throws Exception {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(destination);
            byte[] buffer = new byte[32768];
            int read;
            while ((read = source.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        } finally {
            if (source != null)
                source.close();
            if (os != null)
                os.close();
        }
    }

    public static class RepositoryAccess {
        public RepositoryManager repositoryManager;
        public Repository repository;

        public RepositoryAccess(RepositoryManager repositoryManager, Repository repository) {
            this.repositoryManager = repositoryManager;
            this.repository = repository;
        }

        public RepositoryManager getRepositoryManager() {
            return repositoryManager;
        }

        public Repository getRepository() {
            return repository;
        }
    }

    public static RepositoryAccess promptRepository() throws Exception {
        System.out.println(" == Login To Daisy Repository Server ==");
        String url = InstallHelper.prompt("Address where the Daisy Repository Server is listening [default = http://localhost:9263] :", "http://localhost:9263");
        System.out.println("Enter login (user) and password for Daisy (this should be a user with the Administrator role):");

        RepositoryManager repositoryManager = null;
        Repository repository = null;

        while (repository == null) {
            String login = InstallHelper.prompt("Enter login: ");
            String pwd = CliUtil.promptPassword("Enter password: ", true);
            System.out.println();
            Credentials credentials = new Credentials(login, pwd);

            try {
                repositoryManager = new RemoteRepositoryManager(url, credentials);
                repository = repositoryManager.getRepository(credentials);
            } catch (AuthenticationFailedException e) {
                System.out.println("");
                System.out.println("Login failed, try again.");
                System.out.println("");
            }
        }

        repository.switchRole(Role.ADMINISTRATOR);
        return new RepositoryAccess(repositoryManager, repository);
    }

    public static File promptForEmptyDir(String message, String defaultPath) throws Exception {
        File dir;
        while (true) {
            // defaultPath is optional
            String dirInput = defaultPath == null ? InstallHelper.prompt(message) : InstallHelper.prompt(message + " [ default = " + defaultPath + "]", defaultPath);
            dir = new File(dirInput);
            if (dir.exists() && !dir.isDirectory()) {
                System.out.println("\nThe specified path exists and is not a directory.");
                continue;
            } else if (dir.exists() && dir.list().length > 0) {
                System.out.println("\nThe specified directory exists and is not empty.");
                continue;
            }
            break;
        }
        return dir;
    }

    public static void backupFile(File file) throws Exception {
        File backupFile = new File(file.getAbsolutePath() + ".backup");
        InstallHelper.copyFile(file, backupFile);
        System.out.println("Made backup file " + backupFile.getAbsolutePath());
    }

    public static Document parseFile(File file) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(file);
    }

    public static void saveDocument(File file, Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(file);
        transformer.setOutputProperty("encoding", "UTF-8");
        transformer.transform(source, result);
    }

    public static void setElementValue(Element element, String value) {
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++)
            element.removeChild(nodes.item(i));

        element.appendChild(element.getOwnerDocument().createTextNode(value));
    }

    public static void appendToMatchingLinesInFile(File fileName, String match, String appendText) throws IOException {
        BufferedReader input = null;
        StringBuffer contents;
        try {   
            input = new BufferedReader(new FileReader(fileName));
            String line;
            contents = new StringBuffer();
            while ((line = input.readLine()) != null) {
                contents.append(line.replaceFirst( "^(" + match + ")\\s*$", "$1" + appendText.replaceAll("\\\\", "/")));
                contents.append(System.getProperty("line.separator"));
            }
        }
        finally {
            if (input!= null) {
                input.close();
            }
        }
        Writer output = null;
        try {
            output = new BufferedWriter( new FileWriter(fileName) );
            output.write( contents.toString() );
        }
        finally {
            if (output != null) {
                output.close();
            }
        }
    }

    /**
     * Gets the property value for a given key
     * @param props Object containing the properties
     * @param key Key for which a value should be returned
     * @return Value of the property
     * @throws PropertyNotFoundException Thrown if the property is not found;
     */
    public static String getPropertyValue (Properties props, String key) throws PropertyNotFoundException {
        if ((props.containsKey(key))&&(props.getProperty(key)!=null)){
            return props.getProperty(key);
        }else {
            throw new PropertyNotFoundException(key);
        }
    }

    /**
     * Gets the property value for a given key.  If the key is not found the default value will be returned.
     * @param props Object containing the properties
     * @param key Key for which a value should be returned
     * @param defaultValue Value returned if the value for the key is not found
     * @return Value of the property
     */
    public static String getPropertyValue (Properties props, String key, String defaultValue) {
        String value = defaultValue;
        try {
            value = getPropertyValue(props, key);
        } catch (PropertyNotFoundException e) {
            System.out.println("Value for property " + key + " not found.  Falling back to a default value.");
        }
        return value;
    }

    public static String promptForListItem(String topMessage, String bottomMessage, boolean required, List<String> choices) {
        System.out.println(topMessage);
        for (int i = 0; i < choices.size(); i++) {
            System.out.printf("%5d:  %s\n", i + 1, choices.get(i));
        }

        while (true) {
            String answer = InstallHelper.prompt(String.format(bottomMessage, "[1 - " + choices.size() + "]"),"");
            if  (!required && "".equals(answer)) {
                return null;
            }
            try {
                int choice = Integer.parseInt(answer);
                if (choice > 0 && choice <= choices.size()) {
                    return choices.get(choice - 1);
                }
            } catch (NumberFormatException e) {
            }
        }
    }

    /**
     * Checks if the given path either exists or is an empty directory.
     * If not, adds a message to the list or problems and returns false
     * @param path
     * @param problems
     * @return
     */
    public static boolean verifyEmptyDirectory(String path, List<String> problems) {
        File dir = new File(path);
        if (dir.exists() && !dir.isDirectory()) {
            problems.add(dir + " exists and is not a directory.");
            return false;
        } else if (dir.exists() && dir.list().length > 0) {
            problems.add(dir + " exists and is not empty.");
            return false;
        }
        return true;
    }

}
