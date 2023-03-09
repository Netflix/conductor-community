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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.github.benmanes.caffeine.cache.Cache;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.dao.MetadataDAO;
import com.netflix.conductor.dao.QueueDAO;
import com.netflix.conductor.postgres.config.PostgresProperties;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@ContextConfiguration(
        classes = {
                CachedMetadataDAOTest.TestConfiguration.class,
                PostgresCachingConfiguration.class,
        })
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "conductor.db.type=postgres")
public class CachedMetadataDAOTest {

    private static final TaskDef TASK_DEF_1 = new TaskDef("t1");
    private static final TaskDef TASK_DEF_1_ALT = new TaskDef("t1");
    private static final TaskDef TASK_DEF_2 = new TaskDef("t2");
    @Autowired
    @Qualifier(CacheableMetadataDAO.TASKDEF_CACHE_MANAGER)
    private CacheManager cacheManager;
    @Autowired
    private MetadataDAO cachingMetaDAO;

    @Configuration
    @EnableCaching
    public static class TestConfiguration {

        @Bean
        public QueueDAO getQueueDAO() {
            return mock(QueueDAO.class);
        }

        @Bean
        public MetadataDAO getMetadataDAO() {
            MetadataDAO metaDao = mock(MetadataDAO.class);
            doAnswer(invocation -> invocation.getArguments()[0]).when(metaDao).createTaskDef(any(TaskDef.class));
            doAnswer(invocation -> invocation.getArguments()[0]).when(metaDao).updateTaskDef(any(TaskDef.class));
            doAnswer(invocation -> {
                switch (invocation.getArguments()[0].toString()) {
                    case "t1":
                        return TASK_DEF_1;
                    case "t2":
                        return TASK_DEF_2;
                    default:
                        throw new IllegalArgumentException("Unknown taskdef");
                }
            }).when(metaDao).getTaskDef(any(String.class));
            return metaDao;
        }

        @Bean
        PostgresProperties postgresProperties() {
            final PostgresProperties postgresProperties = new PostgresProperties();
            postgresProperties.setTaskDefCacheRefreshInterval(Duration.of(10, ChronoUnit.MINUTES));
            return postgresProperties;
        }
    }

    @Test
    public void testCachingMetadataDAO() {
        cachingMetaDAO.createTaskDef(TASK_DEF_1);
        Assert.assertSame(TASK_DEF_1, cachingMetaDAO.getTaskDef(TASK_DEF_1.getName()));
        Assert.assertEquals(1, getCacheSize());

        cachingMetaDAO.createTaskDef(TASK_DEF_1);
        Assert.assertSame(TASK_DEF_1, cachingMetaDAO.getTaskDef(TASK_DEF_1.getName()));
        Assert.assertEquals(1, getCacheSize());

        cachingMetaDAO.createTaskDef(TASK_DEF_2);
        Assert.assertSame(TASK_DEF_2, cachingMetaDAO.getTaskDef(TASK_DEF_2.getName()));
        Assert.assertEquals(2, getCacheSize());

        cachingMetaDAO.removeTaskDef(TASK_DEF_2.getName());
        Assert.assertEquals(1, getCacheSize());

        cachingMetaDAO.updateTaskDef(TASK_DEF_1_ALT);
        Assert.assertEquals(1, getCacheSize());
        Assert.assertSame(TASK_DEF_1_ALT, cachingMetaDAO.getTaskDef(TASK_DEF_1_ALT.getName()));
    }

    private void invalidateCache() {
        cacheManager.getCache(CacheableMetadataDAO.TASK_DEF_CACHE_NAME).invalidate();
    }

    private long getCacheSize() {
        return ((Cache<?, ?>) cacheManager
                .getCache(CacheableMetadataDAO.TASK_DEF_CACHE_NAME)
                .getNativeCache()).asMap().size();
    }
}
