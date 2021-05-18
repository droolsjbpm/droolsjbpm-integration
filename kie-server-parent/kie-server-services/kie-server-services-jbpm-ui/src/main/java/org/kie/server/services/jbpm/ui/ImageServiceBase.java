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

package org.kie.server.services.jbpm.ui;

import java.io.ByteArrayInputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jbpm.process.svg.SVGImageProcessor;
import org.jbpm.process.svg.processor.SVGProcessor;
import org.jbpm.services.api.ProcessInstanceNotFoundException;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.model.NodeInstanceDesc;
import org.jbpm.services.api.model.ProcessDefinition;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.kie.api.runtime.query.QueryContext;
import org.kie.server.api.KieServerConstants;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.impl.locator.ContainerLocatorProvider;
import org.kie.server.services.jbpm.ui.img.ImageReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jbpm.process.svg.processor.SVGProcessor.ACTIVE_BORDER_COLOR;
import static org.jbpm.process.svg.processor.SVGProcessor.COMPLETED_BORDER_COLOR;
import static org.jbpm.process.svg.processor.SVGProcessor.COMPLETED_COLOR;
import static org.kie.server.api.KieServerConstants.KIE_SERVER_IMAGESERVICE_MAX_NODES;

public class ImageServiceBase {

    private static final Logger logger = LoggerFactory.getLogger(ImageServiceBase.class);

    /**
     * This causes the image service to limit the number of nodes (performance). 
     * Due to this limitation it could cause a known issue not blurring all the nodes active or completed depending on
     * the process size.
     */
    private static final int MAX_NODES = Integer.parseInt(System.getProperty(KIE_SERVER_IMAGESERVICE_MAX_NODES, "1000"));

    private RuntimeDataService dataService;
    private Map<String, ImageReference> imageReferenceMap;

    private String kieServerLocation;
    private String processInstanceImageLink = "containers/{0}/images/processes/instances/{1}";

    private KieServerRegistry registry;

    public ImageServiceBase() {
        // for tests only
        this.kieServerLocation = "";
    }

    public ImageServiceBase(RuntimeDataService dataService, Map<String, ImageReference> imageReferenceMap, KieServerRegistry registry) {
        this.dataService = dataService;
        this.imageReferenceMap = imageReferenceMap;
        this.registry = registry;

        this.kieServerLocation = this.registry.getConfig().getConfigItemValue(KieServerConstants.KIE_SERVER_LOCATION, System.getProperty(KieServerConstants.KIE_SERVER_LOCATION, "unknown"));
        if (!this.kieServerLocation.endsWith("/")) {
            this.kieServerLocation = kieServerLocation + "/";
        }
    }

    private byte[] getProcessImageAsBytes(String containerId, String processId) {

        ProcessDefinition procDef = dataService.getProcessesByDeploymentIdProcessId(containerId, processId);
        if (procDef == null) {
            throw new IllegalArgumentException("No process found for " + processId + " within container " + containerId);
        }

        String location = "";
        if (procDef.getPackageName() != null && !procDef.getPackageName().trim().isEmpty()) {
            location = procDef.getPackageName().replaceAll("\\.", "/") + "/";
        }
        // get SVG String
        byte[] imageSVG = imageReferenceMap.get(containerId).getImageContent(location, processId);
        if (imageSVG == null) {
            logger.warn("Could not find SVG image file for process '" + processId + "' within container " + containerId);
            return null;
        }

        return imageSVG;
    }

    public String getProcessImage(String containerId, String processId) {
        containerId = registry.getContainerId(containerId, ContainerLocatorProvider.get().getLocator());

        String imageSVGString = null;
        byte[] imageSVG = getProcessImageAsBytes(containerId, processId);
        if (imageSVG != null) {
            ByteArrayInputStream svgStream = new ByteArrayInputStream(imageSVG);
            SVGProcessor processor = new SVGImageProcessor(svgStream).getProcessor();
            imageSVGString = processor.getSVG();
        }

        return imageSVGString;
    }

    public String getActiveProcessImage(String containerId, long procInstId) {
        return getActiveProcessImage(containerId, procInstId, COMPLETED_COLOR, COMPLETED_BORDER_COLOR, ACTIVE_BORDER_COLOR, false);
    }

    public String getActiveProcessImage(String containerId, long procInstId, String completedNodeColor,
                                        String completedNodeBorderColor, String activeNodeBorderColor, Boolean showBadges) {
        ProcessInstanceDesc instance = dataService.getProcessInstanceById(procInstId);
        if (instance == null) {
            throw new ProcessInstanceNotFoundException("No instance found for process instance id " + procInstId);
        }
        String imageSVGString = null;
        // get SVG String
        byte[] imageSVG = getProcessImageAsBytes(instance.getDeploymentId(), instance.getProcessId());
        if (imageSVG != null) {
            // find active nodes and modify image
            Map<String, String> subProcessLinks = new HashMap<>();
            QueryContext qc = MAX_NODES > 0 ? new QueryContext(0, MAX_NODES) : null;
            Collection<NodeInstanceDesc> activeLogs = dataService.getProcessInstanceHistoryActive(procInstId, qc);
            Collection<NodeInstanceDesc> completedLogs = dataService.getProcessInstanceHistoryCompleted(procInstId, qc);
            Map<Long, String> active = new HashMap<Long, String>();
            List<String> completed = new ArrayList<String>();

            for (NodeInstanceDesc activeNode : activeLogs) {
                active.put(activeNode.getId(), activeNode.getNodeId());

                populateSubProcessLink(containerId, activeNode, subProcessLinks);
            }

            for (NodeInstanceDesc completeNode : completedLogs) {
                completed.add(completeNode.getNodeId());

                active.remove(completeNode.getId());

                populateSubProcessLink(containerId, completeNode, subProcessLinks);
            }

            Map<String, Long> badges = null;
            if (showBadges) {
                Collection<NodeInstanceDesc> allNodes = new ArrayList<>();
                allNodes.addAll(completedLogs);
                allNodes.addAll(activeLogs);

                badges = allNodes.stream().collect(Collectors.groupingBy(NodeInstanceDesc::getNodeId, Collectors.counting()));
            }

            ByteArrayInputStream svgStream = new ByteArrayInputStream(imageSVG);

            imageSVGString = SVGImageProcessor.transform(svgStream, completed, new ArrayList<String>(active.values()),
                                                         subProcessLinks, completedNodeColor, completedNodeBorderColor,
                                                         activeNodeBorderColor, badges);

            return imageSVGString;
        }
        throw new IllegalArgumentException("No process found for " + instance.getProcessId() + " within container " + containerId);
    }

    protected void populateSubProcessLink(String containerId, NodeInstanceDesc node, Map<String, String> subProcessLinks) {
        if (node.getReferenceId() != null && node.getNodeType().endsWith("SubProcessNode")) {

            String link = kieServerLocation + MessageFormat.format(processInstanceImageLink, containerId, node.getReferenceId().toString());
            subProcessLinks.put(node.getNodeId(), link);
        }
    }
}
