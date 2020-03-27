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
package org.outerj.daisy.repository.commonimpl.variant;

import org.outerj.daisy.repository.variant.Branches;
import org.outerj.daisy.repository.variant.Branch;
import org.outerx.daisy.x10.BranchesDocument;
import org.outerx.daisy.x10.BranchDocument;

public class BranchesImpl implements Branches {
    private Branch[] branches;

    public BranchesImpl(Branch[] branches) {
        this.branches = branches;
    }

    public Branch[] getArray() {
        return branches;
    }

    public BranchesDocument getXml() {
        BranchesDocument branchesDocument = BranchesDocument.Factory.newInstance();
        BranchDocument.Branch[] branchesXml = new BranchDocument.Branch[branches.length];

        for (int i = 0; i < branches.length; i++) {
            branchesXml[i] = branches[i].getXml().getBranch();
        }

        branchesDocument.addNewBranches().setBranchArray(branchesXml);

        return branchesDocument;
    }

    public int size() {
        return branches.length;
    }
}
