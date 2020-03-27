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
package org.outerj.daisy.repository.serverimpl.query;

import java.util.Locale;

import org.outerj.daisy.query.model.Identifier;
import org.outerj.daisy.query.model.ValueExpr;
import org.outerj.daisy.query.model.Identifier.FieldIdentifier;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.SelectionList;
import org.outerj.daisy.repository.schema.StaticSelectionList;
import org.outerj.daisy.repository.serverimpl.LocalRepositoryManager;

public class FacetValueFormatter implements Formatter {
    
    private Formatter delegate;

    private final Locale locale;

    private LocalRepositoryManager.Context context;

    private AuthenticatedUser user;

    public FacetValueFormatter(Locale locale, LocalRepositoryManager.Context context, AuthenticatedUser user) {
        this.locale = locale;
        this.context = context;
        this.user = user;
        
        this.delegate = new ValueFormatter(locale, context, user);
    }

    public String format(ValueExpr valueExpr, Object value) {
        if (value == null)
            return "";

        if (value instanceof HierarchyPath) {
            HierarchyPath path = (HierarchyPath) value;
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < path.getElements().length; i++) {
                result.append("/");
                result.append(format(valueExpr, path.getElements()[i]));
            }
            return result.toString();
        }

        String formattedValue = null;

        // find label for a static field list
        if (valueExpr instanceof Identifier && ((Identifier) valueExpr).getDelegate() instanceof FieldIdentifier) {
            FieldIdentifier fieldIdentifier = (FieldIdentifier) ((Identifier) valueExpr).getDelegate();
            try {
                FieldType fieldType = context.getCommonRepository().getRepositorySchema().getFieldTypeById(
                        fieldIdentifier.getfieldTypeId(), false, user);
                SelectionList list = fieldType.getSelectionList();
                if (list != null && list instanceof StaticSelectionList) {
                    String listValue = list.getItemLabel(value, locale);
                    if (listValue != null && listValue.length() > 0)
                        formattedValue = listValue;
                }
            } catch (RepositoryException e) {
                // if the field cannot be found just use the stored value
            }
        }

        if (formattedValue == null) {
            formattedValue = this.delegate.format(valueExpr, value);
        }
        return formattedValue;
    }

    public Locale getLocale() {
        return locale;
    }
}