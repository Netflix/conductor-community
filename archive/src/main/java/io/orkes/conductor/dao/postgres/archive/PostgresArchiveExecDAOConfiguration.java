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

import com.netflix.conductor.dao.ExecutionDAO;
import com.netflix.conductor.dao.QueueDAO;
import com.netflix.conductor.postgres.config.PostgresProperties;
import io.orkes.conductor.dao.archive.ArchiveDAO;
import io.orkes.conductor.dao.archive.ArchivedExecutionDAO;
import io.orkes.conductor.metrics.MetricsCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({PostgresProperties.class})
@ConditionalOnProperty(name = "conductor.archive.db.type", havingValue = "postgres")
public class PostgresArchiveExecDAOConfiguration {

    private final MetricsCollector metricsCollector;

    private final ExecutionDAO primaryExecutionDAO;

    private final QueueDAO queueDAO;

    public PostgresArchiveExecDAOConfiguration(
            ExecutionDAO primaryExecutionDAO,
            QueueDAO queueDAO,
            MetricsCollector metricsCollector) {
        this.primaryExecutionDAO = primaryExecutionDAO;
        this.queueDAO = queueDAO;
        this.metricsCollector = metricsCollector;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "conductor.archive.db.enabled", havingValue = "true")
    public ExecutionDAO getExecutionDAO(ArchiveDAO archiveDAO) {
        return new ArchivedExecutionDAO(
                primaryExecutionDAO, archiveDAO, queueDAO, metricsCollector);
    }
}
