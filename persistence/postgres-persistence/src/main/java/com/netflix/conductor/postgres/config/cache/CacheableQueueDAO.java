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

import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.dao.QueueDAO;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;

public class CacheableQueueDAO implements QueueDAO {

    public static final String QUEUE_DETAIL_CACHE_NAME = "queue-detail";
    public static final String QUEUE_DETAIL_VERBOSE_CACHE_NAME = "queue-detail-verbose";
    public static final String QUEUE_CACHE_MANAGER = "queueCacheManager";
    private final QueueDAO delegate;

    public CacheableQueueDAO(QueueDAO delegate) {
        this.delegate = delegate;
    }

    @Override
    public void push(String queueName, String id, long offsetTimeInSecond) {
        delegate.push(queueName, id, offsetTimeInSecond);
    }

    @Override
    public void push(String queueName, String id, int priority, long offsetTimeInSecond) {
        delegate.push(queueName, id, priority, offsetTimeInSecond);
    }

    @Override
    public void push(String queueName, List<Message> messages) {
        delegate.push(queueName, messages);
    }

    @Override
    public boolean pushIfNotExists(String queueName, String id, long offsetTimeInSecond) {
        return delegate.pushIfNotExists(queueName, id, offsetTimeInSecond);
    }

    @Override
    public boolean pushIfNotExists(String queueName, String id, int priority, long offsetTimeInSecond) {
        return delegate.pushIfNotExists(queueName, id, priority, offsetTimeInSecond);
    }

    @Override
    public List<String> pop(String queueName, int count, int timeout) {
        return delegate.pop(queueName, count, timeout);
    }

    @Override
    public List<Message> pollMessages(String queueName, int count, int timeout) {
        return delegate.pollMessages(queueName, count, timeout);
    }

    @Override
    public void remove(String queueName, String messageId) {
        delegate.remove(queueName, messageId);
    }

    @Override
    public int getSize(String queueName) {
        return delegate.getSize(queueName);
    }

    @Override
    public boolean ack(String queueName, String messageId) {
        return delegate.ack(queueName, messageId);
    }

    @Override
    public boolean setUnackTimeout(String queueName, String messageId, long unackTimeout) {
        return delegate.setUnackTimeout(queueName, messageId, unackTimeout);
    }

    @Override
    public void flush(String queueName) {
        delegate.flush(queueName);
    }

    @Override
    @Cacheable(value = QUEUE_DETAIL_CACHE_NAME, cacheManager = QUEUE_CACHE_MANAGER)
    public Map<String, Long> queuesDetail() {
        return delegate.queuesDetail();
    }

    @Override
    @Cacheable(value = QUEUE_DETAIL_VERBOSE_CACHE_NAME, cacheManager = QUEUE_CACHE_MANAGER)
    public Map<String, Map<String, Map<String, Long>>> queuesDetailVerbose() {
        return delegate.queuesDetailVerbose();
    }

    @Override
    public void processUnacks(String queueName) {
        delegate.processUnacks(queueName);
    }

    @Override
    public boolean resetOffsetTime(String queueName, String id) {
        return delegate.resetOffsetTime(queueName, id);
    }

    @Override
    public boolean postpone(String queueName, String messageId, int priority, long postponeDurationInSeconds) {
        return delegate.postpone(queueName, messageId, priority, postponeDurationInSeconds);
    }

    @Override
    public boolean containsMessage(String queueName, String messageId) {
        return delegate.containsMessage(queueName, messageId);
    }
}