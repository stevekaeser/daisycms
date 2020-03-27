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
package org.outerj.daisy.presavehook.image;

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.spi.local.PreSaveHook;
import org.outerj.daisy.plugin.PluginRegistry;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.IIOImage;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.annotation.PreDestroy;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.concurrent.Semaphore;

import com.sun.image.codec.jpeg.JPEGImageDecoder;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGDecodeParam;
import com.drew.metadata.Metadata;
import com.drew.metadata.Directory;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectory;
import com.drew.imaging.jpeg.JpegMetadataReader;

/**
 * A pre-save hook for extracting information from images and generating
 * thumbnail & preview images. It can be configured to work for multiple
 * document types.
 *
 */
public class ImagePreSaveHook implements PreSaveHook {
    private Log log = LogFactory.getLog(getClass());
    private PluginRegistry pluginRegistry;
    private Semaphore imageProcessingSemaphore;
    private Map<String, DoctypeConfig> docTypeConfigs = new HashMap<String, DoctypeConfig>();
    private static final String NAME = "image-pre-save-hook";

    public ImagePreSaveHook(Configuration configuration, PluginRegistry pluginRegistry) throws ConfigurationException {
        this.pluginRegistry = pluginRegistry;
        configure(configuration);
        pluginRegistry.addPlugin(PreSaveHook.class, NAME, this);
    }

    @PreDestroy
    public void destroy() {
        pluginRegistry.removePlugin(PreSaveHook.class, NAME, this);
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        int concurrentOperations = configuration.getChild("maxConcurrentOperations").getValueAsInteger();
        imageProcessingSemaphore = new Semaphore(concurrentOperations, true);

        Configuration[] configs = configuration.getChildren("config");
        for (Configuration config : configs) {
            String docType = config.getAttribute("documentType");

            if (docTypeConfigs.containsKey(docType)) {
                // user overwritten configs seem to come before built-in ones, so use the
                // first one
                continue;
            }

            DoctypeConfig dtconfig = new DoctypeConfig();
            dtconfig.enabled = config.getChild("enabled").getValueAsBoolean(true);
            dtconfig.maxImageSize = config.getChild("maxImageSize").getValueAsInteger(3000000);
            dtconfig.imagePartName = config.getChild("imagePartName").getValue();
            dtconfig.widthFieldName = config.getChild("widthFieldName").getValue(null);
            dtconfig.heightFieldName = config.getChild("heightFieldName").getValue(null);
            dtconfig.previewPartName = config.getChild("previewPartName").getValue(null);
            dtconfig.previewMaxSize = config.getChild("previewMaxSize").getValueAsInteger(250);
            dtconfig.thumbnailPartName = config.getChild("thumbnailPartName").getValue(null);
            dtconfig.thumbnailMaxSize = config.getChild("thumbnailMaxSize").getValueAsInteger(125);
            dtconfig.exifRotate = config.getChild("automaticRotationEnabled").getValueAsBoolean(true);
            dtconfig.jpegQuality = config.getChild("jpegQuality").getValueAsFloat(.85f);

            Configuration[] metadatas = config.getChildren("metadata");
            for (Configuration metadata : metadatas) {
                String type = metadata.getAttribute("type");
                if (!METADATA_VALUE_GETTERS.containsKey(type))
                    throw new ConfigurationException("ImagePreSaveHook: metadata/@type has an invalid value: " + type + " at " + metadata.getLocation());
                dtconfig.addMetadata(metadata.getAttribute("tag"), metadata.getAttribute("field"), type);
            }

            docTypeConfigs.put(docType, dtconfig);
        }
    }

    public void process(Document document, Repository repository) throws Exception {
        // The semaphore is to avoid that dozens of threads would start concurrently generating image thumbnails,
        // which makes little sense as this is not much I/O bound and could eat lots of memory
        imageProcessingSemaphore.acquire();
        try {
            String documentTypeName = repository.getRepositorySchema().getDocumentTypeById(document.getDocumentTypeId(), false).getName();
            DoctypeConfig dtconfig = docTypeConfigs.get(documentTypeName);
            if (dtconfig == null)
                return;
            if (!dtconfig.enabled)
                return;

            Part part = null;
            if (document.hasPart(dtconfig.imagePartName)) {
                part = document.getPart(dtconfig.imagePartName);
            }

            // If the image didn't change and the extracted data is present, don't redo the work.
            if (part != null && part.getDataChangedInVersion() != -1
                    && (dtconfig.previewPartName == null || document.hasPart(dtconfig.previewPartName))
                    && (dtconfig.thumbnailPartName == null || document.hasPart(dtconfig.thumbnailPartName))
                    && (dtconfig.widthFieldName == null || document.hasField(dtconfig.widthFieldName))
                    && (dtconfig.heightFieldName == null || document.hasField(dtconfig.heightFieldName))) {
                return;
            }

            // First, clear out all data which is automatically assigned, so that if extraction of image
            // information fails for some reason, no old data is left in the automatically assigned parts and fields
            if (dtconfig.previewPartName != null)
                document.deletePart(dtconfig.previewPartName);
            if (dtconfig.thumbnailPartName != null)
                document.deletePart(dtconfig.thumbnailPartName);
            if (dtconfig.widthFieldName != null)
                document.deleteField(dtconfig.widthFieldName);
            if (dtconfig.heightFieldName != null)
                document.deleteField(dtconfig.heightFieldName);
            Iterator metadataInfoIt = dtconfig.getMetadataInfoIterator();
            while (metadataInfoIt.hasNext()) {
                MetadataInfo metadataInfo = (MetadataInfo)metadataInfoIt.next();
                document.deleteField(metadataInfo.field);
            }

            // If there's no image data, skip the rest
            if (part == null)
                return;

            // Protection against too large images
            long size = part.getSize();
            if (size > dtconfig.maxImageSize) {
                if (log.isInfoEnabled()) {
                    log.info("Skipped image information extraction as the image was too large.");
                }
            } else if (size == 0) {
                log.info("Skipped image information extraction as the image size was unknown.");
            }

            // Read the image
            InputStream is = null;
            BufferedImage sourceImage;
            Metadata metadata = null;
            try {
                is = part.getDataStream();
                String mimeType = part.getMimeType();
                if (mimeType.equals("image/jpeg") || mimeType.equals("image/x-jpeg")) {
                    JPEGImageDecoder jpegDecoder = JPEGCodec.createJPEGDecoder(is);
                    sourceImage = jpegDecoder.decodeAsBufferedImage();
                    JPEGDecodeParam decodeParam = jpegDecoder.getJPEGDecodeParam();
                    metadata = JpegMetadataReader.readMetadata(decodeParam);
                } else {
                    sourceImage = ImageIO.read(is);
                }
            } finally {
                if (is != null)
                    is.close();
            }

            if (sourceImage == null) {
                // The image could not be read (unsupported format)
                return;
            }

            int exifOrientation = 1;
            if (metadata != null && (dtconfig.isSetMetadata() || dtconfig.exifRotate)) {
                Directory dir = metadata.getDirectory(ExifDirectory.class);
                MetadataInfo metadataInfo;

                Iterator tagIt = dir.getTagIterator();
                while (tagIt.hasNext()) {
                    Tag tag = (Tag)tagIt.next();
                    if (log.isDebugEnabled())
                        log.debug("[" + tag.getTagType() + "] " + tag.getTagName() + " : " + tag.getDescription() + " (" + dir.getObject(tag.getTagType()).getClass().getName() + ") (" + dir.getObject(tag.getTagType()) + ")");
                    if (tag.getTagType() == ExifDirectory.TAG_ORIENTATION) {
                        exifOrientation = dir.getInt(tag.getTagType());
                    }
                    metadataInfo = dtconfig.getMetadataInfo(tag.getTagName());
                    if (metadataInfo != null) {
                        Object value = metadataInfo.getValueGetter().getValue(tag.getTagType(), dir);
                        document.setField(metadataInfo.field, value);
                    }
                }
            }

            if (exifOrientation != 1 && dtconfig.exifRotate) {
                BufferedImage rotatedImage = rotateImage(sourceImage, exifOrientation);
                byte[] imageData = writeJpeg(rotatedImage, dtconfig.jpegQuality);
                document.setPart(dtconfig.imagePartName, "image/jpeg", imageData);
                document.setPartFileName(dtconfig.imagePartName, part.getFileName());

                // let preview creation work from rotated image
                sourceImage = rotatedImage;
            }

            // Extract size info (note that this is done after the exif-based rotation)
            int width = sourceImage.getWidth();
            int height = sourceImage.getHeight();

            if (dtconfig.widthFieldName != null)
                document.setField(dtconfig.widthFieldName, new Long(width));
            if (dtconfig.heightFieldName != null)
                document.setField(dtconfig.heightFieldName, new Long(height));

            // Create preview and thumbnail
            if (dtconfig.previewPartName != null) {
                BufferedImage previewImage = resizeImage(sourceImage, dtconfig.previewMaxSize);
                sourceImage = previewImage; // start thumbnail from here, this is often significantly faster
                byte[] previewData = writeJpeg(previewImage, dtconfig.jpegQuality);
                document.setPart(dtconfig.previewPartName, "image/jpeg", previewData);
            }

            if (dtconfig.thumbnailPartName != null) {
                BufferedImage thumbnailImage = resizeImage(sourceImage, dtconfig.thumbnailMaxSize);
                byte[] thumbnailData = writeJpeg(thumbnailImage, dtconfig.jpegQuality);
                document.setPart(dtconfig.thumbnailPartName, "image/jpeg", thumbnailData);
            }
        } finally {
            imageProcessingSemaphore.release();
        }
    }

    private byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        JPEGImageWriteParam params = (JPEGImageWriteParam)writer.getDefaultWriteParam();
        params.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(quality);
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(data));
        writer.write(null, new IIOImage(image, null, null), params);
        return data.toByteArray();
    }

    private BufferedImage rotateImage(BufferedImage sourceImage, int exifOrientation) {
        // Possible Exif rotation flag values
        //
        // From http://jpegclub.org/exif_orientation.html
        //
        //  1        2       3      4         5            6           7          8
        //
        //888888  888888      88  88      8888888888  88                  88  8888888888
        //88          88      88  88      88  88      88  88          88  88      88  88
        //8888      8888    8888  8888    88          8888888888  8888888888          88
        //88          88      88  88
        //88          88  888888  888888

        if (exifOrientation < 1 || exifOrientation > 8)
            throw new IllegalArgumentException("Invalid exif rotation value: " + exifOrientation + " (should be from 1 to 8).");

        if (exifOrientation == 1) // normally this method is only called when it is not 1, but check anyway
            return sourceImage;

        int origWidth = sourceImage.getWidth();
        int origHeight = sourceImage.getHeight();

        // Create the buffered image.
        int newWidth;
        int newHeight;
        if (exifOrientation <= 4) {
            newWidth = origWidth;
            newHeight = origHeight;
        } else {
            // switch width and height
            newWidth = origHeight;
            newHeight = origWidth;
        }
        BufferedImage bufferedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        // Copy image to buffered image.
        Graphics2D g = bufferedImage.createGraphics();

        // Clear background: important when having transparent images which otherwise use a black background
        g.setColor(Color.white);
        g.fillRect(0, 0, newWidth, newHeight);

        AffineTransform transform;
        switch (exifOrientation) {
            case 2:
                transform = new AffineTransform(-1, 0, 0, 1, origWidth, 0);
                break;
            case 3:
                transform = new AffineTransform(-1, 0, 0, -1, origWidth, origHeight);
                break;
            case 4:
                transform = new AffineTransform(1, 0, 0, -1, 0, origHeight);
                break;
            case 5:
                transform = new AffineTransform(0, 1, 1, 0, 0, 0);
                break;
            case 6:
                transform = new AffineTransform(0, 1, -1, 0, origHeight, 0);
                break;
            case 7:
                transform = new AffineTransform(0, -1, -1, 0, origHeight, origWidth);
                break;
            case 8:
                transform = new AffineTransform(0, -1, 1, 0, 0, origWidth);
                break;
            default:
                throw new IllegalArgumentException("Invalid exif rotation value: " + exifOrientation + " (should be from 1 to 8).");
        }

        g.drawImage(sourceImage, transform, null);
        g.dispose();
        return bufferedImage;
    }

    private BufferedImage resizeImage(BufferedImage sourceImage, int maxSize) {
        Image resizedImage;
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();

        // if it's already small enough, do nothing
        if (Math.max(width, height) <= maxSize)
            return sourceImage;

        if (width > height)
            resizedImage = sourceImage.getScaledInstance(maxSize, (maxSize * height) / width, Image.SCALE_SMOOTH);
        else
            resizedImage = sourceImage.getScaledInstance((maxSize * width) / height, maxSize, Image.SCALE_SMOOTH);

        // Create the buffered image.
        BufferedImage bufferedImage = new BufferedImage(resizedImage.getWidth(null), resizedImage.getHeight(null),
                BufferedImage.TYPE_INT_RGB);

        // Copy image to buffered image.
        Graphics g = bufferedImage.createGraphics();

        // Clear background: important when having transparent images which otherwise use a black background
        g.setColor(Color.white);
        g.fillRect(0, 0, resizedImage.getWidth(null), resizedImage.getHeight(null));

        g.drawImage(resizedImage, 0, 0, null);
        g.dispose();
        return bufferedImage;
    }

    static class DoctypeConfig {
        public boolean enabled;
        public boolean exifRotate;
        public int maxImageSize;
        public String imagePartName;
        public String widthFieldName;
        public String heightFieldName;
        public String previewPartName;
        public int previewMaxSize;
        public String thumbnailPartName;
        public int thumbnailMaxSize;
        public float jpegQuality;
        private Map<String, MetadataInfo> metadatas;

        public boolean isSetMetadata() {
            return metadatas != null;
        }

        public MetadataInfo getMetadataInfo(String tagName) {
            if (metadatas != null)
                return metadatas.get(tagName);
            else
                return null;
        }

        public Iterator getMetadataInfoIterator() {
            if (metadatas != null)
                return metadatas.values().iterator();
            else
                return Collections.EMPTY_LIST.iterator();
        }

        public void addMetadata(String tagName, String field, String type) {
            if (metadatas == null)
                metadatas = new HashMap<String, MetadataInfo>();
            MetadataInfo metadataInfo = new MetadataInfo();
            metadataInfo.field = field;
            metadataInfo.type = type;
            metadatas.put(tagName, metadataInfo);
        }
    }

    static class MetadataInfo {
        public String field;
        public String type;

        public MetadataValueGetter getValueGetter() {
            return METADATA_VALUE_GETTERS.get(type);
        }
    }

    static interface MetadataValueGetter {
        Object getValue(int tagType, Directory directory) throws MetadataException;
    }

    static class StringValueGetter implements MetadataValueGetter {
        public Object getValue(int tagType, Directory directory) {
            return directory.getString(tagType);
        }
    }

    static class DateValueGetter implements MetadataValueGetter {
        public Object getValue(int tagType, Directory directory) throws MetadataException {
            return directory.getDate(tagType);
        }
    }

    static class LongValueGetter implements MetadataValueGetter {
        public Object getValue(int tagType, Directory directory) throws MetadataException {
            return new Long(directory.getLong(tagType));
        }
    }

    static class DoubleValueGetter implements MetadataValueGetter {
        public Object getValue(int tagType, Directory directory) throws MetadataException {
            return new Double(directory.getDouble(tagType));
        }
    }

    static class DescriptionValueGetter implements MetadataValueGetter {
        public Object getValue(int tagType, Directory directory) throws MetadataException {
            return directory.getDescription(tagType);
        }
    }

    private static Map<String, MetadataValueGetter> METADATA_VALUE_GETTERS;
    static {
        METADATA_VALUE_GETTERS = new HashMap<String, MetadataValueGetter>();
        METADATA_VALUE_GETTERS.put("description", new DescriptionValueGetter());
        METADATA_VALUE_GETTERS.put("string", new StringValueGetter());
        METADATA_VALUE_GETTERS.put("datetime", new DateValueGetter());
        METADATA_VALUE_GETTERS.put("long", new LongValueGetter());
        METADATA_VALUE_GETTERS.put("double", new DoubleValueGetter());
    }
}
