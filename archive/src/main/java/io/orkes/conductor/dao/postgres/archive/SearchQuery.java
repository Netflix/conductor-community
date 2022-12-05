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
package io.orkes.conductor.dao.postgres.archive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SearchQuery {

    private List<String> workflowNames;

    private List<String> taskTypes;

    private List<String> workflowIds;

    private List<String> correlationIds;

    private List<String> taskIds;

    private List<String> statuses;

    private long fromStartTime;

    private long toStartTime;

    private SearchQuery() {}

    public static SearchQuery parse(String query) {
        SearchQuery searchQuery = new SearchQuery();

        String[] values = query.split(" AND ");
        for (String value : values) {
            value = value.trim();
            if (value.startsWith("workflowId")) {
                searchQuery.workflowIds = getValues(value);
            }
            if (value.startsWith("correlationId")) {
                searchQuery.correlationIds = getValues(value);
            } else if (value.startsWith("taskId")) {
                searchQuery.taskIds = getValues(value);
            } else if (value.startsWith("workflowType")) {
                searchQuery.workflowNames = getValues(value);
            } else if (value.startsWith("taskType")) {
                searchQuery.taskTypes = getValues(value);
            } else if (value.startsWith("status")) {
                searchQuery.statuses = getValues(value);
            } else if (value.startsWith("startTime")) {

                if (value.contains(">")) {

                    String[] kv = value.split(">");
                    if (kv.length > 0) {
                        try {
                            searchQuery.fromStartTime = Long.parseLong(kv[1].trim());
                        } catch (Exception e) {
                        }
                    }

                } else if (value.contains("<")) {

                    String[] kv = value.split("<");
                    if (kv.length > 0) {
                        try {
                            searchQuery.toStartTime = Long.parseLong(kv[1].trim());
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
        return searchQuery;
    }

    private static List<String> getValues(String keyValue) {
        List<String> values = new ArrayList<>();
        if (keyValue.contains("=")) {
            String[] kv = keyValue.split("=");
            if (kv.length > 0) {
                String value = kv[1].trim();
                // remove quotes from the start and end
                value = value.substring(1, value.length() - 1);
                return Arrays.asList(value.trim());
            }
        } else if (keyValue.contains(" IN ")) {

            String[] kv = keyValue.split(" IN ");
            if (kv.length > 0) {
                String[] inValues = kv[1].trim().substring(1, kv[1].length() - 1).split(",");
                for (String inValue : inValues) {
                    values.add(inValue.trim());
                }
            }
        }
        return values;
    }

    public List<String> getWorkflowNames() {
        return workflowNames;
    }

    public List<String> getWorkflowIds() {
        return workflowIds;
    }

    public List<String> getCorrelationIds() {
        return correlationIds;
    }

    public List<String> getStatuses() {
        return statuses;
    }

    public long getFromStartTime() {
        return fromStartTime;
    }

    public long getToStartTime() {
        if (toStartTime == 0) {
            toStartTime = System.currentTimeMillis();
        }
        return toStartTime;
    }

    public List<String> getTaskTypes() {
        return taskTypes;
    }

    public List<String> getTaskIds() {
        return taskIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchQuery that = (SearchQuery) o;
        return fromStartTime == that.fromStartTime
                && toStartTime == that.toStartTime
                && Objects.equals(workflowNames, that.workflowNames)
                && Objects.equals(workflowIds, that.workflowIds)
                && Objects.equals(statuses, that.statuses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workflowNames, workflowIds, statuses, fromStartTime, toStartTime);
    }
}
