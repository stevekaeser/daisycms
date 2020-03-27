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
package org.outerj.daisy.frontend;

import java.io.IOException;
import java.io.StringReader;

import org.apache.cocoon.forms.formmodel.Group;
import org.apache.cocoon.forms.formmodel.Repeater;
import org.apache.cocoon.forms.formmodel.Union;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerx.daisy.x10DocumentActions.SimpleActionsParametersDocument;
import org.outerx.daisy.x10DocumentActions.SimpleActionsParametersDocument.SimpleActionsParameters;

public class SimpleActionsParametersHandler implements DocumentTaskParametersHandler {

    public String save(Widget group) {
        SimpleActionsParametersDocument simpleActions = SimpleActionsParametersDocument.Factory.newInstance();
        SimpleActionsParameters actionsXml = simpleActions.addNewSimpleActionsParameters();
        Repeater actionRepeater = (Repeater)group.lookupWidget("actions");
        for (int i = 0; i < actionRepeater.getSize(); i++) {
            Repeater.RepeaterRow row = actionRepeater.getRow(i);
            String actionType = (String)row.getChild("actiontype").getValue();
            Union actionUnion = (Union)row.getChild("actionsUnion");
            if (actionType.equals("createVariant")) {
                Group createVariantGroup = (Group)actionUnion.getChild("createVariant");
                String newBranch = createVariantGroup.getChild("newBranchId").getValue().toString();
                String newLanguage = createVariantGroup.getChild("newLanguageId").getValue().toString();
                String startVersion = (String)createVariantGroup.getChild("startVersion").getValue();
                SimpleActionsParameters.CreateVariant action = actionsXml.addNewCreateVariant();
                action.setNewBranch(newBranch);
                action.setNewLanguage(newLanguage);
                action.setStartVersion(startVersion);
            } else if (actionType.equals("deleteVariant")) {
                actionsXml.addNewDeleteVariant();
            } else if (actionType.equals("addToCollection")) {
                Group addToCollectionGroup = (Group)actionUnion.getChild("addToCollection");
                String collection = addToCollectionGroup.getChild("collection").getValue().toString();
                SimpleActionsParameters.AddToCollection action = actionsXml.addNewAddToCollection();
                action.setCollection(collection);
            } else if (actionType.equals("removeFromCollection")) {
                Group removeFromCollectionGroup = (Group)actionUnion.getChild("removeFromCollection");
                String collection = removeFromCollectionGroup.getChild("collection").getValue().toString();
                SimpleActionsParameters.RemoveFromCollection action = actionsXml.addNewRemoveFromCollection();
                action.setCollection(collection);
            } else {
                throw new RuntimeException("Encountered unsupported simple action: " + actionType);
            }
        }
        actionsXml.setValidateOnSave(((Boolean)group.lookupWidget("validateOnSave").getValue()).booleanValue());
        return simpleActions.toString();
    }
    
    public void load(Widget group, String value, Repository repository) {
        try {
            SimpleActionsParametersDocument simpleActions = SimpleActionsParametersDocument.Factory.parse(new StringReader(value));
            SimpleActionsParameters actionsXml = simpleActions.getSimpleActionsParameters();
            Repeater actionRepeater = (Repeater)group.lookupWidget("actions");

            for (XmlObject child: actionsXml.selectChildren(QNameSet.ALL)) {
                if (child instanceof SimpleActionsParameters.CreateVariant) {
                    SimpleActionsParameters.CreateVariant createVariant = (SimpleActionsParameters.CreateVariant) child;
                    Repeater.RepeaterRow row = actionRepeater.addRow();
                    row.getChild("actiontype").setValue("createVariant");
                    Union union = (Union)row.getChild("actionsUnion");
                    Group actionGroup = (Group)union.getChild("createVariant");
                    String branchName = createVariant.getNewBranch();
                    actionGroup.getChild("newBranchId").setValue(repository.getVariantManager().getBranch(branchName, false).getId());
                    String languageName = createVariant.getNewLanguage();
                    actionGroup.getChild("newLanguageId").setValue(repository.getVariantManager().getLanguage(languageName, false).getId());
                    actionGroup.getChild("startVersion").setValue(createVariant.getStartVersion());
                } else if (child instanceof SimpleActionsParameters.DeleteVariant) {
                    //SimpleActionsParameters.DeleteVariant deleteVariant = (SimpleActionsParameters.DeleteVariant) child;
                    Repeater.RepeaterRow row = actionRepeater.addRow();
                    row.getChild("actiontype").setValue("deleteVariant");
                } else if (child instanceof SimpleActionsParameters.AddToCollection) {
                    SimpleActionsParameters.AddToCollection addToCollection = (SimpleActionsParameters.AddToCollection) child;
                    Repeater.RepeaterRow row = actionRepeater.addRow();
                    row.getChild("actiontype").setValue("addToCollection");
                    Union union = (Union)row.getChild("actionsUnion");
                    Group actionGroup = (Group)union.getChild("addToCollection");
                    
                    String collectionName = addToCollection.getCollection();
                    actionGroup.getChild("collection").setValue(repository.getCollectionManager().getCollection(collectionName, false).getId());
                } else if (child instanceof SimpleActionsParameters.RemoveFromCollection) {
                    SimpleActionsParameters.RemoveFromCollection removeFromCollection = (SimpleActionsParameters.RemoveFromCollection) child;
                    Repeater.RepeaterRow row = actionRepeater.addRow();
                    row.getChild("actiontype").setValue("removeFromCollection");
                    Union union = (Union)row.getChild("actionsUnion");
                    Group actionGroup = (Group)union.getChild("removeFromCollection");
                    String collectionName = removeFromCollection.getCollection();
                    actionGroup.getChild("collection").setValue(repository.getCollectionManager().getCollection(collectionName, false).getId());
                }
            }
            
            group.lookupWidget("validateOnSave").setValue(actionsXml.getValidateOnSave());
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to load parameters into form");
        } catch (XmlException xe) {
            throw new RuntimeException("Failed to load parameters into form");
        } catch (RepositoryException re) {
            throw new RuntimeException("Failed to load parameters into form");
        }
    }
    
}
