/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.jbpm.process.svg;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.jbpm.process.svg.model.Transformation;
import org.jbpm.process.svg.processor.SVGProcessor;
import org.jbpm.process.svg.processor.SVGProcessorFactory;
import org.jbpm.process.svg.processor.StunnerSVGProcessor;
import org.w3c.dom.Document;

import static org.jbpm.process.svg.processor.SVGProcessor.ACTIVE_BORDER_COLOR;
import static org.jbpm.process.svg.processor.SVGProcessor.COMPLETED_BORDER_COLOR;
import static org.jbpm.process.svg.processor.SVGProcessor.COMPLETED_COLOR;

public class SVGImageProcessor {

    private SVGProcessor svgProcessor;

    public SVGImageProcessor(InputStream svg) {
        this(svg, true);
    }

    public SVGImageProcessor(InputStream svg, Map<String, String> subProcessLinks, Map<String, Long> badges) {
        try {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
            factory.setValidating(false);
            Document svgDocument = factory.createDocument("http://jbpm.org", svg);

            svgProcessor = new SVGProcessorFactory().create(svgDocument, true);
            if (svgProcessor instanceof StunnerSVGProcessor) {
                ((StunnerSVGProcessor) svgProcessor).setSubProcessLinks(subProcessLinks);
                ((StunnerSVGProcessor) svgProcessor).setNodeBadges(badges);
            }
            svgProcessor.processNodes(svgDocument.getChildNodes());

        } catch (IOException e) {
            throw new RuntimeException("Could not parse svg", e);
        }
    }

    public SVGImageProcessor(InputStream svg, boolean mapById) {

        try {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
            factory.setValidating(false);
            Document svgDocument = factory.createDocument("http://jbpm.org", svg);

            svgProcessor = new SVGProcessorFactory().create(svgDocument, mapById);
            svgProcessor.processNodes(svgDocument.getChildNodes());

        } catch (IOException e) {
            throw new RuntimeException("Could not parse svg", e);
        }
    }

    public SVGProcessor getProcessor() {
        return svgProcessor;
    }

    //Static methods to keep backward compatibility

    public static String transform(InputStream svg, List<String> completed, List<String> active) {
        return transform(svg, completed, active, null, COMPLETED_COLOR, COMPLETED_BORDER_COLOR, ACTIVE_BORDER_COLOR, null);
    }

    public static String transform(InputStream svg, List<String> completed, List<String> active, Map<String, String> subProcessLinks) {
        return transform(svg, completed, active, subProcessLinks, COMPLETED_COLOR, COMPLETED_BORDER_COLOR, ACTIVE_BORDER_COLOR, null);
    }

    public static String transform(InputStream svg, List<String> completed, List<String> active,
                                   Map<String, String> subProcessLinks, String completedNodeColor,
                                   String completedNodeBorderColor, String activeNodeBorderColor, Map<String, Long> badges) {
        SVGProcessor processor = new SVGImageProcessor(svg, subProcessLinks, badges).getProcessor();

        for (String nodeId : completed) {
            if (!active.contains(nodeId)) {
                processor.defaultCompletedTransformation(nodeId, completedNodeColor, completedNodeBorderColor);
            }
        }
        for (String nodeId : active) {
            processor.defaultActiveTransformation(nodeId, activeNodeBorderColor);
        }

        if (subProcessLinks != null) {

            for (Map.Entry<String, String> subProcessLink : subProcessLinks.entrySet()) {
                processor.defaultSubProcessLinkTransformation(subProcessLink.getKey(), subProcessLink.getValue());
            }
        }

        return processor.getSVG();
    }

    public static String transformByName(InputStream svg, List<String> completed, List<String> active) {
        SVGProcessor processor = new SVGImageProcessor(svg, false).getProcessor();
        for (String nodeId : completed) {
            if (!active.contains(nodeId)) {
                processor.defaultCompletedTransformation(nodeId);
            }
        }
        for (String nodeId : active) {
            processor.defaultActiveTransformation(nodeId);

        }
        return processor.getSVG();
    }

    //Delegate just to keep backward compatibility

    public void transform(Transformation t) {
        getProcessor().transform(t);
    }

    public void defaultCompletedTransformation(String nodeId) {
        getProcessor().defaultCompletedTransformation(nodeId);
    }

    public void defaultActiveTransformation(String nodeId) {
        getProcessor().defaultActiveTransformation(nodeId);
    }

    public void defaultSubProcessLinkTransformation(String nodeId, String link) {
        getProcessor().defaultSubProcessLinkTransformation(nodeId, link);
    }

    public String getSVG() {
        return getProcessor().getSVG();
    }
}