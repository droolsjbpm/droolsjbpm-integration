/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.server.services.prometheus;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.jbpm.workflow.instance.node.WorkItemNodeInstance;
import org.kie.api.event.process.DefaultProcessEventListener;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessNodeLeftEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.event.process.SLAViolatedEvent;
import org.kie.api.runtime.process.NodeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.valueOf;
import static org.kie.server.services.prometheus.PrometheusMetrics.millisToSeconds;

public class PrometheusProcessEventListener extends DefaultProcessEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusProcessEventListener.class);

    protected static final Counter numberOfProcessInstancesStarted = Counter.build()
            .name("kie_server_process_instance_started_total")
            .help("Kie Server Started Process Instances")
            .labelNames("container_id", "process_id")
            .register();

    protected static final Counter numberOfSLAsViolated = Counter.build()
            .name("kie_server_process_instance_sla_violated_total")
            .help("Kie Server Process Instances SLA Violated")
            .labelNames("container_id", "process_id", "node_name")
            .register();

    protected static final Counter numberOfProcessInstancesCompleted = Counter.build()
            .name("kie_server_process_instance_completed_total")
            .help("Kie Server Completed Process Instances")
            .labelNames("container_id", "process_id", "status")
            .register();

    protected static final Gauge runningProcessInstances = Gauge.build()
            .name("kie_server_process_instance_running_total")
            .help("Kie Server Running Process Instances")
            .labelNames("container_id", "process_id")
            .register();

    protected static final Summary processInstancesDuration = Summary.build()
            .name("kie_server_process_instance_duration_seconds")
            .help("Kie Server Process Instances Duration")
            .labelNames("container_id", "process_id")
            .register();

    protected static final Summary workItemsDuration = Summary.build()
            .name("kie_server_work_item_duration_seconds")
            .help("Kie Server Work Items Duration")
            .labelNames("name")
            .register();

    protected static void recordRunningProcessInstance(String containerId, String processId) {
        runningProcessInstances.labels(containerId, processId).inc();
    }

    @Override
    public void afterProcessStarted(ProcessStartedEvent event) {
        LOGGER.debug("After process started event: {}", event);
        final WorkflowProcessInstanceImpl processInstance = (WorkflowProcessInstanceImpl) event.getProcessInstance();
        numberOfProcessInstancesStarted.labels(processInstance.getDeploymentId(), processInstance.getProcessId()).inc();
        recordRunningProcessInstance(processInstance.getDeploymentId(), processInstance.getProcessId());
    }

    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {
        LOGGER.debug("After process completed event: {}", event);
        final WorkflowProcessInstanceImpl processInstance = (WorkflowProcessInstanceImpl) event.getProcessInstance();
        runningProcessInstances.labels(processInstance.getDeploymentId(), processInstance.getProcessId()).dec();

        numberOfProcessInstancesCompleted.labels(processInstance.getDeploymentId(), processInstance.getProcessId(), valueOf(processInstance.getState())).inc();

        if (processInstance.getStartDate() != null) {
            final double duration = millisToSeconds(System.currentTimeMillis() - processInstance.getStartDate().getTime());
            processInstancesDuration.labels(processInstance.getDeploymentId(), processInstance.getProcessId()).observe(duration);
            LOGGER.debug("Process Instance duration: {}s", duration);
        }
    }

    @Override
    public void beforeNodeLeft(ProcessNodeLeftEvent event) {
        LOGGER.debug("Before Node left event: {}", event);
        final NodeInstance nodeInstance = event.getNodeInstance();
        if (nodeInstance instanceof WorkItemNodeInstance) {
            WorkItemNodeInstance wi = (WorkItemNodeInstance) nodeInstance;
            if (wi.getTriggerTime() != null) {
                final String name = wi.getWorkItem().getName();
                final double duration = millisToSeconds(System.currentTimeMillis() - wi.getTriggerTime().getTime());
                workItemsDuration.labels(name).observe(duration);
                LOGGER.debug("Work Item {}, duration: {}s", name, duration);
            }
        }
    }

    @Override
    public void afterSLAViolated(SLAViolatedEvent event) {
        LOGGER.debug("After SLA violated event: {}", event);
        final WorkflowProcessInstanceImpl processInstance = (WorkflowProcessInstanceImpl) event.getProcessInstance();
        if (processInstance != null && event.getNodeInstance() != null) {
            numberOfSLAsViolated.labels(processInstance.getDeploymentId(), processInstance.getProcessId(), event.getNodeInstance().getNodeName()).inc();
        }
    }
}
