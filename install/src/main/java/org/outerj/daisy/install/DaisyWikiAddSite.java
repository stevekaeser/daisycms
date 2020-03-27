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
package org.outerj.daisy.install;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.variant.LanguageNotFoundException;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.util.VersionHelper;

import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DaisyWikiAddSite {
    private Document navigationDoc;
    private DocumentCollection collection;
    private Document sampleDoc;

    public static void main(String[] args) throws Exception {
        new DaisyWikiAddSite().install(args);
    }

    private void install(String[] args) throws Exception  {
        Options options = new Options();
        Option confOption = (new Option("c", "conf", true, "Configuration file for automated install"));
        confOption.setArgName("conf-file");
        options.addOption(confOption);
        options.addOption(new Option("v", "version", false, "Print version info"));

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("daisy-wiki-add-site <location of wikidata directory>", options, true);
            System.exit(0);
        }

        if (cmd.hasOption('v')) {
            System.out.println(VersionHelper.getVersionString(getClass().getClassLoader(), "org/outerj/daisy/install/versioninfo.properties"));
            System.exit(0);
        }

        InitialisationProperties initSettings;
        if (cmd.hasOption("c")) {
            initSettings = new InitialisationProperties(new File(cmd.getOptionValue("c")));
        } else {

            initSettings = new InitialisationProperties();

            //
            // wiki data directory
            //
            if (cmd.getArgs().length < 1) {
                System.err.println("Please specify the wikidata directory path as argument.");
                System.exit(1);
            }
            initSettings.setWikiDataDir(new File(cmd.getArgs()[0]));

            //
            // Intro
            //
            InstallHelper.printTitle("Daisy Wiki New Site Creation");
            System.out.println();
            System.out.println("This program will create one or more new Daisy Wiki sites. This consists of:");
            System.out.println(" * creating a collection for the documents of the site(s)");
            System.out.println(" * creating one navigation document per site(s)");
            System.out.println(" * creating one (home) page per the site, linked from the navigation");
            System.out.println(" * adding the site definition(s) in the <wikidata dir>/sites directory");
            System.out.println();

            //
            // Repository
            //
            initSettings.setRepository(InstallHelper.promptRepository().getRepository());
            System.out.println("Connecting to the repository.\n");

            InstallHelper.verticalSpacing(3);
            
            InstallHelper.printSubTitle("Choose between single- or multi-language setup.");
            InstallHelper.verticalSpacing(1);
            System.out.println("A single-language setup consists of one collection and one site with the same name");
            InstallHelper.verticalSpacing(1);
            System.out.println("A multi-language setup consists of:");
            System.out.println(" * multiple languages and sites.  The sites will be called <basename>-<language>");
            System.out.println(" * one collection (whose name is equal to the basename of your sites)");
            InstallHelper.verticalSpacing(1);

            initSettings.setMultiLanguageSetup(InstallHelper.promptYesNo("Use multi-language setup? [yes/no, default: no]", false));
            
            if (!initSettings.isMultiLanguageSetup()) {
                //
                // Site Name
                //
                InstallHelper.verticalSpacing(3);
                InstallHelper.printSubTitle("Choose site name");
                InstallHelper.verticalSpacing(1);
                System.out.println("Please specify a name for the site. This name will be used as:");
                System.out.println(" * name for the collection");
                System.out.println(" * directory name for the directory containing the site-specific stuff");
                initSettings.setSiteName(InstallHelper.prompt("Enter a name for the site (without spaces):"));
    
                //
                // Site Language
                //
                InstallHelper.verticalSpacing(3);
                InstallHelper.printSubTitle("Choose site language");
                InstallHelper.verticalSpacing(1);
                System.out.println("You can now select a language for the site.");
                Language[] languages = initSettings.getRepository().getVariantManager().getAllLanguages(false).getArray();
                System.out.println("Currently defined languages are:\n");
                for (Language language : languages) {
                    System.out.println("     " + language.getName());
                }
                System.out.println("\nTo define a new language, simply enter a non-existing name");
                System.out.println("(names must start with a letter (a-z or A-Z) and can then contain");
                System.out.println("letters, digits and the symbols - and _");
                System.out.println();
                System.out.println("It is recommended to use the 2-letter codes from ISO 639. A list of");
                System.out.println("these codes can be found at:");
                System.out.println("http://ftp.ics.uci.edu/pub/ietf/http/related/iso639.txt");
                System.out.println("");
                System.out.println("If unsure, or when creating a mixed language site, just press enter");
                System.out.println("to use the 'default' language.");
                System.out.println("");
                
                initSettings.addSiteLanguage(InstallHelper.prompt("Enter the language [default: default]:", "default"));
            } else {
                //
                // Site base-name (collection name)
                //
                InstallHelper.verticalSpacing(3);
                InstallHelper.printSubTitle("Choose a basename for your sites.");
                InstallHelper.verticalSpacing(1);
                System.out.println("Your site names will be in this format: <basename>-<language>, and the basename ");
                System.out.println("will be used to name the collection");
                initSettings.setSiteName(InstallHelper.prompt("Enter a basename for the sites (without spaces):"));

                //
                // Languages
                //
                InstallHelper.verticalSpacing(3);
                InstallHelper.printSubTitle("Choose languages for your sites");
                InstallHelper.verticalSpacing(1);
                System.out.println("You can now enter one or more languages for your site.");
                Language[] languages = initSettings.getRepository().getVariantManager().getAllLanguages(false).getArray();
                System.out.println("Currently defined languages are:\n");
                for (Language language : languages) {
                    System.out.println("     " + language.getName());
                }
                System.out.println("\nIf you enter a non-existing names, a new language will be defined");
                System.out.println("(names must start with a letter (a-z or A-Z) and can then contain");
                System.out.println("letters, digits and the symbols - and _");
                System.out.println();
                System.out.println("It is recommended to use the 2-letter codes from ISO 639. A list of");
                System.out.println("these codes can be found at:");
                System.out.println("http://ftp.ics.uci.edu/pub/ietf/http/related/iso639.txt");
                System.out.println("");
                System.out.println("If unsure, or when creating a mixed language site, just press enter");
                System.out.println("to use the 'default' language.");
                System.out.println("");
                
                initSettings.addSiteLanguage(InstallHelper.prompt("Enter the first language [default: default]:", "default"));
                
                String language;
                while (!"".equals(language = InstallHelper.prompt("Enter another language, or just hit enter to finish:", ""))) {
                    initSettings.addSiteLanguage(language);
                }
                
                InstallHelper.verticalSpacing(3);
                InstallHelper.printSubTitle("Activate translation management ?");
                InstallHelper.verticalSpacing(1);
                System.out.println("Daisy comes with translation management features, which help you");
                System.out.println("keep translated documents in sync with a reference language.");
                
                if (InstallHelper.promptYesNo("Do you want to use translation management? [default: yes]", true)) {
                    System.out.println("With translation management, each document has a 'reference language'");
                    System.out.println("In most cases, this language is the same for all documents.");
                    System.out.println("If this is the case, it is useful to specify this language as the default");
                    System.out.println("reference language - this means that new documents will be initialised with");
                    System.out.println("this language as the reference language.");
                    
                    if (initSettings.getSiteLanguages().size() == 1) {
                        System.out.println("Since you only listed one language, this will be used as the default reference language.");
                        initSettings.setDefaultReferenceLanguage(initSettings.getSiteLanguages().get(0));
                    } else {
                        initSettings.setDefaultReferenceLanguage(InstallHelper.promptForListItem("These are the available languages:", "Which language is the default reference language (%s)?", true, initSettings.getSiteLanguages()));
                    }
                }
            }
        }

        InstallHelper.verticalSpacing(3);
        InstallHelper.printSubTitle("Setting up the site");
        InstallHelper.verticalSpacing(1);

        createCollection(initSettings);
        createInitialDocuments(initSettings);
        generateSiteConfs(initSettings);

        System.out.println();
        System.out.println("Finished.");

    }

    private void createCollection(InitialisationProperties settings) throws RepositoryException {
        System.out.println("Creating collection " + settings.getSiteName());
        CollectionManager collectionManager = settings.getRepository().getCollectionManager();
        boolean collectionExists = false;
        try {
            collection = collectionManager.getCollectionByName(settings.siteName, false);
            collectionExists = true;
            InstallHelper.verticalSpacing(1);
            System.out.println("*** Collection warning (harmless) ***");
            System.out.println("A collection with the name " + settings.siteName + " already exists, it will be re-used.");
            InstallHelper.verticalSpacing(1);
        } catch (CollectionNotFoundException e) {
            /* ignore */
        }

        if (!collectionExists) {
            collection = settings.getRepository().getCollectionManager().createCollection(settings.getSiteName());
            collection.save();
            System.out.println("Collection created.");
        }
    }

    private void createInitialDocuments(InitialisationProperties settings) throws UnsupportedEncodingException, RepositoryException {
        String startLanguage;
        if (settings.getDefaultReferenceLanguage() != null) {
            startLanguage = settings.getDefaultReferenceLanguage();
        } else {
            startLanguage = settings.getSiteLanguages().get(0);
        }

        createSampleDoc(settings, startLanguage);
        createNavigationDoc(settings, startLanguage);
        updateNavigationDocument(navigationDoc, settings);
        
        for (String siteLanguage: settings.getSiteLanguages()) {
            if (siteLanguage.equals(startLanguage))
                continue;
            System.out.println("Creating variant for sample document. ("+siteLanguage+")");
            settings.getRepository().createVariant(sampleDoc.getId(), Branch.MAIN_BRANCH_NAME, startLanguage, -1, Branch.MAIN_BRANCH_NAME, siteLanguage, true);
            System.out.println("Creating variant for navigation document. ("+siteLanguage+")");
            settings.getRepository().createVariant(navigationDoc.getId(), Branch.MAIN_BRANCH_NAME, startLanguage, -1, Branch.MAIN_BRANCH_NAME, siteLanguage, true);
        }
    }

    private void updateNavigationDocument(Document document, InitialisationProperties settings)
            throws DocumentTypeInconsistencyException,
            UnsupportedEncodingException, RepositoryException {
        //
        // Update navigation to link to sample doc
        //
        System.out.println("Updating navigation document.");
        StringBuilder navigation = new StringBuilder();
        navigation.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>\n");
        navigation.append("  <d:collections>\n");
        navigation.append("    <d:collection name='").append(settings.getSiteName()).append("'/>\n");
        navigation.append("  </d:collections>\n");
        navigation.append("  <d:doc id='").append(sampleDoc.getId()).append("' label='").append(sampleDoc.getName()).append("'/>\n");
        navigation.append("  <d:group label='All documents A->Z'>\n");
        navigation.append("    <d:query q='select name where true order by name'/>\n");
        navigation.append("  </d:group>\n");
        navigation.append("</d:navigationTree>");
        document.setPart("NavigationDescription", "text/xml", navigation.toString().getBytes("UTF-8"));
        document.save();
        System.out.println("Navigation document updated.");
    }
    
    private void createNavigationDoc(InitialisationProperties settings, String siteLanguage)
            throws DocumentTypeInconsistencyException,
            UnsupportedEncodingException, RepositoryException {
        System.out.println("Creating navigation document. (" + siteLanguage + ")");
        navigationDoc = settings.getRepository().createDocument("Navigation for " + settings.getSiteName(), "Navigation", Branch.MAIN_BRANCH_NAME, siteLanguage);
        String emptyNavigation = "<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'/>";
        navigationDoc.setPart("NavigationDescription", "text/xml", emptyNavigation.getBytes("UTF-8"));
        navigationDoc.addToCollection(collection);
        if (settings.getDefaultReferenceLanguage() != null) {
            navigationDoc.setReferenceLanguageId(settings.getRepository().getVariantManager().getLanguage(settings.getDefaultReferenceLanguage(), false).getId());
        }
        navigationDoc.save();
        System.out.println("Navigation document created, id = " + navigationDoc.getId());
    }

    private void createSampleDoc(InitialisationProperties settings,
            String siteLanguage) throws DocumentTypeInconsistencyException,
            UnsupportedEncodingException, RepositoryException {
        System.out.println("Creating sample document. (" + siteLanguage + ")");
        sampleDoc = settings.getRepository().createDocument(settings.getSiteName() + " home", "SimpleDocument", Branch.MAIN_BRANCH_NAME, siteLanguage);
        String content = "<html><body><h1>Welcome!</h1><p>Hello, welcome to your new site.</p></body></html>";
        sampleDoc.setPart("SimpleDocumentContent", "text/xml", content.getBytes("UTF-8"));
        sampleDoc.addToCollection(collection);
        if (settings.getDefaultReferenceLanguage() != null) {
            sampleDoc.setReferenceLanguageId(settings.getRepository().getVariantManager().getLanguage(settings.getDefaultReferenceLanguage(), false).getId());
        }
        sampleDoc.save();
        System.out.println("Sample document created, id = " + sampleDoc.getId());
    }

    private void generateSiteConfs(InitialisationProperties settings) throws Exception {
        for (String siteLanguage: settings.getSiteLanguages()) {
            StringBuilder siteConfBuffer = new StringBuilder();
            siteConfBuffer.append("<siteconf xmlns=\"http://outerx.org/daisy/1.0#siteconf\">");
            StringBuffer siteTitle = new StringBuffer(settings.getSiteName());
            if (settings.isMultiLanguageSetup()) {
                siteTitle.append(" (").append(siteLanguage).append(")");
            }
            siteConfBuffer.append("\n  <title>").append(siteTitle).append("</title>");
            siteConfBuffer.append("\n  <description>The \"").append(siteTitle).append("\" site</description>");
            siteConfBuffer.append("\n  <skin>default</skin>");
            siteConfBuffer.append("\n  <navigationDocId>").append(navigationDoc.getId()).append("</navigationDocId>");
            siteConfBuffer.append("\n  <homepageDocId>").append(sampleDoc.getId()).append("</homepageDocId>");
            siteConfBuffer.append("\n  <collectionId>").append(collection.getId()).append("</collectionId>");
            siteConfBuffer.append("\n  <contextualizedTree>false</contextualizedTree>");
            siteConfBuffer.append("\n  <branch>").append(Branch.MAIN_BRANCH_NAME).append("</branch>");
            siteConfBuffer.append("\n  <language>").append(siteLanguage).append("</language>");
            if (settings.getDefaultReferenceLanguage() != null) {
                siteConfBuffer.append("\n  <defaultReferenceLanguage>").append(settings.getDefaultReferenceLanguage()).append("</defaultReferenceLanguage>");
            }
            siteConfBuffer.append("\n  <defaultDocumentType>SimpleDocument</defaultDocumentType>");
            siteConfBuffer.append("\n  <newVersionStateDefault>publish</newVersionStateDefault>");
            siteConfBuffer.append("\n  <locking>");
            siteConfBuffer.append("\n    <automatic lockType='pessimistic' defaultTime='15' autoExtend='true'/>");
            siteConfBuffer.append("\n  </locking>");
            siteConfBuffer.append("\n</siteconf>");
            String siteConf = siteConfBuffer.toString();

            File newDir;
            if (settings.isMultiLanguageSetup()) {
                newDir = new File(new File(settings.getWikiDataDir(), "sites"), settings.getSiteName() + "-" + siteLanguage);
            } else {
                newDir = new File(new File(settings.getWikiDataDir(), "sites"), settings.getSiteName());
            }
            newDir.mkdir();
            File siteConfFile = new File(newDir, "siteconf.xml");
            if (siteConfFile.exists()) {
                InstallHelper.backupFile(siteConfFile);
            }
    
            OutputStream os = new FileOutputStream(siteConfFile);
            try {
                OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
                writer.write(siteConf);
                writer.flush();
            } finally {
                os.close();
            }
            System.out.println("Written " + siteConfFile.getAbsolutePath());
    
            File cocoonExtDir = new File(newDir, "cocoon");
            cocoonExtDir.mkdir();
            File sitemap = new File(cocoonExtDir, "sitemap.xmap");
            os = new FileOutputStream(sitemap);
            try {
                OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
                writer.write(readResource("org/outerj/daisy/install/sample-extension-sitemap.xml"));
                writer.flush();
            } finally {
                os.close();
            }
        }
    }

    String readResource(String name) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(name);
        Reader reader = new InputStreamReader(is, "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(reader);

        StringBuilder buffer = new StringBuilder();
        int c = bufferedReader.read();
        while (c != -1) {
            buffer.append((char) c);
            c = bufferedReader.read();
        }

        return buffer.toString();
    }

    private static class InitialisationProperties {
        public static final String DAISYWIKI_DATADIR = "wikiDataDir";
        public static final String DAISY_URL = "daisyUrl";
        public static final String DAISY_LOGIN = "daisyLogin";
        public static final String DAISY_PASSWORD = "daisyPassword";
        public static final String SITE_NAME = "siteName";
        public static final String SITE_LANGUAGE = "siteLanguage";
        public static final String DEFAULT_REFERENCE_LANGUAGE = "defaultReferenceLanguage";

        private String daisyUrl;
        private Repository repository;
        private Credentials credentials;
        private String daisyLogin;
        private String daisyPassword;
        private File wikiDataDir;
        private String siteName;
        private List<String> siteLanguages = new ArrayList<String>();
        private String defaultReferenceLanguage;
        private boolean multiLanguageSetup;

        public InitialisationProperties() {
        }

        public InitialisationProperties(File configProperties) throws PropertyNotFoundException, Exception {
            FileInputStream is = null;
            try {
                is = new FileInputStream(configProperties);
                Properties props = new Properties();
                props.load(is);

                Credentials credentials = new Credentials(InstallHelper.getPropertyValue(props, InitialisationProperties.DAISY_LOGIN), InstallHelper
                        .getPropertyValue(props, InitialisationProperties.DAISY_PASSWORD));
                System.out.println("\nConnecting to the repository.");
                RepositoryManager repositoryManager = new RemoteRepositoryManager(InstallHelper.getPropertyValue(props, InitialisationProperties.DAISY_URL),
                        credentials);
                repository = repositoryManager.getRepository(credentials);
                repository.switchRole(Role.ADMINISTRATOR);
                setWikiDataDir(new File(InstallHelper.getPropertyValue(props,InitialisationProperties.DAISYWIKI_DATADIR)));
                siteName = InstallHelper.getPropertyValue(props,InitialisationProperties.SITE_NAME);

                // first get all the indices from siteLanguage.<index>
                Pattern siteLanguagePattern = Pattern.compile("^"+InitialisationProperties.SITE_LANGUAGE+"\\.(\\d)*$");
                List<Integer> languageIndices = new ArrayList<Integer>();
                for (Object oPropname : props.keySet()) {
                    String propname = (String)oPropname;
                    Matcher matcher = siteLanguagePattern.matcher(propname);
                    if (matcher.matches()) {
                        languageIndices.add(NumberFormat.getIntegerInstance().parse(matcher.group(1)).intValue());
                        multiLanguageSetup = true;
                    }
                }
                
                if (multiLanguageSetup) {
                    // if there are siteLanguage.<index> properties, add languages in the order indicated by <index>
                    Integer[] languageIndicesAry = languageIndices.toArray(new Integer[languageIndices.size()]);
                    Arrays.sort(languageIndicesAry);
                    for (Integer languageIndex: languageIndicesAry) {
                        addSiteLanguage(InstallHelper.getPropertyValue(props, "siteLanguage." + languageIndex));
                    }

                    setDefaultReferenceLanguage(props.getProperty(InitialisationProperties.DEFAULT_REFERENCE_LANGUAGE));
                } else {
                    addSiteLanguage(InstallHelper.getPropertyValue(props,InitialisationProperties.SITE_LANGUAGE, "default"));    
                }

                

            } finally {
                if (is != null)
                    is.close();
                else
                    System.out.println("Could not open " + configProperties.getAbsolutePath() + " exiting ...");
            }
        }

        public Credentials getCredentials() {
            return credentials;
        }

        public void setCredentials(Credentials credentials) {
            this.credentials = credentials;
        }

        public String getDaisyLogin() {
            return daisyLogin;
        }

        public void setDaisyLogin(String daisyLogin) {
            this.daisyLogin = daisyLogin;
        }

        public String getDaisyPassword() {
            return daisyPassword;
        }

        public void setDaisyPassword(String daisyPassword) {
            this.daisyPassword = daisyPassword;
        }

        public String getDaisyUrl() {
            return daisyUrl;
        }

        public void setDaisyUrl(String daisyUrl) {
            this.daisyUrl = daisyUrl;
        }

        public Repository getRepository() {
            return repository;
        }

        public void setRepository(Repository repository) {
            this.repository = repository;
        }

        public File getWikiDataDir() {
            return wikiDataDir;
        }

        public void setWikiDataDir(File wikiDataDir) throws Exception {
            if (!wikiDataDir.exists()) {
                throw new Exception("Could not find the wikidata directory at : " + wikiDataDir.getAbsolutePath());
            }
            this.wikiDataDir = wikiDataDir;

            // check whether sites directory exists and is writable
            File sitesDir = new File(wikiDataDir, "sites");
            if (!sitesDir.exists() || !sitesDir.isDirectory()) {
                throw new Exception("Non-existing directory or not a directory: " + sitesDir.getAbsolutePath());
            }
            if (!sitesDir.canWrite())
                throw new Exception("The directory "+ sitesDir.getAbsolutePath() + " is not writable. Please log in with appropriate access rights.");
        }

        public List<String> getSiteLanguages() {
            return siteLanguages;
        }

        public void addSiteLanguage(String siteLanguage) throws RepositoryException {
            ensureLanguageExists(siteLanguage);
            if ( !siteLanguages.contains(siteLanguage) ) {
                this.siteLanguages.add(siteLanguage);
            }
        }

        private void ensureLanguageExists(String siteLanguage)
                throws RepositoryException {
            try {
               // fetch site language to check its existance
               repository.getVariantManager().getLanguageByName(siteLanguage, false).getName();
            } catch (LanguageNotFoundException e) {
                Language language = repository.getVariantManager().createLanguage(siteLanguage);
                language.save();
            }
        }

        public String getSiteName() {
            return siteName;
        }

        public void setSiteName(String siteName) {
            this.siteName = siteName;
        }

        public boolean isMultiLanguageSetup() {
            return multiLanguageSetup;
        }
        
        public void setMultiLanguageSetup(boolean multiLanguageSetup) {
            this.multiLanguageSetup = multiLanguageSetup;
        }

        public String getDefaultReferenceLanguage() {
            return defaultReferenceLanguage;
        }

        public void setDefaultReferenceLanguage(String defaultReferenceLanguage) throws RepositoryException {
            if (defaultReferenceLanguage != null) {
                ensureLanguageExists(defaultReferenceLanguage);
            }
            this.defaultReferenceLanguage = defaultReferenceLanguage;
        }
        
    }
}