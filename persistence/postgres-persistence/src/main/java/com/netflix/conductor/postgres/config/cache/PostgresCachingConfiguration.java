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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.netflix.conductor.dao.MetadataDAO;
import com.netflix.conductor.dao.QueueDAO;
import com.netflix.conductor.postgres.config.PostgresProperties;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableCaching
@ConditionalOnExpression(
        "'${conductor.db.type}' == 'postgres' and ${conductor.postgres.caching_enabled:true}")
public class PostgresCachingConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresCachingConfiguration.class);

    public static final int MAXIMUM_SIZE = 1000;
    @Autowired
    PostgresProperties properties;

    @PostConstruct
    public void postConstruct() {
        LOGGER.debug("Using caching for postgres persistence");
    }

    /**
     * Prepare cache builder with default settings for caching within QueueDAO.
     *
     * @return caffeine cache builder.
     */
    @Bean
    public Caffeine<Object, Object> queueCachingConfig() {
        return Caffeine.newBuilder()
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(
                        properties.getQueueDetailsCacheRefreshInterval().toMillis(),
                        TimeUnit.MILLISECONDS);
    }

    /**
     * Build cache manager used directly within QueueDAO. Only specific cache names are allowed.
     *
     * @param queueCachingConfig cache builder to use internally.
     * @return caffeine cache manager.
     */
    @Bean(name = CacheableQueueDAO.QUEUE_CACHE_MANAGER)
    public CacheManager queueCacheManager(Caffeine<Object, Object> queueCachingConfig) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(queueCachingConfig);
        caffeineCacheManager.setCacheNames(
                List.of(
                        CacheableQueueDAO.QUEUE_DETAIL_CACHE_NAME,
                        CacheableQueueDAO.QUEUE_DETAIL_VERBOSE_CACHE_NAME));
        return caffeineCacheManager;
    }

    /**
     * Prepare cache builder with default settings for caching within MetadataDAO.
     *
     * @return caffeine cache builder.
     */
    @Bean
    public Caffeine<Object, Object> taskdefCachingConfig() {
        return Caffeine.newBuilder()
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(
                        properties.getTaskDefCacheRefreshInterval().toSeconds(), TimeUnit.SECONDS);
    }

    /**
     * Build cache manager used directly within MetadataDAO. Only specific cache names are allowed.
     *
     * @param taskdefCachingConfig cache builder to use internally.
     * @return caffeine cache manager.
     */
    @Bean(name = CacheableMetadataDAO.TASKDEF_CACHE_MANAGER)
    @Primary
    public CacheManager taskdefCacheManager(Caffeine<Object, Object> taskdefCachingConfig) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(taskdefCachingConfig);
        caffeineCacheManager.setCacheNames(List.of(CacheableMetadataDAO.TASK_DEF_CACHE_NAME));
        return caffeineCacheManager;
    }

    @Bean
    @Primary
    public CacheableMetadataDAO cacheablePostgresMetadataDAO(MetadataDAO postgresMetadataDAO) {
        return new CacheableMetadataDAO(postgresMetadataDAO);
    }

    @Bean
    @Primary
    public CacheableQueueDAO cacheablePostgresQueueDAO(QueueDAO postgresQueueDAO) {
        return new CacheableQueueDAO(postgresQueueDAO);
    }
}
