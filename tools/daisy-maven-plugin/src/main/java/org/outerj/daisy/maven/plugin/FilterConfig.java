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
package org.outerj.daisy.maven.plugin;

import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ClassifierFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ProjectTransitivityFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.artifact.filter.collection.TypeFilter;

public class FilterConfig {
    /**
     * If we should exclude transitive dependencies
     *
     * @since 2.0
     * @optional
     * @parameter expression="${excludeTransitive}" default-value="false"
     */
    protected boolean excludeTransitive;

    /**
     * Comma Separated list of Types to include. Empty String indicates include
     * everything (default).
     *
     * @since 2.0
     * @parameter expression="${includeTypes}" default-value=""
     * @optional
     */
    protected String includeTypes;

    /**
     * Comma Separated list of Types to exclude. Empty String indicates don't
     * exclude anything (default). Ignored if includeTypes is used.
     *
     * @since 2.0
     * @parameter expression="${excludeTypes}" default-value=""
     * @optional
     */
    protected String excludeTypes;

    /**
     * Scope to include. An Empty string indicates all scopes (default).
     *
     * @since 2.0
     * @parameter expression="${includeScope}" default-value="runtime"
     * @optional
     */
    protected String includeScope;

    /**
     * Scope to exclude. An Empty string indicates no scopes (default). Ignored
     * if includeScope is used.
     *
     * @since 2.0
     * @parameter expression="${excludeScope}" default-value=""
     * @optional
     */
    protected String excludeScope;

    /**
     * Comma Separated list of Classifiers to include. Empty String indicates
     * include everything (default).
     *
     * @since 2.0
     * @parameter expression="${includeClassifiers}" default-value=""
     * @optional
     */
    protected String includeClassifiers;

    /**
     * Comma Separated list of Classifiers to exclude. Empty String indicates
     * don't exclude anything (default). Ignored if includeClassifiers is used.
     *
     * @since 2.0
     * @parameter expression="${excludeClassifiers}" default-value=""
     * @optional
     */
    protected String excludeClassifiers;

    /**
     * Comma Seperated list of Artifact names too exclude. Ignored if
     * includeArtifacts is used.
     *
     * @since 2.0
     * @optional
     * @parameter expression="${excludeArtifactIds}" default-value=""
     */
    protected String excludeArtifactIds;

    /**
     * Comma Seperated list of Artifact names to include.
     *
     * @since 2.0
     * @optional
     * @parameter expression="${includeArtifactIds}" default-value=""
     */
    protected String includeArtifactIds;

    /**
     * Comma Seperated list of GroupId Names to exclude. Ignored if
     * includeGroupsIds is used.
     *
     * @since 2.0
     * @optional
     * @parameter expression="${excludeGroupIds}" default-value=""
     */
    protected String excludeGroupIds;

    /**
     * Comma Seperated list of GroupIds to include.
     *
     * @since 2.0
     * @optional
     * @parameter expression="${includeGroupIds}" default-value=""
     */
    protected String includeGroupIds;
    
    public boolean isExcludeTransitive() {
        return excludeTransitive;
    }

    public String getIncludeTypes() {
        return includeTypes;
    }

    public String getExcludeTypes() {
        return excludeTypes;
    }

    public String getIncludeScope() {
        return includeScope;
    }

    public String getExcludeScope() {
        return excludeScope;
    }

    public String getIncludeClassifiers() {
        return includeClassifiers;
    }

    public String getExcludeClassifiers() {
        return excludeClassifiers;
    }

    public String getExcludeArtifactIds() {
        return excludeArtifactIds;
    }

    public String getIncludeArtifactIds() {
        return includeArtifactIds;
    }

    public String getExcludeGroupIds() {
        return excludeGroupIds;
    }

    public String getIncludeGroupIds() {
        return includeGroupIds;
    }

    public FilterArtifacts toFilter(Set<Artifact> artifacts) {
        FilterArtifacts filter = new FilterArtifacts();

        filter.addFilter(new ProjectTransitivityFilter(artifacts, this.excludeTransitive));
        filter.addFilter(new ScopeFilter(this.includeScope, this.excludeScope));
        filter.addFilter(new TypeFilter(this.includeTypes, this.excludeTypes));
        filter.addFilter(new ClassifierFilter(this.includeClassifiers, this.excludeClassifiers));
        filter.addFilter(new GroupIdFilter(this.includeGroupIds, this.excludeGroupIds));
        filter.addFilter(new ArtifactIdFilter(this.includeArtifactIds, this.excludeArtifactIds));
        
        return filter;
    }
}
