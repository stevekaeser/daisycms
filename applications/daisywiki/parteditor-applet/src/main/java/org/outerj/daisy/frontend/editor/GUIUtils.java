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

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;

public class GUIUtils {
    
    public static JButton createIconOnlyButton(Action action) {
        JButton result = new JButton(action);
        if (result.getIcon() == null || result.getIcon().getIconHeight() >= 0) {
            result.setText("");
        }
        return result;
    }

    public static ImageIcon getImageIcon(String urlString, LogTextArea logArea) {
        ImageIcon icon = null;
        try {
            URL url = new URL(urlString);
            icon = new ImageIcon(url);
        } catch (MalformedURLException url) {
            // TODO: logging
        }
        
        if (icon == null || icon.getIconWidth() < 0) {
            return new ImageIcon();
        }
        
        return icon;
    }

}
