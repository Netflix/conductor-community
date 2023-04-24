/*
 * Copyright 2022 Orkes, Inc.
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
package io.orkes.conductor.dao.archive;

import io.orkes.conductor.dao.postgres.archive.PostgresArchiveDAO;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.netflix.conductor.common.metadata.events.EventExecution;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.dao.ExecutionDAO;
import com.netflix.conductor.dao.QueueDAO;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

import io.orkes.conductor.dao.indexer.IndexWorker;
import io.orkes.conductor.metrics.MetricsCollector;

import lombok.extern.slf4j.Slf4j;

import static io.orkes.conductor.dao.indexer.IndexWorker.INDEXER_QUEUE;

@Slf4j
public class ArchivedExecutionDAO implements ExecutionDAO {

    private static final int OFFSET_TIME_SEC = 0;

    private final ExecutionDAO primaryDAO;

    private final ArchiveDAO archiveDAO;

    private final QueueDAO queueDAO;

    private final MetricsCollector metricsCollector;

    private final Clock clock;

    public ArchivedExecutionDAO(
            ExecutionDAO primaryDAO,
            ArchiveDAO archiveDAO,
            QueueDAO queueDAO,
            MetricsCollector metricsCollector) {
        this.primaryDAO = primaryDAO;
        this.archiveDAO = archiveDAO;
        this.queueDAO = queueDAO;
        this.metricsCollector = metricsCollector;
        this.clock = Clock.systemDefaultZone();
        log.info(
                "Initialized {} as Execution DAO with {} as primary DAO",
                ArchivedExecutionDAO.class.getSimpleName(),
                primaryDAO.getClass().getSimpleName());
    }

    ////////////////////////////////////////////////////////////////////////
    //                  Delegate to Primary DAO                           //
    ////////////////////////////////////////////////////////////////////////
    @Override
    public List<TaskModel> getPendingTasksByWorkflow(String taskName, String workflowId) {
        return primaryDAO.getPendingTasksByWorkflow(taskName, workflowId);
    }

    @Override
    public List<TaskModel> createTasks(List<TaskModel> tasks) {
        return metricsCollector
                .getTimer("create_tasks_dao")
                .record(() -> primaryDAO.createTasks(tasks));
    }

    @Override
    public void updateTask(TaskModel task) {
        metricsCollector
                .getTimer("update_task_dao", "taskType", task.getTaskDefName())
                .record(
                        () -> {
                            primaryDAO.updateTask(task);
                            if (task.getStatus().isTerminal()) {
                                metricsCollector.recordTaskComplete(task);
                            }
                        });
    }

    @Override
    public boolean exceedsInProgressLimit(TaskModel task) {
        return primaryDAO.exceedsInProgressLimit(task);
    }

    @Override
    public boolean removeTask(String taskId) {
        return primaryDAO.removeTask(taskId);
    }

    @Override
    public List<TaskModel> getPendingTasksForTaskType(String taskType) {
        return primaryDAO.getPendingTasksForTaskType(taskType);
    }

    @Override
    public void removeFromPendingWorkflow(String workflowType, String workflowId) {
        primaryDAO.removeFromPendingWorkflow(workflowType, workflowId);
    }

    @Override
    public List<String> getRunningWorkflowIds(String workflowName, int version) {
        return primaryDAO.getRunningWorkflowIds(workflowName, version);
    }

    @Override
    public List<WorkflowModel> getPendingWorkflowsByType(String workflowName, int version) {
        return primaryDAO.getPendingWorkflowsByType(workflowName, version);
    }

    @Override
    public long getPendingWorkflowCount(String workflowName) {
        return primaryDAO.getPendingWorkflowCount(workflowName);
    }

    @Override
    public List<TaskModel> getTasks(String taskType, String startKey, int count) {
        // This method is only intended to show pending tasks
        return primaryDAO.getTasks(taskType, startKey, count);
    }

    @Override
    public long getInProgressTaskCount(String taskDefName) {
        return primaryDAO.getInProgressTaskCount(taskDefName);
    }

    @Override
    public boolean canSearchAcrossWorkflows() {
        return true;
    }

    ////////////////////////////////////////////////////////////////////////
    //                  Hybrid Mode                                       //
    ////////////////////////////////////////////////////////////////////////

    @Override
    public String updateWorkflow(WorkflowModel workflow) {
        return metricsCollector
                .getTimer("update_workflow_dao", "workflowName", workflow.getWorkflowName())
                .record(
                        () -> {
                            workflow.setUpdatedTime(System.currentTimeMillis());
                            String id = primaryDAO.updateWorkflow(workflow);
                            queueForIndexing(workflow, false);
                            if (workflow.getStatus().isTerminal()) {
                                metricsCollector.recordWorkflowComplete(workflow);
                            }
                            return id;
                        });
    }

    @Override
    public TaskModel getTask(String taskId) {
        return metricsCollector
                .getTimer("get_task_dao")
                .record(
                        () -> {
                            TaskModel task = primaryDAO.getTask(taskId);
                            return task;
                        });
    }

    @Override
    public List<TaskModel> getTasks(List<String> taskIds) {
        return metricsCollector
                .getTimer("get_tasks_dao")
                .record(() -> primaryDAO.getTasks(taskIds));
    }

    @Override
    public List<TaskModel> getTasksForWorkflow(String workflowId) {
        return metricsCollector
                .getTimer("get_tasks_for_workflow_dao")
                .record(
                        () -> {
                            List<TaskModel> tasks = primaryDAO.getTasksForWorkflow(workflowId);
                            if (tasks == null || tasks.isEmpty()) {
                                tasks = archiveDAO.getWorkflow(workflowId, true).getTasks();
                            }
                            return tasks;
                        });
    }

    @Override
    public String createWorkflow(WorkflowModel workflow) {
        return metricsCollector
                .getTimer("create_workflow_dao", "workflowName", workflow.getWorkflowName())
                .record(
                        () -> {
                            workflow.setUpdatedTime(System.currentTimeMillis());
                            String workflowId = primaryDAO.createWorkflow(workflow);
                            queueForIndexing(workflow, true);
                            return workflowId;
                        });
    }

    @Override
    public boolean removeWorkflow(String workflowId) {
        boolean removed = primaryDAO.removeWorkflow(workflowId);
        if (!removed) {
            removed = archiveDAO.removeWorkflow(workflowId);
        }
        return removed;
    }

    @Override
    public boolean removeWorkflowWithExpiry(String workflowId, int ttlSeconds) {
        return primaryDAO.removeWorkflowWithExpiry(workflowId, ttlSeconds);
    }

    @Override
    public WorkflowModel getWorkflow(String workflowId) {
        return getWorkflow(workflowId, false);
    }

    @Override
    public WorkflowModel getWorkflow(String workflowId, boolean includeTasks) {
        WorkflowModel workflow = primaryDAO.getWorkflow(workflowId, includeTasks);
        if (workflow == null) {
            log.debug("Not found in primary dao, going to archive {}", workflowId);
            workflow =
                    metricsCollector
                            .getTimer("get_workflow_archive_dao", "includeTasks", "" + includeTasks)
                            .record(() -> archiveDAO.getWorkflow(workflowId, includeTasks));
        }
        return workflow;
    }

    @Override
    public List<String> getWorkflowPath(String workflowId) {
        // Load path from archive
        final List<String> workflowPath;
        try {
            workflowPath = archiveDAO.getWorkflowPath(workflowId);
        } catch (Exception e) {
            log.warn("Unable to load workflow path from archive, using primary DAO", e);
            return primaryDAO.getWorkflowPath(workflowId);
        }

        if (workflowPath == null || workflowPath.size() == 0) {
            log.debug("Unable to load workflow path from archive, using primary DAO");
            // unable to load path from archive, use primary
            return primaryDAO.getWorkflowPath(workflowId);
        }

        if (workflowPath.size() == 1 && workflowPath.contains(PostgresArchiveDAO.UNKNOWN)) {
            log.debug("Unable to load workflow path from archive, using primary DAO");
            // archive cannot find anything, use primary
            return primaryDAO.getWorkflowPath(workflowId);
        }

        if (workflowPath.size() > 1 && workflowPath.contains(PostgresArchiveDAO.UNKNOWN)) {
            log.debug("Unable to load complete workflow path from archive, using also primary DAO");
            // archive cannot find complete path, use primary and join the 2 together
            final List<String> primarySubPath = primaryDAO.getWorkflowPath(workflowPath.get(1));
            // primary cannot find the path either
            if (primarySubPath == null || primarySubPath.size() == 0) {
                throw new IllegalStateException("Unable to fetch workflow path from archive nor from primary db");
            }
            // remove unknown
            workflowPath.remove(0);
            // remove workflowId since it will be present in both
            workflowPath.remove(0);
            primarySubPath.addAll(workflowPath);
            return primarySubPath;
        }

        return workflowPath;
    }

    @Override
    public List<WorkflowModel> getWorkflowsByType(
            String workflowName, Long startTime, Long endTime) {
        List<WorkflowModel> workflows = new ArrayList<>();
        List<String> workflowIds =
                archiveDAO.getWorkflowIdsByType(workflowName, startTime, endTime);
        for (String workflowId : workflowIds) {
            workflows.add(getWorkflow(workflowId));
        }

        return workflows;
    }

    @Override
    public List<WorkflowModel> getWorkflowsByCorrelationId(
            String workflowName, String correlationId, boolean includeTasks) {
        List<String> ids =
                archiveDAO.getWorkflowIdsByCorrelationId(
                        workflowName, correlationId, false, includeTasks);
        return ids.stream()
                .map(id -> getWorkflow(id, includeTasks))
                .filter(wf -> wf != null)
                .collect(Collectors.toList());
    }

    @Override
    public boolean addEventExecution(EventExecution eventExecution) {
        boolean added = primaryDAO.addEventExecution(eventExecution);
        return added;
    }

    @Override
    public void updateEventExecution(EventExecution eventExecution) {
        primaryDAO.updateEventExecution(eventExecution);
    }

    @Override
    public void removeEventExecution(EventExecution eventExecution) {
        primaryDAO.removeEventExecution(eventExecution);
    }

    @Override
    public List<WorkflowModel> getWorkflowFamily(String workflowId, boolean summaryOnly) {
        final List<WorkflowModel> workflowDeep = archiveDAO.getWorkflowFamily(workflowId, summaryOnly);

        if (summaryOnly) {
            return workflowDeep;
        }

        // If some workflow bodies were not loaded from
        return workflowDeep.stream()
                .map(wf -> wf instanceof WorkflowModelSummary ? loadWorkflowFromPrimary(wf) : wf)
                .collect(Collectors.toList());
    }

    private WorkflowModel loadWorkflowFromPrimary(WorkflowModel wf) {
        log.debug("Unable to load workflow: {} from archive, loading from primary", wf.getWorkflowId());
        return primaryDAO.getWorkflow(wf.getWorkflowId());
    }

    private void queueForIndexing(WorkflowModel workflow, boolean created) {

        if (!created && !workflow.getStatus().isTerminal()) {
            // Do nothing!  We only index the workflow once its created and once its completed
            return;
        }
        String messageId =
                IndexWorker.WORKFLOW_ID_PREFIX
                        + workflow.getWorkflowId()
                        + ":"
                        + workflow.getStatus();
        long offsetTime = OFFSET_TIME_SEC;

        if (workflow.getStatus().isTerminal()) {
            // Move ahead of the queue

            // Below is how the score is calculated for pushing the message to the sorted set
            // double score = Long.valueOf(clock.millis() + message.getTimeout()).doubleValue() +
            // priority;

            // Making the time to be negative pushes the message at the beginning of the queue
            // Reducing the current time by 1s second, to ensure any mismatches do not cause score
            // to be negative
            // Negative score is allowed,but when querying the messages, the min score is set to 0
            offsetTime = -1 * ((clock.millis() / 1000) - 1000);
        }

        if (!created && !workflow.getStatus().isTerminal()) {
            // If this is not a newly created workflow and is not yet completed,
            // We add a random delay to index
            // Adding a delay ensures two things:
            // 1. If the workflow completes in the next 1-2 seconds, the completed status will
            // remove the pending
            //   workflow indexing --> see the block below
            // 2. Probabiliy that multiple arallel threads/workers picking up the same workflow Id
            // reduces
            //   avoiding database row lock contention

            int delayInSeconds = Math.max(1, new Random().nextInt(10));
            offsetTime = delayInSeconds;
        }

        queueDAO.push(INDEXER_QUEUE, messageId, offsetTime);

        if (workflow.getStatus().isTerminal()) {
            // Remove any previous message, so we can avoid indexing it twice
            messageId =
                    IndexWorker.WORKFLOW_ID_PREFIX
                            + workflow.getWorkflowId()
                            + ":"
                            + Workflow.WorkflowStatus.RUNNING.toString();
            queueDAO.ack(INDEXER_QUEUE, messageId);
        }
    }
}
