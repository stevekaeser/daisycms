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
package org.outerj.daisy.runtime;

import org.outerj.daisy.runtime.component.ContainerConfig;
import org.outerj.daisy.runtime.classloading.ClassPathEntry;
import org.outerj.daisy.runtime.classloading.ArtifactSharingMode;
import org.outerj.daisy.runtime.repository.ArtifactRef;
import org.outerj.daisy.runtime.cli.Logging;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 *
 * Implementation note: if multiple threads would concurrently use this class,
 * the log output of the different threads could produce meaningless interfered
 * results (if needed, can be easily fixed by outputting related things as one
 * log statement, but for current usage this is unneeded).
 */
class ClassLoaderConfigurer {
    private List<ContainerConfig> containerConfigs;
    private boolean enableSharing;
    private Map<String, ArtifactHolder> artifacts = new HashMap<String, ArtifactHolder>();
    private List<ArtifactRef> sharedArtifacts = new ArrayList<ArtifactRef>();

    private final Log classLoadingLog = LogFactory.getLog(Logging.CLASSLOADING_LOG_CATEGORY);
    private final Log reportLog = LogFactory.getLog(Logging.CLASSLOADING_REPORT_CATEGORY);

    /**
     * Checks the class loader configurations of the containers (throws Exceptions in case of errors),
     * builds and returns a list of shareable artifacts, and adjust the class loader configurations
     * by marking the shareable artifacts.
     */
    public static List<ArtifactRef> configureClassPaths(List<ContainerConfig> containerConfigs, boolean enableSharing) {
        return new ClassLoaderConfigurer(containerConfigs, enableSharing).configure();
    }

    private ClassLoaderConfigurer(List<ContainerConfig> containerConfigs, boolean enableSharing) {
        this.containerConfigs = containerConfigs;
        this.enableSharing = enableSharing;
    }

    public List<ArtifactRef> configure() {
        buildInverseIndex();
        handleArtifacts();
        logReport();
        return sharedArtifacts;
    }

    private void handleArtifacts() {
        for(ArtifactHolder holder : artifacts.values()) {
            handleArtifact(holder);
        }
    }

    private void handleArtifact(ArtifactHolder holder) {
        // If the artifact is share-required by some containers:
        //    - check everyone uses the same version
        //    - check that if other containers have this artifact as shared or private dependency, they are also all of the same version
        //    - put the artifact in the shared classloader and remove it from the individual containers
        if (holder.required.size() > 0) {
            Set<String> versions = new HashSet<String>();
            for (ArtifactUser user : holder.required) {
                versions.add(user.version);
            }

            if (versions.size() > 1) {
                classLoadingLog.error("Multiple containers use different versions of the share-required artifact " + holder);
                for (ArtifactUser user : holder.required) {
                    classLoadingLog.error("  version " + user.version + " by " + user.container.getId());
                }

                throw new DaisyRTException("Multiple containers use different versions of the share-required artifact " + holder + ". Enable classloading logging to see details.");
            }

            for (ArtifactUser user : holder.allowed) {
                versions.add(user.version);
            }
            for (ArtifactUser user : holder.prohibited) {
                versions.add(user.version);
                // we don't consider this a fatal error (for now)
                classLoadingLog.warn("Artifact required for sharing " + holder + " is also a prohibited from sharing by " + user.container.getId());
            }

            if (versions.size() > 1) {
                throw new DaisyRTException("The artifact " + holder + " is required for sharing by one or more containers but other containers use a different version of this artifact with allowed or prohibited sharing. Enable classloading logging to see details.");
            }

            ArtifactRef ref = new ArtifactRef(holder.groupId, holder.artifactId, versions.iterator().next());
            sharedArtifacts.add(ref);

            for (ArtifactUser user : holder.required)
                user.container.getClassLoadingConfig().enableSharing(ref);
            for (ArtifactUser user : holder.allowed)
                user.container.getClassLoadingConfig().enableSharing(ref);
            for (ArtifactUser user : holder.prohibited)
                user.container.getClassLoadingConfig().enableSharing(ref);

        } else if (holder.allowed.size() > 0) {
            Set<String> versions = new HashSet<String>();
            for (ArtifactUser user : holder.allowed)
                versions.add(user.version);
            for (ArtifactUser user : holder.prohibited) {
                classLoadingLog.warn("Allowed-for-sharing artifact " + holder + " is also a prohibited-from-sharing dependency of " + user.container.getId());
                versions.add(user.version);
            }

            if (versions.size() == 1 && enableSharing && (holder.allowed.size() + holder.prohibited.size() > 1)) {
                // everyone uses same version, artifact can be in shared classloader
                classLoadingLog.info("All containers use the same version of the allowed-for-sharing artifact " + holder + ", so adding it to the common classloader.");
                ArtifactRef ref = new ArtifactRef(holder.groupId, holder.artifactId, versions.iterator().next());
                sharedArtifacts.add(ref);

                for (ArtifactUser user : holder.allowed)
                    user.container.getClassLoadingConfig().enableSharing(ref);
                for (ArtifactUser user : holder.prohibited)
                    user.container.getClassLoadingConfig().enableSharing(ref);
            } else if (versions.size() == 1 && enableSharing) {
                classLoadingLog.info("Shareable artifact " + holder + " is only used by one container, so won't add it to the common classloader.");
            } else if (versions.size() == 1 && !enableSharing) {
                classLoadingLog.info("All containers use the same version of the shareable artifact " + holder + ", if sharing wasn't disabled it would be adding to the common classloader.");
            } else {
                classLoadingLog.info("Multiple versions in use of shareable artifact " + holder + ", hence not adding it to the common classloader.");
            }
        } else if (holder.prohibited.size() > 0) {
            Set<String> versions = new HashSet<String>();
            for (ArtifactUser user : holder.prohibited) versions.add(user.version);

            if (versions.size() == 1 && holder.prohibited.size() > 1) {
                // log info:
                classLoadingLog.info("Multiple containers have a sharing-prohibited dependency on artifact " + holder + " and all use the same version. It might make sense to allow sharing the dependency.");
                for (ArtifactUser user : holder.prohibited)
                    classLoadingLog.info("  " + user.container.getId());
            }
        }
    }

    /**
     * Builds an index of artifacts with pointers to the containers that use them
     * (= the inverse of the list of containers having the list of artifacts they use).
     */
    private void buildInverseIndex() {
        for (ContainerConfig containerConf : containerConfigs) {
            List<ClassPathEntry> classPathEntries = containerConf.getClassLoadingConfig().getEntries();
            for (ClassPathEntry entry : classPathEntries) {
                ArtifactRef artifact = entry.getArtifactRef();
                ArtifactHolder holder = getArtifactHolder(artifact);
                holder.add(entry.getSharingMode(), artifact.getVersion(), containerConf);
            }
        }
    }

    private ArtifactHolder getArtifactHolder(ArtifactRef artifact) {
        ArtifactHolder holder = artifacts.get(getKey(artifact));
        if (holder == null) {
            holder = new ArtifactHolder(artifact.getGroupId(), artifact.getArtifactId());
            artifacts.put(getKey(artifact), holder);
        }
        return holder;
    }

    private String getKey(ArtifactRef artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    private void logReport() {
        if (!reportLog.isInfoEnabled())
            return;

        reportLog.info("Common classpath:");
        for (ArtifactRef artifact : sharedArtifacts) {
            reportLog.info("  -> " + artifact.toString());
        }

        for (ContainerConfig containerConf : containerConfigs) {
            reportLog.info("Classpath of container " + containerConf.getId());
            List<ArtifactRef> artifacts = containerConf.getClassLoadingConfig().getUsedArtifacts();

            if (artifacts.isEmpty()) {
                reportLog.info("  (empty)");
            } else {
                for (ArtifactRef artifact : artifacts) {
                    reportLog.info("  -> " + artifact.toString());
                }
            }
        }
    }

    private static class ArtifactHolder {
        String groupId;
        String artifactId;
        List<ArtifactUser> required = new ArrayList<ArtifactUser>();
        List<ArtifactUser> allowed = new ArrayList<ArtifactUser>();
        List<ArtifactUser> prohibited = new ArrayList<ArtifactUser>();

        public ArtifactHolder(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        public void add(ArtifactSharingMode sharingMode, String version, ContainerConfig container) {
            switch (sharingMode) {
                case REQUIRED:
                    required.add(new ArtifactUser(version, container));
                    break;
                case ALLOWED:
                    allowed.add(new ArtifactUser(version, container));
                    break;
                case PROHIBITED:
                    prohibited.add(new ArtifactUser(version, container));
                    break;
            }
        }

        public String toString() {
            return groupId + ":" + artifactId;
        }
    }

    private static class ArtifactUser {
        String version;
        ContainerConfig container;

        public ArtifactUser(String version, ContainerConfig container) {
            this.version = version;
            this.container = container;
        }
    }
}
