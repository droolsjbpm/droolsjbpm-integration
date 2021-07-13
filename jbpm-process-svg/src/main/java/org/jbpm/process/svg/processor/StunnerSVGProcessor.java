package org.jbpm.process.svg.processor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jbpm.process.svg.model.NodeSummary;
import org.jbpm.process.svg.model.RenderType;
import org.jbpm.process.svg.model.SetSubProcessLinkTransformation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGElement;

public class StunnerSVGProcessor extends AbstractSVGProcessor {

    private static String SPECIAL_SUB_PROCESS_INSTANCE_ID = "_subProcessReusableNormalReusableIcon";
    private static String OLD_SPECIAL_SUB_PROCESS_INSTANCE_ID = "undefined";

    public StunnerSVGProcessor(Document svgDocument) {
        super(svgDocument, true);
    }

    private Map<String, String> subProcessLinks;

    private Map<String, Long> nodeBadges = new HashMap<>();

    public void setSubProcessLinks(final Map<String, String> subProcessLinks) {
        this.subProcessLinks = subProcessLinks;
    }

    public void setNodeBadges(Map<String, Long> nodeBadges) {
        this.nodeBadges = nodeBadges;
    }

    @Override
    public void defaultCompletedTransformation(String nodeId ,String completedNodeColor, String completeBorderColor) {
        transform((summary) -> {
            Optional.ofNullable(summary.getNode(nodeId)).ifPresent(node -> {
                Optional.ofNullable(node.getBackground()).ifPresent(background -> {
                    background.setAttribute("fill", completedNodeColor);
                    setNodeBorderColor(node.getRenderType(), node.getBorder(), completeBorderColor);
                });
            });
        });
    }

    @Override
    public void defaultActiveTransformation(String nodeId, String activeNodeBorderColor) {
        transform((summary) -> {
            Optional.ofNullable(summary.getNode(nodeId)).ifPresent(node -> {
                Optional.ofNullable(node.getBorder()).ifPresent(border -> {
                    setNodeBorderColor(node.getRenderType(), border, activeNodeBorderColor);
                });
            });
        });
    }

    private void setNodeBorderColor(Optional<RenderType> renderType, Element border, String color) {
        final RenderType render = renderType.orElse(RenderType.STROKE);
        switch (render) {
            case STROKE:
                border.setAttribute("stroke-width", "2");
                border.setAttribute("stroke", color);
                break;
            case FILL:
                border.setAttribute("fill", color);
                break;
        }
    }

    @Override
    public void defaultSubProcessLinkTransformation(String nodeId, String link) {
        transform(new SetSubProcessLinkTransformation(nodeId, link));
    }

    private void processNode(final Node parent, final String nodeId) {
        final NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node node = nodes.item(i);
            final NamedNodeMap attributes = node.getAttributes();
            if (attributes != null) {
                final Node svgIdNode = attributes.getNamedItem("id");
                if (svgIdNode != null) {
                    final String value = svgIdNode.getNodeValue();
                    if (Objects.nonNull(value)) {
                        Map<String, String> parameters =
                                Stream.of(value.substring(value.indexOf("?") + 1))
                                        .filter(Objects::nonNull)
                                        .map(str -> Collections.list(new StringTokenizer(str, "&")))
                                        .flatMap(list -> list.stream())
                                        .map(String::valueOf)
                                        .filter(str -> str.split("=").length == 2)
                                        .collect(Collectors.toMap(v -> v.split("=")[0], v -> v.split("=")[1]));

                        NodeSummary nodeSummary = summary.getNodesMap().getOrDefault(nodeId, new NodeSummary(nodeId, null, null, null, null, null));
                        Element border = Objects.equals(parameters.get("shapeType"), "BORDER") ? (Element) node : nodeSummary.getBorder();
                        Element background = Objects.equals(parameters.get("shapeType"), "BACKGROUND") ? (Element) node : nodeSummary.getBackground();
                        RenderType renderType = RenderType.valueOf(Optional.ofNullable(parameters.get("renderType")).orElse(nodeSummary.getRenderType().orElse(RenderType.STROKE).name()));
                        Element plusButton = null;
                        if (subProcessLinks != null && subProcessLinks.containsKey(nodeId) && (value.equals(nodeId + SPECIAL_SUB_PROCESS_INSTANCE_ID) || value.equals(nodeId + OLD_SPECIAL_SUB_PROCESS_INSTANCE_ID))) {
                            plusButton = (Element) node;
                        }
                        summary.addNode(new NodeSummary(nodeId, border, background, null, null, renderType, plusButton));
                    }
                    break;
                }
            }
            processNode(node, nodeId);
        }
    }

    private void renderBadges(Node node, Long badgesCount) {
        Element svgElement = (Element) node;
        String nodeId = ((SVGElement) svgElement).getId();
        Element jbpmNodeBadge = svgDocument.createElement("g");
        jbpmNodeBadge.setAttribute("jbpm-node-badge", nodeId);

        Element jbpmNodeBadgeRect = svgDocument.createElement("rect");
        Element jbpmNodeBadgeText = svgDocument.createElement("text");

        jbpmNodeBadgeRect.setAttribute("x", "0");
        jbpmNodeBadgeRect.setAttribute("y", "0");
        jbpmNodeBadgeRect.setAttribute("width", "25");
        jbpmNodeBadgeRect.setAttribute("height", "20");
        jbpmNodeBadgeRect.setAttribute("rx", "5");
        jbpmNodeBadgeRect.setAttribute("ry", "5");
        jbpmNodeBadgeRect.setAttribute("fill", "grey");
        jbpmNodeBadgeRect.setAttribute("opacity", "0.5");

        jbpmNodeBadgeText.setAttribute("font-size", "10pt");
        jbpmNodeBadgeText.setAttribute("font-weight", "normal");
        jbpmNodeBadgeText.setAttribute("font-family", "Open Sans");
        jbpmNodeBadgeText.setAttribute("font-style", "normal");
        jbpmNodeBadgeText.setAttribute("text-anchor", "middle");
        jbpmNodeBadgeText.setAttribute("fill", "white");
        jbpmNodeBadgeText.setAttribute("x", "12");
        jbpmNodeBadgeText.setAttribute("y", "15");
        jbpmNodeBadgeText.setTextContent(String.valueOf(badgesCount));

        jbpmNodeBadge.appendChild(jbpmNodeBadgeRect);
        jbpmNodeBadge.appendChild(jbpmNodeBadgeText);
        svgElement.appendChild(jbpmNodeBadge);
    }

    @Override
    public void processNodes(NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            NamedNodeMap attributes = node.getAttributes();
            if (attributes != null) {
                Node svgIdNode = attributes.getNamedItem("id");
                if (svgIdNode != null) {
                    Node nodeIdNode = attributes.getNamedItem("bpmn2nodeid");
                    if (nodeIdNode != null) {
                        String nodeId = nodeIdNode.getNodeValue();
                        if (nodeBadges != null) {
                            nodeBadges.computeIfPresent(nodeId, (key, value) -> {
                                renderBadges(node, value);
                                return null;
                            });
                        }
                        if (nodeId != null) {
                            //process bpmn2 node to parse the attributes
                            processNode(node, nodeId);
                        }
                    }
                }
            }
            processNodes(node.getChildNodes());
        }
    }
}
