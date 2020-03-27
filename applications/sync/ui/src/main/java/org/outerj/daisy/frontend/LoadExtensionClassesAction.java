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
package org.outerj.daisy.frontend;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.acting.Action;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;

public class LoadExtensionClassesAction implements Action, ThreadSafe {

  private List<File> handledDirs = new ArrayList<File>();

  public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters)
      throws Exception {
    File libraryDir = new File(parameters.getParameter("libraryDir"));
    if (!handledDirs.contains(libraryDir)) {
      ClassPathHacker classPathHacker = new ClassPathHacker(this.getClass().getClassLoader());
      if (libraryDir.exists() && libraryDir.isDirectory()) {
        for (File jar : libraryDir.listFiles(new JarFileFilter())) {
          classPathHacker.addFile(jar);
        }
      }
      handledDirs.add(libraryDir);
    }
    return null;
  }

  private class JarFileFilter implements FileFilter {

    public boolean accept(File pathname) {
      if (pathname.getName().endsWith(".jar"))
        return true;
      else
        return false;
    }
  }

  private class ClassPathHacker {
    // nicked this class from a pleasant fellow on the java forums.
    // http://forum.java.sun.com/thread.jspa?forumID=32&hilite=false&start=0&threadID=300557&range=15&q=

    private final Class[] parameters = new Class[] { URL.class };

    private final ClassLoader classLoader;

    public ClassPathHacker(ClassLoader classLoader) {
      this.classLoader = classLoader;
    }

    public void addFile(String s) throws IOException {
      File f = new File(s);
      addFile(f);
    }

    public void addFile(File f) throws IOException {
      addURL(f.toURL());
    }

    public void addURL(URL u) throws IOException {

      URLClassLoader sysloader = (URLClassLoader) classLoader;
      Class sysclass = URLClassLoader.class;

      try {
        Method method = sysclass.getDeclaredMethod("addURL", parameters);
        method.setAccessible(true);
        method.invoke(sysloader, new Object[] { u });
      } catch (Throwable t) {
        t.printStackTrace();
        throw new IOException("Error, could not add URL to system classloader");
      }
    }
  }
}
