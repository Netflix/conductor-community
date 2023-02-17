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
package com.netflix.conductor.postgres.dao;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import javax.sql.DataSource;

import org.springframework.retry.support.RetryTemplate;

import com.netflix.conductor.common.metadata.events.EventExecution;
import com.netflix.conductor.common.metadata.tasks.TaskExecLog;
import com.netflix.conductor.common.run.SearchResult;
import com.netflix.conductor.common.run.TaskSummary;
import com.netflix.conductor.common.run.WorkflowSummary;
import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.dao.IndexDAO;
import com.netflix.conductor.postgres.util.PostgresIndexQueryBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PostgresIndexDAO extends PostgresBaseDAO implements IndexDAO {

    public PostgresIndexDAO(
            RetryTemplate retryTemplate, ObjectMapper objectMapper, DataSource dataSource) {
        super(retryTemplate, objectMapper, dataSource);
    }

    @Override
    public void indexWorkflow(WorkflowSummary workflow) {
        String INSERT_WORKFLOW_INDEX_SQL =
                "INSERT INTO workflow_index (workflow_id, correlation_id, workflow_type, start_time, status, json_data)"
                        + "VALUES (?, ?, ?, ?, ?, ?::JSONB) ON CONFLICT (workflow_id) \n"
                        + "DO UPDATE SET correlation_id = EXCLUDED.correlation_id, workflow_type = EXCLUDED.workflow_type, "
                        + "start_time = EXCLUDED.start_time, status = EXCLUDED.status, json_data = EXCLUDED.json_data";

        TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(workflow.getStartTime());
        Timestamp startTime = Timestamp.from(Instant.from(ta));

        queryWithTransaction(
                INSERT_WORKFLOW_INDEX_SQL,
                q ->
                        q.addParameter(workflow.getWorkflowId())
                                .addParameter(workflow.getCorrelationId())
                                .addParameter(workflow.getWorkflowType())
                                .addParameter(startTime)
                                .addParameter(workflow.getStatus().toString())
                                .addJsonParameter(workflow)
                                .executeUpdate());
    }

    @Override
    public SearchResult<WorkflowSummary> searchWorkflowSummary(
            String query, String freeText, int start, int count, List<String> sort) {
        PostgresIndexQueryBuilder queryBuilder =
                new PostgresIndexQueryBuilder(
                        "workflow_index", query, freeText, start, count, sort);

        List<WorkflowSummary> results =
                queryWithTransaction(
                        queryBuilder.getQuery(),
                        q -> {
                            queryBuilder.addParameters(q);
                            return q.executeAndFetch(WorkflowSummary.class);
                        });

        // To avoid making a second potentially expensive query to postgres say we've
        // got enough results for another page so the pagination works
        int totalHits = results.size() == count ? start + count + 1 : start + results.size();
        return new SearchResult<>(totalHits, results);
    }

    @Override
    public void indexTask(TaskSummary task) {
        String INSERT_TASK_INDEX_SQL =
                "INSERT INTO task_index (task_id, task_type, task_def_name, status, start_time, update_time, workflow_type, json_data)"
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?::JSONB) ON CONFLICT (task_id) "
                        + "DO UPDATE SET task_type = EXCLUDED.task_type, task_def_name = EXCLUDED.task_def_name, "
                        + "status = EXCLUDED.status, update_time = EXCLUDED.update_time, json_data = EXCLUDED.json_data";

        TemporalAccessor updateTa = DateTimeFormatter.ISO_INSTANT.parse(task.getUpdateTime());
        Timestamp updateTime = Timestamp.from(Instant.from(updateTa));

        TemporalAccessor startTa = DateTimeFormatter.ISO_INSTANT.parse(task.getStartTime());
        Timestamp startTime = Timestamp.from(Instant.from(startTa));

        queryWithTransaction(
                INSERT_TASK_INDEX_SQL,
                q ->
                        q.addParameter(task.getTaskId())
                                .addParameter(task.getTaskType())
                                .addParameter(task.getTaskDefName())
                                .addParameter(task.getStatus().toString())
                                .addParameter(startTime)
                                .addParameter(updateTime)
                                .addParameter(task.getWorkflowType())
                                .addJsonParameter(task)
                                .executeUpdate());
    }

    @Override
    public SearchResult<TaskSummary> searchTaskSummary(
            String query, String freeText, int start, int count, List<String> sort) {
        PostgresIndexQueryBuilder queryBuilder =
                new PostgresIndexQueryBuilder("task_index", query, freeText, start, count, sort);

        List<TaskSummary> results =
                queryWithTransaction(
                        queryBuilder.getQuery(),
                        q -> {
                            queryBuilder.addParameters(q);
                            return q.executeAndFetch(TaskSummary.class);
                        });

        // To avoid making a second potentially expensive query to postgres say we've
        // got enough results for another page so the pagination works
        int totalHits = results.size() == count ? start + count + 1 : start + results.size();
        return new SearchResult<>(totalHits, results);
    }

    @Override
    public void addTaskExecutionLogs(List<TaskExecLog> logs) {
        String INSERT_LOG =
                "INSERT INTO task_execution_logs (task_id, created_time, log) VALUES (?, ?, ?)";
        for (TaskExecLog log : logs) {
            queryWithTransaction(
                    INSERT_LOG,
                    q ->
                            q.addParameter(log.getTaskId())
                                    .addParameter(new Timestamp(log.getCreatedTime()))
                                    .addParameter(log.getLog())
                                    .executeUpdate());
        }
    }

    @Override
    public List<TaskExecLog> getTaskExecutionLogs(String taskId) {
        return queryWithTransaction(
                "SELECT log, task_id, created_time FROM task_execution_logs WHERE task_id = ? ORDER BY created_time ASC",
                q ->
                        q.addParameter(taskId)
                                .executeAndFetch(
                                        rs -> {
                                            List<TaskExecLog> result = new ArrayList<>();
                                            while (rs.next()) {
                                                TaskExecLog log = new TaskExecLog();
                                                log.setLog(rs.getString("log"));
                                                log.setTaskId(rs.getString("task_id"));
                                                log.setCreatedTime(
                                                        rs.getDate("created_time").getTime());
                                                result.add(log);
                                            }
                                            return result;
                                        }));
    }

    @Override
    public void setup() {}

    @Override
    public CompletableFuture<Void> asyncIndexWorkflow(WorkflowSummary workflow) {
        throw new UnsupportedOperationException(
                "asyncIndexWorkflow is not supported for postgres indexing");
    }

    @Override
    public CompletableFuture<Void> asyncIndexTask(TaskSummary task) {
        throw new UnsupportedOperationException(
                "asyncIndexTask is not supported for postgres indexing");
    }

    @Override
    public SearchResult<String> searchWorkflows(
            String query, String freeText, int start, int count, List<String> sort) {
        throw new UnsupportedOperationException(
                "searchWorkflows is not supported for postgres indexing");
    }

    @Override
    public SearchResult<String> searchTasks(
            String query, String freeText, int start, int count, List<String> sort) {
        throw new UnsupportedOperationException(
                "searchTasks is not supported for postgres indexing");
    }

    @Override
    public void removeWorkflow(String workflowId) {
        throw new UnsupportedOperationException(
                "removeWorkflow is not supported for postgres indexing");
    }

    @Override
    public CompletableFuture<Void> asyncRemoveWorkflow(String workflowId) {
        throw new UnsupportedOperationException(
                "asyncRemoveWorkflow is not supported for postgres indexing");
    }

    @Override
    public void updateWorkflow(String workflowInstanceId, String[] keys, Object[] values) {
        throw new UnsupportedOperationException(
                "updateWorkflow is not supported for postgres indexing");
    }

    @Override
    public CompletableFuture<Void> asyncUpdateWorkflow(
            String workflowInstanceId, String[] keys, Object[] values) {
        throw new UnsupportedOperationException(
                "asyncUpdateWorkflow is not supported for postgres indexing");
    }

    @Override
    public void removeTask(String workflowId, String taskId) {
        throw new UnsupportedOperationException(
                "removeTask is not supported for postgres indexing");
    }

    @Override
    public CompletableFuture<Void> asyncRemoveTask(String workflowId, String taskId) {
        throw new UnsupportedOperationException(
                "asyncRemoveTask is not supported for postgres indexing");
    }

    @Override
    public void updateTask(String workflowId, String taskId, String[] keys, Object[] values) {
        throw new UnsupportedOperationException(
                "updateTask is not supported for postgres indexing");
    }

    @Override
    public CompletableFuture<Void> asyncUpdateTask(
            String workflowId, String taskId, String[] keys, Object[] values) {
        throw new UnsupportedOperationException(
                "asyncUpdateTask is not supported for postgres indexing");
    }

    @Override
    public String get(String workflowInstanceId, String key) {
        throw new UnsupportedOperationException("get is not supported for postgres indexing");
    }

    @Override
    public CompletableFuture<Void> asyncAddTaskExecutionLogs(List<TaskExecLog> logs) {
        throw new UnsupportedOperationException(
                "asyncAddTaskExecutionLogs is not supported for postgres indexing");
    }

    @Override
    public void addEventExecution(EventExecution eventExecution) {
        throw new UnsupportedOperationException(
                "addEventExecution is not supported for postgres indexing");
    }

    @Override
    public List<EventExecution> getEventExecutions(String event) {
        throw new UnsupportedOperationException(
                "getEventExecutions is not supported for postgres indexing");
    }

    @Override
    public CompletableFuture<Void> asyncAddEventExecution(EventExecution eventExecution) {
        throw new UnsupportedOperationException(
                "asyncAddEventExecution is not supported for postgres indexing");
    }

    @Override
    public void addMessage(String queue, Message msg) {
        throw new UnsupportedOperationException(
                "addMessage is not supported for postgres indexing");
    }

    @Override
    public CompletableFuture<Void> asyncAddMessage(String queue, Message message) {
        throw new UnsupportedOperationException(
                "asyncAddMessage is not supported for postgres indexing");
    }

    @Override
    public List<Message> getMessages(String queue) {
        throw new UnsupportedOperationException(
                "getMessages is not supported for postgres indexing");
    }

    @Override
    public List<String> searchArchivableWorkflows(String indexName, long archiveTtlDays) {
        throw new UnsupportedOperationException(
                "searchArchivableWorkflows is not supported for postgres indexing");
    }

    public long getWorkflowCount(String query, String freeText) {
        throw new UnsupportedOperationException(
                "getWorkflowCount is not supported for postgres indexing");
    }
}
