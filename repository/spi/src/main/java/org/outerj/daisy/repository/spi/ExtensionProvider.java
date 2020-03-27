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
package org.outerj.daisy.repository.spi;

import org.outerj.daisy.repository.Repository;

/**
 * Interface to be implemented for adding Extension's to the repository server.
 *
 * <p>In the repository server, the ExtensionProvider should be registered
 * as a plugin with the {@link org.outerj.daisy.plugin.PluginRegistry PluginRegistry}
 */
public interface ExtensionProvider {
    
    /**
     * Creates an instance of the extension for the specified user (ie Repository instance).
     */
    Object createExtension(Repository repository);
}
