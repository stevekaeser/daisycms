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
package org.outerj.daisy.tools.docidconvertor;

import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.util.Constants;
import org.outerj.daisy.util.CliUtil;
import org.outerj.daisy.htmlcleaner.HtmlCleanerTemplate;
import org.outerj.daisy.htmlcleaner.HtmlCleanerFactory;
import org.outerj.daisy.htmlcleaner.HtmlCleaner;
import org.xml.sax.InputSource;
import org.apache.commons.cli.*;

import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This tool updates non-namespaces document references to ones with namespaces.
 *
 * Problems to think about:
 *   - is there a possibility to disable email notifications?
 */
public class DocIdConvertor {
    private String repositoryURL;
    private String repoUser;
    private String repoPwd;
    private String repositoryNamespace;
    private Repository repository;
    private String htmlCleanerConfig;
    private HtmlCleanerTemplate htmlCleanerTemplate;

    public static void main(String[] args) throws Exception {
        Options options = new Options();

        Option repoURLOption = new Option("l", "repo-url", true, "Repository server URL, e.g. http://localhost:9263");
        repoURLOption.setRequired(true);
        options.addOption(repoURLOption);

        Option repoUserOption = new Option("u", "repo-user", true, "Daisy repository login");
        repoUserOption.setRequired(true);
        options.addOption(repoUserOption);

        Option repoPwdOption = new Option("p", "repo-pwd", true, "Daisy repository password");
        repoPwdOption.setRequired(false);
        options.addOption(repoPwdOption);

        Option cleanerConfOption = new Option("c", "cleanerconf", true, "Path to HTML cleaner config (htmlcleaner.xml)");
        cleanerConfOption.setRequired(true);
        options.addOption(cleanerConfOption);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("daisy-docid-convertor", options, true);
            System.exit(1);
        }

        String pwd;
        if (cmd.hasOption(repoPwdOption.getOpt())) {
            pwd = cmd.getOptionValue(repoPwdOption.getOpt());
        } else {
            pwd = CliUtil.promptPassword("Password for " + cmd.getOptionValue(repoUserOption.getOpt()) + ": ", true);
        }

        DocIdConvertor convertor = new DocIdConvertor(cmd.getOptionValue('l'), cmd.getOptionValue('u'),
                pwd, cmd.getOptionValue('c'));
        convertor.run();
    }

    public DocIdConvertor(String repositoryURL, String repoUser, String repoPwd, String htmlCleanerConfig) {
        this.repositoryURL = repositoryURL;
        this.repoUser = repoUser;
        this.repoPwd = repoPwd;
        this.htmlCleanerConfig  = htmlCleanerConfig;
    }

    private void run() throws Exception {
        setupHtmlCleaner();
        setupRepository();

        // Get a list of all document variants
        VariantKey[] documents = repository.getQueryManager().performQueryReturnKeys("select id where true option search_last_version = 'true', include_retired = 'true'", Locale.getDefault());

        log("Will process " + documents.length + " documents.");

        List succeededDocuments = new ArrayList(documents.length);
        List failedDocuments = new ArrayList();

        for (int i = 0; i < documents.length; i++) {
            try {
                log("Will work on " + documents[i]  + " [" + i + " / " + documents.length + "]");
                Document document = repository.getDocument(documents[i], true);
                updateDocument(document);
                succeededDocuments.add(documents[i]);
            } catch (Throwable e) {
                logError("Error updating " + documents[i], e);
                failedDocuments.add(documents[i]);
            }
        }

        log("Finished. Processed " + succeededDocuments.size() + " documents successfully, failed on " + failedDocuments.size() + " documents.");

    }

    private boolean hasAdminRole(long[] roles) {
        for (int i = 0; i < roles.length; i++) {
            if (roles[i] == Role.ADMINISTRATOR)
                return true;
        }
        return false;
    }

    public void log(String text) {
        System.out.println(text);
    }

    public void logError(String text, Throwable e) {
        System.out.println(text);
        e.printStackTrace(System.out);
    }

    private void updateDocument(Document document) throws RepositoryException {
        boolean anyUpdates = false;
        anyUpdates = anyUpdates | updateLinks(document);
        anyUpdates = anyUpdates | updateParts(document);

        if (anyUpdates) {
            document.save(false);
            log("Document saved.");
        } else {
            log("Document was not changed.");
        }
    }

    private boolean updateLinks(Document document) {
        Link[] links = document.getLinks().getArray();

        // first check if any link needs adjusting
        boolean needsUpdate = false;
        for (int i = 0; i < links.length; i++) {
            if (updateLink(links[i].getTarget()) != null) {
                needsUpdate = true;
                break;
            }
        }

        if (needsUpdate) {
            document.clearLinks();
            for (int i = 0; i < links.length; i++) {
                String newLink = updateLink(links[i].getTarget());
                document.addLink(links[i].getTitle(), newLink != null ? newLink : links[i].getTarget());
            }
        }

        return needsUpdate;
    }

    private boolean updateParts(Document document) throws RepositoryException {
        boolean anyUpdates = false;
        Part[] parts = document.getParts().getArray();
        for (int i = 0; i < parts.length; i++) {
            Part part = parts[i];
            PartType partType = repository.getRepositorySchema().getPartTypeById(part.getTypeId(), false);
            PartDocIdUpdater partUpdater = null;
            if (partType.isDaisyHtml()) {
                partUpdater = new DaisyHtmlDocIdUpdater();
            } else if (partType.getName().equals("NavigationDescription")) {
                partUpdater = new NavigationDocIdUpdater();
            } else if (partType.getName().equals("BookDefinitionDescription")) {
                partUpdater = new BookDocIdUpdater();
            } else if (partType.getName().equals("BookPublicationsDefault")) {
                partUpdater = new PropertiesDocIdUpdater();
            } else if (partType.getName().equals("BookMetadata")) {
                partUpdater = new PropertiesDocIdUpdater();
            }

            if (partUpdater != null) {
                try {
                    byte[] data = partUpdater.update(part, this);
                    if (data != null) {
                        String fileName = part.getFileName();
                        document.setPart(part.getTypeId(), part.getMimeType(), data);
                        if (fileName != null)
                            document.setPartFileName(part.getTypeId(), fileName);
                        anyUpdates = true;
                    }
                } catch (Throwable e) {
                    logError("Error updating part " + partType.getName() + " of document " + document.getVariantKey(), e);
                }
            }
        }
        return anyUpdates;
    }


    public static final Pattern DAISY_LINK_PATTERN = Pattern.compile("^daisy:([0-9]{1,19}(?:-[a-zA-Z0-9_]{1,200})?)((@([^:#]*)(:([^:#]*))?(:([^:#]*))?)?(#.+)?)$");

    public String updateLink(String link) {
        Matcher linkMatcher = DAISY_LINK_PATTERN.matcher(link);
        if (linkMatcher.matches()) {
            String docId = linkMatcher.group(1);
            Matcher docIdMatcher = Constants.DAISY_COMPAT_DOCID_PATTERN.matcher(docId);
            if (docIdMatcher.matches() && docIdMatcher.group(2) == null) {
                String remainderOfLink = linkMatcher.group(2) != null ? linkMatcher.group(2) : "";
                String newLink = "daisy:" + docIdMatcher.group(1) + "-" + repositoryNamespace + remainderOfLink;
                return newLink;
            }
        }
        return null;
    }

    public String updateId(String docId) {
        Matcher docIdMatcher = Constants.DAISY_COMPAT_DOCID_PATTERN.matcher(docId);
        if (docIdMatcher.matches() && docIdMatcher.group(2) == null) {
            String newLink = docIdMatcher.group(1) + "-" + repositoryNamespace;
            return newLink;
        }
        return null;
    }

    private void setupHtmlCleaner() throws Exception {
        try {
            HtmlCleanerFactory factory = new HtmlCleanerFactory();
            htmlCleanerTemplate = factory.buildTemplate(new InputSource(this.htmlCleanerConfig));
        } catch (Throwable e) {
            throw new Exception("Error setting up HTML cleaner from " + htmlCleanerConfig, e);
        }
    }

    public HtmlCleaner getHtmlCleaner() {
        return htmlCleanerTemplate.newHtmlCleaner();
    }

    private void setupRepository() throws Exception {
        RepositoryManager repositoryManager = new RemoteRepositoryManager(repositoryURL, new Credentials(repoUser, repoPwd));
        repository = repositoryManager.getRepository(new Credentials(repoUser, repoPwd));

        if (!hasAdminRole(repository.getAvailableRoles())) {
            log("Warning: the user " + repoUser + " does not have the Administrator role. Hence the user might not have access to all documents.");
        } else {
            repository.switchRole(Role.ADMINISTRATOR);
        }

        repositoryNamespace = repository.getNamespaceManager().getRepositoryNamespace();
        log("The namespace of the repository server is " + repositoryNamespace);
    }

}
