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
package org.outerj.daisy.sync;


import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;



public interface Attribute extends Serializable, Cloneable{

  public abstract String getExternalName();

  public abstract List<String> getValues();

  public abstract String getDaisyName();

  public abstract void setDaisyName(String daisyFieldName);

  public abstract void setType(AttributeType type) throws Exception;

  public abstract void setExternalName(String name);

  public abstract void addValue(String value);

  public abstract void addAllValues(Collection<String> values);

  public abstract void setValues(List<String> values);

  public abstract AttributeType getType();

  public abstract Entity getEntity();
  
  public abstract void setEntity(Entity entity);
  
  public abstract void setMultivalue(boolean isMultivalue) throws Exception;
  
  public abstract boolean isMultivalue();

  public abstract Attribute clone();
  
  public class AttributeNameComparator implements Comparator<Attribute> {

    public int compare(Attribute o1, Attribute o2) {    
        String name1 = o1.getExternalName() != null ? o1.getExternalName() : o1.getDaisyName();
        String name2 = o2.getExternalName() != null ? o2.getExternalName() : o2.getDaisyName();
        
        return name1.compareToIgnoreCase(name2);
    }
  }
}