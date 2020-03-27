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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MimeTypeEntry implements Serializable {
    
    private static final long serialVersionUID = 5365839018258909552L;
    
    private Set<String> titles;
    private List<ApplicationEntry> applications;
    private ApplicationEntry defaultApplicationEntry;
    
    public MimeTypeEntry() {
        this.titles = new HashSet<String>();
        this.applications = new ArrayList<ApplicationEntry>();
    }

    /**
     * Get the default application (the first one in the list)
     * @return
     */
    public ApplicationEntry getDefaultApplication() {
        return defaultApplicationEntry;
    }

    /**
     * Sets the default application (i.e. make sure the entry becomes the first in the list) 
     * @param applicationEntry
     */
    public void setDefaultApplicationEntry(ApplicationEntry applicationEntry) {
        defaultApplicationEntry = applicationEntry;
        
        if  (applicationEntry != null && !titles.contains(applicationEntry.getTitle())) {
            titles.add(applicationEntry.getTitle());
            applications.add(0, applicationEntry);
        } else {
            for (Iterator<ApplicationEntry> it = applications.iterator(); it.hasNext();) {
                ApplicationEntry entry = it.next();
                if (entry.getTitle().equals(applicationEntry.getTitle())) {
                    it.remove();
                }
            }
            applications.add(0, applicationEntry);
        }
        
    }

    /**
     * These methods are used when loading defaults.
     * @param application
     */
    public void addApplicationEntry(ApplicationEntry applicationEntry) {
        if  (applicationEntry != null && !titles.contains(applicationEntry.getTitle())) {
            titles.add(applicationEntry.getTitle());
            applications.add(applicationEntry);
        }
    }

    /**
     * Removes applications and icons that are not found in the list
     */
    public void cleanUp() {
        Iterator<ApplicationEntry> appIter = applications.iterator();
        while ( appIter.hasNext() ) {
            ApplicationEntry entry = appIter.next();
            entry.cleanUp();
            if (!entry.keep()) {
                titles.remove(entry.getTitle());
                appIter.remove();
            }
        }
        if ( !applications.contains(defaultApplicationEntry)) {
            defaultApplicationEntry = null;
        }
    }
    
    public List<ApplicationEntry> getApplicationEntries() {
        return Collections.unmodifiableList(applications);
    }
    
    public Set<String> getTitles() {
        return Collections.unmodifiableSet(titles);
    }
    
    public MimeTypeEntry deepCopy() {
        MimeTypeEntry copy = new MimeTypeEntry();
        copy.applications = new ArrayList();
        for (ApplicationEntry app: applications) {
            copy.titles.add(app.getTitle());
            copy.applications.add(app.copy());
        }
        if (defaultApplicationEntry != null) {
            copy.defaultApplicationEntry = defaultApplicationEntry.copy();
        }
        return copy;
    }

    public ApplicationEntry createApplicationEntry(String title,
            String application, String arguments) {

        if (title.trim().length() == 0) {
            title = new File(application).getName();
        }
        String newTitle = title;
        int i = 2;
        while (titles.contains(newTitle)) {
            newTitle = title + " ("+ (i++) + ")";
        }
        return new ApplicationEntry(newTitle, application, arguments); 
    }
    
}
