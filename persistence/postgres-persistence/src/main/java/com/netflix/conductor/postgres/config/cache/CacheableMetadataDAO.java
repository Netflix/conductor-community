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
package com.netflix.conductor.postgres.config.cache;

import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.dao.MetadataDAO;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

public class CacheableMetadataDAO implements MetadataDAO {
    public static final String TASK_DEF_CACHE_NAME = "task-def";
    public static final String TASKDEF_CACHE_MANAGER = "taskdefCacheManager";
    private final MetadataDAO delegate;

    public CacheableMetadataDAO(MetadataDAO delegate) {
        this.delegate = delegate;
    }

    @Override
    @CachePut(value = TASK_DEF_CACHE_NAME, cacheManager = TASKDEF_CACHE_MANAGER, key="{#taskDef.getName}")
    public TaskDef createTaskDef(TaskDef taskDef) {
        return delegate.createTaskDef(taskDef);
    }

    @Override
    @CachePut(value = TASK_DEF_CACHE_NAME, cacheManager = TASKDEF_CACHE_MANAGER, key="{#taskDef.getName}")
    public TaskDef updateTaskDef(TaskDef taskDef) {
        return delegate.updateTaskDef(taskDef);
    }

    @Override
    @Cacheable(value = TASK_DEF_CACHE_NAME, cacheManager = TASKDEF_CACHE_MANAGER, key="{#name}")
    public TaskDef getTaskDef(String name) {
        return delegate.getTaskDef(name) ;
    }

    @Override
    @CacheEvict(value = TASK_DEF_CACHE_NAME, cacheManager = TASKDEF_CACHE_MANAGER, key="{#name}")
    public void removeTaskDef(String name) {
        delegate.removeTaskDef(name);
    }

    @Override
    public List<TaskDef> getAllTaskDefs() {
        return delegate.getAllTaskDefs();
    }

    @Override
    public void createWorkflowDef(WorkflowDef def) {
        delegate.createWorkflowDef(def);
    }

    @Override
    public void updateWorkflowDef(WorkflowDef def) {
        delegate.updateWorkflowDef(def);
    }

    @Override
    public Optional<WorkflowDef> getLatestWorkflowDef(String name) {
        return delegate.getLatestWorkflowDef(name);
    }

    @Override
    public Optional<WorkflowDef> getWorkflowDef(String name, int version) {
        return delegate.getWorkflowDef(name, version);
    }

    @Override
    public void removeWorkflowDef(String name, Integer version) {
        delegate.removeWorkflowDef(name, version);
    }

    @Override
    public List<WorkflowDef> getAllWorkflowDefs() {
        return delegate.getAllWorkflowDefs();
    }
}