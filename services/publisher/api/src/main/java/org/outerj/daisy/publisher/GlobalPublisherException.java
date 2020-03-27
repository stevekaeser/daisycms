/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.publisher;

import org.outerj.daisy.repository.RepositoryException;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Exception wrapped around any other exception that is thrown while executing
 * a publisher request. The publisher has one global try-catch around the execution,
 * where it will wrap any occured exception in an instance of this exception.
 *
 * <p>A exception chain will never contain more than one instance of GlobalPublisherException,
 * at least not as far as produced by the publisher.
 *
 * <p>The GlobalPublisherException provides a publisher execution stack on the moment
 * the exception occured.
 */
public class GlobalPublisherException  extends RepositoryException {
    private String message;
    private List<String> locationStack;

    public GlobalPublisherException(String message, List<String> locationStack, Throwable cause) {
        super(cause);
        this.message = message;
        this.locationStack = locationStack;
    }

    public GlobalPublisherException(Map state) {
        this.message = (String)state.get("message");

        int i = 1;
        String location;
        while ((location = (String)state.get("location." + i)) != null) {
            if (locationStack == null)
                locationStack = new ArrayList<String>();
            locationStack.add(location);
            i++;
        }
    }

    public Map<String, String> getState() {
        Map<String, String> state = new HashMap<String, String>();

        if (message != null)
            state.put("message", message);

        if (locationStack != null) {
            for (int i = 0 ; i < locationStack.size(); i++) {
                state.put("location." + (i + 1), locationStack.get(i));
            }
        }

        return state;
    }

    public String getMessage() {
        if (locationStack == null) {
            return getRawMessage();
        } else {
            StringBuilder locationMessage = new StringBuilder();
            for (String location : locationStack) {
                if (locationMessage.length() > 0)
                    locationMessage.append(", at ");
                locationMessage.append(location);
            }
            return message + " Publisher execution stack: " + locationMessage;
        }
    }

    public String getRawMessage() {
        return message;
    }

    public List<String> getLocationStack() {
        return locationStack;
    }
}

