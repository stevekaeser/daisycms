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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaLoader;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaLoadListener;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaLoadResult;
import org.outerj.daisy.tools.importexport.model.schema.ImpExpSchemaDexmlizer;
import org.outerj.daisy.tools.importexport.model.schema.ImpExpSchema;

public class SchemaUploader {
    public static void main (String[] args) throws Exception{
        Options options = new Options();
        Option schemaOption = new Option("s", "schema", true, "Schema file to be uploaded");
        schemaOption.setRequired(true);
        schemaOption.setArgName("schema-file");
        Option urlOption = new Option("r", "url", true, "Repository URL");
        urlOption.setRequired(true);
        urlOption.setArgName("repository-url");        
        Option userOption = new Option("u", "user", true, "Repository user");
        userOption.setRequired(true);
        userOption.setArgName("username");
        Option passwordOption = new Option("p", "password", true, "Repository user password");
        passwordOption.setRequired(true);
        passwordOption.setArgName("password");
        options.addOption(schemaOption);
        options.addOption(urlOption);
        options.addOption(userOption);
        options.addOption(passwordOption);
        options.addOption("h", "help", false, "Show this message");
        
        CommandLineParser parser = new PosixParser();
        InputStream is = null;
        try {
            CommandLine cli = parser.parse(options, args);
            
            File file = new File(cli.getOptionValue("s"));
            String url = cli.getOptionValue("r");
            Credentials cred = new Credentials(cli.getOptionValue("u"), cli.getOptionValue("p"));

            RepositoryManager manager = new RemoteRepositoryManager(url, cred);
            Repository repository = manager.getRepository(cred);
            repository.switchRole(Role.ADMINISTRATOR);

            is = new FileInputStream(file);
            load(is, repository);
        } catch (ParseException e) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("daisy-schema-uploader", options, true);
        } finally {
            if (is != null)
                is.close();
        }
    }

    static void load(InputStream is, Repository repository) throws Exception {
        ImpExpSchema impExpSchema = ImpExpSchemaDexmlizer.fromXml(is, repository,
                new ImpExpSchemaDexmlizer.Listener() {
            public void info(String message) {
                System.out.println(message);
            }
        });

        SchemaLoader.load(impExpSchema, repository, false, false, new MySchemaLoadListener());
    }

    static class MySchemaLoadListener implements SchemaLoadListener {
        public void conflictingFieldType(String fieldTypeName, ValueType requiredType, ValueType foundType) throws Exception {
            System.out.println("WARNING!!! Field type " + fieldTypeName + " already exists, and has a different value type: expected " + requiredType + " but is " + foundType);
        }

        public void conflictingMultiValue(String fieldTypeName, boolean needMultivalue, boolean foundMultivalue) throws Exception {
            System.out.println("WARNING!!! Field type " + fieldTypeName + " already exists, and has a different multi-value property: expected " + needMultivalue + " but is " + foundMultivalue);
        }

        public void conflictingHierarchical(String fieldTypeName, boolean needHierarchical, boolean foundHierarchical) throws Exception {
            System.out.println("WARNING!!! Field type " + fieldTypeName + " already exists, and has a different hierarchical property: expected " + needHierarchical + " but is " + foundHierarchical);
        }

        public void fieldTypeLoaded(String fieldTypeName, SchemaLoadResult result) {
            System.out.println("Field type " + fieldTypeName + " : " + result);
        }

        public void partTypeLoaded(String partTypeName, SchemaLoadResult result) {
            System.out.println("Part type " + partTypeName + " : " + result);
        }

        public void documentTypeLoaded(String documentTypeName, SchemaLoadResult result) {
            System.out.println("Document type " + documentTypeName + " : " + result);
        }

        public void done() {
            System.out.println("Done.");
        }

        public boolean isInterrupted() {
            return false;
        }
    }
}
