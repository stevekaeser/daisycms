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
package org.outerj.daisy.frontend.editor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public class SystemHelper {
    
    public interface ExecStrategy {
        public void exec(String[] args, File workingDirectory) throws IOException;
        public boolean isApplicable();
    }
    
    public interface OpenFileStrategy {
        public void openFile(File file) throws IOException;
        public boolean isApplicable();
    }
    
    public interface ShowFileStrategy {
        public void showFile(File file) throws IOException ;
        public boolean isApplicable();
    }
    
    protected List<ExecStrategy> execStrategies;
    protected List<OpenFileStrategy> openFileStrategies;
    protected List<ShowFileStrategy> showFileStrategies;
    
    private ExecStrategy macExecStrategy = new ExecStrategy() {
        public void exec(String[] args, File workingDirectory) throws IOException {
            if (args.length > 0) {
                String application = args[0];
                File f = new File(application);
                
                if (!f.exists() && new File(application + ".app").exists()) {
                    f = new File(application + ".app");
                    args[0] = f.getAbsolutePath();
                }
                
                if (f.getName().endsWith(".app")) {
                    String[] args2 = new String[args.length + 2];
                    args2[0] = "/usr/bin/open";
                    args2[1] = "-a";
                    System.arraycopy(args, 0, args2, 2, args.length);
                    args = args2;
                }
            }
            Runtime.getRuntime().exec(args, null, workingDirectory);
        }
        
        public boolean isApplicable() {
            return isMac();
        }
    };
    
    private ExecStrategy defaultExecStrategy = new ExecStrategy() {
        public void exec(String[]args, File workingDirectory) throws IOException {
            Runtime.getRuntime().exec(args, null, workingDirectory);
        }

        public boolean isApplicable() {
            return true;
        }
    };
    
    public OpenFileStrategy j6OpenStrategy = new OpenFileStrategy() {
        
        public void openFile(File file) {
            try {
                Class desktopClass = Class.forName("java.awt.Desktop");
                Class action = Class.forName("java.awt.Desktop.Action");
                Method valueOf = action.getMethod("valueOf", String.class);
                Method isSupported = action.getMethod("isSupported", action);
                Method getDesktop = desktopClass.getMethod("getDesktop");
                Method open = desktopClass.getMethod("open", File.class);
                Method edit = desktopClass.getMethod("edit", File.class);
                
                Object desktop = getDesktop.invoke(null);
                Object editAction = valueOf.invoke(null, "EDIT");
                Object openAction = valueOf.invoke(null, "OPEN");
                
                Boolean editSupported = (Boolean)isSupported.invoke(desktop, editAction);
                if (editSupported.booleanValue()) {
                    edit.invoke(desktop, file);
                    return;
                }
                Boolean openSupported = (Boolean)isSupported.invoke(desktop, openAction);
                if (openSupported.booleanValue()) {
                    open.invoke(desktop, file);
                    return;
                }
                
                System.err.println("Could not open '" + file.getAbsolutePath() + "'");
                
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        }

        public boolean isApplicable() {
            try {
                Class desktopClass = Class.forName("java.awt.Desktop");
                Class action = Class.forName("java.awt.Desktop.Action");
                Method valueOf = action.getMethod("valueOf", String.class);
                Method isSupported = action.getMethod("isSupported", action);
                Method getDesktop = desktopClass.getMethod("getDesktop");
                Method open = desktopClass.getMethod("open", File.class);
                Method edit = desktopClass.getMethod("edit", File.class);
                
                Object desktop = getDesktop.invoke(null);
                Object editAction = valueOf.invoke(null, "EDIT");
                Object openAction = valueOf.invoke(null, "OPEN");
                
                Boolean editSupported = (Boolean)isSupported.invoke(desktop, editAction);
                Boolean openSupported = (Boolean)isSupported.invoke(desktop, editAction);
                return editSupported.booleanValue() || openSupported.booleanValue();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            return false;
        }
    };
    
    public OpenFileStrategy windowsOpenStrategy = new OpenFileStrategy() {
        public void openFile(File file) throws IOException {
            if (file.isDirectory()) {
                Runtime.getRuntime().exec(new String[] { "explorer", file.getAbsolutePath() });
            } else {
                Runtime.getRuntime().exec(new String[] { "cmd", "/c", "start", file.getAbsolutePath() });
            }
        }

        public boolean isApplicable() {
            return isWindows();
        }
    };
    
    public OpenFileStrategy macOpenStrategy = new OpenFileStrategy() {
        public void openFile(File file) throws IOException {
            Runtime.getRuntime().exec(new String[] { "/usr/bin/open", file.getAbsolutePath() });
        }

        public boolean isApplicable() {
            return isMac();
        }
    };
    
    public OpenFileStrategy linuxOpenStrategy = new OpenFileStrategy() {
        public void openFile(File file) throws IOException {
            if (file.isDirectory()) {
                Runtime.getRuntime().exec(new String[] { "/usr/bin/nautilus", file.getAbsolutePath() });
            } else {
                Runtime.getRuntime().exec(new String[] { "/usr/bin/nautilus", file.getParentFile().getAbsolutePath() });
            }
        }

        public boolean isApplicable() {
            return isLinux();
        }
        
    };
    
    public ShowFileStrategy windowsShowStrategy = new ShowFileStrategy() {
        public void showFile(File file) throws IOException {
            if (file.isDirectory()) {
                Runtime.getRuntime().exec(new String[] { "explorer", file.getAbsolutePath() });
            } else {
                Runtime.getRuntime().exec(new String[] { "explorer", "/e,/select,\"" + file.getAbsolutePath() + "\""});
            }
        }

        public boolean isApplicable() {
            return isWindows();
        }
    };
 
    public ShowFileStrategy macShowStrategy = new ShowFileStrategy() {
        public void showFile(File file) throws IOException {
            if (file.isDirectory()) {
                Runtime.getRuntime().exec(new String[] { "/usr/bin/open", file.getAbsolutePath() });        
            } else {
                Runtime.getRuntime().exec(new String[] { "/usr/bin/open", file.getParentFile().getAbsolutePath() });        
            }
        }

        public boolean isApplicable() {
            return isMac();
        }
    };
 
    public ShowFileStrategy linuxShowStrategy = new ShowFileStrategy() {
        public void showFile(File file) throws IOException {
            if (file.isDirectory()) {
                Runtime.getRuntime().exec(new String[] { "nautilus", file.getAbsolutePath() });
            } else {
                Runtime.getRuntime().exec(new String[] { "nautilus", file.getParentFile().getAbsolutePath() });
            }
        }

        public boolean isApplicable() {
            return isLinux();
        }
    };
    
    public ShowFileStrategy j6ShowStrategy = new ShowFileStrategy() {
        public void showFile(File file) throws IOException {
            if (file.isDirectory()) {
                j6OpenStrategy.openFile(file);
            } else {
                j6OpenStrategy.openFile(file.getParentFile());
            }
        }
        
        public boolean isApplicable() {
            return j6OpenStrategy.isApplicable();
        }
    };
    
    public void exec(String[] cmdArray, File workingDirectory) throws IOException {
        int i = 0;
        while (i < execStrategies.size() && ! execStrategies.get(i).isApplicable()) {
            i++;
        }
        if (i < execStrategies.size()) {
            execStrategies.get(i).exec(cmdArray, workingDirectory);
        }
    }
    
    public static boolean isMac() {
        String osName = System.getProperty("os.name");
        Pattern osPattern = Pattern.compile("^mac os", Pattern.CASE_INSENSITIVE);
        return (osPattern.matcher(osName).find());
    }

    public static boolean isWindows() {
        String osName = System.getProperty("os.name");
        Pattern osPattern = Pattern.compile("^windows", Pattern.CASE_INSENSITIVE);
        return (osPattern.matcher(osName).find());
    }
    
    public static boolean isLinux() {
        String osName = System.getProperty("os.name");
        Pattern osPattern = Pattern.compile("^linux", Pattern.CASE_INSENSITIVE);
        return (osPattern.matcher(osName).find());
    }
 

    public SystemHelper() {
        execStrategies = new ArrayList<ExecStrategy>();
        execStrategies.add(macExecStrategy);
        execStrategies.add(defaultExecStrategy);
        
        openFileStrategies = new ArrayList<OpenFileStrategy>();
        openFileStrategies.add(windowsOpenStrategy);
        openFileStrategies.add(macOpenStrategy);
        openFileStrategies.add(linuxOpenStrategy);
        openFileStrategies.add(j6OpenStrategy);
        
        showFileStrategies = new ArrayList<ShowFileStrategy>();
        showFileStrategies.add(macShowStrategy);
        showFileStrategies.add(windowsShowStrategy);
        showFileStrategies.add(linuxShowStrategy);
        showFileStrategies.add(j6ShowStrategy);
        
    }

    public void openFile(File file) throws IOException {
        int i = 0;
        while (i < openFileStrategies.size() && ! openFileStrategies.get(i).isApplicable()) {
            i++;
        }
        if (i < openFileStrategies.size()) {
            openFileStrategies.get(i).openFile(file);
        }
    }

    public void showFile(File file) throws IOException {
        int i = 0;
        while (i < showFileStrategies.size() && ! showFileStrategies.get(i).isApplicable()) {
            i++;
        }
        if (i < showFileStrategies.size()) {
            showFileStrategies.get(i).showFile(file);;
        }
    }


    public static boolean haveDesktopAPI() {
        try {
            Class desktopClass = Class.forName("java.awt.Desktop");
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }
    

}
