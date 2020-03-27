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
package org.outerj.daisy.repository;

import org.outerj.daisy.repository.acl.AclDetailPermission;

import java.util.*;

public class AccessDetailViolationException extends DocumentWriteDeniedException implements LocalizedException {
    private List<AccessDetailViolation> violations = new ArrayList<AccessDetailViolation>();

    private static final String VIOLATION_PREFIX = "perm.";

    public AccessDetailViolationException() {
    }

    public AccessDetailViolationException(Map<String, String> params) {
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (param.getKey().startsWith(VIOLATION_PREFIX)) {
                AclDetailPermission permission = AclDetailPermission.fromString(param.getKey().substring(VIOLATION_PREFIX.length()));
                String attribute = param.getValue().equals("") ? null : param.getValue();
                addViolation(permission, attribute);
            }
        }
    }

    @Override
    public Map<String, String> getState() {
        Map<String, String> state = new HashMap<String, String>();
        for (AccessDetailViolation violation : violations) {
            state.put(VIOLATION_PREFIX + violation.getViolatedPermission(), violation.getAttribute() == null ? "" : violation.getAttribute());
        }
        return state;
    }


    public String getMessage(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("org/outerj/daisy/repository/messages", locale);

        StringBuilder message = new StringBuilder();
        message.append(bundle.getString("access-detail-violation-exception")).append(" ");

        boolean first = true;
        for (AccessDetailViolation violation : violations) {
            if (first)
                first = false;
            else
                message.append(", ");

            message.append(bundle.getString("writedetail." + violation.getViolatedPermission()));
            if (violation.getAttribute() != null)
                message.append(" ").append(violation.getAttribute());
        }

        return message.toString();
    }

    @Override
    public String getMessage() {
        return getMessage(Locale.getDefault());
    }

    public void addViolation(AclDetailPermission violatedPermission, String attribute) {
        violations.add(new AccessDetailViolation(violatedPermission, attribute));
    }

    public boolean isEmpty() {
        return violations.isEmpty();
    }

    public class AccessDetailViolation {
        private AclDetailPermission violatedPermission;
        private String attribute;

        public AccessDetailViolation(AclDetailPermission violatedPermission, String attribute) {
            this.violatedPermission = violatedPermission;
            this.attribute = attribute;
        }

        public AclDetailPermission getViolatedPermission() {
            return violatedPermission;
        }

        public String getAttribute() {
            return attribute;
        }
    }
}
