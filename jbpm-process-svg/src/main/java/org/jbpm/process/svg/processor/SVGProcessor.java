package org.jbpm.process.svg.processor;

import org.jbpm.process.svg.model.Transformation;
import org.w3c.dom.NodeList;

public interface SVGProcessor {

    String COMPLETED_COLOR = "#C0C0C0";
    String COMPLETED_BORDER_COLOR = "#030303";
    String ACTIVE_BORDER_COLOR = "#FF0000";
    String SHOW_INSTANCE_BADGES_DEFAULT = "false";

    void transform(Transformation t);

    void defaultCompletedTransformation(String nodeId, String completedNodeColor, String completedNodeBorderColor);

    void defaultActiveTransformation(String nodeId, String activeNodeBorderColor);

    void defaultCompletedTransformation(String nodeId);

    void defaultActiveTransformation(String nodeId);

    void defaultSubProcessLinkTransformation(String nodeId, String link);

    String getSVG();

    void processNodes(NodeList nodes);
}
