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

import java.util.Comparator;
import java.util.Iterator;

public class EntityValueComparator implements Comparator<Entity> {

  public int compare(Entity e1, Entity e2) {
    int comp = e1.getAttributes().size() == e2.getAttributes().size() ? 0 : -1;
    Iterator<Attribute> attributeIterator = e1.getAttributes().iterator();
    while(attributeIterator.hasNext() && comp == 0) {
      Attribute attr1 = attributeIterator.next();
      Attribute attr2 = e2.getAttributeByDaisyName(attr1.getDaisyName(), attr1.getType());
      if (attr2 == null){
        attr2 = e2.getAttributeByExternalName(attr1.getExternalName());
        if (attr2 == null){
            comp = -1;
            continue;
        }            
      }
      if (attr1.getValues() != null && attr2.getValues() != null) {      
        comp = attr1.getValues().size() == attr2.getValues().size() ? 0 : 
        (attr1.getValues().size() < attr2.getValues().size() ? -1 : 1);
        if (comp == 0)
          comp = attr1.getValues().containsAll(attr2.getValues()) ? 0 : -1;
      } else {
        if (attr1.getValues() == null && attr2.getValues() == null)
          comp = 0;
        else
          comp = -1;
      }
      
       
    }
    return comp;
  }



}
