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
package org.outerj.daisy.textextraction.impl;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.outerj.daisy.textextraction.TextExtractor;
import org.outerj.daisy.plugin.PluginRegistry;

/**
 * Text extractor for Microsoft Excel files.
 */
public class MSExcelTextExtractor extends AbstractTextExtractor implements TextExtractor {

    public MSExcelTextExtractor() {
        super();
    }

    public MSExcelTextExtractor(List<String> mimeTypes, PluginRegistry pluginRegistry) {
        super(mimeTypes, pluginRegistry);
    }

    protected String getName() {
        return getClass().getName();
    }

    public String getText(InputStream is) throws Exception {
        HSSFWorkbook excelWb = new HSSFWorkbook(is);
        StringBuilder contentBuffer = new StringBuilder();
        //contentBuffer.append("starting extraction\n");
        int numberOfSheets = excelWb.getNumberOfSheets();
        //contentBuffer.append("Number of sheets: " + numberOfSheets + "\n");
        for(int i=0; i<numberOfSheets; i++) {
            HSSFSheet sheet = excelWb.getSheetAt(i);
            int numberOfRows = sheet.getPhysicalNumberOfRows();
            if(numberOfRows > 0) {
                //contentBuffer.append("Number of rows: " + numberOfRows + "\n");
                Iterator rowIt = sheet.rowIterator();
                while(rowIt.hasNext()) {
                    HSSFRow row = (HSSFRow)rowIt.next();
                    if(row != null) {
                        Iterator it = row.cellIterator();
                        while(it.hasNext()) {
                            HSSFCell cell = (HSSFCell)it.next();
                            switch(cell.getCellType()) {
                                case HSSFCell.CELL_TYPE_NUMERIC:
                                    String num = Double.toString(cell.getNumericCellValue()).trim();
                                    if(num.length() > 0)
                                        contentBuffer.append(num).append(" ");
                                    break;
                                case HSSFCell.CELL_TYPE_STRING:
                                    try {
                                        String text = cell.getStringCellValue().trim();
                                        if(text.length() > 0)
                                            contentBuffer.append(text).append(" ");
                                    }
                                    catch(Exception e) {
                                    }
                                    break;
                                default:
                                    //might cause error !!!
                                    try {
                                        String otext = cell.getStringCellValue().trim();
                                        if(otext.length() > 0)
                                            contentBuffer.append(otext).append(" ");
                                    }
                                    catch(Exception e) {
                                    }
                                    break;
                            }
                        }
                    }
                }
            }
        }
        
        return contentBuffer.toString();
    }
}
