/*
 * Copyright 2021 Orkes, Inc.
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
package io.orkes.conductor.dao.postgres;

import java.util.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.netflix.conductor.common.config.ObjectMapperProvider;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.core.config.ConductorProperties;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

import io.orkes.conductor.dao.postgres.archive.PostgresArchiveDAO;
import io.orkes.conductor.id.TimeBasedUUIDGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PostgresArchiveDAOTest {

    private final ObjectMapper objectMapper = new ObjectMapperProvider().getObjectMapper();
    private PostgresDAOTestUtil testPostgres;
    private PostgresArchiveDAO archiveDAO;
    public PostgreSQLContainer<?> postgreSQLContainer;
    public GenericContainer redis;
    private TimeBasedUUIDGenerator timeBasedUUIDGenerator = new TimeBasedUUIDGenerator();

    @BeforeAll
    public void setup() {
        redis =
                new GenericContainer(DockerImageName.parse("redis:6.2.6-alpine"))
                        .withExposedPorts(6379);
        redis.start();

        ConductorProperties conductorProperties = new ConductorProperties();

        postgreSQLContainer =
                new PostgreSQLContainer<>(DockerImageName.parse("postgres:11.15-alpine"))
                        .withDatabaseName("conductor");
        postgreSQLContainer.start();
        testPostgres = new PostgresDAOTestUtil(postgreSQLContainer, objectMapper);

        archiveDAO =
                new PostgresArchiveDAO(
                        testPostgres.getObjectMapper(),
                        testPostgres.getDataSource(),
                        testPostgres.getDataSource());
    }

    @AfterAll
    public void teardown() {
        testPostgres.getDataSource().close();
        postgreSQLContainer.stop();
    }

    @Test
    public void testIndexLargeDoc() {
        WorkflowDef def = new WorkflowDef();
        def.setName("pending_count_correlation_jtest");

        WorkflowModel workflow = createTestWorkflow();
        Map<String, Object> output = workflow.getTasks().get(0).getOutputData();
        for (int i = 0; i < 100; i++) {
            output.put("key_" + i, generateRandomString());
        }

        archiveDAO.createOrUpdateWorkflow(workflow);
    }

    private static String generateRandomString() {
        Random random = new Random();
        int wordCount = 5;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wordCount; i++) {
            sb.append(generateRandomWord());
            sb.append(" ");
        }
        return sb.toString();
    }

    private static String generateRandomWord() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();
        int targetStringLength = 15;

        String generatedString =
                random.ints(leftLimit, rightLimit + 1)
                        .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                        .limit(targetStringLength)
                        .collect(
                                StringBuilder::new,
                                StringBuilder::appendCodePoint,
                                StringBuilder::append)
                        .toString();

        return generatedString;
    }

    @Test
    public void testOlderDataIndex() {
        WorkflowDef def = new WorkflowDef();
        def.setName("pending_count_correlation_jtest");

        WorkflowModel workflow = createTestWorkflow();
        workflow.setWorkflowDefinition(def);
        workflow.setWorkflowId(UUID.randomUUID().toString());

        archiveDAO.createOrUpdateWorkflow(workflow);
        WorkflowModel found = archiveDAO.getWorkflow(workflow.getWorkflowId(), false);
        assertNotNull(found);
        assertEquals(workflow.getWorkflowId(), found.getWorkflowId());
    }

    @Test
    public void testIndexWorkflow() {
        WorkflowDef def = new WorkflowDef();
        def.setName("pending_count_correlation_jtest");

        WorkflowModel workflow = createTestWorkflow();
        workflow.setWorkflowDefinition(def);

        generateWorkflows(workflow, 10);
        List<String> bycorrelationId =
                archiveDAO.getWorkflowIdsByCorrelationId(
                        "pending_count_correlation_jtest", "corr001", true, true);
        assertNotNull(bycorrelationId);
        assertEquals(10, bycorrelationId.size());
        System.out.println("Workflow Ids: " + bycorrelationId);

        List<String> bycorrelationId2 =
                archiveDAO.getWorkflowIdsByCorrelationId(
                        "pending_count_correlation_jtest", "corr001", true, true);
        System.out.println("Workflow Ids: " + bycorrelationId2);
        System.out.println("Workflow Ids: " + (bycorrelationId.size() == bycorrelationId2.size()));

        workflow.setStatus(WorkflowModel.Status.COMPLETED);
        workflow.setUpdatedTime(System.currentTimeMillis());
        workflow.getTasks().forEach(t -> t.setStatus(TaskModel.Status.COMPLETED));
        workflow.setUpdatedTime(System.currentTimeMillis());
        archiveDAO.createOrUpdateWorkflow(workflow);
        WorkflowModel found = archiveDAO.getWorkflow(workflow.getWorkflowId(), false);
        assertNotNull(found);
        assertNotNull(workflow.getTasks());
        assertFalse(workflow.getTasks().isEmpty());

        // Updating it back to running status shouldn't do anything!
        workflow.setStatus(WorkflowModel.Status.RUNNING);
        archiveDAO.createOrUpdateWorkflow(workflow);
        found = archiveDAO.getWorkflow(workflow.getWorkflowId(), false);
        assertNotNull(found);
        assertEquals(WorkflowModel.Status.COMPLETED, found.getStatus());
    }

    @Test
    public void testTablePartitioning() {
        WorkflowDef def = new WorkflowDef();
        def.setName("pending_count_correlation_jtest");

        WorkflowModel workflow = createTestWorkflow();
        workflow.setWorkflowDefinition(def);
        workflow.setWorkflowId("4c66564d-c1e8-11ec-8f29-d2403f37b380");
        workflow.setStatus(WorkflowModel.Status.COMPLETED);
        archiveDAO.createOrUpdateWorkflow(workflow);
        WorkflowModel found = archiveDAO.getWorkflow(workflow.getWorkflowId(), false);
        assertNotNull(found);
        assertEquals(WorkflowModel.Status.COMPLETED, found.getStatus());
    }

    @Test
    public void testFindByCorrelationId() {
        WorkflowDef def = new WorkflowDef();
        def.setName("pending_count_correlation_jtest");
        String correlationId = "correlation_id#001";

        WorkflowModel workflow1 = createTestWorkflow();
        workflow1.setWorkflowDefinition(def);
        workflow1.setWorkflowId(timeBasedUUIDGenerator.generate());
        workflow1.setStatus(WorkflowModel.Status.RUNNING);
        workflow1.setCorrelationId(correlationId);
        archiveDAO.createOrUpdateWorkflow(workflow1);

        WorkflowModel workflow2 = createTestWorkflow();
        workflow2.setWorkflowDefinition(def);
        workflow2.setWorkflowId(timeBasedUUIDGenerator.generate());
        workflow2.setStatus(WorkflowModel.Status.COMPLETED);
        workflow2.setCorrelationId(correlationId);
        archiveDAO.createOrUpdateWorkflow(workflow2);

        List<String> found =
                archiveDAO.getWorkflowIdsByCorrelationId(def.getName(), correlationId, true, true);
        assertNotNull(found);
        assertEquals(2, found.size());
        assertTrue(found.contains(workflow1.getWorkflowId()));
        assertTrue(found.contains(workflow2.getWorkflowId()));

        found = archiveDAO.getWorkflowIdsByCorrelationId(def.getName(), correlationId, false, true);
        assertNotNull(found);
        assertEquals(1, found.size());
        assertTrue(found.contains(workflow1.getWorkflowId()));
    }

    protected List<String> generateWorkflows(WorkflowModel workflow, int count) {
        List<String> workflowIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String workflowId = new TimeBasedUUIDGenerator().generate();
            workflow.setWorkflowId(workflowId);
            workflow.setCorrelationId("corr001");
            workflow.setStatus(WorkflowModel.Status.RUNNING);
            workflow.setCreateTime(System.currentTimeMillis());
            archiveDAO.createOrUpdateWorkflow(workflow);
            workflowIds.add(workflowId);
        }
        return workflowIds;
    }

    protected WorkflowModel createTestWorkflow() {
        WorkflowDef def = new WorkflowDef();
        def.setName("Junit Workflow");
        def.setVersion(3);
        def.setSchemaVersion(2);

        WorkflowModel workflow = new WorkflowModel();
        workflow.setWorkflowDefinition(def);
        workflow.setCorrelationId("correlationX");
        workflow.setCreatedBy("junit_tester");
        workflow.setEndTime(200L);

        Map<String, Object> input = new HashMap<>();
        input.put("param1", "param1 value");
        input.put("param2", 100);
        workflow.setInput(input);

        Map<String, Object> output = new HashMap<>();
        output.put("ouput1", "output 1 value");
        output.put("op2", 300);
        workflow.setOutput(output);

        workflow.setOwnerApp("workflow");
        workflow.setParentWorkflowId("parentWorkflowId");
        workflow.setParentWorkflowTaskId("parentWFTaskId");
        workflow.setReasonForIncompletion("missing recipe");
        workflow.setReRunFromWorkflowId("re-run from id1");
        workflow.setCreateTime(90L);
        workflow.setStatus(WorkflowModel.Status.FAILED);
        workflow.setWorkflowId(timeBasedUUIDGenerator.generate());

        List<TaskModel> tasks = new LinkedList<>();

        TaskModel task = new TaskModel();
        task.setScheduledTime(1L);
        task.setSeq(1);
        task.setTaskId(timeBasedUUIDGenerator.generate());
        task.setReferenceTaskName("t1");
        task.setWorkflowInstanceId(workflow.getWorkflowId());
        task.setTaskDefName("task1");
        task.setStatus(TaskModel.Status.COMPLETED);

        TaskModel task2 = new TaskModel();
        task2.setScheduledTime(2L);
        task2.setSeq(2);
        task2.setTaskId(timeBasedUUIDGenerator.generate());
        task2.setReferenceTaskName("t2");
        task2.setWorkflowInstanceId(workflow.getWorkflowId());
        task2.setTaskDefName("task2");
        task2.setStatus(TaskModel.Status.COMPLETED);

        TaskModel task3 = new TaskModel();
        task3.setScheduledTime(2L);
        task3.setSeq(3);
        task3.setTaskId(timeBasedUUIDGenerator.generate());
        task3.setReferenceTaskName("t3");
        task3.setWorkflowInstanceId(workflow.getWorkflowId());
        task3.setTaskDefName("task3");
        task3.setStatus(TaskModel.Status.IN_PROGRESS);

        tasks.add(task);
        tasks.add(task2);
        tasks.add(task3);

        workflow.setTasks(tasks);

        workflow.setUpdatedBy("junit_tester");
        workflow.setUpdatedTime(800L);

        return workflow;
    }
}
