/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.jbpm.process.svg.processor.SVGProcessor;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.junit.Assert.*;
import static org.jbpm.process.svg.processor.SVGProcessor.ACTIVE_BORDER_COLOR;
import static org.jbpm.process.svg.processor.SVGProcessor.COMPLETED_BORDER_COLOR;
import static org.jbpm.process.svg.processor.SVGProcessor.COMPLETED_COLOR;

public class TestEvalutionSVG {

    private XPath xpath = XPathFactory.newInstance().newXPath();

    @Test
    public void test() throws Exception {
        List<String> completed = new ArrayList<String>();
        completed.add("_343B16DA-961A-49BF-8697-9A86DEAFBAF4");
        List<String> active = new ArrayList<String>();
        active.add("_6063D302-9D81-4C86-920B-E808A45377C2");
        String svg = SVGImageProcessor.transform(TestEvalutionSVG.class.getResourceAsStream("/evaluation-svg.svg"), completed, active);

        // verify transformation
        Document svgDocument = readSVG(svg);
        validateNodesMarkedAsActive(svgDocument, active, SVGProcessor.ACTIVE_BORDER_COLOR);
        validateNodesMarkedAsCompleted(svgDocument, completed, SVGProcessor.COMPLETED_COLOR);
    }

    @Test
    public void testByName() throws Exception {
        List<String> completedID = new ArrayList<String>();
        completedID.add("_6063D302-9D81-4C86-920B-E808A45377C2");
        List<String> activeID = new ArrayList<String>();
        activeID.add("_AE5BF0DC-B720-4FDE-9499-5ED89D41FB1A");

        List<String> completed = new ArrayList<String>();
        completed.add("Self Evaluation");
        List<String> active = new ArrayList<String>();
        active.add("PM Evaluation");
        String svg = SVGImageProcessor.transformByName(TestEvalutionSVG.class.getResourceAsStream("/evaluation-svg.svg"), completed, active);

        // verify transformation
        Document svgDocument = readSVG(svg);
        validateNodesMarkedAsActive(svgDocument, activeID, SVGProcessor.ACTIVE_BORDER_COLOR);
        validateNodesMarkedAsCompleted(svgDocument, completedID, SVGProcessor.COMPLETED_COLOR);
    }

    @Test
    public void testCompletedAndActive() throws Exception {
        List<String> completed = new ArrayList<String>();
        completed.add("_343B16DA-961A-49BF-8697-9A86DEAFBAF4");
        completed.add("_6063D302-9D81-4C86-920B-E808A45377C2");
        List<String> active = new ArrayList<String>();
        active.add("_6063D302-9D81-4C86-920B-E808A45377C2");
        String svg = SVGImageProcessor.transform(TestEvalutionSVG.class.getResourceAsStream("/evaluation-svg.svg"), completed, active);

        // verify transformation
        Document svgDocument = readSVG(svg);
        validateNodesMarkedAsActive(svgDocument, active, SVGProcessor.ACTIVE_BORDER_COLOR);
        // remove it as it should be not considered completed and was already asserted as active
        completed.remove("_6063D302-9D81-4C86-920B-E808A45377C2");
        validateNodesMarkedAsCompleted(svgDocument, completed, SVGProcessor.COMPLETED_COLOR);
    }

    @Test
    public void testSubProcessLink() throws Exception {
        List<String> completed = new ArrayList<String>();
        List<String> active = new ArrayList<String>();
        active.add("_35262208-8B3E-457E-8D8A-798E70CC280D");

        Map<String, String> links = new HashMap<>();
        links.put("_35262208-8B3E-457E-8D8A-798E70CC280D", "http://localhost/processes/1");
        String svg = SVGImageProcessor.transform(TestEvalutionSVG.class.getResourceAsStream("/call-activity-svg.svg"), completed, active, links);

        // verify transformation
        Document svgDocument = readSVG(svg);
        validateNodesMarkedAsActive(svgDocument, active, SVGProcessor.ACTIVE_BORDER_COLOR);
        validateCallActivityLinked(svgDocument, active, links);
    }

    @Test
    public void testStunnerSubProcessLink() throws Exception {
        String elementId = "_D254259B-8C23-4498-9BE1-995E2DC66726";
        List<String> completed = new ArrayList<String>();
        List<String> active = new ArrayList<String>();
        active.add(elementId);

        Map<String, String> links = new HashMap<>();
        links.put(elementId, "http://localhost/processes/1");
        String svg = SVGImageProcessor.transform(TestEvalutionSVG.class.getResourceAsStream("/stunner.parentP-svg.svg"), completed, active, links);

        // verify transformation
        Document svgDocument = readSVG(svg);

        Element subprocessPlusIcon = svgDocument.getElementById(elementId+"_subProcessReusableNormalReusableIcon");
        String onclick = subprocessPlusIcon.getAttribute("onclick");
        assertNotNull(onclick);
        assertEquals("window.open('http://localhost/processes/1')", onclick);
        String style = subprocessPlusIcon.getAttribute("style");
        assertNotNull(style);
        assertEquals("cursor: pointer;", style);

    }

    @Test
    public void testCustomColor() throws Exception {
        String completedNodeColor = "#888888";
        String completedNodeBorderColor = "#888887";
        String activeNodeBorderColor = "#888886";
        List<String> completed = new ArrayList<String>();
        completed.add("_343B16DA-961A-49BF-8697-9A86DEAFBAF4");
        List<String> active = new ArrayList<String>();
        active.add("_6063D302-9D81-4C86-920B-E808A45377C2");
        String svg = SVGImageProcessor.transform(TestEvalutionSVG.class.getResourceAsStream("/evaluation-svg.svg"),
                                                 completed, active, null, completedNodeColor,
                                                 completedNodeBorderColor, activeNodeBorderColor, null);

        // verify transformation
        Document svgDocument = readSVG(svg);
        validateNodesMarkedAsActive(svgDocument, active, activeNodeBorderColor);
        validateNodesMarkedAsCompleted(svgDocument, completed, completedNodeColor);
    }

    @Test
    public void testViewBoxAttributeAddition() throws Exception {
        List<String> completed = new ArrayList<String>();
        completed.add("_343B16DA-961A-49BF-8697-9A86DEAFBAF4");
        List<String> active = new ArrayList<String>();
        active.add("_6063D302-9D81-4C86-920B-E808A45377C2");
        String svg = SVGImageProcessor.transform(TestEvalutionSVG.class.getResourceAsStream("/evaluation-svg.svg"),
                                                 completed, active, null, "#888888",
                                                 "#888887", "#888886", null);

        Document svgDocument = readSVG(svg);
        assertEquals("", ((Element) svgDocument.getFirstChild()).getAttribute("width"));
        assertEquals("", ((Element) svgDocument.getFirstChild()).getAttribute("height"));
        assertEquals("0 0 3000 2000", svgDocument.getFirstChild().getAttributes().getNamedItem("viewBox").getNodeValue());
    }

    // helper methods for verifying svg transformation

    private void validateNodesMarkedAsActive(Document svgDocument, List<String> activeNodes, String activeNodeBorderColor) throws XPathExpressionException {
        for (String activeNode : activeNodes) {

            XPathExpression expr = xpath.compile("//*[@bpmn2nodeid='" + activeNode + "']");
            Element element = (Element) expr.evaluate(svgDocument, XPathConstants.NODE);

            if (element == null) {
                fail("Active element " + activeNode + " not found in the document");
            }
            String svgId = element.getAttribute("id");

            Element border = svgDocument.getElementById(svgId + "bg_frame");

            String marker = border.getAttribute("stroke");
            assertNotNull(marker);
            assertEquals(activeNodeBorderColor, marker);
            String markerWidth = border.getAttribute("stroke-width");
            assertNotNull(markerWidth);
            assertEquals("2", markerWidth);
        }
    }

    private void validateNodesMarkedAsCompleted(Document svgDocument, List<String> completedNodes, String completedNodeColor) throws XPathExpressionException {

        for (String completedNode : completedNodes) {
            XPathExpression expr = xpath.compile("//*[@bpmn2nodeid='" + completedNode + "']");
            Element element = (Element) expr.evaluate(svgDocument, XPathConstants.NODE);

            if (element == null) {
                fail("Completed element " + completedNode + " not found in the document");
            }
            String svgId = element.getAttribute("id");
            Element background = svgDocument.getElementById(svgId + "fill_el");

            String marker = background.getAttribute("stop-color");
            assertNotNull(marker);
            assertEquals(completedNodeColor, marker);
        }
    }

    private void validateCallActivityLinked(Document svgDocument, List<String> activeNodes, Map<String, String> links) throws XPathExpressionException {
        for (String activeNode : activeNodes) {

            XPathExpression expr = xpath.compile("//*[@bpmn2nodeid='" + activeNode + "']");
            Element element = (Element) expr.evaluate(svgDocument, XPathConstants.NODE);

            if (element == null) {
                fail("Active element " + activeNode + " not found in the document");
            }
            String svgId = element.getAttribute("id");

            Element border = svgDocument.getElementById(svgId + "pimg");

            String onclick = border.getAttribute("onclick");
            assertNotNull(onclick);
            assertEquals("", onclick);
            String link = border.getAttributeNS("http://www.w3.org/1999/xlink", "href");
            assertNotNull(link);
            assertEquals(links.get(activeNode), link);
            String target = border.getAttribute("target");
            assertNotNull(target);
            assertEquals("_blank", target);
        }
    }

    private Document readSVG(String svgContent) throws IOException {
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        factory.setValidating(false);
        Document svgDocument = factory.createDocument("http://jbpm.org", new StringReader(svgContent));

        return svgDocument;
    }

    @Test
    public void testRenderBadges() throws Exception {
        List<String> completed = new ArrayList<String>();
        String completedNodeId = "_FCDCAA8D-378C-4ADF-9A8C-053FF7363D67";
        completed.add(completedNodeId);
        List<String> active = new ArrayList<String>();
        String activeNodeId = "_D254259B-8C23-4498-9BE1-995E2DC66726";
        active.add(activeNodeId);

        Map<String, Long> nodeBadges = new HashMap<>();
        nodeBadges.put(completedNodeId, 1L);
        nodeBadges.put(activeNodeId, 1L);
        String svg = SVGImageProcessor.transform(TestEvalutionSVG.class.getResourceAsStream("/stunner.parentP-svg.svg"), completed,
                                                 active, null, COMPLETED_BORDER_COLOR, COMPLETED_COLOR, ACTIVE_BORDER_COLOR, nodeBadges);

        // verify transformation
        Document svgDocument = readSVG(svg);
        Element completedNode = svgDocument.getElementById(completedNodeId);
        assertNodeBadgesofNode(completedNode, completedNodeId);

        Element activeNode = svgDocument.getElementById(activeNodeId);
        assertNodeBadgesofNode(activeNode, activeNodeId);
    }


    private void assertNodeBadgesofNode(Element completedNode, String targetNode) {
        Node nodeIdNode = completedNode.getAttributes().getNamedItem("bpmn2nodeid");
        assertNotNull(nodeIdNode);
        NodeList nodeList = completedNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node nodeBadgeNode = nodeList.item(i).getAttributes().getNamedItem("jbpm-node-badge");
            if (nodeBadgeNode != null) {
                assertEquals(targetNode, nodeBadgeNode.getNodeValue());
                Node rectNode = nodeList.item(i).getChildNodes().item(0);
                assertNotNull(rectNode);
                Node textNode = nodeList.item(i).getChildNodes().item(1);
                assertNotNull(textNode);
                assertEquals("1", textNode.getFirstChild().getNodeValue());
            } else {
                Node idNode = nodeList.item(i).getAttributes().getNamedItem("id");
                if (idNode != null && targetNode.equals(idNode.getNodeValue())) {
                    assertTrue(false);
                }
            }
        }
    }
}
