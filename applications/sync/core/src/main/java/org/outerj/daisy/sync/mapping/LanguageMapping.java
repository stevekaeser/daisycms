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
package org.outerj.daisy.sync.mapping;

import java.util.ArrayList;
import java.util.List;

import org.outerj.daisy.sync.Entity;

public class LanguageMapping implements Mapping {
	private String languageName;
	// to map this language, at least one of these fields should be filled in 
	private String[] requiredAttributes;

    private List<Mapping> childMappings = new ArrayList<Mapping>();

	public LanguageMapping(String name, String[] requiredFields) {
		this.languageName = name;
		this.requiredAttributes = requiredFields;
	}

   public LanguageMapping(String name, String requiredFields) {
        this.languageName = name;
        String patternStr = "[, ]+";
        this.requiredAttributes = requiredFields!=null?requiredFields.split(patternStr):null;
    }
	
	public void addChildMapping(Mapping mapping) {
		childMappings.add(mapping);
	}

	public void applyMapping(Entity entity) throws MappingException {
		entity.setLanguage(languageName);

		for (Mapping mapping : childMappings) {
			mapping.applyMapping(entity);
		}
	}

	public String getName() {
		return languageName;
	}

	public void setName(String name) {
		this.languageName = name;
	}

    public String[] getRequiredFields() {
        return requiredAttributes;
    }


	public void setRequiredFields(String[] requiredFields) {
        this.requiredAttributes = requiredFields;
    }

    public List<Mapping> getChildMappings() {
		return childMappings;
	}

	public void setChildMappings(List<Mapping> childMappings) {
		this.childMappings = childMappings;
	}

}
