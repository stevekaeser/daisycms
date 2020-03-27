/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.frontend.editor;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class ApplicationEntry implements Serializable {
    
    private static final long serialVersionUID = -2955330310976786634L;
    
    protected String title;
    protected final List<String> applicationPaths = new ArrayList<String>();
    protected String arguments;
    
    public ApplicationEntry(String title, String applicationPath, String arguments) {
        if (applicationPath == null && title == null) {
            throw new NullPointerException("applicationPath and title should not be both null");
        }
        if (title == null || title.trim().equals("")) {
            this.title = new File(applicationPath).getName();
        } else {
            this.title = title;
        }

        this.arguments = arguments;
        addApplication(applicationPath);
    }
    
    public String getTitle() {
        return title;
    }
    public String getApplication() {
        if  (applicationPaths.size() == 0)
            return null;
        return applicationPaths.get(0);
    }
    public String getArguments() {
        return arguments;
    }
    
    public void addApplication(String applicationPath) {
        if (applicationPath == null)
            return;
        applicationPaths.add(applicationPath);
    }
    public void removeApplication(String applicationPath) {
        applicationPaths.remove(applicationPath);
    }
    
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof ApplicationEntry) {
            ApplicationEntry otherApp = (ApplicationEntry)obj;
            
            return safeEquals(title, otherApp.title)
             && safeEquals(applicationPaths, otherApp.applicationPaths)
             && safeEquals(arguments, otherApp.arguments);
            
        }
        
        return false;
    }
    
    private boolean safeEquals(Object o1, Object o2) {
        if (o1 == null && o1 != o2)
            return false;
        if (o2 == null)
            return false;
        return o1.equals(o2);
    }

    public int hashCode() {
        // The calculation technique for this hashcode is taken from the HashCodeBuilder
        // of Jakarta Commons Lang, which in itself is based on techniques from the
        // "Effective Java" book by Joshua Bloch.
        final int iConstant = 159;
        int iTotal = 615;

        iTotal = iTotal * iConstant + safeHash(title);
        iTotal = appendHash(safeHash(applicationPaths), iTotal, iConstant);
        iTotal = appendHash(safeHash(arguments), iTotal, iConstant);

        return iTotal;
    }
    
    public int safeHash(Object o) {
        if (o == null) return 137;
        else return o.hashCode();
    }
    
    private int appendHash(long value, int iTotal, int iConstant) {
        return iTotal * iConstant + ((int) (value ^ (value >> 32)));
    }
    
    public String toString() {
        return new StringBuffer("ApplicationDescription[title=").append(title)
            .append(",applicationPaths=").append(applicationPaths)
            .append(",arguments=").append(arguments).append("]").toString();
    }

    public void cleanUp() {
        replaceEnvironmentVariables(applicationPaths);
        
        removeNonExistingPaths(applicationPaths);
    }

    private void replaceEnvironmentVariables(List<String> paths) {
        Pattern envVar = Pattern.compile("%(^%)%");
        for (int i = 0; i < paths.size(); i++) {
            paths.set(i, SystemUtils.replaceEnvironmentVariables(paths.get(i)));
        }
    }

    private void removeNonExistingPaths(List<String> paths) {
        Iterator<String> pathIter = paths.iterator();
        while (pathIter.hasNext()) {
            String path = pathIter.next();
            if (!new File(path).exists() && !new File(path + ".app").exists()) {
                pathIter.remove();
            }
        }
    }

    public List<String> getApplicationPaths() {
        return (List<String>)Collections.unmodifiableList(applicationPaths);
    }
    
    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public boolean keep() {
        return !applicationPaths.isEmpty();
    }
    
    public ApplicationEntry copy() {
        return new ApplicationEntry(getTitle(), getApplication(), getArguments());
    }

}
