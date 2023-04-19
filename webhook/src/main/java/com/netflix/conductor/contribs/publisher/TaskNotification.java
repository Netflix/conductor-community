/*
 * Copyright 2022 Netflix, Inc.
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

import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.run.TaskSummary;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

@JsonFilter("SecretRemovalFilter")
public class TaskNotification extends TaskSummary {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStatusPublisher.class);

    public String workflowTaskType;
    private String domainGroupMoId = "";
    private String accountMoId = "";
    private ObjectMapper objectMapper = new ObjectMapper();

    public String getDomainGroupMoId() {
        return domainGroupMoId;
    }

    public String getAccountMoId() {
        return accountMoId;
    }

    public TaskNotification(Task task) {
        super(task);
        workflowTaskType = task.getWorkflowTask().getType();

        boolean isFusionMetaPresent = task.getInputData().containsKey("_ioMeta");
        if (!isFusionMetaPresent) {
            return;
        }

        LinkedHashMap fusionMeta = (LinkedHashMap) task.getInputData().get("_ioMeta");
        domainGroupMoId =
                fusionMeta.containsKey("DomainGroupMoId")
                        ? fusionMeta.get("DomainGroupMoId").toString()
                        : "";
        accountMoId =
                fusionMeta.containsKey("AccountMoId")
                        ? fusionMeta.get("AccountMoId").toString()
                        : "";
    }

    String toJsonString() {
        String jsonString;
        SimpleBeanPropertyFilter theFilter =
                SimpleBeanPropertyFilter.serializeAllExcept("input", "output");
        FilterProvider provider =
                new SimpleFilterProvider().addFilter("SecretRemovalFilter", theFilter);
        try {
            jsonString = objectMapper.writer(provider).writeValueAsString(this);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to convert Task: {} to String. Exception: {}", this, e);
            throw new RuntimeException(e);
        }
        return jsonString;
    }

    String toJsonStringWithInputOutput() {
        String jsonString;
        try {
            SimpleBeanPropertyFilter emptyFilter = SimpleBeanPropertyFilter.serializeAllExcept();
            FilterProvider provider =
                    new SimpleFilterProvider().addFilter("SecretRemovalFilter", emptyFilter);

            jsonString = objectMapper.writer(provider).writeValueAsString(this);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to convert Task: {} to String. Exception: {}", this, e);
            throw new RuntimeException(e);
        }
        return jsonString;
    }
}