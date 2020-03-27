#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
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
package ${package};

import javax.annotation.PreDestroy;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

import org.outerj.daisy.plugin.PluginRegistry;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.spi.local.PreSaveHook;

public class SamplePreSaveHook implements PreSaveHook {

  public static final String NAME = "SamplePreSaveHook";

  private PluginRegistry pluginRegistry;

  public SamplePreSaveHook(Configuration configuration, PluginRegistry pluginRegistry) throws ConfigurationException {
      this.pluginRegistry = pluginRegistry;
      configure(configuration);
      pluginRegistry.addPlugin(PreSaveHook.class, NAME, this);
  } 

  public void configure(Configuration configuration) {
  }

  @PreDestroy
  public void destroy() {
      pluginRegistry.removePlugin(PreSaveHook.class, NAME, this);
  }

 public void process(Document document, Repository repository) throws Exception {
    /* Here you can make changes to the document, look up things in the repository,
     * even access your own datastore. You should not call document.save(). This can cause infinite loops */

    //document.setCustomField("foo", "bar");
  }

}
