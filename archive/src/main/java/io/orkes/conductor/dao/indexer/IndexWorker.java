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
package io.orkes.conductor.dao.indexer;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.netflix.conductor.dao.ExecutionDAO;
import com.netflix.conductor.dao.QueueDAO;
import com.netflix.conductor.metrics.Monitors;
import com.netflix.conductor.model.WorkflowModel;

import io.orkes.conductor.dao.archive.ArchiveDAO;
import io.orkes.conductor.metrics.MetricsCollector;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "conductor.archive.db.enabled", havingValue = "true")
public class IndexWorker {

    public static final String INDEXER_QUEUE = "_index_queue";

    public static final String WORKFLOW_ID_PREFIX = "wf:";

    public static final String EVENT_ID_PREFIX = "e:";

    private final QueueDAO queueDAO;

    private final ArchiveDAO archiveDAO;

    private final ExecutionDAO primaryExecDAO;

    private final MetricsCollector metricsCollector;

    private ScheduledExecutorService executorService;

    private int pollBatchSize;

    private int ttlSeconds = 30;

    public IndexWorker(
            QueueDAO queueDAO,
            ArchiveDAO execDAO,
            @Qualifier("primaryExecutionDAO") ExecutionDAO primaryExecDAO,
            IndexWorkerProperties properties,
            MetricsCollector metricsCollector) {
        this.queueDAO = queueDAO;
        this.archiveDAO = execDAO;
        this.primaryExecDAO = primaryExecDAO;
        this.pollBatchSize = properties.getPollBatchSize();
        this.metricsCollector = metricsCollector;

        int threadCount = properties.getThreadCount();
        int pollingInterval = properties.getPollingInterval();
        if (threadCount > 0) {
            this.executorService =
                    Executors.newScheduledThreadPool(
                            threadCount,
                            new ThreadFactoryBuilder().setNameFormat("indexer-thread-%d").build());

            for (int i = 0; i < threadCount; i++) {
                this.executorService.scheduleWithFixedDelay(
                        () -> {
                            try {
                                pollAndExecute();
                            } catch (Throwable t) {
                                log.error(t.getMessage(), t);
                            }
                        },
                        10,
                        pollingInterval,
                        TimeUnit.MILLISECONDS);
            }
        }

        log.info(
                "IndexWorker::INIT with primaryExecDAO = {}, threadCount = {}, pollingInterval = {} and pollBatchSize = {}",
                primaryExecDAO,
                threadCount,
                pollingInterval,
                pollBatchSize);
    }

    private void pollAndExecute() {

        try {

            List<String> ids = queueDAO.pop(INDEXER_QUEUE, pollBatchSize, 1000); // 1 second

            for (String id : ids) {
                if (id.startsWith(WORKFLOW_ID_PREFIX)) {
                    String workflowId = id.substring(WORKFLOW_ID_PREFIX.length());
                    workflowId = workflowId.substring(0, workflowId.lastIndexOf(':'));
                    indexWorkflow(workflowId);
                    queueDAO.ack(INDEXER_QUEUE, id);
                } else if (id.startsWith(EVENT_ID_PREFIX)) {
                    String eventId = id.substring(EVENT_ID_PREFIX.length());
                    eventId = eventId.substring(0, eventId.lastIndexOf(':'));
                    indexEvent(eventId);
                    queueDAO.ack(INDEXER_QUEUE, id);
                }
            }

        } catch (Throwable e) {
            Monitors.error(IndexWorker.class.getSimpleName(), "pollAndExecute");
            log.error(e.getMessage(), e);
        } finally {
            Monitors.recordQueueDepth(INDEXER_QUEUE, queueDAO.getSize(INDEXER_QUEUE), "");
        }
    }

    private void indexWorkflow(String workflowId) {
        WorkflowModel workflow = primaryExecDAO.getWorkflow(workflowId, true);
        if (workflow == null) {
            log.warn("Cannot find workflow in the primary DAO: {}", workflowId);
            return;
        }

        metricsCollector
                .getTimer("archive_workflow", "workflowName", "" + workflow.getWorkflowName())
                .record(() -> archiveDAO.createOrUpdateWorkflow(workflow));

        if (workflow.getStatus().isTerminal()) {
            primaryExecDAO.removeWorkflowWithExpiry(workflowId, ttlSeconds);
        }
    }

    private void indexEvent(String eventId) {
        log.trace("Indexing event {}", eventId);
        // Do nothing for now
    }
}
