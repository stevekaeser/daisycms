/*
 * Copyright 2008 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.publisher.serverimpl.docpreparation;

import java.util.Map;

import javax.xml.namespace.QName;

import org.outerj.daisy.repository.query.ValueExpression;

public class LinkAnnotationConfig {
    private QName attribute;
    private boolean navigationPath;
    private boolean imageAnnotations;
    private Map<String, ValueExpression> customAnnotations;

    public LinkAnnotationConfig(QName attribute, boolean navigationPath, boolean imageAnnotations, Map<String, ValueExpression> customAnnotations) {
        this.attribute = attribute;
        this.navigationPath = navigationPath;
        this.imageAnnotations = imageAnnotations;
        this.customAnnotations = customAnnotations;
    }

    public QName getAttribute() {
        return attribute;
    }

    public boolean navigationPath() {
        return navigationPath;
    }

    public boolean imageAnnotations() {
        return imageAnnotations;
    }

    public Map<String, ValueExpression> getCustomAnnotations() {
        return customAnnotations;
    }
}
