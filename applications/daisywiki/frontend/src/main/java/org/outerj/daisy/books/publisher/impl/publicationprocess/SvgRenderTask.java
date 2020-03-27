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
package org.outerj.daisy.books.publisher.impl.publicationprocess;

import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerj.daisy.xmlutil.ForwardingContentHandler;
import org.outerj.daisy.books.publisher.impl.util.PublicationLog;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.*;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.cocoon.xml.AttributesImpl;
import org.outerx.daisy.x10Bookstoremeta.ResourcePropertiesDocument;
import org.w3c.dom.Document;
import org.w3c.dom.svg.SVGDocument;

import javax.xml.parsers.SAXParser;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.awt.*;
import java.awt.geom.Dimension2D;

/**
 * A book publication process task to render SVG images.
 * Reacts on a renderSVG tag, renders the referenced image, and replaces this tag with
 * a normal img tag.
 *
 * <p>In contrast to using a Cocoon pipeline with the SVG serializer
 * (e.g. using the {@link CallPipelineTask}), this task allows to specify
 * a dpi parameter to generate high-resolution images.
 */
public class SvgRenderTask implements PublicationProcessTask {
    private final String input;
    private final String output;
    private final String outputPrefix;
    private final String format;
    private final float dpi;
    private final float defaultDpi = 96;
    private final float quality;
    private final Color backgroundColor;
    private final boolean enableScripts;
    private final float maxPrintWidth;
    private final float maxPrintHeight;
    private static final String NS = "http://outerx.org/daisy/1.0#bookSvgRenderTask";

    public SvgRenderTask(Map attributes) throws Exception {
        this.input = PublicationProcessTaskUtil.getRequiredAttribute(attributes, "input", "renderSVG");
        this.output = PublicationProcessTaskUtil.getRequiredAttribute(attributes, "output", "renderSVG");

        if (attributes.containsKey("outputPrefix")) {
            outputPrefix = (String)attributes.get("outputPrefix");
        } else {
            outputPrefix = "from-svg/";
        }

        if (attributes.containsKey("format")) {
            format = (String)attributes.get("format");
        } else {
            format = "jpg";
        }

        if (attributes.containsKey("dpi")) {
            dpi = parseFloat((String)attributes.get("dpi"), "dpi");
        } else {
            dpi = defaultDpi;
        }

        if (attributes.containsKey("quality")) {
            quality = parseFloat((String)attributes.get("quality"), "quality");
        } else {
            quality = 1f;
        }

        if (attributes.containsKey("backgroundColor")) {
            backgroundColor = parseColor((String)attributes.get("backgroundColor"));
        } else {
            backgroundColor = null;
        }

        if (attributes.containsKey("enableScripts")) {
            enableScripts = "true".equalsIgnoreCase((String)attributes.get("enableScripts"));
        } else {
            enableScripts = false;
        }

        if (attributes.containsKey("maxPrintWidth")) {
            maxPrintWidth = parseFloat((String)attributes.get("maxPrintWidth"), "maxPrintWidth");
        } else {
            maxPrintWidth = 6.45f;
        }

        if (attributes.containsKey("maxPrintHeight")) {
            maxPrintHeight = parseFloat((String)attributes.get("maxPrintHeight"), "maxPrintHeight");
        } else {
            maxPrintHeight = 8.6f;
        }
    }

    private static float parseFloat(String value, String name) throws Exception {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new Exception("Invalid float value specified for " + name + ": " + value);
        }
    }

    private static Color parseColor(String value) throws Exception {
        if (!value.startsWith("#") && value.length() != 7) {
            throw new Exception("Color specification should be in the form #FFFFFF, not: " + value);
        }

        int r, g, b;
        try {
            r = Integer.parseInt(value.substring(1, 3), 16);
            g = Integer.parseInt(value.substring(3, 5), 16);
            b = Integer.parseInt(value.substring(5, 7), 16);
        } catch (NumberFormatException e) {
            throw new Exception("Invalid color specification: " + value);
        }

        return new Color(r, g, b);
    }

    public void run(PublicationContext context) throws Exception {
        context.getPublicationLog().info("Running SVG render task.");
        BookInstance bookInstance = context.getBookInstance();
        String publicationOutputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());
        String startXmlLocation = publicationOutputPath + input;
        InputStream is = null;
        OutputStream os = null;
        try {
            is = bookInstance.getResource(startXmlLocation);
            os = bookInstance.getResourceOutputStream(publicationOutputPath + output);
            SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
            XmlSerializer serializer = new XmlSerializer(os);
            SvgRenderHandler svgRenderHandler = new SvgRenderHandler(serializer, context);
            parser.getXMLReader().setContentHandler(svgRenderHandler);
            parser.getXMLReader().parse(new InputSource(is));
        } finally {
            if (is != null)
                try { is.close(); } catch (Exception e) { /* ignore */ }
            if (os != null)
                try { os.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    class SvgRenderHandler extends ForwardingContentHandler {
        private PublicationContext context;

        public SvgRenderHandler(ContentHandler consumer, PublicationContext context) {
            super(consumer);
            this.context = context;
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (namespaceURI.equals(NS) && localName.equals("renderSVG")) {
                InputStream svgInputStream = null;
                OutputStream svgOutputStream = null;
                try {
                    String svgPath = atts.getValue("bookStorePath");
                    svgInputStream = context.getBookInstance().getResource(svgPath);

                    SAXSVGDocumentFactory svgDocumentFactory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
                    SVGDocument svgDocument = (SVGDocument)svgDocumentFactory.createDocument(context.getBookInstance().getResourceURI(svgPath).toString(), svgInputStream);

                    Dimension2D svgDimension = getSvgDimension(svgDocument);

                    float svgWidth = (float)svgDimension.getWidth();
                    float svgHeight = (float)svgDimension.getHeight();
                    float width = (float)Math.floor(svgWidth * (dpi / defaultDpi));
                    float height = (float)Math.floor(svgHeight * (dpi / defaultDpi));

                    Transcoder transcoder;
                    if (format.equals("png"))
                        transcoder = new JPEGTranscoder();
                    else if (format.equals("jpg"))
                        transcoder = new PNGTranscoder();
                    else
                        throw new Exception("svgRender task: unrecognized format: " + format);

                    transcoder.setErrorHandler(new MyErrorHandler(context.getPublicationLog()));
                    transcoder.addTranscodingHint(JPEGTranscoder.KEY_HEIGHT, new Float(height));
                    if (backgroundColor != null)
                        transcoder.addTranscodingHint(JPEGTranscoder.KEY_BACKGROUND_COLOR, backgroundColor);
                    transcoder.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(quality));
                    if (enableScripts) {
                        transcoder.addTranscodingHint(JPEGTranscoder.KEY_EXECUTE_ONLOAD, Boolean.TRUE);
                        transcoder.addTranscodingHint(JPEGTranscoder.KEY_CONSTRAIN_SCRIPT_ORIGIN, Boolean.FALSE);
                    }

                    TranscoderInput transcoderInput = new TranscoderInput(svgDocument);

                    String publicationOutputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());
                    String svgOutputPath = publicationOutputPath + outputPrefix + PublicationProcessTaskUtil.getFileName(svgPath);
                    svgOutputStream = context.getBookInstance().getResourceOutputStream(svgOutputPath);
                    TranscoderOutput transcoderOutput = new TranscoderOutput(svgOutputStream);

                    // the actual SVG rendering
                    transcoder.transcode(transcoderInput, transcoderOutput);

                    // write a meta file
                    ResourcePropertiesDocument propertiesDocument = ResourcePropertiesDocument.Factory.newInstance();
                    propertiesDocument.addNewResourceProperties().setMimeType("image/" + format);
                    context.getBookInstance().storeResourceProperties(svgOutputPath, propertiesDocument);

                    float printWidth = width / dpi;
                    float printHeight = height / dpi;
                    if (printWidth > maxPrintWidth) {
                        printHeight = printHeight * (maxPrintWidth / printWidth);
                        printWidth = maxPrintWidth;
                    }
                    if (printHeight > maxPrintHeight) {
                        printWidth = printWidth * (maxPrintHeight / printHeight);
                        printHeight = maxPrintHeight;
                    }

                    AttributesImpl attrs = new AttributesImpl();
                    attrs.addCDATAAttribute("src", "bookinstance:" + svgOutputPath);
                    attrs.addCDATAAttribute("width", String.valueOf((int)width));
                    attrs.addCDATAAttribute("height", String.valueOf((int)height));
                    attrs.addCDATAAttribute("print-width", String.valueOf(printWidth) + "in");
                    attrs.addCDATAAttribute("print-height", String.valueOf(printHeight) + "in");
                    startElement("", "img", "img", attrs);
                    endElement("", "img", "img");
                } catch (Exception e) {
                    throw new SAXException("Error rendering SVG.", e);
                } finally {
                    if (svgInputStream != null)
                        try { svgInputStream.close(); } catch (Exception e) { /* ignore */ }
                    if (svgOutputStream != null)
                        try { svgOutputStream.close(); } catch (Exception e) { /* ignore */ }
                }
            } else {
                super.startElement(namespaceURI, localName, qName, atts);
            }
        }

        private Dimension2D getSvgDimension(Document document) {
            UserAgentAdapter userAgent = new UserAgentAdapter();
            BridgeContext ctx = new BridgeContext(userAgent);
            GVTBuilder builder = new GVTBuilder();
            ctx.setDynamicState(enableScripts ? BridgeContext.DYNAMIC : BridgeContext.STATIC);
            builder.build(ctx, document);
            return ctx.getDocumentSize();
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (namespaceURI.equals(NS) && localName.equals("renderSVG")) {
                // don't propagatge
            } else {
                super.endElement(namespaceURI, localName, qName);
            }
        }
    }

    static class MyErrorHandler implements ErrorHandler {
        private PublicationLog log;

        public MyErrorHandler(PublicationLog log) {
            this.log = log;
        }

        public void error(TranscoderException transcoderException) throws TranscoderException {
            log.error("Error reported by Batik.", transcoderException);
        }

        public void fatalError(TranscoderException transcoderException) throws TranscoderException {
            log.error("Fatal error reported by Batik.", transcoderException);
        }

        public void warning(TranscoderException transcoderException) throws TranscoderException {
            log.error("Warning reported by Batik.", transcoderException);
        }
    }

}
