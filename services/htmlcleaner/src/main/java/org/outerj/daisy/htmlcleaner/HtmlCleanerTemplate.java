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
package org.outerj.daisy.htmlcleaner;

import java.util.*;

/**
 * This is a thread-safe, reusable object containing the configuration for
 * the HtmlCleaner. Instances of this object can be obtained from the
 * {@link HtmlCleanerFactory}. A concrete HtmlCleaner can be obtained
 * by using the method {@link #newHtmlCleaner()}.
 */
public class HtmlCleanerTemplate {
    int maxLineWidth = 80;
    Map<String, OutputElementDescriptor> outputElementDescriptors = new HashMap<String, OutputElementDescriptor>();
    Set<String> allowedSpanClasses = new HashSet<String>();
    Set<String> allowedDivClasses = new HashSet<String>();
    Set<String> allowedParaClasses = new HashSet<String>();
    Set<String> allowedPreClasses = new HashSet<String>();
    Set<String> allowedOlClasses = new HashSet<String>();
    Set<String> dropDivClasses = new HashSet<String>();
    Set<String> dropTableClasses = new HashSet<String>();
    Map<String, ElementDescriptor> descriptors = new HashMap<String, ElementDescriptor>();
    String imgAlternateSrcAttr;
    String linkAlternateHrefAttr;
    private boolean initialised = false;

    HtmlCleanerTemplate() {
        // package-private constructor
    }

    void addOutputElement(String tagName, int beforeOpen, int afterOpen, int beforeClose, int afterClose, boolean inline) {
        if (initialised)
            throw new IllegalStateException();
        if (tagName == null)
            throw new NullPointerException();
        OutputElementDescriptor descriptor = new OutputElementDescriptor(beforeOpen, afterOpen, beforeClose, afterClose, inline);
        outputElementDescriptors.put(tagName, descriptor);
    }

    void setMaxLineWidth(int lineWidth) {
        if (initialised)
            throw new IllegalStateException();
        this.maxLineWidth = lineWidth;
    }

    void addAllowedSpanClass(String clazz) {
        if (initialised)
            throw new IllegalStateException();
        if (clazz == null)
            throw new NullPointerException();
        allowedSpanClasses.add(clazz);
    }

    void addAllowedDivClass(String clazz) {
        if (initialised)
            throw new IllegalStateException();
        if (clazz == null)
            throw new NullPointerException();
        allowedDivClasses.add(clazz);
    }

    void addDropDivClass(String clazz) {
        if (initialised)
            throw new IllegalStateException();
        if (clazz == null)
            throw new NullPointerException();
        dropDivClasses.add(clazz);
    }

    void addDropTableClass(String clazz) {
        if (initialised)
            throw new IllegalStateException();
        if (clazz == null)
            throw new NullPointerException();
        dropTableClasses.add(clazz);
    }

    
    
    void addAllowedParaClass(String clazz) {
        if (initialised)
            throw new IllegalStateException();
        if (clazz == null)
            throw new NullPointerException();
        allowedParaClasses.add(clazz);
    }

    void addAllowedPreClass(String clazz) {
        if (initialised)
            throw new IllegalStateException();
        if (clazz == null)
            throw new NullPointerException();
        allowedPreClasses.add(clazz);
    }

    void addAllowedOlClass(String clazz) {
        if ( initialised)
            throw new IllegalStateException();
        if (clazz == null)
            throw new NullPointerException();
        allowedOlClasses.add(clazz);
    }

    void addAllowedElement(String tagName, String[] attributes) {
        if (initialised)
            throw new IllegalStateException();
        if (tagName == null)
            throw new NullPointerException();

        ElementDescriptor descriptor = new ElementDescriptor(tagName);
        for (String attribute : attributes) {
            descriptor.addAttribute(attribute);
        }

        descriptors.put(tagName, descriptor);
    }

    void initialize() throws Exception {
        if (initialised)
            throw new IllegalStateException();
        // build our descriptor model:
        //  - retrieve the one for XHTML (so that we have information about content models)
        //  - filter it to only contain the elements the user configured
        Map<String, ElementDescriptor> full = new XhtmlDescriptorBuilder().build();
        relax(full);
        narrow(full, descriptors);
        descriptors = full;
        initialised = true;
    }

    /**
     * Modifies the full map so that it only contains elements and attributes
     * from the subset, but retains the child element information.
     */
    private void narrow(Map<String, ElementDescriptor> full, Map subset) {
        String[] fullKeys = full.keySet().toArray(new String[full.size()]);
        for (String fullKey : fullKeys) {
            if (!subset.containsKey(fullKey))
                full.remove(fullKey);
        }

        for (ElementDescriptor elementDescriptor : full.values()) {
            String[] childNames = elementDescriptor.getChildren().toArray(new String[0]);
            Set<String> newChilds = new HashSet<String>();
            for (String childName : childNames) {
                if (subset.containsKey(childName))
                    newChilds.add(childName);
            }
            elementDescriptor.setChildren(newChilds);
            elementDescriptor.setAttributes(((ElementDescriptor)subset.get(elementDescriptor.getName())).getAttributes());
        }
    }

    private void relax(Map<String, ElementDescriptor> descriptors) {
        // HTML doesn't allow ul's to be nested directly, but that's what all these HTML
        // editors create, so relax that restriction a bit
        ElementDescriptor ulDescriptor = descriptors.get("ul");
        if (ulDescriptor != null) {
            ulDescriptor.getChildren().add("ul");
            ulDescriptor.getChildren().add("ol");
        }

        ElementDescriptor olDescriptor = descriptors.get("ol");
        if (olDescriptor != null) {
            olDescriptor.getChildren().add("ul");
            olDescriptor.getChildren().add("ol");
        }

        // In fact, the gecko HTML editor can't even handle the correct thing, so
        // force ul/ul and ol/ol nesting
        ElementDescriptor liDescriptor = descriptors.get("li");
        if (liDescriptor != null) {
            liDescriptor.getChildren().remove("ul");
            liDescriptor.getChildren().remove("ol");
        }
    }

    public HtmlCleaner newHtmlCleaner() {
        return new HtmlCleaner(this);
    }

    void setImgAlternateSrcAttr(String name) {
        this.imgAlternateSrcAttr = name;
    }

    public void setLinkAlternateHrefAttr(String linkAlternateHrefAttr) {
        this.linkAlternateHrefAttr = linkAlternateHrefAttr;
    }
}
