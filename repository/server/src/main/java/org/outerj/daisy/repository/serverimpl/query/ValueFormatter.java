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

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

import org.outerj.daisy.query.model.QValueType;
import org.outerj.daisy.query.model.ValueExpr;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.InvalidDocumentIdException;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.serverimpl.LocalRepositoryManager;

public class ValueFormatter implements Formatter {
    private final DateFormat dateFormat;

    private final DateFormat dateTimeFormat;

    private final NumberFormat decimalFormat;

    private final Locale locale;

    private LocalRepositoryManager.Context context;

    private AuthenticatedUser user;

    public ValueFormatter(Locale locale, LocalRepositoryManager.Context context, AuthenticatedUser user) {
        this.locale = locale;
        dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
        dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
        decimalFormat = NumberFormat.getNumberInstance(locale);
        this.context = context;
        this.user = user;
    }

    public String format(ValueExpr valueExpr, Object value) {
        String formattedValue;
        QValueType outputValueType = valueExpr.getOutputValueType();
        
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
        
        switch (outputValueType) {
            case DATE:
                formattedValue = dateFormat.format((Date) value);
                break;
            case DATETIME:
                formattedValue = dateTimeFormat.format((Date) value);
                break;
            case DECIMAL:
                formattedValue = decimalFormat.format(value);
                break;
            case DOUBLE:
                formattedValue = decimalFormat.format(value);
                break;
            case VERSION_STATE:
                formattedValue = LocalQueryManager.getLocalizedString(value.toString(), locale);
                break;
            case LINK:
                VariantKey variantKey = (VariantKey) value;
                String label = null;
                try {
                    Document linkedDoc = context.getCommonRepository().getDocument(variantKey.getDocumentId(),
                            variantKey.getBranchId(), variantKey.getLanguageId(), false, user);
                    if (linkedDoc.getLiveVersion() != null)
                        label = linkedDoc.getLiveVersion().getDocumentName();
                    else
                        label = linkedDoc.getName();
                } catch (InvalidDocumentIdException e) {
                    // ignore exception
                } catch (RepositoryException e) {
                    // ignore exception (non existing doc variant)
                }
                if (label == null)
                    label = "daisy:" + variantKey.getDocumentId() + "@" + variantKey.getBranchId() + ":"
                            + variantKey.getLanguageId();
                formattedValue = label;
                break;
            default:
                formattedValue = value.toString();
        }
        
        return formattedValue;
    }

    public Locale getLocale() {
        return locale;
    }
}