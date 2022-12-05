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
package io.orkes.conductor.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;
import com.netflix.spectator.api.CompositeRegistry;
import com.netflix.spectator.api.Spectator;
import com.netflix.spectator.micrometer.MicrometerRegistry;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusRenameFilter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MetricsCollector {

    private static final CompositeRegistry spectatorRegistry = Spectator.globalRegistry();

    private final MeterRegistry meterRegistry;

    private static final Map<String, Map<List<Tag>, Counter>> counters = new ConcurrentHashMap<>();

    private static final Map<String, Map<List<Tag>, Timer>> timers = new ConcurrentHashMap<>();

    private static final Map<String, Map<List<Tag>, Gauge>> gauges = new ConcurrentHashMap<>();

    private static double[] percentiles = new double[] {0.5, 0.75, 0.90, 0.95, 0.99};

    private final boolean skipLabels;

    public MetricsCollector(MeterRegistry meterRegistry, Environment env) {

        Boolean skipLabels = env.getProperty("conductor.metrics.skipLabels", Boolean.class);
        if (skipLabels == null) {
            this.skipLabels = false;
        } else {
            this.skipLabels = skipLabels;
        }

        this.meterRegistry = meterRegistry;
        final MicrometerRegistry metricsRegistry = new MicrometerRegistry(this.meterRegistry);

        this.meterRegistry
                .config()
                .meterFilter(new PrometheusRenameFilter())
                .meterFilter(MeterFilter.denyNameStartsWith("task_queue_wait"))
                .meterFilter(MeterFilter.denyNameStartsWith("dao_payload_size"))
                .meterFilter(MeterFilter.denyNameStartsWith("dao_requests"))
                .meterFilter(MeterFilter.denyNameStartsWith("workflow_execution"))
                .meterFilter(MeterFilter.denyNameStartsWith("tasks_in_workflow"))
                .meterFilter(MeterFilter.denyNameStartsWith("task_pending_time"))
                .meterFilter(MeterFilter.denyNameStartsWith("workflow_start_success"))
                .meterFilter(MeterFilter.denyNameStartsWith("task_execution"));

        if (this.skipLabels) {
            this.meterRegistry
                    .config()
                    .meterFilter(MeterFilter.denyNameStartsWith("workflow_failure"))
                    .meterFilter(MeterFilter.denyNameStartsWith("workflow_running"));
        }

        spectatorRegistry.add(metricsRegistry);

        log.info("skipLabels: {}", this.skipLabels);
    }

    public Timer getTimer(String name, String... additionalTags) {
        List<Tag> tags = tags(additionalTags);
        return timers.computeIfAbsent(name, s -> new ConcurrentHashMap<>())
                .computeIfAbsent(
                        tags,
                        t -> {
                            Timer.Builder timerBuilder =
                                    Timer.builder(name)
                                            .description(name)
                                            .publishPercentiles(percentiles);
                            if (!this.skipLabels) {
                                timerBuilder = timerBuilder.tags(tags);
                            }
                            return timerBuilder.register(meterRegistry);
                        });
    }

    public Counter getCounter(String name, String... additionalTags) {
        List<Tag> tags = tags(additionalTags);
        return counters.computeIfAbsent(name, s -> new ConcurrentHashMap<>())
                .computeIfAbsent(
                        tags,
                        t -> {
                            Counter.Builder builder = Counter.builder(name).description(name);
                            if (!this.skipLabels) {
                                builder = builder.tags(tags);
                            }
                            return builder.register(meterRegistry);
                        });
    }

    public void recordGauge(String name, Number value, String... additionalTags) {
        List<Tag> tags = tags(additionalTags);
        gauges.computeIfAbsent(name, s -> new ConcurrentHashMap<>())
                .computeIfAbsent(
                        tags,
                        t -> {
                            Gauge.Builder<Supplier<Number>> builder =
                                    Gauge.builder(name, () -> value);

                            if (!this.skipLabels) {
                                builder = builder.tags(tags);
                            }
                            return builder.register(meterRegistry);
                        });
    }

    private void gauge(String name, Number value, String... additionalTags) {
        Gauge.builder(name, () -> value).register(meterRegistry);
    }

    public void recordWorkflowComplete(WorkflowModel workflow) {
        String workflowName = workflow.getWorkflowName();
        if (skipLabels) {
            workflowName = "None";
        }
        long duration = workflow.getEndTime() - workflow.getCreateTime();
        getTimer(
                        "workflow_completed",
                        "workflowName",
                        workflowName,
                        "status",
                        workflow.getStatus().toString())
                .record(duration, TimeUnit.MILLISECONDS);
    }

    public void recordTaskComplete(TaskModel task) {
        String taskType = task.getTaskDefName();
        if (skipLabels) {
            taskType = "None";
        }
        getTimer("task_completed", "taskType", taskType, "status", task.getStatus().toString())
                .record((task.getEndTime() - task.getStartTime()), TimeUnit.MILLISECONDS);
    }

    private static List<Tag> tags(String... additionalTags) {
        List<Tag> tags = new ArrayList<>();

        for (int j = 0; j < additionalTags.length - 1; j++) {
            String tk = additionalTags[j];
            String tv = "" + additionalTags[j + 1];
            if (!tv.isEmpty()) {
                tags.add(Tag.of(tk, tv));
            }
            j++;
        }
        return tags;
    }
}
