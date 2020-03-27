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
import java.util.Date;

import org.outerj.daisy.repository.VariantKey;

public interface Entity extends Serializable, Cloneable{

  public abstract boolean isDaisyDeleted();

  public abstract void setDaisyDeleted(boolean daisyDeleted);

  public abstract VariantKey getDaisyVariantKey();

  public abstract void setDaisyVariantKey(VariantKey daisyVariantKey);

  public abstract long getDaisyVersion();

  public abstract void setDaisyVersion(long daisyVersion);

  public abstract boolean isExternalDeleted();

  public abstract void setExternalDeleted(boolean externalDeleted);

  public abstract Date getExternalLastModified();

  public abstract void setExternalLastModified(Date externalLastModified);

  public abstract SyncState getState();

  public abstract void setState(SyncState state);

  public abstract Date getUpdateTimestamp();

  public abstract void setUpdateTimestamp(Date updateTimestamp);

  public abstract String getName();

  public abstract long getExternalId();

  public abstract Date getLastModified();

  public abstract void addAttribute(Attribute attribute);

  public abstract void removeAttribute(Attribute attribute);

  public abstract Attribute getAttributeByExternalName(String externalname);

  public abstract Attribute getAttributeByDaisyName(String daisyName, AttributeType type);

  public abstract Collection<Attribute> getAttributes();

  public abstract void setAttributes(Collection<Attribute> attributes);

  public abstract void setExternalId(long externalId);

  public abstract void setName(String name);

  public abstract String getInternalName();

  public abstract void setInternalName(String internalName);
  
  public abstract String getLanguage();
  
  public abstract void setLanguage(String language);
  
  public abstract boolean containsAttributeExternalName(String[] externalnames);
  
  public abstract Entity clone();

}