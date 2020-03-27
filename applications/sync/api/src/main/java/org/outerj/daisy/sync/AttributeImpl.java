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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AttributeImpl implements Attribute {
  private static final long serialVersionUID = 7398849578555341718L;

  private String externalName;

  private List<String> values;

  private String daisyName;

  private AttributeType type;

  private Entity entity;

  private boolean isMultivalue;

  public AttributeImpl() {
    this(null, null);
  }

  public AttributeImpl(String name, String value) {
    this(name, value, null, null);
  }

  public AttributeImpl(String externalName, String value, String internalName, AttributeType type) {
    this.externalName = externalName;
    if (value != null)
      addValue(value);
    this.daisyName = internalName;
    this.type = type;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.outerj.daisy.sync.Attribute#getExternalName()
   */
  public String getExternalName() {
    return externalName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.outerj.daisy.sync.Attribute#getValues()
   */
  public List<String> getValues() {
    return values;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.outerj.daisy.sync.Attribute#getDaisyName()
   */
  public String getDaisyName() {
    return daisyName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.outerj.daisy.sync.Attribute#setDaisyName(java.lang.String)
   */
  public void setDaisyName(String daisyFieldName) {
    if (entity != null) {
      entity.removeAttribute(this);
      this.daisyName = daisyFieldName;
      entity.addAttribute(this);
    } else {
      this.daisyName = daisyFieldName;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.outerj.daisy.sync.Attribute#setType(org.outerj.daisy.sync.AttributeImpl.AttributeType)
   */
  public void setType(AttributeType type) throws Exception {
    if (isMultivalue && type != AttributeType.FIELD)
      throw new Exception("Only attributes of the " + AttributeType.FIELD.toString() + " may be multivalued");

    if (entity != null) {
      entity.removeAttribute(this);
      this.type = type;
      entity.addAttribute(this);
    } else {
      this.type = type;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.outerj.daisy.sync.Attribute#setExternalName(java.lang.String)
   */
  public void setExternalName(String name) {
    if (entity != null) {
      entity.removeAttribute(this);
      this.externalName = name;
      entity.addAttribute(this);
    } else {
      this.externalName = name;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.outerj.daisy.sync.Attribute#addValue(java.lang.String)
   */
  public void addValue(String value) {
    if (values == null)
      values = new ArrayList<String>();
    if (isMultivalue)
      values.add(value);
    else {
      StringBuffer s = new StringBuffer();
      if (values.size() > 0) {
        s.append(values.get(0));
        s.append(" ");
        this.values.remove(0);
      }
      s.append(value);
      values.add(0, s.toString());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.outerj.daisy.sync.Attribute#addAllValues(java.util.Collection)
   */
  public void addAllValues(Collection<String> values) {
    if (values != null) {
      if (this.values == null)
        this.values = new ArrayList<String>();
      if (isMultivalue)
        this.values.addAll(values);
      else {
        StringBuffer s = new StringBuffer();
        if (this.values.size() > 0) {
          s.append(this.values.get(0));
          this.values.remove(0);
        }
        for (String value : values) {
          if (s.length() > 0)
            s.append(" ");
          s.append(value);
        }
        this.values.add(0, s.toString());
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.outerj.daisy.sync.Attribute#setValues(java.util.List)
   */
  public void setValues(List<String> values) {
    this.values = null;
    addAllValues(values);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.outerj.daisy.sync.Attribute#getType()
   */
  public AttributeType getType() {
    return type;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.outerj.daisy.sync.Attribute#getEntity()
   */
  public Entity getEntity() {
    return entity;
  }

  // TODO might look into making this private
  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append(this.externalName);
    buffer.append(", ");
    buffer.append(this.daisyName);

    return buffer.toString();
  }

  public boolean isMultivalue() {
    return isMultivalue;
  }

  public void setMultivalue(boolean isMultivalue) throws Exception {
    if (isMultivalue && this.type != AttributeType.FIELD)
      throw new Exception("Only attributes with type " + AttributeType.FIELD.toString() + " may be multivalue");
    this.isMultivalue = isMultivalue;
  }

  
  public Attribute clone() {
        AttributeImpl attrCopy = null;
        try {
            attrCopy = new AttributeImpl(this.externalName, null, this.daisyName, this.type);
            attrCopy.setMultivalue(this.isMultivalue);
            if (this.values != null) {
                attrCopy.addAllValues(this.values);
            }
        } catch (Exception e) {
            throw new RuntimeException("This shouldn't happen during a clone", e);
        }

        return attrCopy;
    }

}
