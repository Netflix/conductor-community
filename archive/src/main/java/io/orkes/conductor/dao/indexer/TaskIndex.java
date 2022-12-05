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
import java.util.Map;

import com.netflix.conductor.model.TaskModel;

public class TaskIndex {

    private TaskModel task;

    private int maxValueLen;

    public TaskIndex(TaskModel task, int maxValueLen) {
        this.task = task;
        this.maxValueLen = maxValueLen;
    }

    public String toIndexString() {
        StringBuilder sb = new StringBuilder();
        append(sb, task.getTaskType());
        append(sb, task.getTaskId());
        append(sb, task.getDomain());
        append(sb, task.getReferenceTaskName());
        append(sb, task.getStatus().toString());
        append(sb, task.getExternalInputPayloadStoragePath());
        append(sb, task.getExternalOutputPayloadStoragePath());
        append(sb, task.getInputData());
        append(sb, task.getOutputData());
        append(sb, task.getReasonForIncompletion());
        append(sb, task.getWorkerId());

        return sb.toString();
    }

    @Override
    public String toString() {
        return toIndexString();
    }

    private String toString(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        append(sb, map);
        return sb.toString();
    }

    private void append(StringBuilder sb, Object value) {
        if (value instanceof String || value instanceof Number) {
            sb.append(" ");
            sb.append(value.toString());
        } else if (value instanceof Map) {
            sb.append(" ");
            append(sb, (Map) value);
        } else if (value instanceof List) {
            List values = (List) value;
            for (Object valueObj : values) {
                sb.append(" ");
                append(sb, valueObj);
            }
        }
    }

    private void append(StringBuilder sb, Map<String, Object> map) {

        map.entrySet()
                .forEach(
                        entry -> {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            sb.append(" ");
                            sb.append(key);
                            if (value instanceof String) {
                                sb.append(" ");
                                String valueString = value.toString();
                                sb.append(
                                        valueString.substring(
                                                0, Math.min(valueString.length(), maxValueLen)));
                            } else if (value instanceof Number) {
                                sb.append(" ");
                                sb.append(value.toString());
                            } else if (value instanceof Map) {
                                sb.append(" ");
                                append(sb, (Map) value);
                            } else if (value instanceof List) {
                                List values = (List) value;
                                for (Object valueObj : values) {
                                    sb.append(" ");
                                    append(sb, valueObj);
                                }
                            }
                        });
    }
}
