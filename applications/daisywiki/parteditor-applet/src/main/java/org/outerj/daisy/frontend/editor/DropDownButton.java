/*
 *  copied (with modifications) from: Process Dashboard - Data Automation Tool for high-maturity processes
 *  Copyright (C) 2003-2008 Tuma Solutions, LLC
 *  The author(s) may be contacted at:
 *  Process Dashboard Group
 *  c/o Ken Raisor
 *  6137 Wardleigh Road
 *  Hill AFB, UT 84056-5843
 *  E-Mail POC:  processdash-devel@lists.sourceforge.net
 *  
 * The original code is GPL, licence changed to ASL with permission.
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;

public class DropDownButton extends JPanel {

    public static final int NO_ACTION = 0;
    public static final int RUN_FIRST_MENU_OPTION = 1;
    public static final int OPEN_DROP_DOWN_MENU = 2;

    private JMenuBar menuBar;
    private DDMButton mainButton;
    private JButton dropDownButton;
    private boolean dropDownEnabled = false;
    private int mainButtonDefaultAction = RUN_FIRST_MENU_OPTION;
    private Icon enabledDownArrow, disDownArrow;
    private DropDownMenu menu;
    private MainButtonListener mainButtonListener = new MainButtonListener();

    public DropDownButton()                 { this(new DDMButton());     }
    public DropDownButton(Action a)         { this(new DDMButton(a));    }
    public DropDownButton(String text)      { this(new DDMButton(text)); }

    private DropDownButton(DDMButton main_button) {
        // create the drop down menu and register listeners
        menu = new DropDownMenu();
        menu.getPopupMenu().addContainerListener(new MenuContainerListener());
        menuBar = new JMenuBar();
        menuBar.add(menu);

        // save the main button and register listeners
        mainButton = main_button;
        mainButton.addActionListener(mainButtonListener);

        // create the drop-down button and register listeners
        enabledDownArrow = new SmallDownArrow();
        disDownArrow = new SmallDisabledDownArrow();
        dropDownButton = new JButton(disDownArrow);
        dropDownButton.setDisabledIcon(disDownArrow);
        dropDownButton.addMouseListener(new DropDownListener());
        dropDownButton.setFocusPainted(false);

        // perform operating-system-specific tweaks on our components
        configureGUI();

        // add our elements to the component hierarchy
        add(dropDownButton);
        add(mainButton);
        add(menuBar);

        setEnabled(false);
    }

    private void configureGUI() {
        setBackground(null);
        setOpaque(false);
        if (UIManager.getLookAndFeel().getID().equals("Aqua"))
            configureMacGUI();
        else
            configureNormalGUI();
    }

    private void configureNormalGUI() {
        setLayout(new NormalDropDownButtonLayout());

        mainButton.setBorder(new RightChoppedBorder(mainButton.getBorder(), 2));
    }

    private void configureMacGUI() {
        setLayout(new MacDropDownButtonLayout());
        
        int leftBlank = (isTextButton() ?
                DROP_DOWN_LEFT_PAD_TEXT : DROP_DOWN_LEFT_PAD_ICON);
        dropDownButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, Color.gray),
            BorderFactory.createEmptyBorder(0, leftBlank, 0, 0)));
        dropDownButton.setHorizontalAlignment(SwingConstants.LEFT);
    }

    /** Return true if this button is displaying text, false if it is
     * displaying an icon.  (Note that displaying BOTH is not supported
     * by this class at this time.)
     */
    private boolean isTextButton() {
        return mainButton.getText() != null && mainButton.getText().trim().length() != 0;
    }

    /** Request a specific margin around the button contents.
     *
     * NOTE: this method must be called before any icons are configured for
     * this drop-down-button.
     */
    protected void setMainButtonMargin(Insets i) {
        if (!UIManager.getLookAndFeel().getID().equals("Aqua"))
            mainButton.setMargin(i);
        mainButton.extraPadding = i.right;
    }


    public JButton getButton()         { return mainButton;     }
    //public JButton getDropDownButton() { return dropDownButton; }
    public JMenu   getMenu()           { return menu;           }

    public void setEnabled(boolean enable) {
        mainButton.setEnabled(enable);
        dropDownButton.setEnabled(enable);
    }
    public boolean isEnabled() {
        return mainButton.isEnabled();
    }
    public boolean isEmpty() {
        return (menu.getItemCount() == 0);
    }

    /** Set the behavior of the main button.
     *
     * @param enable if true, a click on the main button will trigger
     *    an actionPerformed() on the first item in the popup menu.
     */
    public void setRunFirstMenuOption(boolean enable) {
        setMainButtonBehavior(enable ? RUN_FIRST_MENU_OPTION : NO_ACTION);
    }

    /** @return true if a click on the main button will trigger an
     *    actionPerformed() on the first item in the popup menu.
     */
    public boolean getRunFirstMenuOption() {
        return mainButtonDefaultAction == RUN_FIRST_MENU_OPTION;
    }

    /** Set the behavior of the main button.
     *
     * @param enable if true, a click on the main button will cause the drop
     *    down menu to be displayed.
     */
    public void setOpenPopupMenu(boolean enable) {
        setMainButtonBehavior(enable ? OPEN_DROP_DOWN_MENU : NO_ACTION);
    }

    /** @return true if a click on the main button will cause the drop down
     *    menu to be displayed.
     */
    public boolean getOpenPopupMenu() {
        return mainButtonDefaultAction == OPEN_DROP_DOWN_MENU;
    }

    public void setMainButtonBehavior(int which) {
        mainButton.removeActionListener(mainButtonListener);
        mainButtonDefaultAction = which;
        setEnabled(which == NO_ACTION || isEmpty() == false);
        if (which != NO_ACTION)
            mainButton.addActionListener(mainButtonListener);
    }

    private void setDropDownEnabled(boolean enable) {
        dropDownEnabled = enable;
        dropDownButton.setIcon(enable ? enabledDownArrow : disDownArrow);
        if (mainButtonDefaultAction != NO_ACTION)
            setEnabled(enable);
    }

    @Override
    public Dimension getMaximumSize() {
        return super.getPreferredSize();
    }

    /** This object responds to events on the main button. */
    private class MainButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (getRunFirstMenuOption() && !isEmpty()) {
                JMenuItem defaultItem = menu.getItem(0);
                if (defaultItem != null)
                    defaultItem.doClick(0);
            } else if (getOpenPopupMenu() && !isEmpty()) {
                if (dropDownEnabled) menu.doClick(0);
            }
        }
    }

    /** This object responds to events on the drop-down button. */
    private class DropDownListener extends MouseAdapter {
        boolean pressHidPopup = false;
        public void mouseClicked(MouseEvent e) {
            if (dropDownEnabled && !pressHidPopup) menu.doClick(0);
        }
        public void mousePressed(MouseEvent e) {
            if (dropDownEnabled) menu.dispatchMouseEvent(e);
            if (menu.isPopupMenuVisible())
                pressHidPopup = false;
            else
                pressHidPopup = true;

        }
        public void mouseReleased(MouseEvent e) { }
    }

    /** This object watches for insertion/deletion of menu items in
     * the popup menu, and disables the drop-down button when the
     * popup menu becomes empty. */
    private class MenuContainerListener implements ContainerListener {
        public void componentAdded(ContainerEvent e) {
            setDropDownEnabled(true);
        }
        public void componentRemoved(ContainerEvent e) {
            setDropDownEnabled(!isEmpty());
        }
    }

    /** An adapter that wraps a border object, and chops some number of
     *  pixels off the right hand side of the border.
     */
    private class RightChoppedBorder implements Border {
        private Border b;
        private int w;

        public RightChoppedBorder(Border b, int width) {
            this.b = b;
            this.w = width;
        }

        public void paintBorder(Component c,
                                Graphics g,
                                int x,
                                int y,
                                int width,
                                int height) {
            Shape clipping = g.getClip();
            g.setClip(x, y, width, height);
            b.paintBorder(c, g, x, y, width + w, height);
            g.setClip(clipping);
        }

        public Insets getBorderInsets(Component c) {
            Insets i = b.getBorderInsets(c);
            return new Insets(i.top, i.left, i.bottom, i.right-w);
        }

        public boolean isBorderOpaque() {
            return b.isBorderOpaque();
        }
    }

    private class DropDownMenu extends JMenu {
        public void dispatchMouseEvent(MouseEvent e) {
            processMouseEvent(e);
        }
    }


    /** An icon to draw a small downward-pointing arrow.
     */
    private static class SmallDownArrow implements Icon {

        Color arrowColor = Color.black;

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(arrowColor);
            g.drawLine(x, y, x+4, y);
            g.drawLine(x+1, y+1, x+3, y+1);
            g.drawLine(x+2, y+2, x+2, y+2);
        }

        public int getIconWidth() {
            return 6;
        }

        public int getIconHeight() {
            return 4;
        }

    }

    /** An icon to draw a disabled small downward-pointing arrow.
     */
    private static class SmallDisabledDownArrow extends SmallDownArrow {

        public SmallDisabledDownArrow() {
            arrowColor = new Color(140, 140, 140);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            super.paintIcon(c, g, x, y);
            g.setColor(Color.red);
            g.drawLine(x+3, y+2, x+4, y+1);
            g.drawLine(x+3, y+3, x+5, y+1);
        }
    }

    /** A specialized class for the main button; appends extra space to the
     * right edge of the button on Mac OS X.  On other platforms, behaves
     * exactly like a plain JButton.
     */
    private static class DDMButton extends JButton {

        private int extraPadding = 2;

        public DDMButton()            {}
        public DDMButton(Action a)    { super(a); }
        public DDMButton(Icon icon)   { super(icon); }
        public DDMButton(String text) { super(text); }

        @Override
        public void setIcon(Icon icon) {
            super.setIcon(padIcon(icon));
        }

        @Override
        public void setDisabledIcon(Icon icon) {
            super.setDisabledIcon(padIcon(icon));
        }

        @Override
        public void setDisabledSelectedIcon(Icon icon) {
            super.setDisabledSelectedIcon(padIcon(icon));
        }

        private Icon padIcon(Icon icon) {
            if (UIManager.getLookAndFeel().getID().equals("Aqua") && icon != null)
                return new PaddedIcon(icon, 2, 0, 0,
                    MAC_EXTRA_PAD_FOR_DROP_DOWN + extraPadding);
            else
                return icon;
        }

        @Override
        public void setText(String text) {
            if (UIManager.getLookAndFeel().getID().equals("Aqua") && text != null) {
                super.setText(text + "  ");
            } else {
                super.setText(text);
            }
        }
        
    }

    /** Custom layout manager to arrange our children on PC/Unix systems.
     */
    private class NormalDropDownButtonLayout implements LayoutManager {

        private Rectangle bounds = new Rectangle();

        public void layoutContainer(Container parent) {
            parent.getBounds(bounds);
            menuBar.setBounds(0, 0, 0, bounds.height);
            mainButton.setBounds(0, 0, bounds.width - PC_DROP_DOWN_WIDTH,
                bounds.height);
            dropDownButton.setBounds(bounds.width - PC_DROP_DOWN_WIDTH, 0,
                PC_DROP_DOWN_WIDTH, bounds.height);
        }

        public Dimension minimumLayoutSize(Container parent) {
            Dimension result = mainButton.getMinimumSize();
            result.width += PC_DROP_DOWN_WIDTH;
            return result;
        }

        public Dimension preferredLayoutSize(Container parent) {
            Dimension result = mainButton.getPreferredSize();
            result.width += PC_DROP_DOWN_WIDTH;
            return result;
        }

        public void addLayoutComponent(String name, Component comp) {}
        public void removeLayoutComponent(Component comp) {}

    }

    /** Custom layout manager to arrange our children on Mac OS X systems.
     */
    private class MacDropDownButtonLayout implements LayoutManager {

        private Rectangle bounds = new Rectangle();
        private Insets mainInsets = new Insets(0,0,0,0);

        public void layoutContainer(Container parent) {
            parent.getBounds(bounds);

            mainButton.setBounds(0, 0, bounds.width, bounds.height);
            mainInsets = mainButton.getMargin();

            int ddWidth, top, bottom;
            if (isTextButton()) {
                ddWidth = mainInsets.right + MAC_DROP_DOWN_LEFT_ADJUST_TEXT;
                top = mainInsets.top;
                bottom = mainInsets.bottom;
            } else {
                ddWidth = mainInsets.right + MAC_DROP_DOWN_LEFT_ADJUST_ICON;
                top = mainInsets.top - MAC_ICON_BORDER_EXTRA_TOP;
                bottom = mainInsets.bottom - MAC_ICON_BORDER_EXTRA_BOTTOM;
            }
            dropDownButton.setBounds(
                bounds.width - ddWidth,        // left edge of drop down button
                top,                           // top edge of drop down button
                ddWidth,                       // width of drop down button
                bounds.height - top - bottom); // height of drop down button

            menuBar.setBounds(mainInsets.left, 0, 0, bounds.height - bottom);
        }

        public Dimension minimumLayoutSize(Container parent) {
            return mainButton.getMinimumSize();
        }

        public Dimension preferredLayoutSize(Container parent) {
            return mainButton.getPreferredSize();
        }

        public void addLayoutComponent(String name, Component comp) {}
        public void removeLayoutComponent(Component comp) {}

    }
    
    public static class PaddedIcon implements Icon {

        private Icon delegate;
        private int top, left, bottom, right;

        public PaddedIcon(Icon delegate, int top, int left, int bottom, int right) {
            this.delegate = delegate;
            this.top = top;
            this.left = left;
            this.bottom = bottom;
            this.right = right;
        }

        public int getIconHeight() {
            return delegate.getIconHeight() + top + bottom;
        }

        public int getIconWidth() {
            return delegate.getIconWidth() + left + right;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            delegate.paintIcon(c, g, x+left, y+top);
        }
        
    }


    private static final int PC_DROP_DOWN_WIDTH = 11;
    private static final int DROP_DOWN_LEFT_PAD_ICON = 2;
    private static final int DROP_DOWN_LEFT_PAD_TEXT = 3;
    private static final int MAC_EXTRA_PAD_FOR_DROP_DOWN = 8;
    private static final int MAC_DROP_DOWN_LEFT_ADJUST_TEXT = 1;
    private static final int MAC_DROP_DOWN_LEFT_ADJUST_ICON = 6;
    private static final int MAC_ICON_BORDER_EXTRA_TOP = 1;
    private static final int MAC_ICON_BORDER_EXTRA_BOTTOM = 4;
}
