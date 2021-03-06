/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.repository;

import java.util.Map;

/**
 * Thrown if something goes wrong in the repository.
 */
public class RepositoryException extends Exception {
    public RepositoryException() {
        super();
    }
    
    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RepositoryException(Throwable cause) {
        super(cause);
    }

    /**
     * Exceptions which can externalize their state as a map containing
     * string-string pairs can override this method to do so. In that
     * case, they should also have a constructor taking a Map as argument
     * allowing reconstruction of the exception.
     *
     * <p>This is used to reconstruct server-side exceptions in the repository
     * client.
     */
    public Map<String, String> getState() {
        return null;
    }
}
