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
package org.outerj.daisy.workflow.commonimpl;


public class WfMultiValue {
    
    private long id;
    
    private WfMultiValueUnit[] values;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public WfMultiValueUnit[] getValues() {
        return values;
    }

    public void setValues(WfMultiValueUnit[] values) {
        this.values = values;
    }

    public Object[] unwrap() {
        Object[] result = new Object[values.length];
        for (int i=0; i<values.length; i++) { // TODO: add a "class" and "convertor" field to WfMultiValue, which would work similar to the class and convertor in jbpm's variable table
            result[i] = values[i].getStringValue();
        }
        return result;
    }

}
