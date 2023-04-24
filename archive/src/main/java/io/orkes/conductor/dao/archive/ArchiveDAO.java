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

import java.util.List;

import com.netflix.conductor.common.metadata.tasks.TaskExecLog;
import com.netflix.conductor.model.WorkflowModel;

public interface ArchiveDAO {

    // Workflow Methods

    void createOrUpdateWorkflow(WorkflowModel workflow);

    boolean removeWorkflow(String workflowId);

    WorkflowModel getWorkflow(String workflowId, boolean includeTasks);

    List<String> getWorkflowPath(String workflowId);

    List<String> getWorkflowIdsByType(String workflowName, Long startTime, Long endTime);

    List<String> getWorkflowIdsByCorrelationId(
            String workflowName, String correlationId, boolean includeClosed, boolean includeTasks);

    ScrollableSearchResult<String> searchWorkflows(
            String query, String freeText, int start, int count);

    List<TaskExecLog> getTaskExecutionLogs(String taskId);

    void addTaskExecutionLogs(List<TaskExecLog> logs);

    List<WorkflowModel> getWorkflowFamily(String workflowId, boolean summaryOnly);
}
