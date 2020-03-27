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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public class MimeTypeDialog extends JDialog {
    
    private LogTextArea logArea;
    
    private MimeTypeRegistry mainRegistry;
    private MimeTypeRegistry localRegistry;
    
    // managePanel
    private ListSelectionModel selectionModel;
    private DefaultTableModel tableModel;
    private JTable table;
    private ApplicationCellEditor applicationCellEditor;
    private ActionCellEditor actionCellEditor;
    private ApplicationDialog applicationDialog;
    
    private int defaultRowIdx;
    
    private Action removeAction;
    private Action upAction;
    private Action downAction;
    private Action newAction;
    
    private String mimeType;
    private boolean returnOk;
    
    protected Map<String, String> iconUrls;

    public MimeTypeDialog(MimeTypeRegistry mimeTypeRegistry, String mimeType, LogTextArea logTextArea, Map<String, String> iconUrls) {
        setTitle("Applications for mimetype '" + mimeType + "'");
        this.mainRegistry = mimeTypeRegistry;
        this.mimeType = mimeType;
        this.logArea = logTextArea;
        this.iconUrls = iconUrls;
        
        this.applicationDialog = createApplicationDialog();
        
        getContentPane().add(createMainPanel(), BorderLayout.CENTER);
        //getContentPane().add(createOkPanel());
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentShown(ComponentEvent e) {
                returnOk = false;
                loadMimeTypeRegistry();
            }
            
        });
        
    }
    
    private ApplicationDialog createApplicationDialog() {
        ApplicationDialog dialog = new ApplicationDialog(this, mainRegistry, mimeType);
        dialog.setTitle("New application");
        dialog.getOkButton().getAction().addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("applicationEntry")) {
                    ApplicationEntry appEntry = (ApplicationEntry)evt.getNewValue();
                    if (appEntry != null) {
                        insertRow(appEntry);
                    }
                }
            }
            
        });

        return dialog;
    }

    public JComponent createMainPanel() {
        JPanel result = new JPanel();
        result.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        result.setLayout(new BorderLayout());

        result.add(createManagePanel(), BorderLayout.CENTER);

        JPanel buttonPanel = createDialogButtons();
        
        result.add(buttonPanel, BorderLayout.SOUTH);

        // set a reasonable size
        Dimension size = new Dimension(640, 480);
        setPreferredSize(size);
        setSize(size);
        
        return result;
    }

    private JPanel createDialogButtons() {
        Action okAction = new OkAction("Ok");
        Action cancelAction = new CancelAction("Cancel");
        JButton okButton = new JButton(okAction);
        JButton cancelButton = new JButton(cancelAction);

        JPanel dialogButtons = new JPanel();
        dialogButtons.setLayout(new BoxLayout(dialogButtons, BoxLayout.LINE_AXIS));
        dialogButtons.add(Box.createHorizontalGlue());
        dialogButtons.add(okButton);
        dialogButtons.add(Box.createHorizontalStrut(5));
        dialogButtons.add(cancelButton);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));
        buttonPanel.add(Box.createVerticalStrut(5));
        buttonPanel.add(dialogButtons);
        
        getRootPane().setDefaultButton(okButton);
        return buttonPanel;
    }
    
    private JPanel createManagePanel() {
        JPanel managePanel = new JPanel();
        managePanel.setLayout(new BorderLayout());
        
        JPanel upDownPanel = new JPanel();
        managePanel.add(upDownPanel, BorderLayout.EAST);
        
        removeAction = new RemoveAction("remove", GUIUtils.getImageIcon(iconUrls.get("remove"), logArea));
        upAction = new UpAction("up", GUIUtils.getImageIcon(iconUrls.get("moveUp"), logArea));
        downAction = new DownAction("down", GUIUtils.getImageIcon(iconUrls.get("moveDown"), logArea));
        newAction = new NewAction("new", GUIUtils.getImageIcon(iconUrls.get("new"), logArea));

        JButton newButton = GUIUtils.createIconOnlyButton(newAction);
        JButton upButton = GUIUtils.createIconOnlyButton(upAction);
        JButton downButton = GUIUtils.createIconOnlyButton(downAction);

        upDownPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        upDownPanel.add(newButton, c);
        upDownPanel.add(Box.createVerticalStrut(5), c);
        upDownPanel.add(upButton, c);
        upDownPanel.add(Box.createVerticalStrut(5), c);
        upDownPanel.add(downButton, c);
        
        tableModel = new DefaultTableModel() {

            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
            
        };
        
        tableModel.addColumn("Title");
        tableModel.addColumn("Application");
        tableModel.addColumn("Arguments");
        tableModel.addColumn("Actions");
        
        table = new JTable(tableModel);
        applicationCellEditor = new ApplicationCellEditor();
        actionCellEditor = new ActionCellEditor();
        Dimension cellSize = actionCellEditor.renderPanel.getPreferredSize();
        table.setRowHeight(cellSize.height);

        TableColumnModel columnModel = table.getColumnModel();
        int actionColumnWidth = cellSize.width;
        if (actionColumnWidth < table.getTableHeader().getColumnModel().getColumn(3).getPreferredWidth()) {
            actionColumnWidth = table.getTableHeader().getColumnModel().getColumn(3).getPreferredWidth();
        }
        columnModel.getColumn(3).setPreferredWidth(actionColumnWidth);
        columnModel.getColumn(3).setMinWidth(actionColumnWidth);
        columnModel.getColumn(3).setMaxWidth(actionColumnWidth);
        // FIXME: setPreferredWidth is ignored. We do not want set[Min/Max]Width because
        // that disables resizing
        columnModel.getColumn(0).setPreferredWidth(50);
        columnModel.getColumn(1).setPreferredWidth(200);
        columnModel.getColumn(2).setPreferredWidth(50);

        selectionModel = table.getSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        columnModel.getColumn(1).setCellRenderer(new ApplicationCellEditor());
        columnModel.getColumn(1).setCellEditor(new ApplicationCellEditor());
        
        columnModel.getColumn(3).setCellRenderer(actionCellEditor);
        columnModel.getColumn(3).setCellEditor(actionCellEditor);
        
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        managePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        return managePanel;
    }

    private void loadMimeTypeRegistry() {
        tableModel.setRowCount(0);
        localRegistry = new MimeTypeRegistry(null);
        
        MimeTypeEntry entry = mainRegistry.getMimeTypeEntry(mimeType).deepCopy();

        localRegistry.setMimeTypeEntry(mimeType, entry);

        defaultRowIdx = -1;
        List<ApplicationEntry> appEntries = entry.getApplicationEntries();
        for (int i = 0; i < appEntries.size(); i++) {
            ApplicationEntry appEntry = appEntries.get(i);
            if (appEntry.equals(entry.getDefaultApplication())) {
                defaultRowIdx = i;
            }
            addRow(appEntry);
        }
    }

    private void insertRow(ApplicationEntry app) {
        int idx = selectionModel.getLeadSelectionIndex();
        if (idx < 0) {
            idx = table.getRowCount();
        } else if (idx >= table.getRowCount() - 1) {
            idx = table.getRowCount();
        } else {
            idx ++;
        }
        tableModel.insertRow(idx, new Object[] { app.getTitle(), app.getApplication(), app.getArguments(), null });
        selectionModel.setSelectionInterval(idx, idx);
        defaultRowIdx = idx;
    }

    private void addRow(ApplicationEntry app) {
        tableModel.addRow(new Object[] { app.getTitle(), app.getApplication(), app.getArguments(), null });
    }

    private class OkAction extends AbstractAction {

        public OkAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
                return;
            }
            
            returnOk = true;
            MimeTypeDialog.this.setVisible(false);
        
            // update the mimetype entry
            MimeTypeEntry localMimeTypeEntry = new MimeTypeEntry();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String title = (String)tableModel.getValueAt(i, 0);
                String application = (String)tableModel.getValueAt(i, 1);
                String args = (String)tableModel.getValueAt(i, 2);
                ApplicationEntry appEntry = new ApplicationEntry(title, application, args);
                localMimeTypeEntry.addApplicationEntry(appEntry);
                if (defaultRowIdx == i) {
                    localMimeTypeEntry.setDefaultApplicationEntry(appEntry);
                }
            }
            
            //ApplicationEntry currentDefault = mainRegistry.getMimeTypeEntry(mimeType).getDefaultApplication();
            //List<ApplicationEntry> entries = new ArrayList(localMimeTypeEntry.getApplicationEntries());
            
            mainRegistry.setMimeTypeEntry(mimeType, localMimeTypeEntry);
            mainRegistry.store();
        }
        
    }
    
    private class CancelAction extends AbstractAction {

        public CancelAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            closeCancel();
        }
        
    }
    
    private void closeCancel() {
        if (table.isEditing()) {
            table.getCellEditor().cancelCellEditing();
        }

        returnOk = false;
        setVisible(false);
    }

    public boolean isReturnOk() {
        return returnOk;
    }
    
    /**
     * Cell that renders a panel with the actions "remove", "up", "down"
     * @author karel
     */
    public class ActionCellEditor extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {
        private Object editorValue;
        protected ActionPanel renderPanel;
        protected ActionPanel editPanel;
        
        public ActionCellEditor() {
            renderPanel = new ActionPanel();
            editPanel = new ActionPanel();
        }
        
        public Object getCellEditorValue() {
            return editorValue;
        }

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            renderPanel.prepare(value, isSelected, hasFocus);
            return renderPanel;
        }

        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {
            editPanel.prepare(value, true, true);
            return editPanel;
        }
    }
    
    /**
     * Cell that renders a panel with the actions "remove", "up", "down"
     * @author karel
     */
    public class ApplicationCellEditor extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {
        private Object editorValue;
        protected ApplicationPanel renderPanel;
        protected ApplicationPanel editPanel;
        
        public ApplicationCellEditor() {
            renderPanel = new ApplicationPanel();
            editPanel = new ApplicationPanel();
        }
        
        public Object getCellEditorValue() {
            return editPanel.textField.getText();
            
        }

        public boolean stopCellEditing() {
            String txt = editPanel.textField.getText();
            File f = new File(txt);
            if (f.exists() || new File(txt + ".app").exists()) {
                fireEditingStopped();
                return true;
            } else {
                JOptionPane.showMessageDialog(table, "The file '"+txt+"' does not exist.");
                return false;
            }
        }

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            renderPanel.prepare(value, isSelected, hasFocus);
            return renderPanel;
        }

        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {
            editPanel.prepare(value, true, true);
            editPanel.textField.setCaretPosition(renderPanel.textField.getText().length());
            return editPanel;
        }
    }
    
    public class ActionPanel extends JPanel {
        private Color defaultBg;
        
        public ActionPanel() {
            defaultBg = getBackground();

            JButton removeButton = GUIUtils.createIconOnlyButton(removeAction);

            setLayout(new GridBagLayout());
            this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
            add(removeButton);
        }

        public void prepare(Object value, boolean isSelected, boolean hasFocus) {
            setBackground(isSelected?UIManager.getColor("Table.selectionBackground"):defaultBg);
        }
    }
    
    public class ApplicationPanel extends JPanel {
        private JTextField textField;
        private Color defaultBg;
        
        public ApplicationPanel() {
            defaultBg = getBackground();
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

            textField = new JTextField();
            textField.add(Box.createHorizontalGlue());
            add(textField);
            add(new JButton(new ShowFileChooserAction("...", textField)));
        }

        public void prepare(Object value, boolean isSelected, boolean hasFocus) {
            textField.setText(value == null? "": value.toString());
            setBackground(isSelected?UIManager.getColor("Table.selectionBackground"):defaultBg);
        }
    }
    
    private class ShowFileChooserAction extends AbstractAction {
        JFileChooser fileChooser = new JFileChooser();
        JTextField target;
        
        public ShowFileChooserAction(String name, JTextField target) {
            super(name);
            this.target = target;
        }

        public void actionPerformed(ActionEvent e) {
            fileChooser.setSelectedFile(new File(target.getText()));
            int answer = fileChooser.showDialog(MimeTypeDialog.this, "Select");
            if (answer == JFileChooser.APPROVE_OPTION) {
                target.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        }
        
    }

    private class RemoveAction extends AbstractAction {
        public RemoveAction(String name) {
            super(name);
        }
        
        public RemoveAction(String name, Icon icon) {
          super(name, icon);
        }

        public void actionPerformed(ActionEvent e) {
            int idx = selectionModel.getMinSelectionIndex();
          
            // important: without this the editor is rendered even after the row is removed
            table.getCellEditor().stopCellEditing();

            if (defaultRowIdx > idx) {
                defaultRowIdx--;
            } else if (defaultRowIdx == idx) {
                defaultRowIdx = -1;
            }
            tableModel.removeRow(idx);
            if (idx >= tableModel.getRowCount()) {
                selectionModel.setSelectionInterval(tableModel.getRowCount() - 1, tableModel.getRowCount() - 1);
            } else {
                selectionModel.setSelectionInterval(idx, idx);
            }
            table.repaint();
        }
       
   }
   
   private class UpAction extends AbstractAction {
       
       public UpAction(String name) {
           super(name);
       }
       
       public UpAction(String name, Icon icon) {
           super(name, icon);
       }

       public void actionPerformed(ActionEvent e) {
           int idx = selectionModel.getLeadSelectionIndex();
           if (idx > 0 && idx < tableModel.getRowCount()) {
               System.out.println(idx);
               tableModel.moveRow(idx, idx, idx - 1);
               selectionModel.clearSelection();
               selectionModel.setSelectionInterval(idx - 1 , idx - 1);
           }
       }
   }

   private class DownAction extends AbstractAction {
       
       public DownAction(String name) {
           super(name);
       }
   
       public DownAction(String name, Icon icon) {
           super(name, icon);
       }

       public void actionPerformed(ActionEvent e) {
           int idx = selectionModel.getLeadSelectionIndex();
           if (idx >= 0 && idx < tableModel.getRowCount() - 1) {
               tableModel.moveRow(idx, idx, idx + 1);
               selectionModel.clearSelection();
               selectionModel.setSelectionInterval(idx + 1 , idx + 1);
           }
       }
   }

   private class NewAction extends AbstractAction {
       
       public NewAction(String name) {
           super(name);
       }
   
       public NewAction(String name, Icon icon) {
           super(name, icon);
       }

       public void actionPerformed(ActionEvent e) {
           applicationDialog.setVisible(true);
       }
   }

}
