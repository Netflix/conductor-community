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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.netflix.conductor.common.config.ObjectMapperProvider;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

import io.orkes.conductor.dao.postgres.archive.PostgresArchiveDAO;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.*;
import com.zaxxer.hikari.HikariDataSource;

public class PostgresArchivePerformanceTest {

    private PostgresArchiveDAO archiveDAO;
    private ObjectMapper objectMapper = new ObjectMapperProvider().getObjectMapper();
    private HikariDataSource dataSource;
    private static final Faker[] fakers =
            new Faker[] {
                new Faker(new Locale("en_US")),
                new Faker(new Locale("nb-NO")),
                new Faker(new Locale("zh-CN")),
                new Faker(new Locale("fi-FI"))
            };

    private static final int TASK_COUNT = 5;

    private Faker faker;

    public PostgresArchivePerformanceTest() {
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:postgresql://localhost/neel");
        dataSource.setUsername("postgres");
        dataSource.setPassword("postgres");
        dataSource.setAutoCommit(false);
        dataSource.setMaximumPoolSize(10);
        archiveDAO = new PostgresArchiveDAO(objectMapper, dataSource, dataSource);
        faker = new Faker();
    }

    public void generateWorkflowsAndIndex(int count) {
        WorkflowDef workflowDef = generateWorkflowDef();
        for (int i = 0; i < count; i++) {
            WorkflowModel workflow = createWorkflowRun(workflowDef);
            archiveDAO.createOrUpdateWorkflow(workflow);
            System.out.println(workflow.getStatus() + " : " + workflow.getWorkflowId());
        }
    }

    private WorkflowDef generateWorkflowDef() {
        WorkflowDef def = new WorkflowDef();
        def.setName("load_test_workflow");
        def.setVersion(1);
        def.setOwnerEmail("loadtest@orkes.io");
        def.setDescription("load testing workflow");

        for (int i = 0; i < TASK_COUNT; i++) {
            WorkflowTask workflowTask = new WorkflowTask();
            workflowTask.setType(TaskType.SIMPLE.name());
            workflowTask.setTaskReferenceName("task_" + i);
            workflowTask.setName("task-" + i);
            TaskDef taskDef = new TaskDef();
            taskDef.setName("task-" + i);
            taskDef.setOwnerEmail("dev@orkes.io");
            workflowTask.setTaskDefinition(taskDef);
            def.getTasks().add(workflowTask);
        }
        return def;
    }

    protected WorkflowModel createWorkflowRun(WorkflowDef workflowDef) {

        WorkflowModel workflow = new WorkflowModel();
        String workflowId = UUID.randomUUID().toString();
        workflow.setWorkflowId(workflowId);
        workflow.setWorkflowDefinition(workflowDef);
        workflow.setCorrelationId(faker.gameOfThrones().character());
        workflow.setCreatedBy("junit_tester");
        workflow.setEndTime(200L);

        workflow.setInput(getAddress(faker));
        workflow.setOutput(getCommerceData(faker));

        workflow.setOwnerApp("workflow");
        workflow.setReRunFromWorkflowId(UUID.randomUUID().toString());
        workflow.setCreateTime(System.currentTimeMillis() - 10000L);
        workflow.setWorkflowId(UUID.randomUUID().toString());

        List<TaskModel> tasks = new LinkedList<>();
        boolean failed = false;
        String reason = null;
        for (int i = 0; i < TASK_COUNT; i++) {
            TaskModel task = new TaskModel();
            task.setScheduledTime(1L);
            task.setSeq(i + 1);

            task.setTaskId(UUID.randomUUID().toString());
            task.setReferenceTaskName("task_" + i);
            task.setWorkflowInstanceId(workflow.getWorkflowId());
            task.setTaskDefName("task-" + i);
            task.setWorkerId(faker.idNumber().valid());
            task.setTaskType(TaskType.SIMPLE.name());
            task.setWorkflowTask(workflowDef.getTasks().get(i));
            task.setWorkflowType(workflow.getWorkflowName());

            int status = new Random().nextInt(10000);
            if (status >= 20) {
                task.setStatus(TaskModel.Status.COMPLETED);
            } else if (status < 10) {
                task.setStatus(TaskModel.Status.FAILED);
                failed = true;
                reason = faker.chuckNorris().fact();
                task.setReasonForIncompletion(reason);
            } else if (status >= 10 && status < 20) {
                task.setStatus(TaskModel.Status.TIMED_OUT);
                failed = true;
                reason = faker.chuckNorris().fact();
                task.setReasonForIncompletion(reason);
            }

            Faker faker2 = fakers[new Random().nextInt(fakers.length)];
            task.getOutputData().putAll(getCommerceData(faker2));
            task.getOutputData().putAll(getAddress(faker2));
            task.getOutputData().putAll(getDemographics(faker2));
            task.getInputData().putAll(getAddress(faker2));

            tasks.add(task);
        }
        workflow.setTasks(tasks);

        if (failed) {
            workflow.setStatus(WorkflowModel.Status.FAILED);
            workflow.setReasonForIncompletion(reason);
        } else {
            workflow.setStatus(WorkflowModel.Status.COMPLETED);
        }

        workflow.setUpdatedBy("junit_tester");
        workflow.setUpdatedTime(System.currentTimeMillis());

        return workflow;
    }

    private Map<String, Object> getCommerceData(Faker faker) {
        Commerce commerce = faker.commerce();
        Map<String, Object> data = new HashMap<>();
        data.put("department", commerce.department());
        data.put("price", commerce.price());
        data.put("material", commerce.material());
        data.put("promo_code", commerce.promotionCode());
        data.put("product_name", commerce.productName());
        return data;
    }

    private Map<String, Object> getAddress(Faker faker) {
        Address address = faker.address();
        Map<String, Object> data = new HashMap<>();

        data.put("apt", address.buildingNumber());
        data.put("full_address", address.fullAddress());
        data.put("city", address.city());
        data.put("street", address.streetAddress());
        data.put("country", address.country());
        data.put("zip", address.zipCode());

        return data;
    }

    private Map<String, Object> getDemographics(Faker faker) {
        Demographic demographic = faker.demographic();
        Map<String, Object> data = new HashMap<>();

        data.put("demonym", demographic.demonym());
        data.put("education", demographic.educationalAttainment());
        data.put("marital_status", demographic.maritalStatus());
        data.put("race", demographic.race());
        data.put("sex", demographic.sex());
        return data;
    }

    public static void main(String[] args) {
        System.out.println("Start");
        PostgresArchivePerformanceTest tester = new PostgresArchivePerformanceTest();
        ExecutorService es = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 1000; i++) {
            es.submit(
                    () -> {
                        tester.generateWorkflowsAndIndex(1000);
                    });
        }
        es.shutdown();

        System.out.println("Done");
    }
}
