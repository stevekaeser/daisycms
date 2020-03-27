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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.map.MultiKeyMap;
import org.outerj.daisy.repository.VariantKey;

public class EntityImpl implements Entity {
	private static final long serialVersionUID = -5566169985039728801L;

	private String name;

	private String language;

	private long externalId;

	private Date externalLastModified;

	private boolean externalDeleted = false;

	private String internalName;

	private SerializableVariantKey daisyVariantKey;

	private long daisyVersion;

	private boolean daisyDeleted = false;

	private Date updateTimestamp = new Date();

	private SyncState state;

	private Map<String, Attribute> nameAttributes = new HashMap<String, Attribute>();

	private MultiKeyMap daisyTypedAttributes = new MultiKeyMap();

	public EntityImpl() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#isDaisyDeleted()
	 */
	public boolean isDaisyDeleted() {
		return daisyDeleted;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#setDaisyDeleted(boolean)
	 */
	public void setDaisyDeleted(boolean daisyDeleted) {
		this.daisyDeleted = daisyDeleted;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#getDaisyVariantKey()
	 */
	public VariantKey getDaisyVariantKey() {
		if (daisyVariantKey != null)
			return daisyVariantKey.toVariantKey();
		else
			return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#setDaisyVariantKey(org.outerj.daisy.repository.VariantKey)
	 */
	public void setDaisyVariantKey(VariantKey daisyVariantKey) {
		if (daisyVariantKey != null) {
			this.daisyVariantKey = new SerializableVariantKey(daisyVariantKey);
		} else {
			this.daisyVariantKey = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#getDaisyVersion()
	 */
	public long getDaisyVersion() {
		return daisyVersion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#setDaisyVersion(long)
	 */
	public void setDaisyVersion(long daisyVersion) {
		this.daisyVersion = daisyVersion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#isExternalDeleted()
	 */
	public boolean isExternalDeleted() {
		return externalDeleted;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#setExternalDeleted(boolean)
	 */
	public void setExternalDeleted(boolean externalDeleted) {
		this.externalDeleted = externalDeleted;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#getExternalLastModified()
	 */
	public Date getExternalLastModified() {
		return externalLastModified;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#setExternalLastModified(java.util.Date)
	 */
	public void setExternalLastModified(Date externalLastModified) {
		this.externalLastModified = externalLastModified;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#getState()
	 */
	public SyncState getState() {
		return state;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#setState(org.outerj.daisy.sync.EntityImpl.SyncState)
	 */
	public void setState(SyncState state) {
		this.state = state;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#getUpdateTimestamp()
	 */
	public Date getUpdateTimestamp() {
		return updateTimestamp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#setUpdateTimestamp(java.util.Date)
	 */
	public void setUpdateTimestamp(Date updateTimestamp) {
		this.updateTimestamp = updateTimestamp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#getName()
	 */
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#getExternalId()
	 */
	public long getExternalId() {
		return externalId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#getLastModified()
	 */
	public Date getLastModified() {
		return externalLastModified;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#addAttribute(org.outerj.daisy.sync.Attribute)
	 */
	public void addAttribute(Attribute attribute) {
		attribute.setEntity(this);
		if (attribute.getExternalName() != null)
			nameAttributes.put(attribute.getExternalName(), attribute);
		if (attribute.getDaisyName() != null && attribute.getType() != null)
			daisyTypedAttributes.put(attribute.getDaisyName(), attribute
					.getType(), attribute);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#removeAttribute(org.outerj.daisy.sync.Attribute)
	 */
	public void removeAttribute(Attribute attribute) {
		if (attribute.getExternalName() != null)
			nameAttributes.remove(attribute.getExternalName());
		if (attribute.getDaisyName() != null && attribute.getType() != null)
			daisyTypedAttributes.remove(attribute.getDaisyName(), attribute
					.getType());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#getAttributeByExternalName(java.lang.String)
	 */
	public Attribute getAttributeByExternalName(String externalname) {
		return nameAttributes.get(externalname);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#getAttributeByDaisyName(java.lang.String,
	 *      org.outerj.daisy.sync.Attribute.AttributeType)
	 */
	public Attribute getAttributeByDaisyName(String daisyName,
			AttributeType type) {
		return (Attribute) daisyTypedAttributes.get(daisyName, type);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#getAttributes()
	 */
	public Collection<Attribute> getAttributes() {
        
		Set<Attribute> attributes = new TreeSet<Attribute>(new Attribute.AttributeNameComparator());
        attributes.addAll(nameAttributes.values());
		attributes.addAll(daisyTypedAttributes.values());
		return attributes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#setAttributes(java.util.Collection)
	 */
	public void setAttributes(Collection<Attribute> attributes) {
		this.nameAttributes = new HashMap<String, Attribute>();
		this.daisyTypedAttributes = new MultiKeyMap();
		for (Attribute attribute : attributes) {
			addAttribute(attribute);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#setExternalId(long)
	 */
	public void setExternalId(long externalId) {
		this.externalId = externalId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#setName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#getInternalName()
	 */
	public String getInternalName() {
		return internalName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.outerj.daisy.sync.Entity#setInternalName(java.lang.String)
	 */
	public void setInternalName(String internalName) {
		this.internalName = internalName;
	}

	private class SerializableVariantKey implements Serializable,
			Comparable<SerializableVariantKey> {
		private static final long serialVersionUID = 3273083746583730695L;

		private final String documentId;

		private final long branchId;

		private final long languageId;

		public SerializableVariantKey(VariantKey key) {
			if (key != null) {
				this.documentId = key.getDocumentId();
				this.branchId = key.getBranchId();
				this.languageId = key.getLanguageId();
			} else {
				throw new NullPointerException("Variant key may not be null");
			}
		}

		public SerializableVariantKey(String documentId, long branchId,
				long languageId) {
			if (documentId == null)
				throw new IllegalArgumentException("documentId can not be null");
			this.documentId = documentId;
			this.branchId = branchId;
			this.languageId = languageId;
		}

		public String getDocumentId() {
			return documentId;
		}

		public long getBranchId() {
			return branchId;
		}

		public long getLanguageId() {
			return languageId;
		}

		public int compareTo(SerializableVariantKey o) {
			SerializableVariantKey otherKey = (SerializableVariantKey) o;
			int docCompareResult = documentId.compareTo(otherKey.documentId);
			if (docCompareResult == 0) {
				if (branchId == otherKey.branchId) {
					if (languageId == otherKey.languageId) {
						return 0;
					} else if (languageId < otherKey.languageId) {
						return -1;
					} else {
						return 1;
					}
				} else if (branchId < otherKey.branchId) {
					return -1;
				} else {
					return 1;
				}
			} else {
				return docCompareResult;
			}
		}

		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			} else if (obj instanceof SerializableVariantKey) {
				SerializableVariantKey otherKey = (SerializableVariantKey) obj;
				return (this.documentId.equals(otherKey.documentId)
						&& this.branchId == otherKey.branchId && this.languageId == otherKey.languageId);
			}

			return false;
		}

		public int hashCode() {
			// The calculation technique for this hashcode is taken from the
			// HashCodeBuilder
			// of Jakarta Commons Lang, which in itself is based on techniques
			// from the
			// "Effective Java" book by Joshua Bloch.
			final int iConstant = 159;
			int iTotal = 615;

			iTotal = iTotal * iConstant + documentId.hashCode();
			iTotal = appendHash(branchId, iTotal, iConstant);
			iTotal = appendHash(languageId, iTotal, iConstant);

			return iTotal;
		}

		private int appendHash(long value, int iTotal, int iConstant) {
			return iTotal * iConstant + ((int) (value ^ (value >> 32)));
		}

		public String toString() {
			return " {document ID " + documentId + ", branch ID " + branchId
					+ ", language ID " + languageId + "}";
		}

		public VariantKey toVariantKey() {
			return new VariantKey(documentId, branchId, languageId);
		}
	}


	/***
	 * Check if entity contains at least one of the attributes specified in externalnames
	 * @param externalnames an array containing the externalnames
	 * @return
	 */
   public boolean containsAttributeExternalName(String[] externalnames) {
       if(externalnames == null)
           return true;
       for(String externalName : externalnames){
           if (nameAttributes.containsKey(externalName)) {
               return true;
           }
       }
        return false;
    }

	
	@Override
	public Entity clone() {
		EntityImpl entityCopy = null;
		try {
			entityCopy = (EntityImpl)super.clone();
			entityCopy.nameAttributes = new HashMap<String, Attribute>();
			entityCopy.daisyTypedAttributes = new MultiKeyMap();
			for (Attribute attr : this.getAttributes()) {
				Attribute attrCopy = attr.clone();
				attrCopy.setEntity(entityCopy);
				entityCopy.addAttribute(attrCopy);
			}
		}catch (CloneNotSupportedException e) {
			// Shouldn't happen
			throw new InternalError("help");
		}
		return entityCopy;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

}
