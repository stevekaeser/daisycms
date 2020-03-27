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
package org.outerj.daisy.largedatasettest;

import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.query.QueryHelper;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.tools.importexport.model.schema.ImpExpSchemaDexmlizer;
import org.outerj.daisy.tools.importexport.model.schema.ImpExpSchema;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaLoader;
import org.outerj.daisy.tools.importexport.import_.schema.BaseSchemaLoadListener;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaLoadResult;
import org.apache.commons.cli.*;
import org.outerx.daisy.x10.SearchResultDocument;

import java.io.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Locale;

public class LargeDatasetTest {
    // config parameters
    private int size = 100;
    private int runs = 300;
    private String repositoryURL = "http://localhost:9263";
    private String repositoryLogin = "testuser";
    private String repositoryPassword = "testuser";
    private String scowlPath = "scowl-6.zip";
    private Mode mode = Mode.LOAD;

    // other instance data
    private Repository repository;
    private List<String> smallWordList;
    private List<String> largeWordList;
    private Random random;
    private PrintWriter resultWriter;

    // constants
    private static final String DOCTYPE = "LargeDatasetTestDoctype";
    private static final String PARTNAME = "LargeDatasetTestContent";
    private static final String LONGFIELD = "LargeDatasetTestLongField";
    private static final String STRINGFIELD = "LargeDatasetTestStringField";
    private static final String MVSTRINGFIELD = "LargeDatasetTestMVStringField";
    private static final String SCOWL_BIG_WORDLIST = "final/english-words.70";
    private static final String SCOWL_SMALL_WORDLIST = "final/english-words.10";
    private static enum Mode { LOAD, LOAD_UPDATE, SEARCH }

    public static void main(String[] args) throws Exception {
        new LargeDatasetTest().run(args);
    }

    public void run(String[] args) throws Exception {
        config(args);

        setup();
        checkDoctype();
        loadWordLists();

        random = new Random(System.currentTimeMillis());

        switch (mode) {
            case LOAD:
            case LOAD_UPDATE:
                runLoadTest();
                break;
            case SEARCH:
                runSearchTest();
                break;
            default:
                throw new RuntimeException("Unrecognized mode: " + mode);
        }
    }

    private void config(String[] args) {
        Options cliOptions = new Options();

        Option modeOption = new Option("m", "mode", true, "Mode: load or search");
        cliOptions.addOption(modeOption);

        Option repoURLOption = new Option("l", "repo-url", true, "Repository server URL, e.g. http://localhost:9263");
        cliOptions.addOption(repoURLOption);

        Option repoUserOption = new Option("u", "repo-user", true, "Daisy repository login");
        cliOptions.addOption(repoUserOption);

        Option repoPwdOption = new Option("p", "repo-pwd", true, "Daisy repository password");
        cliOptions.addOption(repoPwdOption);

        Option scowlOption = new Option("s", "scowl", true, "Path to scowl zip");
        cliOptions.addOption(scowlOption);

        Option runsOption = new Option("x", "runs", true, "Number of times to run");
        cliOptions.addOption(runsOption);

        Option sizeOption = new Option("y", "size", true, "Size of each run");
        cliOptions.addOption(sizeOption);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        boolean showHelp = false;
        try {
            cmd = parser.parse(cliOptions, args);
        } catch (ParseException e) {
            showHelp = true;
        }

        if (showHelp) {
            printHelp(cliOptions);
            System.exit(0);
        }

        String modeString = getStringOption(cmd, modeOption, mode.toString()).toUpperCase();
        this.mode = Mode.valueOf(modeString);
        repositoryURL = getStringOption(cmd, repoURLOption, repositoryURL);
        repositoryLogin = getStringOption(cmd, repoUserOption, repositoryLogin);
        repositoryPassword = getStringOption(cmd, repoPwdOption, repositoryPassword);
        scowlPath = getStringOption(cmd, scowlOption, scowlPath);
        runs = getIntOption(cmd, runsOption, runs);
        size = getIntOption(cmd, sizeOption, size);
    }

    private String getStringOption(CommandLine cmd, Option option, String defaultValue) {
        if (!cmd.hasOption(option.getOpt())) {
            System.out.println("-" + option.getOpt() + " not specified, assuming " + defaultValue);
            return defaultValue;
        } else {
            return cmd.getOptionValue(option.getOpt());
        }
    }

    private int getIntOption(CommandLine cmd, Option option, int defaultValue) {
        if (!cmd.hasOption(option.getOpt())) {
            System.out.println("-" + option.getOpt() + " not specified, assuming " + defaultValue);
            return defaultValue;
        } else {
            try {
                return Integer.parseInt(cmd.getOptionValue(option.getOpt()));
            } catch (NumberFormatException e) {
                System.out.println("Invalid integer value for parameter -" + option.getOpt() + " : " + cmd.getOptionValue(option.getOpt()));
                System.exit(1);
                return 0; // won't happen
            }
        }
    }

    private void printHelp(Options cliOptions) {
        HelpFormatter help = new HelpFormatter();
        help.printHelp("large-dataset-test", cliOptions, true);
    }

    private void setup() throws Exception {
        System.out.println("Connecting to the repository...");

        RemoteRepositoryManager repositoryManager = new RemoteRepositoryManager(repositoryURL, new Credentials("guest", "guest"));
        repository = repositoryManager.getRepository(new Credentials(repositoryLogin, repositoryPassword));
        repository.switchRole(Role.ADMINISTRATOR);
    }

    private void checkDoctype() throws Exception {
        System.out.println("Assuring schema exists...");

        String schemaPath = "largedataset-schema.xml";
        InputStream is = this.getClass().getResourceAsStream(schemaPath);
        if (is == null)
            throw new Exception("Resource not found: " + schemaPath);

        ImpExpSchema schema = ImpExpSchemaDexmlizer.fromXml(is, repository, new ImpExpSchemaDexmlizer.Listener() {
            public void info(String message) {
                System.out.println(message);
            }
        });

        SchemaLoader.load(schema, repository, false, false, new MySchemaLoadListener());
    }

    public class MySchemaLoadListener extends BaseSchemaLoadListener {
        public void fieldTypeLoaded(String fieldTypeName, SchemaLoadResult result) {
            System.out.println("Field type " + fieldTypeName + " : " + result);
        }

        public void partTypeLoaded(String partTypeName, SchemaLoadResult result) {
            System.out.println("Part type " + partTypeName + " : " + result);
        }

        public void documentTypeLoaded(String documentTypeName, SchemaLoadResult result) {
            System.out.println("Document type " + documentTypeName + " : " + result);
        }

        public boolean isInterrupted() {
            return false;
        }
    }

    private void loadWordLists() throws Exception {
        System.out.println("Loading word lists...");
        File scowlFile = new File(scowlPath);
        if (!scowlFile.exists()) {
            System.out.println("Scowl file not found at " + scowlPath);
            System.exit(1);
        }

        ZipFile file = new ZipFile(scowlPath);

        ZipEntry bigListEntry = file.getEntry(SCOWL_BIG_WORDLIST);
        if (bigListEntry == null)
            throw new Exception("Could not find word list: " + SCOWL_BIG_WORDLIST);
        largeWordList = new ArrayList<String>(50000);
        loadWords(largeWordList, file.getInputStream(bigListEntry));
        System.out.println("Loaded large word list, # entries = " + largeWordList.size());

        ZipEntry smallListEntry = file.getEntry(SCOWL_SMALL_WORDLIST);
        if (smallListEntry == null)
            throw new Exception("Could not find word list: " + SCOWL_SMALL_WORDLIST);
        smallWordList = new ArrayList<String>(50000);
        loadWords(smallWordList, file.getInputStream(smallListEntry));
        System.out.println("Loaded small word list, # entries = " + smallWordList.size());
    }

    private void loadWords(List<String> target, InputStream is) throws Exception {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "latin1"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() > 0) {
                    target.add(line);
                }
            }
        } finally {
            if (is != null)
                is.close();
        }
    }

    private void runLoadTest() throws Exception {
        System.out.println("Starting data-load test...");

        String resultFileName = "load-timings-" + System.currentTimeMillis() + ".csv";
        System.out.println("Will write timing data to " + resultFileName);
        File outputFile = new File(resultFileName);
        resultWriter = new PrintWriter(new FileWriter(outputFile));

        try {
            resultWriter.println("type\taverage\tcount\tmin\tmax");

            for (int i = 0; i < runs; i++) {
                System.out.printf("Performing run %d of %d.\n", i + 1, runs);
                loadData(i);
            }
        } finally {
            resultWriter.close();
        }
    }

    private void loadData(int run) throws Exception {
        System.out.println("Creating data.");

        List<String> createdDocumentIds = new ArrayList<String>();
        Profiler createProfiler = new Profiler();
        Profiler updateProfiler = new Profiler();

        // Create documents
        for (int i = 0; i < size; i++) {
            System.out.printf("Creating document %d of %d (run %d of %d)\n", i + 1, size, run + 1, runs);
            String name = getRandomWordFromSmallList() + " " + getRandomWordFromSmallList() + " " + getRandomWordFromSmallList();
            Document document = repository.createDocument(name, DOCTYPE);
            document.setPart(PARTNAME, "text/xml", getRandomText(500).getBytes("UTF-8"));
            document.setField(LONGFIELD, new Long(random.nextInt(1000)));
            document.setField(STRINGFIELD, getRandomWordFromSmallList());
            document.setField(MVSTRINGFIELD, new Object[] { getRandomWordFromSmallList(), getRandomWordFromSmallList(), getRandomWordFromSmallList() });
            createProfiler.start();
            document.save();
            createProfiler.stop();
            createdDocumentIds.add(document.getId());
        }
        resultWriter.printf("load\t%d\t%d\t%d\t%d\n", createProfiler.getAverageInMillis(), createProfiler.getCount(), createProfiler.getMinInMillis(), createProfiler.getMaxInMillis());
        resultWriter.flush();
        createProfiler.dump("Document save time");

        if (mode == Mode.LOAD_UPDATE) {
            // Update the created documents
            for (int i = 0; i < createdDocumentIds.size(); i++) {
                String id = createdDocumentIds.get(i);
                System.out.printf("Updating document %s -- %d of %d (run %d of %d)\n", id, i + 1, size, run + 1, runs);
                Document document = repository.getDocument(id, true);
                document.lock(5000, LockType.PESSIMISTIC);
                Part part = document.getPart(PARTNAME);
                String data = new String(PartHelper.streamToByteArrayAndClose(part.getDataStream(), (int)part.getSize()), "UTF-8");
                String prefix = "<html><body><p>";
                if (!data.startsWith(prefix))
                    throw new RuntimeException("Data part seems to be modified");

                String newData = "<html><body><p><b>updated!</b> " + data.substring(prefix.length());
                document.setPart(PARTNAME, "text/xml", newData.getBytes("UTF-8"));

                updateProfiler.start();
                document.save();
                document.releaseLock();
                Version firstVersion = document.getVersion(1);
                firstVersion.setState(VersionState.DRAFT);
                firstVersion.save();
                document.getVersions();
                updateProfiler.stop();
            }
            resultWriter.printf("update\t%d\t%d\t%d\t%d\n", updateProfiler.getAverageInMillis(), updateProfiler.getCount(), updateProfiler.getMinInMillis(), updateProfiler.getMaxInMillis());
            resultWriter.flush();
            updateProfiler.dump("Document update time");
        }
    }

    private String getRandomWordFromSmallList() {
        return smallWordList.get(random.nextInt(smallWordList.size()));
    }

    private String getRandomWordFromLargeList() {
        return largeWordList.get(random.nextInt(largeWordList.size()));
    }

    private String getRandomText(int words) {
        StringBuilder builder = new StringBuilder(15 * words);
        builder.append("<html><body><p>");
        for (int i = 0; i < words; i++) {
            if (i > 50 && i % 50 == 0) {
                builder.append("</p><p>");
            }
            builder.append(getRandomWordFromLargeList());
            builder.append(' ');
        }
        builder.append("</p></body></html>");
        return builder.toString();
    }

    class Profiler {
        private boolean started = false;
        private long start;
        private long min = Long.MAX_VALUE;
        private long max = 0;
        private long total = 0;
        private long count = 0;

        public void start() {
            started = true;
            start = System.nanoTime();
        }

        public long stop() {
            if (!started)
                throw new RuntimeException("Called stop without being started.");

            long elapsed = System.nanoTime() - start;

            if (elapsed < min)
                min = elapsed;

            if (elapsed > max)
                max = elapsed;

            total += elapsed;
            count++;

            started = false;

            return elapsed / 1000000;
        }

        public void dump(String title) {
            System.out.printf("%s: average: %d, samples: %d, min: %d, max %d\n", title, getAverageInMillis(), count, getMinInMillis(), getMaxInMillis());
        }

        public long getMinInMillis() {
            return min / 1000000;
        }

        public long getMaxInMillis() {
            return max / 1000000;
        }

        public long getCount() {
            return count;
        }

        public long getAverageInMillis() {
            return total / count / 1000000;
        }
    }

    private void runSearchTest() throws Exception {
        System.out.println("Starting search test...");

        String resultFileName = "search-timings-" + System.currentTimeMillis() + ".csv";
        System.out.println("Will write timing data to " + resultFileName);
        File outputFile = new File(resultFileName);
        resultWriter = new PrintWriter(new FileWriter(outputFile));

        try {
            resultWriter.println("Type\tTotal time\tResult set size\tParse and prepare time\tRDBMS query time\tFulltext query time\tMerge time\tACL filter time\tSort time\tOutput generation time\tQuery");

            for (int i = 0; i < runs; i++) {
                System.out.printf("Performing run %d of %d.\n", i + 1, runs);
                search(i);
            }
        } finally {
            resultWriter.close();
        }
    }

    private void search(int run) throws Exception {
        // Fulltext search
        String query = "select id, name where FullText(" + QueryHelper.formatString(getRandomWordFromLargeList()) + ")";
        performQuery("Fulltext", query);

        // Fulltext + metadata search
        query = "select id, name where FullText(" + QueryHelper.formatString(getRandomWordFromLargeList()) + ")"
                + " and documentType = " + QueryHelper.formatString(DOCTYPE);
        performQuery("FulltextDoctype", query);

        // Query on string field
        query = "select id, name where $LargeDatasetTestStringField = " + QueryHelper.formatString(getRandomWordFromSmallList() + " order by name");
        performQuery("StringField", query);

        // Query on two string fields
        query = "select id, name where $LargeDatasetTestStringField = "
                + QueryHelper.formatString(getRandomWordFromSmallList()) + " or $LargeDatasetTestStringField = "
                + QueryHelper.formatString(getRandomWordFromSmallList());
        performQuery("TwoStringFields", query);

        // Query on string and number field
        query = "select id, name where $LargeDatasetTestStringField = "
                + QueryHelper.formatString(getRandomWordFromSmallList())
                + " and $LargeDatasetTestLongField between 500 and 800";
        performQuery("StringAndNumberField", query);

        // Query on string field whose result should be empty
        query = "select id, name where $LargeDatasetTestStringField = 'biebabeloeba'";
        performQuery("StringFieldNonExistingValue", query);

        // Query all docs -- limit clause is there since we're not really interested in the result and building large results takes a lot of time/space
        query = "select id, name where documentType = " + QueryHelper.formatString(DOCTYPE) + " limit 500";
        performQuery("AllDocs", query);
    }

    private void performQuery(String type, String query) throws Exception {
        for (int i = 0; i < size; i++) {
            long start = System.nanoTime();
            SearchResultDocument result = repository.getQueryManager().performQuery(query, Locale.US);
            long elapsed = (System.nanoTime() - start) / 1000000;
            outputSearchTiming(type, elapsed, result);
        }
    }

    private void outputSearchTiming(String type, long duration, SearchResultDocument result) {
        int resultSize = result.getSearchResult().getRows().sizeOfRowArray();
        SearchResultDocument.SearchResult.ExecutionInfo executionInfo = result.getSearchResult().getExecutionInfo();
        resultWriter.printf("%s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%s\n", type, duration, resultSize,
                executionInfo.getParseAndPrepareTime(),
                executionInfo.getRdbmsQueryTime(),
                executionInfo.getFullTextQueryTime(),
                executionInfo.getMergeTime(),
                executionInfo.getAclFilterTime(),
                executionInfo.getSortTime(),
                executionInfo.getOutputGenerationTime(),
                executionInfo.getQuery());
        resultWriter.flush();

        System.out.printf("Performed query of type %s in %d\n", type, duration);
    }
}
