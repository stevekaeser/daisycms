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

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ApplicationDialog extends JDialog {
    
    // newPanel
    private JTextField applicationTextField;
    private Action applicationButtonAction;
    private JButton chooseApplicationButton;
    private JFileChooser applicationFileChooser;

    private JTextField argumentsTextField;
    private JTextField titleTextField;
    
    private MimeTypeRegistry mimeTypeRegistry;
    private String mimeType;

    protected ApplicationEntry applicationEntry = null;
    protected JButton okButton;
    
    public ApplicationDialog(Dialog owner, MimeTypeRegistry mimeTypeRegistry, String mimeType) {
        super(owner, "Open with new application");
        initialize(mimeTypeRegistry, mimeType);
    }
    
    public ApplicationDialog(Frame owner, MimeTypeRegistry mimeTypeRegistry, String mimeType, Map<String, String> iconUrls) {
        super(owner, "Open with new application");
        initialize(mimeTypeRegistry, mimeType);
    }

    private void initialize(MimeTypeRegistry mimeTypeRegistry, String mimeType) {
        this.mimeTypeRegistry = mimeTypeRegistry;
        this.mimeType = mimeType;
        
        JPanel mainPanel = createMainPanel();
        getContentPane().add(mainPanel);
        
        applicationFileChooser = new JFileChooser();
        
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentShown(ComponentEvent e) {
                applicationEntry = null;

                titleTextField.setText("");
                applicationTextField.setText("");
                argumentsTextField.setText("%f");
                //chooseApplicationButton.getAction().actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, "dummy"));
            }
            
        });
        
        // setMinimumSize seems to be ignored, keeping it here in case 
        // it ever starts having the desired effect. 
        mainPanel.setMinimumSize(new Dimension(400, 200));
        mainPanel.setPreferredSize(new Dimension(400,200));
        pack();
    }
    
    private JPanel createMainPanel() {
        JPanel result = new JPanel();
        result.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        
        result.add(createFormPanel());
        
        result.add(createDialogButtons());

        return result;
    }

    private JPanel createDialogButtons() {
        
        okButton = new JButton(new OkAction("Ok"));
        JButton cancelButton = new JButton(new CancelAction("Cancel"));

        JPanel dialogButtons = new JPanel();
        dialogButtons.setLayout(new BoxLayout(dialogButtons, BoxLayout.LINE_AXIS));
        dialogButtons.add(Box.createHorizontalGlue());
        dialogButtons.add(okButton);
        dialogButtons.add(Box.createHorizontalStrut(5));
        dialogButtons.add(cancelButton);
        
        getRootPane().setDefaultButton(okButton);
        
        return dialogButtons;
    }
    
    private JPanel createFormPanel() {
        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints cLast = new GridBagConstraints();
        cLast.gridwidth = GridBagConstraints.REMAINDER;
        cLast.weightx = 0.0;
        cLast.fill = GridBagConstraints.BOTH;
        
        FormUtility fu = new FormUtility();
        JPanel form = new JPanel();
        form.setLayout(gridBagLayout);

        titleTextField = new JTextField();
        fu.addLabel(new JLabel("Title"), form);
        fu.addMiddleField(titleTextField, form);
        form.add(Box.createGlue(), cLast);
        
        applicationTextField = new JTextField();
        applicationButtonAction = new ShowFileChooserAction("Choose...", applicationTextField);
        chooseApplicationButton = new JButton(applicationButtonAction);
        
        fu.addLabel(new JLabel("Application"), form);
        fu.addMiddleField(applicationTextField, form);
        form.add(chooseApplicationButton, cLast);
        
        argumentsTextField = new JTextField("%f");
        fu.addLabel(new JLabel("Arguments"), form);
        fu.addMiddleField(argumentsTextField, form);
        form.add(Box.createGlue(), cLast);

        cLast.gridx = 1;
        JLabel argumentsInfo = new JLabel("%f = local file name,  %m = mime type");
        form.add(argumentsInfo, cLast);

        return form;
    }
    
    private class ShowFileChooserAction extends AbstractAction {
        JTextField target;
        
        public ShowFileChooserAction(String name, JTextField target) {
            super(name);
            this.target = target;
        }

        public void actionPerformed(ActionEvent e) {
            applicationFileChooser.setSelectedFile(new File(target.getText()));
            int answer = applicationFileChooser.showDialog(ApplicationDialog.this, "Select");
            if (answer == JFileChooser.APPROVE_OPTION) {
                target.setText(applicationFileChooser.getSelectedFile().getAbsolutePath());
            }
        }
        
    }

    private class OkAction extends AbstractAction {
        
        public OkAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            String app = applicationTextField.getText().trim();
            File appFile = new File(app);
            
            if (!appFile.exists() && new File(app + ".app").exists()) {
                appFile = new File(app + ".app");
            } else if (!appFile.exists() && !new File(app + ".app").exists()) {
                JOptionPane.showMessageDialog(ApplicationDialog.this, "The file '"+applicationTextField.getText() + "' does not exist");
                return;
            }
            
            MimeTypeEntry entry = mimeTypeRegistry.getMimeTypeEntry(mimeType);
            
            String title = titleTextField.getText();
            String application = applicationTextField.getText();
            String arguments = argumentsTextField.getText();
            applicationEntry = new ApplicationEntry(title, application, arguments);
            
            setVisible(false);

            this.firePropertyChange("applicationEntry", null, applicationEntry);
            
        }
    }
    
    private class CancelAction extends AbstractAction {
        
        public CancelAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
        
    }
    
    public JButton getOkButton() {
        return okButton;
    }
    
    public ApplicationEntry getApplicationEntry() {
        return applicationEntry;
    }
    
    public JButton getChooseApplicationButton() {
        return chooseApplicationButton;
    }
    
}
