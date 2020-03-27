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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.vfs.AllFileSelector;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.UserAuthenticator;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.auth.StaticUserAuthenticator;
import org.apache.commons.vfs.provider.http.HttpFileSystemConfigBuilder;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.outerj.daisy.maven.plugin.os.OsUtils;

/**
 * <p>
 * To start from scratch, use this mojo to download the daisy package, unpack it
 * and link or move it to the correct location.
 * </p>
 *
 * <p>
 * Simple usage:
 *
 * <pre>
 * mvn daisy:get-runtime -DremoteRuntimeURL=&quot;http://mesh.dl.sourceforge.net/sourceforge/daisycms/daisy-2.2&quot;
 * </pre>
 *
 * </p>
 *
 * <p>
 * Fully configured:
 *
 * <pre>
 * mvn daisy:get-runtime -DremoteRuntimeURL=&quot;http://mesh.dl.sourceforge.net/sourceforge/daisycms/daisy-2.2&quot; -DproxyHost=intproxy -DproxyPort=8080 -DproxyUsername=username -DproxyPassword=password
 * </pre>
 * Note that if you have configured a proxy in your settings.xml, it will automatically be used.
 * </p>
 *
 * @author Jan Hoskens
 *
 * @goal get-runtime
 * @aggregator
 * @description Download a runtime to the local filesystem.
 */
public class DaisyGetRuntime extends AbstractDaisyMojo {

    /**
     * Give the remote url to the daisy runtime package. Note that you should
     * not use an extension as the mojo will append the extension according to
     * the platform you're working on (eg windows translates to appending
     * ".zip", linux to ".tar.gz")
     *
     * @parameter expression="${remoteRuntimeURI}"
     * @required
     */
    private String remoteRuntimeURL;

    /**
     * Force the plugin to remove any existing files if needed.
     *
     * @parameter expression="${force}" default-value="false"
     */
    private boolean force;

    /**
     * This parameter won't allow the creation of links instead of making
     * copies. As a default, a Linux machine will get symbolic links to ease the
     * development setup.
     *
     * @parameter expression="${noLink}" default-value="false"
     */
    private boolean noLink;

    /**
     * @parameter expression="${proxyHost}"
     */
    private String proxyHost;

    /**
     * @parameter expression="${proxyPort}" default-value="8080"
     */
    private int proxyPort;

    /**
     * @parameter expression="${proxyUsername}"
     */
    private String proxyUsername;

    /**
     * @parameter expression="${proxyPassword}"
     */
    private String proxyPassword;

    /**
     * @component
     */
    private WagonManager wagonManager;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            File daisyRuntime = daisyHome;
            boolean versionEqual = true;
            try {
                versionCheck();
            } catch (MojoExecutionException mee) {
                versionEqual = false;
            }
            if (daisyRuntime.exists()) {
                if (force && !versionEqual) {
                    getLog().info("Force: deleting existing runtime.");
                    OsUtils.deleteFile(daisyRuntime);
                }
            }
            if (daisyRuntime.exists()) {
                getLog().warn("Daisy runtime already exists - nothing to be done.");
                return;
            }

            String suffix = OsUtils.isWindows() ? ".zip" : ".tar.gz";
            String downloadUrl = remoteRuntimeURL.concat(suffix);
            
            String distroDirname = remoteRuntimeURL.substring(remoteRuntimeURL.lastIndexOf("/")+1);
            File distroDir = new File(daisyRuntime.getParent(), distroDirname);
            
            String distroFilename = distroDirname.concat(suffix);
            File distroFile = new File(daisyRuntime.getParent(), distroFilename);

            if (distroDir.exists()) {
                getLog().warn(distroDir.getAbsolutePath().concat(" already exists, will not download and unzip distribution"));
            } else {
                if (distroFile.exists()) {
                    getLog().warn(distroFile.getAbsolutePath().concat(" already exists, will not download it"));
                } else if (!wagonManager.isOnline()) {
                    throw new MojoFailureException("Maven is in offline mode, refusing to download " + remoteRuntimeURL);
                } else {
                    getLog().info("About to download ".concat(downloadUrl));
                    downloadDaisyDistribution(downloadUrl, distroFile);
                }
                
                getLog().info("About to extract ".concat(distroFile.getAbsolutePath()));
                // check whether we have a packaged zip or tar.gz and unpack it
                // commons-vfs, can untar and unzip, but it's really slow for untarring
                if (distroFilename.endsWith("tar.gz")) {
                    executeCommand(new String[] { "tar", "-xvf", distroFile.getAbsolutePath(), "-C",
                            daisyRuntime.getParent() });
                } else if (distroFilename.endsWith(".zip")) {
                    executeCommand(new String[] { "unzip", distroFile.getAbsolutePath(), "-d",
                            daisyRuntime.getParentFile().getAbsolutePath() });
                }
            }

            // create links or move the directory to daisyserver
            if (OsUtils.isLinux() && !noLink) {
                createLink(distroDir, daisyRuntime);
            } else {
                FileUtils.moveDirectory(distroDir, daisyRuntime);
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Cannot retrieve runtime.", e);
        }

    }

    private void downloadDaisyDistribution(String downloadUrl, File distroFile)
            throws FileSystemException {
        FileObject remoteRuntime;
        FileObject localRuntime;

        ProxyInfo proxyInfo = wagonManager.getProxy("http");
        if ((proxyHost == null) && (proxyInfo != null)) {
            proxyHost = proxyInfo.getHost();
            proxyPort = proxyInfo.getPort();
            proxyPassword = proxyInfo.getPassword();
            proxyUsername = proxyInfo.getUserName();
        }
   
        // construct remote and local runtime url, download if local doesn't
        // exist.
        FileSystemManager fsManager = VFS.getManager();
        FileSystemOptions fileSystemOptions = new FileSystemOptions();
        if (proxyHost != null) {
            getLog().info("Using proxy: " + proxyHost + ":" + proxyPort);
            HttpFileSystemConfigBuilder httpFileSystemConfigBuilder = (HttpFileSystemConfigBuilder) fsManager
                    .getFileSystemConfigBuilder("http");
            httpFileSystemConfigBuilder.setProxyHost(fileSystemOptions, proxyHost);
            httpFileSystemConfigBuilder.setProxyPort(fileSystemOptions, proxyPort);
            if (proxyUsername != null) {
                UserAuthenticator authenticator = new StaticUserAuthenticator(null, proxyUsername,
                        proxyPassword);
                httpFileSystemConfigBuilder.setProxyAuthenticator(fileSystemOptions, authenticator);
            }
        }
        remoteRuntime = fsManager.resolveFile(downloadUrl, fileSystemOptions);
        localRuntime = fsManager.resolveFile(distroFile.getAbsolutePath());

        localRuntime.copyFrom(remoteRuntime, new AllFileSelector());
    }
}
