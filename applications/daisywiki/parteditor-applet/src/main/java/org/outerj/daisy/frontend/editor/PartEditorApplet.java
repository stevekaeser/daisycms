/*
 * Copyright 2002-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.frontend.editor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import netscape.javascript.JSObject;

/**
 * 
 * @author karel
 */
public class PartEditorApplet extends JApplet {
    
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    private static final String SESSIONID_PARAMETER = "jsessionid";
    
    private boolean init = false;
    
    private String initFailedReason;
    private Throwable initFailedException;
    
    private DownloadRunner downloadRunner;
    private Thread downloadThread;
    private UploadRunner uploadRunner;
    private Thread uploadThread;

    protected MimeTypeRegistry mimeTypeRegistry;
    /** when a fileOpen action is called before the file download is finished, lastOpenAction.actionPerformed is triggered when the download is completed.
     * We only remember the 'lastOpenAction' to prevent multiple applications starting up when the download completes
     */
    protected Action lastOpenAction;

    // file and progress info
    protected Action downloadAction;
    protected BoundedRangeModel rangeModel;
    protected JLabel iconLabel;
    protected JLabel downloadButton;
    protected JLabel fileLabel;

    protected CardLayout cardLayout;
    protected JPanel progressPanel;
    protected JLabel progressText;
    protected JLabel statusText;
    
    // open / settings
    protected Action systemOpenAction;
    protected Action settingsDialogAction;
    protected DropDownButton openWithButton;
    protected JMenu openWithMenu;

    // save / cancel
    protected Action saveAction;
    protected Action cancelAction;
    protected JButton cancelButton;
    protected JButton saveButton;
    
    // status bar
    protected JLabel showDownloadLocationButton;
    protected Action showDownloadLocationAction;
    protected JLabel showLogButton;
    protected int errorCount = 0;
    
    // log 
    protected JFrame logDialog;
    protected LogTextArea logArea;
    protected JScrollPane logScrollPane;
    
    // icons
    protected final Map<String, String> iconUrls = new HashMap<String, String>(); 
    
    private String jsessionid;
    private String baseURL;
    private URL formURL;
    private URL downloadURL;
    private URL uploadURL;

    // parameters
    private String p_mountPoint;
    private String p_siteName;
    private String p_skin;

    private String p_documentId;
    private String p_branch;
    private String p_language;
    private String p_partTypeName;
    private String p_partTypeId;
    
    private String p_formPath;
    private String p_imagesPath;
    private String p_iconPaths;
    private String p_downloadPath;
    private String p_uploadPath;
    private String p_fileName;
    private String p_mimeType;
    private String p_mimeTypeDefaultsPath;
    private String p_widgetId;
    
    private File downloadDirectory;
    private String downloadedFileName;
    private File downloadedFile;
    private long downloadedFileLength;
    private long downloadedFileLastModified;
    private byte[] downloadedFileMD5Sum;
    
    private ImageIcon fileIcon;
    private MimeTypeDialog mimeTypeDialog;
    private ApplicationDialog applicationDialog;
    
    private SystemHelper systemHelper;
    private ImageIcon statusErrorIcon;
    
    private Map<String, String> extraPostParams;
    
    private Map<ApplicationEntry, Component> customOpenMenuItems = new HashMap<ApplicationEntry, Component>();
    
    public void init() {
        super.init();
        
        // create log area beforehand so we can start using it immediately 
        logArea = new LogTextArea();

        initializeParameters();
        
        initializeURLs();

        initializeIconURLs();
        
        initializeFileIcon(); 
        
        // preload the errorStatusIcon (otherwise it will lag the GUI when an error occurs);
        statusErrorIcon = GUIUtils.getImageIcon(iconUrls.get("statusError"), logArea);
        
        initializeGUI();
        
        try {
            init = initialize();
        } catch (Exception e) {
            initFailedException = e;
        }
        if (!init) {
            logArea.log(initFailedReason, initFailedException);
            setErrorStatus();
            setStatus("Initialisation failed");
        } else {
            startDownload();
        }
        
    }

    private void initializeFileIcon() {
        if (p_iconPaths != null) {
            String[] paths = p_iconPaths.split("\\s+");
            for (String path: paths) {
                try {
                    fileIcon = new ImageIcon(new URL(baseURL + path));
                    if (fileIcon.getIconWidth() >= 0) {
                        return;
                    }
                } catch (MalformedURLException e) {
                    logArea.log("Could not build URL for path: " + path, e);
                }
            }
        }
        
        if (fileIcon == null || fileIcon.getIconWidth() < 0) {
            fileIcon = new ImageIcon(getClass().getClassLoader().getResource("org/outerj/daisy/frontend/editor/unknown.gif"));
        }
    }

    private void initializeIconURLs() {
        iconUrls.put("refresh", baseURL + p_imagesPath + "/refresh.gif");
        iconUrls.put("showFile", baseURL + p_imagesPath + "/downloadlocation.gif");
        iconUrls.put("cancel", baseURL + p_imagesPath + "/cancel.gif");
        iconUrls.put("save", baseURL + p_imagesPath + "/save.gif");
        iconUrls.put("remove", baseURL + p_imagesPath + "/delete.gif");
        iconUrls.put("statusOk", baseURL + p_imagesPath + "/status_ok.gif");
        iconUrls.put("statusError", baseURL + p_imagesPath + "/status_error.gif");
        iconUrls.put("moveUp", baseURL + p_imagesPath + "/up.gif");
        iconUrls.put("moveDown", baseURL + p_imagesPath + "/down.gif");
        iconUrls.put("new", baseURL + p_imagesPath + "/new.gif");
    }

    private void initializeURLs() {
        URL docBase = getDocumentBase();
        
        String debugBaseURL = getParameter("debugBaseURL");
        if (debugBaseURL != null) {
            baseURL = debugBaseURL;
        } else {
            baseURL = new StringBuffer().append(docBase.getProtocol())
                .append("://").append(docBase.getHost())
                .append(":").append(docBase.getPort()).toString();
        }
        
        formURL = createURLForPathParameter("formPath");
        downloadURL = createURLForPathParameter("downloadPath");
        uploadURL = createURLForPathParameter("uploadPath");
    }

    private URL createURLForPathParameter(String paramName) {
        try {
            return new URL(baseURL + getParameter(paramName));
        } catch (MalformedURLException e) {
            logArea.log(String.format("Could not create url for param %s (%s)", paramName, getParameter(paramName)),e);
        }
        return null;
    }

    private void initializeParameters() {
        p_mountPoint = getParameter("mountPoint");
        p_siteName = getParameter("siteName");
        p_skin = getParameter("skin");

        p_documentId = getParameter("documentId");
        p_branch = getParameter("branch");
        p_language = getParameter("language");
        p_partTypeName = getParameter("partTypeName");
        p_partTypeId = getParameter("partTypeId");

        p_formPath = getParameter("formPath");
        p_imagesPath = getParameter("imagesPath");
        p_iconPaths = getParameter("iconPaths");
        p_downloadPath = getParameter("downloadPath");
        p_uploadPath = getParameter("uploadPath");
        p_fileName = getParameter("fileName");
        if (p_fileName == null || p_fileName.trim().length() == 0) {
            p_fileName = "data";
        }
        
        p_mimeType = getParameter("mimeType");
        p_mimeTypeDefaultsPath = getParameter("mimeTypeDefaultsPath");
        p_widgetId = getParameter("widgetId");
    }
    
    public void start() {
        super.start();
    }
    
    public void stop() {
        super.stop();
    }
    
    protected long copy(InputStream input, OutputStream output) throws IOException {
        return copy(new InputStreamReader(input), new OutputStreamWriter(output));
    }
    
    protected long copy(Reader input, Writer output) throws IOException {
        char[] buffer = new char[DEFAULT_BUFFER_SIZE];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read((buffer)))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
    
    /**
     * Returns true if initialisation succeeded.
     * When initialization fails, initFailureReason and/or initFailureException should be set. 
     * @return
     * @throws Exception
     */
    protected boolean initialize() throws Exception {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));

        jsessionid = getParameter(SESSIONID_PARAMETER);
        
        downloadedFileName = new StringBuffer(tmpDir.getAbsolutePath())
            .append(File.separator).append("daisy-tmp")
            .append(File.separator).append(p_documentId).append("@").append(p_branch).append("@").append(p_language)
            .append(File.separator).append(p_fileName).toString();
        downloadDirectory = new File(downloadedFileName).getParentFile();
        
        URL mimeTypeDefaultsURL = null;
        try {
            mimeTypeDefaultsURL = new URL(baseURL + p_mimeTypeDefaultsPath);
            logArea.log("URL for default mimetype registry: " + mimeTypeDefaultsURL);
        } catch (MalformedURLException e) {
            logArea.log("URL for default mimetype registry is invalid: " + (baseURL + p_mimeTypeDefaultsPath));
        }
        mimeTypeRegistry.initialize(mimeTypeDefaultsURL);
        
        initializeOpenWithMenu();
        
        logArea.log("jsessionid:" + jsessionid);
        logArea.log("downloadURL: " + downloadURL);
        logArea.log("downloadDirectory: " + downloadDirectory);
        logArea.log("downloadedFileName: " + downloadedFileName);
        logArea.log("downloadedFile: " + downloadedFile);

        return true;
    }
    
    private void initializeOpenWithMenu() {
        openWithMenu.removeAll();
        
        openWithMenu.add(systemOpenAction);
        openWithMenu.addSeparator();

        MimeTypeEntry mimeTypeEntry = mimeTypeRegistry.getMimeTypeEntry(p_mimeType);
        List<ApplicationEntry> appEntries = mimeTypeEntry.getApplicationEntries();
        for (ApplicationEntry appEntry: appEntries) {
            openWithMenu.add(getMenuItem(appEntry));
        }
        openWithMenu.addSeparator();
        openWithMenu.add(new JMenuItem(new ShowNewApplicationDialogAction("Other...")));
        
        openWithButton.getButton().setAction(getMenuItem(mimeTypeEntry.getDefaultApplication()).getAction());

    }
    
    private Frame findParentFrame(){ 
        Container c = this; 
        while(c != null){ 
          if (c instanceof Frame) 
            return (Frame)c; 

          c = c.getParent(); 
        } 
        return (Frame)null; 
    } 

    protected void initializeGUI() {
        
        logScrollPane = new JScrollPane(logArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        logDialog = new JFrame("Part-editor applet log");
        logDialog.setSize(new Dimension(640, 480));
        logDialog.add(logScrollPane);
        
        systemHelper = new SystemHelper();
        
        // create actions:
        // create models:
        rangeModel = new DefaultBoundedRangeModel();

        // Set up mimeTypeDialog
        mimeTypeRegistry = new MimeTypeRegistry(logArea);

        applicationDialog = createApplicationDialog();
        mimeTypeDialog = createMimeTypeDialog();
        
        // Create all the relevant widgets
        downloadButton = new JLabel();
        
        JPanel main = new JPanel();
        main.setBackground(new Color(238,238,238)); // The default background is too dark in Safari, so we set it for everybody here.
        main.setLayout(new BoxLayout(main, BoxLayout.PAGE_AXIS));
        
        JPanel topBorderPanel = new JPanel(new BorderLayout());
        topBorderPanel.setOpaque(false);
        topBorderPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = createHorizontalPanel();

        iconLabel = new JLabel(fileIcon);
        JPanel iconPanel = createVerticalPanel();
        iconPanel.add(iconLabel);
        iconPanel.add(Box.createVerticalGlue());
        topPanel.add(iconPanel);
        topPanel.add(Box.createHorizontalStrut(5));
        
        JPanel filePanelCenter = createVerticalPanel();
        filePanelCenter.add(createFilePanel());
        topPanel.add(filePanelCenter);
        
        
        topPanel.add(Box.createHorizontalGlue());
        topPanel.add(Box.createHorizontalStrut(5));
        
        JPanel topNorth = new JPanel(new BorderLayout());
        topNorth.setOpaque(false);
        topNorth.add(topPanel, BorderLayout.NORTH);
        topBorderPanel.add(topNorth, BorderLayout.CENTER);
        topBorderPanel.add(createOpenButtonPanel(), BorderLayout.EAST);

        main.add(topBorderPanel);
        
        JPanel bottomPanel = createVerticalPanel();
        main.add(Box.createVerticalGlue());
        main.add(bottomPanel);
        
        JPanel buttons = createCloseButtonPanel();

        bottomPanel.add(buttons);
        bottomPanel.setMinimumSize(new Dimension(0,30));

        JPanel statusBar = createStatusBar();
        bottomPanel.add(statusBar);
        
        this.getContentPane().add(main);
        
        if  (systemHelper == null) {
            logArea.log("SystemHelper not set.  Applet will not work properly");
        }
        
    }

    private JPanel createVerticalPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        return panel;
    }

    private JPanel createHorizontalPanel() {
        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.LINE_AXIS));
        return topPanel;
    }

    private ApplicationDialog createApplicationDialog() {
        final ApplicationDialog dialog = new ApplicationDialog(findParentFrame(), mimeTypeRegistry, p_mimeType, iconUrls);
        dialog.setModal(true);
        dialog.getOkButton().getAction().addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("applicationEntry")) {
                    ApplicationEntry appEntry = (ApplicationEntry)evt.getNewValue();
                    if (appEntry != null) {
                        MimeTypeEntry mimeTypeEntry = mimeTypeRegistry.getMimeTypeEntry(p_mimeType);
                        appEntry = mimeTypeEntry.createApplicationEntry(appEntry.getTitle(), appEntry.getApplication(), appEntry.getArguments());
                        mimeTypeEntry.setDefaultApplicationEntry(appEntry);
                        mimeTypeRegistry.store();
                        initializeOpenWithMenu();
                        
                        getMenuItem(appEntry).getAction().actionPerformed(new ActionEvent(evt.getSource(), ActionEvent.ACTION_PERFORMED, "dummy"));
                    }
                }
            }
            
        });
        return dialog;
    }

    private MimeTypeDialog createMimeTypeDialog() {
        final MimeTypeDialog dialog = new MimeTypeDialog(mimeTypeRegistry, p_mimeType, logArea, iconUrls);
        dialog.setModal(true);
        dialog.addComponentListener(new ComponentAdapter() {

            public void componentHidden(ComponentEvent e) {
                if (dialog.isReturnOk()) {
                    initializeOpenWithMenu();
                }
            }
            
        });
        return dialog;
    }

    private JPanel createStatusBar() {
        Border loweredBorder = BorderFactory.createBevelBorder(BevelBorder.LOWERED);

        showDownloadLocationAction = new ShowFileLocationAction("Browse", GUIUtils.getImageIcon(iconUrls.get("showFile"), logArea));
        showDownloadLocationButton = createLabelButton(showDownloadLocationAction);
        
        statusText = new JLabel(" ");
        statusText.setOpaque(false);
        statusText.setMinimumSize(new Dimension(0, (int)statusText.getPreferredSize().getHeight()));
        showLogButton = createLabelButton(new ShowLogAction("Log", GUIUtils.getImageIcon(iconUrls.get("statusOk"), logArea)));

        JPanel statusBar = createHorizontalPanel();
        
        JPanel s1 = createStatusBarPanel(loweredBorder);
        JPanel s2 = createStatusBarPanel(loweredBorder);
        JPanel s3 = createStatusBarPanel(loweredBorder);
        
        s1.add(Box.createHorizontalStrut(4));
        s1.add(showDownloadLocationButton);
        s1.add(Box.createHorizontalStrut(2));

        s2.add(Box.createHorizontalStrut(2));
        s2.add(statusText);
        s2.add(Box.createHorizontalGlue());
        s2.add(Box.createHorizontalStrut(2));

        s3.add(Box.createHorizontalStrut(2));
        s3.add(showLogButton);
        s3.add(Box.createHorizontalStrut(4));

        statusBar.add(s1);
        statusBar.add(s2);
        statusBar.add(s3);
        
        return statusBar;
    }
    
    private JLabel createLabelButton(final Action action) {
        String name = (String)action.getValue(Action.NAME);
        Icon icon = (Icon)action.getValue(Action.SMALL_ICON);
        
        JLabel result = null;
        if (icon.getIconHeight() >= 0) {
            result = new JLabel(icon);
            result.setToolTipText(name);
        } else {
            result = new JLabel(name);
        }
        result.setOpaque(false);
        
        result.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                action.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, "dummy"));
            }
            
        });
        return result;
    }

    private JPanel createStatusBarPanel(Border border) {
        JPanel result = createHorizontalPanel();
        result.setBorder(border);
        return result;
    }
    
    private Component createOpenButtonPanel() {
        settingsDialogAction = new SettingsDialogAction("Settings ...");
        systemOpenAction = new OpenFileAction("System default", null);

        JPanel buttons = createVerticalPanel();

        openWithButton = new DropDownButton();
        openWithButton.setRunFirstMenuOption(false);
        openWithMenu = openWithButton.getMenu();
        openWithButton.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JButton settingsButton = createButton(settingsDialogAction);
        settingsButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        
        buttons.add(openWithButton);
        buttons.add(Box.createVerticalStrut(5));
        buttons.add(settingsButton);
        buttons.add(Box.createVerticalGlue());

        return buttons;
    }

    private JButton createButton(Action action) {
        JButton b = new JButton(action);
        return b;
    }

    private JPanel createCloseButtonPanel() {
        JPanel buttons = createHorizontalPanel();

        cancelAction = new CancelAction("Cancel");
        saveAction = new UploadAction("Save");
        cancelButton = createButton(cancelAction);
        saveButton = createButton(saveAction);
        
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttons.add(Box.createHorizontalGlue());
        buttons.add(cancelButton);
        buttons.add(Box.createHorizontalStrut(5));
        buttons.add(saveButton);
        return buttons;
    }
    
    private Component createFilePanel() {
        JPanel panel = createVerticalPanel();
        
        downloadAction = new DownloadAction("refresh", GUIUtils.getImageIcon(iconUrls.get("refresh"), logArea));
        downloadButton = createLabelButton(downloadAction);
        fileLabel = new JLabel(p_fileName);
        fileLabel.setOpaque(false);
        
        JPanel top = new JPanel();
        top.setLayout(new BorderLayout());
        top.setOpaque(false);
        
        Box downloadBox = new Box(BoxLayout.LINE_AXIS);
        downloadBox.add(downloadButton);
        downloadBox.add(Box.createHorizontalStrut(5));
        
        top.add(downloadBox, BorderLayout.WEST);
        
        top.add(fileLabel, BorderLayout.CENTER);
        
        progressPanel = createHorizontalPanel();
        cardLayout = new CardLayout();
        progressPanel.setLayout(cardLayout);
        
        progressText = new JLabel("[[progress]]");
        progressText.setOpaque(false);
        rangeModel = new DefaultBoundedRangeModel();
        JProgressBar progressBar = new JProgressBar(rangeModel);

        JPanel progressTextPanel = createVerticalPanel();
        progressTextPanel.add(progressText);
        
        JPanel barPanel = createVerticalPanel();
        barPanel.add(progressBar);
        barPanel.add(Box.createVerticalGlue());

        progressPanel.add(barPanel, "bar");
        progressPanel.add(progressTextPanel, "text");
        
        panel.add(top);
        panel.add(Box.createVerticalStrut(5));
        panel.add(progressPanel);
        return panel;
    }

    /**
     * @throws IOException
     */
    public void openFile( ApplicationEntry appEntry ) throws IOException {
        lastOpenAction = null;
        
        if (appEntry == null) {
            systemHelper.openFile(downloadedFile);
        } else {
            try {
                String[] cmdArray = buildCommandArguments( appEntry.getApplication(), appEntry.getArguments() );
                logArea.log("About to launch with arguments " + Arrays.asList(cmdArray));
                systemHelper.exec(cmdArray, downloadDirectory);
            } catch (Exception ex) {
                logArea.log("Could not launch the program", ex);
                JOptionPane.showMessageDialog(PartEditorApplet.this, "Could not launch the program:" + ex.getMessage());
            }
        }
    }
    
    public void startDownload() {
        if (uploadThread != null && uploadThread.isAlive()) {
            JOptionPane.showMessageDialog(this, "Upload is currently in progress, download not started");
            return;
        }
        if (downloadThread != null && downloadThread.isAlive()) {
            JOptionPane.showMessageDialog(this, "Download is already in progress.");
            return;
        }
        
        cardLayout.show(progressPanel, "bar");
        downloadRunner = new DownloadRunner(downloadURL, rangeModel);
        downloadThread = new Thread(downloadRunner);
        downloadThread.start();
    }
    
    public void startUpload() {
        if (downloadThread != null && downloadThread.isAlive()) {
            JOptionPane.showMessageDialog(this, "Download is currently in progress, upload not started");
            return;
        }
        if (uploadThread != null && uploadThread.isAlive()) {
            JOptionPane.showMessageDialog(this, "Upload is already in progress.");
            return;
        }

        cardLayout.show(progressPanel, "bar");
        uploadRunner = new UploadRunner(uploadURL, rangeModel);
        uploadThread = new Thread(uploadRunner);
        uploadThread.start();
    }
    
    private class DownloadRunner implements Runnable {
        private boolean cancelled = false;
        private URL url;
        private BoundedRangeModel model;
        
        public DownloadRunner(URL url, BoundedRangeModel model) {
            if (url == null) {
                throw new NullPointerException("url should not be null");
            }
            if (model == null) {
                throw new NullPointerException("model should not be null");
            }
            this.url = url;
            this.model = model;
            model.setValue(0);
            model.setMinimum(0);
            model.setMaximum(100);
        }
        
        public void run() {
            try {
                InputStream is = null;
                OutputStream os = null;
                URLConnection urlConnection = null;
                boolean hasErrors = false;
                File tmpFile = null;
                cancelled = false;
                
                try {
                    
                    logArea.log("initialising download");
    
                    urlConnection = url.openConnection();
                    urlConnection.addRequestProperty("Cookie","JSESSIONID=" + jsessionid);

                    if (!downloadDirectory.exists() && !downloadDirectory.mkdirs()) {
                        setStatus("Could not create download directory for " + downloadDirectory);
                        // setErrorStatus will follow
                    }
                    tmpFile = new File(downloadedFileName);

                    is = urlConnection.getInputStream();
                    os = new FileOutputStream(tmpFile);
                    
                    model.setMaximum(urlConnection.getContentLength());
                    
                    logArea.log("content length: " + model.getMaximum());
                    
                    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                    long count = 0;
                    int n = 0;
    
                    while (-1 != (n = is.read((buffer))) && !cancelled) {
                        logArea.log("downloading ...");
                        os.write(buffer, 0, n);
                        count += n;
                        
                        model.setValue((int)count);
                    }
                    
                    if (cancelled) {
                        logArea.log("Download was interrupted, cleaning up now.");
                        cleanUp();
                    }
                    
                } catch (IOException ioe) {
                    hasErrors = true;
                    
                    logArea.log("IOException during download: ", ioe);
                    
                    // try to get some additional log info
                    StringWriter sw = new StringWriter();
                    
                    try {
                        Method m = urlConnection.getClass().getMethod("getErrorStream", new Class[]{});
                    
                        Object result = m.invoke(urlConnection);
                        if (result != null) {
                            copy(new InputStreamReader((InputStream)result), sw);
                            logArea.log(sw.getBuffer().toString());
                        } else {
                            logArea.log("getErrorStream returned null");
                        }
                    } catch (Exception e) {
                        logArea.log("Could not get error stream after failed download: " , e);
                    }

                    cleanUp();
                    
                } finally {
                    try { if (is != null) { is.close(); } } catch (IOException e) { logArea.log("Error closing input stream", e); };
                    try { if (os != null) { os.close(); } } catch (IOException e) { logArea.log("Error closing output stream", e); };
                }

                if (!hasErrors && !cancelled) {
                    downloadedFile = tmpFile;
                }
                downloadFinished(hasErrors, cancelled);
                
                
            } catch (Exception e) {
                logArea.log("Unhandled exception: ", e);
            }
            
        }

        private void cleanUp() {
            if (downloadedFile != null && downloadedFile.exists() && !downloadedFile.delete()) {
                logArea.log("File cleanup failed.");
            }

            downloadedFile = null;
        }
    }
    
    private boolean isDownloadBusy() {
        return downloadThread != null && downloadThread.isAlive();
    }
    
    private void downloadFinished(boolean hasErrors, boolean userAborted) {
        cardLayout.show(progressPanel, "text");
        if (hasErrors) {
            setErrorStatus();
            progressText.setText("Download failed");
            JOptionPane.showMessageDialog(this, "Download failed. Check log for details.");
            logArea.log("Download finished unsuccesfully.");
            lastOpenAction = null;
        } else if (userAborted) {
            progressText.setText("Download cancelled");
            logArea.log("Download cancelled by user");
            lastOpenAction = null;
        } else {
            logArea.log("Download finished.");
            
            downloadedFileLength = downloadedFile.length();
            progressText.setText(humanReadableByteSize(downloadedFileLength));
            downloadedFileLastModified = downloadedFile.lastModified();
            try {
                downloadedFileMD5Sum = getMD5Sum(downloadedFile);
            } catch (Exception e) {
                downloadedFileMD5Sum = null;
                logArea.log("Failed to calculate md5 checksum");
            
            }
            
            if (lastOpenAction != null) {
                lastOpenAction.actionPerformed(new ActionEvent(downloadRunner, ActionEvent.ACTION_PERFORMED, "dummy"));
            }
        }
    }
    
    private String humanReadableByteSize(long bytes) {
        if (bytes < 1024) {
            return String.format("%d bytes", bytes);
        }
        
        float fbytes = ((float)bytes) / 1024;
        if (bytes / 1024 < 1024) {
            return String.format("%.3f kB", fbytes);
        }
        
        fbytes = ((float)bytes) / 1024 / 1024;
        return String.format("%.3f MB", fbytes);
        
    }

    private void uploadFinished(boolean errors, boolean cancelled) {
        if (errors) {
            JOptionPane.showMessageDialog(this, "Upload failed.  Check log for details");
            setStatus("Upload failed.");
            setErrorStatus();
            logArea.log("Upload finished unsuccesfully.");
        } else if (cancelled) {
            setStatus("Upload cancelled.");
            logArea.log("Upload interrupted by user");
        } else {
            String message = "Upload completed.";
            setStatus(message);
            logArea.log(message);
            askBrowserToRemoveApplet(message);
        }
    }
    
    public void askBrowserToRemoveApplet(String message) {
        File tmpFile = new File(downloadedFileName);
        if  ( tmpFile.exists() ) {
            tmpFile.delete();
        }
        downloadedFile = null;
        downloadDirectory.delete(); // will fail if there still are files, but that is ok
        
        boolean ok = false;
        JSObject window = null;
        try {
            window = JSObject.getWindow(PartEditorApplet.this);
        } catch (Exception e) {
            // Computer says no.
            logArea.log("LiveConnect doesn't work, I will reload the page instead.");
        }
        if  (window != null) {
            try {
                window.call("hideEditApplet", new Object[] { p_widgetId }); // call destroyApplet in browser
                ok = true;
            } catch (NoClassDefFoundError ncdfe) {
                logArea.log("Could not call editAppletController.hideEditApplet(id) in browser", ncdfe);
            } catch (Throwable t) {
                // catching Throwable makes sure we get all possible feedback in case it does not work
                logArea.log("Unexpected exception: ", t);
            }
        }
        logArea.log("after first attempt to close applet: ok = " + ok);
        if (!ok) {
            setStatus("Closing ... ");
            // running this in a separate thread to prevent the UI from blocking.
            new Thread(new Runnable() {
                public void run() {
                    getAppletContext().showDocument(formURL, "_self");
                }
            }).run();
            setStatus(message + " Refreshing page in browser.");
        }

    }
    
    private abstract class CatchAllAction extends AbstractAction {
        
        public CatchAllAction(String name) {
            super(name);
        }
        
        public CatchAllAction(String name, Icon icon) {
            super(name, icon);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                logArea.log("action: " + getName());
                onActionPerformed(e);
                logArea.log("done");
            } catch (Exception ex) {
                logArea.log("Action failed: ", ex);
            }
        }
        
        protected abstract void onActionPerformed(ActionEvent e) throws Exception;
    }
    
    private class DownloadAction extends CatchAllAction {
        
        public DownloadAction(String name) {
            super(name);
        }
        
        public DownloadAction(String name, Icon icon) {
            super(name, icon);
        }

        @Override
        public void onActionPerformed(ActionEvent event) throws Exception {
            logArea.log("Performing download action");
            
            lastOpenAction = null;
            if (downloadedFile != null && downloadedFile.exists()) {
                downloadedFile.delete();
                startDownload();
            } else {
                downloadedFile = null;
                startDownload();
            }
        }

    }
    
    private class UploadAction extends CatchAllAction {
        
        public UploadAction(String name) {
            super(name);
        }
        
        public UploadAction(String name, Icon icon) {
            super(name, icon);
        }

        @Override
        public void onActionPerformed(ActionEvent event) throws Exception {
            if (downloadedFile == null) {
                if  (isDownloadBusy()) {
                    downloadRunner.cancelled = true;
                    setStatus("Download cancelled.");
                    askBrowserToRemoveApplet("Closing without changes.");
                }
            } else if (isUploadBusy()) {
                JOptionPane.showMessageDialog(PartEditorApplet.this, "Upload already in progress. Please wait a bit.");
            } else {
                setStatus("Closing");
                boolean md5changed = checkMD5SumChanged();
                boolean lastModifiedChanged = downloadedFileLastModified != downloadedFile.lastModified();

                if (md5changed) {
                    startUpload();
                } else {
                    String message = "The local file has not changed.  Upload anyway?";

                    if (!lastModifiedChanged) {
                        message = "The local file was not changed.  Perhaps you forgot to save your changes.  Upload anyway?";
                    }
                    
                    int answer = JOptionPane.showConfirmDialog(PartEditorApplet.this, message, "Daisy Edit Applet", JOptionPane.YES_NO_CANCEL_OPTION);
                    if ( answer == JOptionPane.YES_OPTION ) {
                        startUpload();
                    } else if (answer == JOptionPane.NO_OPTION) {
                        askBrowserToRemoveApplet("Closing without changes.");
                    } // else: cancelled

                }
            }
        }

    }
    
    /**
     * checks if the file has not changed.
     * A file has changed if its md5sum has changed
     */
    public boolean checkMD5SumChanged() {
        if (downloadedFileMD5Sum == null) return false;
        try {
            byte[] newMD5Sum = getMD5Sum(downloadedFile);
            if (Arrays.equals(downloadedFileMD5Sum, newMD5Sum)) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    private byte[] getMD5Sum(File file) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        InputStream is = new FileInputStream(downloadedFile);
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read = 0;
        while( (read = is.read(buffer)) > 0) {
            digest.update(buffer, 0, read);
        }
        return  digest.digest();
    }
    
    private class ShowFileLocationAction extends CatchAllAction {
        
        public ShowFileLocationAction(String name) {
            super(name);
        }
        
        public ShowFileLocationAction(String name, Icon icon) {
            super(name, icon);
        }
        
        @Override
        public void onActionPerformed(ActionEvent event) throws Exception {
            logArea.log("Performing showFileLocation action");

            File expectedLocation = new File(downloadedFileName);
            if (!expectedLocation.exists()) {
                systemHelper.showFile(expectedLocation);
            } else {
                systemHelper.showFile(expectedLocation.getParentFile());
            }
        }

    }
    
    private class SettingsDialogAction extends CatchAllAction {

        public SettingsDialogAction(String name) {
            super(name);
        }

        @Override
        protected void onActionPerformed(ActionEvent e) throws Exception {
            mimeTypeDialog.setVisible(true);
        }
        
    }

    // TODO: single-character spaces are nice, but what about environment variables etc? (I doubt that Runtime.exec() handles them, 
    // but should check anyway
    /**
     * Note that downloadedFile should not be null at the time of calling this
     */
    private String[] buildCommandArguments(String application, String arguments) {
        List<String> args = new ArrayList<String>();
        args.add(application);
        
        if (arguments == null || arguments.trim().length() == 0) {
            args.add(downloadedFile.getAbsolutePath());
        } else {
            StringBuffer buf = new StringBuffer();
            boolean escaped = false;
            for (char c: arguments.toCharArray()) {
                if (escaped) {
                    escaped = false;
                    switch(c) {
                        case '%':
                            buf.append('%');
                            break;
                        case 'f':
                            buf.append(downloadedFileName);
                            break;
                        case 'm':
                            buf.append(p_mimeType);
                            break;
                        default:
                            buf.append(c);
                    }
                } else {
                    switch(c) {
                    case '%':
                        escaped = true;
                        break;
                    case ' ':
                        if (buf.length() > 0) {
                            args.add(buf.toString());
                            buf.setLength(0);
                        }
                        break;
                    default:
                        buf.append(c);
                    }
                }
            }
            if (buf.length() > 0) {
                args.add(buf.toString());
            }
        }
        
        return (String[]) args.toArray(new String[args.size()]);
    }
    
    private JMenuItem getMenuItem(ApplicationEntry appEntry) {
        if (!customOpenMenuItems.containsKey(appEntry)) {
            Action action = new OpenFileAction( appEntry == null? "System default":appEntry.getTitle(), appEntry );
            customOpenMenuItems.put(appEntry, new JMenuItem(action));
        }
        return (JMenuItem)customOpenMenuItems.get(appEntry);
    }
    
    private class CancelAction extends CatchAllAction {

        public CancelAction(String name) {
            super(name);
        }
        
        public CancelAction(String name, Icon icon) {
            super(name, icon);
        }

        @Override
        protected void onActionPerformed(ActionEvent e) throws Exception {
            setStatus("Cancelling.");
            if (isUploadBusy()) {
                uploadRunner.cancelled = true;
            }
            if  (isDownloadBusy()) {
                downloadRunner.cancelled = true;
            }
            
            if (downloadedFile == null){
                askBrowserToRemoveApplet("Cancelling.");
            } else {
                boolean md5changed = checkMD5SumChanged();

                if (md5changed) {
                    int answer = JOptionPane.showConfirmDialog(PartEditorApplet.this, "The local file was changed.  Lose changes?", "Daisy Edit Applet", JOptionPane.YES_NO_OPTION);
                    if ( answer == JOptionPane.YES_OPTION ) {
                        askBrowserToRemoveApplet("Cancelling.");
                    }
                } else {
                    askBrowserToRemoveApplet("Cancelling.");
                }
            }
        }

    }
    
    private boolean isUploadBusy() {
        return uploadThread != null && uploadThread.isAlive();
    }

    private class OpenFileAction extends CatchAllAction {
        
        private ApplicationEntry appEntry;
        
        public OpenFileAction(String title, ApplicationEntry appEntry) {
            super(title);
            this.appEntry = appEntry;
        }

        @Override
        protected void onActionPerformed(ActionEvent e) throws Exception {
            if (e.getSource() instanceof DownloadRunner) {
                lastOpenAction = null;
                openFile( appEntry );
            } else {
                if (e.getSource() instanceof JMenuItem) {
                    // write the configuration file
                    logArea.log("About to check if this already is the default action");
                    if (!safeEquals(appEntry, mimeTypeRegistry.getMimeTypeEntry(p_mimeType).getDefaultApplication())) {
                        logArea.log("It was not the default action, storing changes");
                        mimeTypeRegistry.getMimeTypeEntry(p_mimeType).setDefaultApplicationEntry(appEntry);
                        mimeTypeRegistry.store();
                    } else {
                        logArea.log("Already the default action");
                    }
                    
                    openWithButton.getButton().setAction(this);
                }
                
                if (isDownloadBusy()) {
                    JOptionPane.showMessageDialog(PartEditorApplet.this, "Download in progress. Please wait.");
                    //lastOpenAction = this; // this makes the action be re-performed when the download is complete. usually annoying
                } else if (!new File(downloadedFileName).exists()) {
                    lastOpenAction = this;
                    startDownload();
                } else {
                    openFile(appEntry);
                }
            }
        }
    }

    private boolean safeEquals(Object o1, Object o2) {
        if (o1 == null && o1 != o2)
            return false;
        if (o2 == null)
            return false;
        return o1.equals(o2);
    }
    
    private class UploadRunner implements Runnable {
        private boolean cancelled = false;
        private URL url;
        private BoundedRangeModel model;
        
        public UploadRunner(URL url, BoundedRangeModel model) {
            if (url == null) {
                throw new NullPointerException("url should not be null");
            }
            if (model == null) {
                throw new NullPointerException("model should not be null");
            }
            this.url = url;
            this.model = model;
            model.setValue(0);
            model.setMinimum(0);
            model.setMaximum(100);
        }
        
        public void run() {
            try {
                InputStream is = null;
                OutputStream os = null;
                URLConnection urlConnection = null;
                boolean hasErrors = false;
                try {
                    logArea.log("initialising upload");
    
                    String boundary = "-----" + randomString();

                    urlConnection = url.openConnection();
                    urlConnection.addRequestProperty("Cookie","JSESSIONID=" + jsessionid);
                    urlConnection.setRequestProperty("Content-Type",
                            "multipart/form-data; boundary=" +
                            boundary);
                    
                    urlConnection.setDoOutput(true);
                    
                    downloadedFile = new File(downloadedFileName);

                    is = new FileInputStream(downloadedFile);
                    os = urlConnection.getOutputStream();
                    
                    model.setMaximum((int)downloadedFile.length());
                    
                    Writer w = new OutputStreamWriter(os);
                    
                    w.write("--");
                    w.write(boundary);
                    w.write("\r\n");
                    w.write("Content-Disposition: form-data; name=\"part_" + p_partTypeId + ".upload-part\"; filename=\"" + p_fileName + "\"\r\n");
                    w.write("Content-Type: " + p_mimeType + "\r\n");
                    w.write("\r\n");
                    
                    logArea.log("file size: " + model.getMaximum());
                    
                    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                    long count = 0;
                    int n = 0;
                    
                    w.flush();

                    while (-1 != (n = is.read((buffer))) && !cancelled) {
                        logArea.log("uploading...");
                        os.write(buffer, 0, n);
                        count += n;
                        
                        model.setValue((int)count);
                    }
                    
                    w.write("\r\n");
                    
                    if (!cancelled) {
                        addStringParameter(w, boundary, "activeForm", "part-" + p_partTypeName);
                        addStringParameter(w, boundary, "forms_submit_id", "part_" + p_partTypeId + ".upload-part");
                        addStringParameter(w, boundary, "skipCrossEditorFields", "true");
                    } else {
                        logArea.log("Upload was cancelled.");
                    }
                    
                    w.write("--");
                    w.write(boundary);
                    w.write("--");
                    w.write("\r\n");
                    
                    w.flush();

                    //send the request and log the response:
                    StringWriter sw = new StringWriter();
                    copy(new InputStreamReader(urlConnection.getInputStream()), sw);
                    logArea.log(sw.getBuffer().toString());
                    
                } catch (IOException ioe) {
                    hasErrors = true;
                    
                    logArea.log("Upload failed with IOException: ", ioe);
                    
                    // try to get some additional log info
                    StringWriter sw = new StringWriter();
                    
                    Method m = urlConnection.getClass().getMethod("getErrorStream", new Class[]{});
                    
                    try {
                        Object result = m.invoke(urlConnection);
                        if (result != null) {
                            copy(new InputStreamReader((InputStream)result), sw);
                            logArea.log(sw.getBuffer().toString());
                        } else {
                            logArea.log("getErrorStream returned null");
                        }
                    } catch (Exception e) {
                        logArea.log("Could not get error stream after failed upload: " , e);
                    }
                    
                } finally {
                    try { if (is != null) { is.close(); } } catch (IOException e) { logArea.log("Error closing input stream", e); };
                    try { if (os != null) { os.close(); } } catch (IOException e) { logArea.log("Error closing output stream", e); };
                }
                
                uploadFinished(hasErrors, cancelled);
                
            } catch (Exception e) {
                logArea.log("Unhandled exception: ", e);
            }
            
        }

        private void addStringParameter(Writer w, String boundary,
                String name, String value) throws IOException {
            w.write("--");
            w.write(boundary);
            w.write("\r\n");
            w.write(String.format("Content-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n", name, value));
        }

    }
    
    /**
     * @return a random hexadecimal string based on a Long (64 bits = 8 bytes = 16 hexadecimal characters)
     */
    public String randomString() {
        Random random = new Random();
        return Long.toString(random.nextLong(), 16);
    }
    
    private void setStatus(String status) {
        statusText.setText(status);
        logArea.log("STATUS: " + status);
    }
        
    private class ShowLogAction extends AbstractAction {
        
        public ShowLogAction(String name) {
            super(name);
        }
        
        public ShowLogAction(String name, Icon icon) {
            super(name, icon);
        }

        public void actionPerformed(ActionEvent e) {
            logDialog.setVisible(!logDialog.isVisible());
        }
        
    }
    
    private class ShowNewApplicationDialogAction extends AbstractAction {
        
        public ShowNewApplicationDialogAction(String name) {
            super(name);
        }
        
        public void actionPerformed(ActionEvent e) {
            applicationDialog.setVisible(true);
        }
        
    }
    
    public void setErrorStatus() {
        errorCount++;
        showLogButton.setIcon(statusErrorIcon);
        showLogButton.setText("" + errorCount + " error(s)");
    }

}
