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
package org.outerj.daisy.query.model;

import org.outerj.daisy.repository.query.QueryException;

public class ExprDocDataMissingException extends QueryException {
    private String name;
    private String location;

    public ExprDocDataMissingException(String name, String location) {
        this.name = name;
        this.location = location;
    }


    public String getMessage() {
        return name + " is used in a location where it cannot be evaluated because no document is available, at " + location;
    }
}
