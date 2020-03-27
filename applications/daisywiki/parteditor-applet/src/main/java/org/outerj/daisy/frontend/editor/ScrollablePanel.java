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
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

public class ScrollablePanel extends JComponent {
    
    private final LogTextArea logArea;
    private JButton scrollLeft;
    private JButton scrollRight;
    private JComponent component;
    private JScrollPane scrollPane;

    public int selectedPosition = 0;
    public Component selected = null;
    
    public ScrollablePanel(LogTextArea logArea, final JComponent component) {
        this.logArea = logArea;
        setLayout(new BorderLayout());
        if (component == null) {
            throw new NullPointerException("component should not be null");
        }
        this.component = component;
        scrollPane = new JScrollPane(component, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        
        scrollLeft = new JButton(new AbstractAction("<") {
            public void actionPerformed(ActionEvent e) {
                scrollLeft();
            }
        });
        scrollRight = new JButton(new AbstractAction(">") {
            public void actionPerformed(ActionEvent e) {
                scrollRight();
            }
        });
        add(scrollLeft, BorderLayout.WEST);
        add(scrollPane, BorderLayout.CENTER);
        add(scrollRight, BorderLayout.EAST);
        
        if ( component.getComponentCount() > 0) {
            selected = component.getComponent(0);
        }
    }
    
    public void scrollLeft() {
        if (selectedPosition == 0) 
            return;
        Component[] c = component.getComponents();
        selectedPosition--;
        centerComponent(c[selectedPosition]);
    }
    
    public void scrollRight() {
        if (selectedPosition >= getComponentCount() - 1) 
            return;
        Component[] c = component.getComponents();
        selectedPosition++;
        centerComponent(c[selectedPosition]);
    }

    /** 
     * horizontally scroll the viewport to the selected component
     * @param child
     */
    public void centerComponent(Component child) {
        Rectangle bounds = child.getBounds();
        Rectangle viewRect = scrollPane.getViewport().getViewRect();
        
        bounds.x -= (viewRect.width - bounds.width) / 2;
        bounds.width = viewRect.width;
        bounds.y = 0;
        
        scrollRectToVisible(bounds);
    }
    
   // workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6333318
    // (example code found in bug description)
    public void scrollRectToVisible( Rectangle contentRectangle )
    {
       logArea.log("content rect: " + contentRectangle);
        
       // I had to right all this code because JViewPort's 'scrollRectToVisible()'
       // appears to have a bug. It won't scroll for you if your content area
       // ( rectangle ) is above or to the left of the viewport's rectangle??
       Rectangle viewportRectangle = scrollPane.getViewport().getViewRect();
       logArea.log("viewportRect: " + viewportRectangle);
       if ( !viewportRectangle.contains( contentRectangle ) )
       {
          int outCode = viewportRectangle.outcode( contentRectangle.getLocation() );
          if ( ( outCode & Rectangle2D.OUT_LEFT ) > 0 || ( outCode & Rectangle2D.OUT_TOP ) > 0 )
             scrollPane.getViewport().setViewPosition( contentRectangle.getLocation() );
          else
          {
             outCode = viewportRectangle.outcode( new Point( contentRectangle.x + contentRectangle.width, contentRectangle.y + contentRectangle.height ) );
             if ( ( outCode & Rectangle2D.OUT_RIGHT ) > 0 || ( outCode & Rectangle2D.OUT_BOTTOM ) > 0 )
                scrollPane.getViewport().scrollRectToVisible( contentRectangle );
          }
       }
    }

}
