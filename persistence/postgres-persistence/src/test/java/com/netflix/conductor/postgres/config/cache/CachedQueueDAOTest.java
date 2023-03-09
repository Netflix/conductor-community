/*
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

import static org.mockito.Mockito.mock;

import com.github.benmanes.caffeine.cache.Cache;
import com.netflix.conductor.dao.MetadataDAO;
import com.netflix.conductor.dao.QueueDAO;
import com.netflix.conductor.postgres.config.PostgresProperties;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@ContextConfiguration(
        classes = {
                CachedQueueDAOTest.TestConfiguration.class,
                PostgresCachingConfiguration.class,
        })
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "conductor.db.type=postgres")
public class CachedQueueDAOTest {

    @Autowired
    @Qualifier(CacheableQueueDAO.QUEUE_CACHE_MANAGER)
    private CacheManager cacheManager;
    @Autowired
    private QueueDAO cachingQueueDAO;

    @Configuration
    @EnableCaching
    public static class TestConfiguration {

        @Bean
        public QueueDAO getCachingQueueDAO() {
            AtomicLong counter = new AtomicLong();
            QueueDAO queueDAO = mock(QueueDAO.class);
            Mockito.doAnswer(invocation -> Map.of("queue", counter.incrementAndGet())).when(queueDAO).queuesDetail();
            return new CacheableQueueDAO(queueDAO);
        }

        @Bean
        PostgresProperties postgresProperties() {
            final PostgresProperties postgresProperties = new PostgresProperties();
            postgresProperties.setQueueDetailsCacheRefreshInterval(Duration.of(10, ChronoUnit.MINUTES));
            return postgresProperties;
        }

        @Bean
        public MetadataDAO getMetadataDAO() {
            return mock(MetadataDAO.class);
        }
    }

    @Test
    public void testCachingQueueDAO() {
        cachingQueueDAO.queuesDetail();
        Assert.assertEquals(1, getCacheSize());
        Assert.assertEquals(1, getCounterFromCache());
        cachingQueueDAO.queuesDetail();
        Assert.assertEquals(1, getCacheSize());
        Assert.assertEquals(1, getCounterFromCache());

        invalidateCache();
        Assert.assertEquals(0, getCacheSize());
        cachingQueueDAO.queuesDetail();
        Assert.assertEquals(1, getCacheSize());
        Assert.assertEquals(2, getCounterFromCache());
    }

    private void invalidateCache() {
        cacheManager.getCache(CacheableQueueDAO.QUEUE_DETAIL_CACHE_NAME).invalidate();
    }

    private long getCounterFromCache() {
        return (long) cacheManager
                .getCache(CacheableQueueDAO.QUEUE_DETAIL_CACHE_NAME)
                .get(new SimpleKey(), Map.class)
                .values().iterator().next();
    }

    private long getCacheSize() {
        return ((Cache<?, ?>) cacheManager
                .getCache(CacheableQueueDAO.QUEUE_DETAIL_CACHE_NAME)
                .getNativeCache()).asMap().size();
    }
}
