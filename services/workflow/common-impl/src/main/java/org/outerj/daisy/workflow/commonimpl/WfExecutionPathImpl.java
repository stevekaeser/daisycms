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

import org.outerj.daisy.workflow.WfExecutionPath;
import org.outerj.daisy.workflow.WfNodeDefinition;
import org.outerx.daisy.x10Workflow.ExecutionPathDocument;

import java.util.*;

public class WfExecutionPathImpl implements WfExecutionPath {
    private String path;
    private Date start;
    private Date end;
    private Collection<WfExecutionPath> children;
    private WfNodeDefinition node;

    public WfExecutionPathImpl(String path, Date start, Date end, Collection<WfExecutionPath> children, WfNodeDefinition node) {
        if (path == null)
            throw new IllegalArgumentException("Null argument: path");
        if (start == null)
            throw new IllegalArgumentException("Null argument: start");
        if (children == null)
            throw new IllegalArgumentException("Null argument: children");
        if (node == null)
            throw new IllegalArgumentException("Null argument: node");

        this.path = path;
        this.start = start;
        this.end = end;
        this.children = Collections.unmodifiableCollection(children);
        this.node = node;
    }

    public String getPath() {
        return path;
    }

    public Date getStart() {
        return (Date)start.clone();
    }

    public Date getEnd() {
        return end != null ? (Date)end.clone() : null;
    }

    public Collection<WfExecutionPath> getChildren() {
        return children;
    }

    public WfNodeDefinition getNode() {
        return node;
    }

    public ExecutionPathDocument getXml() {
        ExecutionPathDocument document = ExecutionPathDocument.Factory.newInstance();
        ExecutionPathDocument.ExecutionPath xml = document.addNewExecutionPath();
        xml.setPath(path);
        xml.setStart(WfXmlHelper.getCalendar(start));
        if (end != null)
            xml.setEnd(WfXmlHelper.getCalendar(end));

        ExecutionPathDocument.ExecutionPath[] pathsXml = new ExecutionPathDocument.ExecutionPath[children.size()];
        int i = 0;
        for (WfExecutionPath path : children) {
            pathsXml[i] = path.getXml().getExecutionPath();
            i++;
        }
        xml.addNewChildren().setExecutionPathArray(pathsXml);

        xml.setNodeDefinition(node.getXml().getNodeDefinition());

        return document;
    }
}
