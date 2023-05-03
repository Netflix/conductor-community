/*
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.contribs.publisher;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.core.dal.ExecutionDAOFacade;
import com.netflix.conductor.core.listener.WorkflowStatusListener;
import com.netflix.conductor.model.WorkflowModel;

@Singleton
public class WorkflowStatusPublisher implements WorkflowStatusListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowStatusPublisher.class);
    private static final Integer QDEPTH =
            Integer.parseInt(
                    System.getenv().getOrDefault("ENV_WORKFLOW_NOTIFICATION_QUEUE_SIZE", "50"));
    private BlockingQueue<WorkflowModel> blockingQueue = new LinkedBlockingDeque<>(QDEPTH);
    private RestClientManager rcm;
    private ExecutionDAOFacade executionDAOFacade;

    class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        public void uncaughtException(Thread t, Throwable e) {
            LOGGER.info("An exception has been captured\n");
            LOGGER.info("Thread: {}\n", t.getName());
            LOGGER.info("Exception: {}: {}\n", e.getClass().getName(), e.getMessage());
            LOGGER.info("Stack Trace: \n");
            e.printStackTrace(System.out);
            LOGGER.info("Thread status: {}\n", t.getState());
            new ConsumerThread().start();
        }
    }

    class ConsumerThread extends Thread {

        public void run() {
            this.setUncaughtExceptionHandler(new ExceptionHandler());
            String tName = Thread.currentThread().getName();
            LOGGER.info("{}: Starting consumer thread", tName);

            WorkflowNotification workflowNotification = null;
            WorkflowModel workflow = null;
            while (true) {
                try {
                    workflow = blockingQueue.take();
                    workflowNotification = new WorkflowNotification(workflow.toWorkflow());
                    String jsonWorkflow = workflowNotification.toJsonString();
                    LOGGER.info("Publishing WorkflowNotification: {}", jsonWorkflow);
                    if (workflowNotification.getAccountMoId().equals("")) {
                        LOGGER.info(
                                "Skip workflow '{}' notification. Account Id is empty.",
                                workflowNotification.getWorkflowId());
                        continue;
                    }
                    if (workflowNotification.getDomainGroupMoId().equals("")) {
                        LOGGER.info(
                                "Skip workflow '{}' notification. Domain group is empty.",
                                workflowNotification.getWorkflowId());
                        continue;
                    }
                    publishWorkflowNotification(workflowNotification);
                    LOGGER.debug(
                            "Workflow {} publish is successful.",
                            workflowNotification.getWorkflowId());
                    Thread.sleep(5);
                } catch (Exception e) {
                    if (workflowNotification != null) {
                        LOGGER.error(
                                " Error while publishing workflow. Hence updating elastic search index workflowid {} workflowname {} correlationId {}",
                                workflow.getWorkflowId(),
                                workflow.getWorkflowName(),
                                workflow.getCorrelationId());
                        // TBD executionDAOFacade.indexWorkflow(workflow);
                    } else {
                        LOGGER.error("Failed to publish workflow: Workflow is NULL");
                    }
                    LOGGER.error("Error on publishing workflow", e);
                }
            }
        }
    }

    @Inject
    public WorkflowStatusPublisher(RestClientManager rcm, ExecutionDAOFacade executionDAOFacade) {
        this.rcm = rcm;
        this.executionDAOFacade = executionDAOFacade;
        ConsumerThread consumerThread = new ConsumerThread();
        consumerThread.start();
    }

    @Override
    public void onWorkflowCompleted(WorkflowModel workflow) {
        LOGGER.debug(
                "workflows completion {} {}", workflow.getWorkflowId(), workflow.getWorkflowName());
        try {
            blockingQueue.put(workflow);
        } catch (Exception e) {
            LOGGER.error(
                    "Failed to enqueue workflow: Id {} Name {}",
                    workflow.getWorkflowId(),
                    workflow.getWorkflowName());
            LOGGER.error(e.toString());
        }
    }

    @Override
    public void onWorkflowTerminated(WorkflowModel workflow) {
        LOGGER.debug(
                "workflows termination {} {}",
                workflow.getWorkflowId(),
                workflow.getWorkflowName());
        try {
            blockingQueue.put(workflow);
        } catch (Exception e) {
            LOGGER.error(
                    "Failed to enqueue workflow: Id {} Name {}",
                    workflow.getWorkflowId(),
                    workflow.getWorkflowName());
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public void onWorkflowCompletedIfEnabled(WorkflowModel workflow) {
        onWorkflowCompleted(workflow);
    }

    @Override
    public void onWorkflowTerminatedIfEnabled(WorkflowModel workflow) {
        onWorkflowTerminated(workflow);
    }

    private void publishWorkflowNotification(WorkflowNotification workflowNotification)
            throws IOException {
        String jsonWorkflow = workflowNotification.toJsonStringWithInputOutput();
        rcm.postNotification(
                RestClientManager.NotificationType.WORKFLOW,
                jsonWorkflow,
                workflowNotification.getDomainGroupMoId(),
                workflowNotification.getAccountMoId(),
                workflowNotification.getWorkflowId());
    }
}
