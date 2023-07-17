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

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.core.dal.ExecutionDAOFacade;

@Singleton
// public class TaskStatusPublisher implements TaskStatusListener {
public class TaskStatusPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStatusPublisher.class);
    private static final Integer QDEPTH =
            Integer.parseInt(
                    System.getenv().getOrDefault("ENV_TASK_NOTIFICATION_QUEUE_SIZE", "50"));
    private BlockingQueue<Task> blockingQueue = new LinkedBlockingDeque<>(QDEPTH);

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
            Task task = null;
            TaskNotification taskNotification = null;
            while (true) {
                try {
                    task = blockingQueue.take();
                    taskNotification = new TaskNotification(task);
                    String jsonTask = taskNotification.toJsonString();
                    LOGGER.info("Publishing TaskNotification: {}", jsonTask);
                    if (taskNotification.getTaskType().equals("SUB_WORKFLOW")) {
                        LOGGER.info(
                                "Skip task '{}' notification. Task type is SUB_WORKFLOW.",
                                taskNotification.getTaskId());
                        continue;
                    }
                    if (taskNotification.getAccountMoId().equals("")) {
                        LOGGER.info(
                                "Skip task '{}' notification. Account Id is empty.",
                                taskNotification.getTaskId());
                        continue;
                    }
                    if (taskNotification.getDomainGroupMoId().equals("")) {
                        LOGGER.info(
                                "Skip task '{}' notification. Domain group is empty.",
                                taskNotification.getTaskId());
                        continue;
                    }
                    publishTaskNotification(taskNotification);
                    LOGGER.debug("Task {} publish is successful.", taskNotification.getTaskId());
                    Thread.sleep(5);
                } catch (Exception e) {
                    if (taskNotification != null) {
                        LOGGER.error(
                                "Error while publishing task. Hence updating elastic search index taskId {} taskname {}",
                                task.getTaskId(),
                                task.getTaskDefName());
                        // TBD executionDAOFacade.indexTask(task);

                    } else {
                        LOGGER.error("Failed to publish task: Task is NULL");
                    }
                    LOGGER.error("Error on publishing ", e);
                }
            }
        }
    }

    @Inject
    public TaskStatusPublisher(RestClientManager rcm, ExecutionDAOFacade executionDAOFacade) {
        this.rcm = rcm;
        this.executionDAOFacade = executionDAOFacade;
        ConsumerThread consumerThread = new ConsumerThread();
        consumerThread.start();
    }

    // @Override
    public void onTaskScheduled(Task task) {
        try {
            blockingQueue.put(task);
        } catch (Exception e) {
            LOGGER.error(
                    "Failed to enqueue task: Id {} Type {} of workflow {} ",
                    task.getTaskId(),
                    task.getTaskType(),
                    task.getWorkflowInstanceId());
            LOGGER.error(e.toString());
        }
    }

    private void publishTaskNotification(TaskNotification taskNotification) throws IOException {
        String jsonTask = taskNotification.toJsonStringWithInputOutput();
        rcm.postNotification(
                RestClientManager.NotificationType.TASK,
                jsonTask,
                taskNotification.getDomainGroupMoId(),
                taskNotification.getAccountMoId(),
                taskNotification.getTaskId(), null);
    }
}
