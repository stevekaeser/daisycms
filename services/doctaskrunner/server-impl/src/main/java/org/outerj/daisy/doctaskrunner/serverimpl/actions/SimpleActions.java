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
package org.outerj.daisy.doctaskrunner.serverimpl.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.doctaskrunner.TaskContext;
import org.outerj.daisy.doctaskrunner.TaskException;
import org.outerj.daisy.doctaskrunner.TaskSpecification;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerx.daisy.x10DocumentActions.SimpleActionsParametersDocument;
import org.outerx.daisy.x10DocumentActions.SimpleActionsParametersDocument.SimpleActionsParameters;


public class SimpleActions extends AbstractJavascriptDocumentAction {
    
    boolean validateOnSave = false;
    private String script;
    List<SimpleAction> actions = new ArrayList<SimpleAction>();

    @Override
    public String getScriptCode() throws Exception {
        return script;
    }

    public void fromXml(SimpleActionsParameters simpleActionsParameters) {
        validateOnSave = simpleActionsParameters.getValidateOnSave();
        XmlObject[] actionsXml = simpleActionsParameters.selectChildren(QNameSet.ALL);
        for (int i = 0; i < actionsXml.length; i++) {
            XmlObject actionXml = actionsXml[i];
            if (actionXml instanceof SimpleActionsParameters.CreateVariant) {
                SimpleActionsParameters.CreateVariant createVariantXml = (SimpleActionsParameters.CreateVariant)actionXml;
                addCreateVariant(createVariantXml.getStartVersion(), createVariantXml.getNewBranch(), createVariantXml.getNewLanguage());
            } else if (actionXml instanceof SimpleActionsParameters.DeleteVariant) {
                addDeleteVariant();
            } else if (actionXml instanceof SimpleActionsParameters.AddToCollection) {
                SimpleActionsParameters.AddToCollection addToCollectionXml = (SimpleActionsParameters.AddToCollection)actionXml;
                addAddToCollection(addToCollectionXml.getCollection());
            } else if (actionXml instanceof SimpleActionsParameters.RemoveFromCollection) {
                SimpleActionsParameters.RemoveFromCollection removeFromCollectionXml = (SimpleActionsParameters.RemoveFromCollection)actionXml;
                addRemoveFromCollection(removeFromCollectionXml.getCollection());
            } else {
                throw new RuntimeException("Unrecognized simple action: " + actionXml);
            }
        }
    }
    
    @Override
    public void onSetup(VariantKey[] keys,
            TaskSpecification taskSpecification, TaskContext taskContext,
            Repository repository) throws Exception {
        fromXml(SimpleActionsParametersDocument.Factory.parse(taskSpecification.getParameters()).getSimpleActionsParameters());
        
        StringBuilder buffer = new StringBuilder();
        buffer.append("var document = null;\n");
        Iterator<SimpleAction> actionsIt = actions.iterator();
        while (actionsIt.hasNext()) {
            SimpleAction action = (SimpleAction)actionsIt.next();
            action.addScriptCode(buffer);
        }
        buffer.append("\n\nif (document != null)\n    document.save(").append(validateOnSave).append(");");
        script = buffer.toString();
    }

    @Override
    public void onTearDown() throws Exception {
        // TODO Auto-generated method stub
    }
    

    interface SimpleAction {
        void addScriptCode(StringBuilder buffer) throws RepositoryException ;
        void addXml(SimpleActionsParameters simpleActionsParametersXml);
    }

    class CreateVariantAction implements SimpleAction {
        private String startVersion;
        private String newBranch;
        private String newLanguage;

        public CreateVariantAction(String startVersion, String newBranch, String newLanguage) {
            this.startVersion = startVersion;
            this.newBranch = newBranch;
            this.newLanguage = newLanguage;
        }

        public void addScriptCode(StringBuilder buffer) throws RepositoryException {
            VariantManager variantManager = repository.getVariantManager();
            long branchId = variantManager.getBranch(newBranch, false).getId();
            long languageId = variantManager.getLanguage(newLanguage, false).getId();
            long startVersionId;
            if (startVersion.equalsIgnoreCase("live")) {
                startVersionId = -2;
            } else if (startVersion.equalsIgnoreCase("last")) {
                startVersionId = -1;
            } else {
                try {
                    startVersionId = Long.parseLong(startVersion);
                } catch (NumberFormatException e) {
                    throw new TaskException("Invalid version: " + startVersion);
                }
            }

            buffer.append("\n");
            buffer.append("repository.createVariant(variantKey.getDocumentId(), variantKey.getBranchId(), variantKey.getLanguageId(), ");
            buffer.append(startVersionId);
            buffer.append(", ");
            buffer.append(branchId);
            buffer.append(", ");
            buffer.append(languageId);
            buffer.append(", true);\n");
        }

        public void addXml(SimpleActionsParameters simpleActionsXml) {
            SimpleActionsParameters.CreateVariant createVariant = simpleActionsXml.addNewCreateVariant();
            createVariant.setStartVersion(startVersion);
            createVariant.setNewBranch(newBranch);
            createVariant.setNewLanguage(newLanguage);
        }
    }

    class DeleteVariantAction implements SimpleAction {
        public void addScriptCode(StringBuilder buffer) throws RepositoryException {
            buffer.append("\nrepository.deleteVariant(variantKey);\n");
        }

        public void addXml(SimpleActionsParameters simpleActionsXml) {
            simpleActionsXml.addNewDeleteVariant();
        }
    }

    class AddToCollectionAction implements SimpleAction {
        private final String collection;

        public AddToCollectionAction(String collection) {
            this.collection = collection;
        }

        public void addScriptCode(StringBuilder buffer) throws RepositoryException {
            long collectionId = repository.getCollectionManager().getCollection(collection, false).getId();
            buffer.append("\n");
            buffer.append(LAZY_LOAD_DOCUMENT);
            buffer.append("\nvar collection = repository.getCollectionManager().getCollection(").append(collectionId).append(", false);");
            buffer.append("\ndocument.addToCollection(collection);");
            buffer.append("\n");
        }

        public void addXml(SimpleActionsParameters simpleActionsXml) {
            SimpleActionsParameters.AddToCollection addToCollectionXml = simpleActionsXml.addNewAddToCollection();
            addToCollectionXml.setCollection(collection);
        }
    }

    class RemoveFromCollectionAction implements SimpleAction {
        private final String collection;

        public RemoveFromCollectionAction(String collection) {
            this.collection = collection;
        }

        public void addScriptCode(StringBuilder buffer) throws RepositoryException {
            long collectionId = repository.getCollectionManager().getCollection(collection, false).getId();
            buffer.append("\n");
            buffer.append(LAZY_LOAD_DOCUMENT);
            buffer.append("\nvar collection = repository.getCollectionManager().getCollection(").append(collectionId).append(", false);\n");
            buffer.append("\ndocument.removeFromCollection(collection);\n");
            buffer.append("\n");
        }

        public void addXml(SimpleActionsParameters simpleActionsXml) {
            SimpleActionsParameters.RemoveFromCollection removeFromCollectionXml = simpleActionsXml.addNewRemoveFromCollection();
            removeFromCollectionXml.setCollection(collection);
        }
    }

    private static final String LAZY_LOAD_DOCUMENT = "if (document == null)\n    document = repository.getDocument(variantKey, true);\n";

    public void addCreateVariant(String startVersion, String newBranch, String newLanguage) {
        actions.add(new CreateVariantAction(startVersion, newBranch, newLanguage));
    }

    public void addDeleteVariant() {
        actions.add(new DeleteVariantAction());
    }

    public void addAddToCollection(String collection) {
        actions.add(new AddToCollectionAction(collection));
    }

    public void addRemoveFromCollection(String collection) {
        actions.add(new RemoveFromCollectionAction(collection));
    }
    
    public String getParameters() {
        SimpleActionsParametersDocument doc = SimpleActionsParametersDocument.Factory.newInstance();
        SimpleActionsParameters paramsXml = doc.addNewSimpleActionsParameters();
        Iterator<SimpleAction> actionsIt = actions.iterator();
        while (actionsIt.hasNext()) {
            SimpleAction action = (SimpleAction)actionsIt.next();
            action.addXml(paramsXml);
        }
        return doc.toString();
    }


}
