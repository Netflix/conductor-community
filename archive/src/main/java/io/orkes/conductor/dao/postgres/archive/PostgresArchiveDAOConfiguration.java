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

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.logging.log4j.util.Strings;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import com.netflix.conductor.dao.ExecutionDAO;
import com.netflix.conductor.dao.QueueDAO;
import com.netflix.conductor.postgres.config.PostgresProperties;

import io.orkes.conductor.dao.archive.ArchiveDAO;
import io.orkes.conductor.dao.archive.ArchivedExecutionDAO;
import io.orkes.conductor.metrics.MetricsCollector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({PostgresProperties.class})
@Import(DataSourceAutoConfiguration.class)
@ConditionalOnProperty(name = "conductor.archive.db.type", havingValue = "postgres")
public class PostgresArchiveDAOConfiguration {

    private final DataSource dataSource;

    private final MetricsCollector metricsCollector;

    private final ExecutionDAO primaryExecutionDAO;

    private final QueueDAO dynoQueueDAO;

    private final Environment environment;

    private final ObjectMapper objectMapper;

    public PostgresArchiveDAOConfiguration(
            ObjectMapper objectMapper,
            Environment environment,
            DataSource dataSource,
            ExecutionDAO primaryExecutionDAO,
            QueueDAO dynoQueueDAO,
            MetricsCollector metricsCollector) {

        this.objectMapper = objectMapper;
        this.environment = environment;
        this.dataSource = dataSource;
        this.primaryExecutionDAO = primaryExecutionDAO;
        this.dynoQueueDAO = dynoQueueDAO;
        this.metricsCollector = metricsCollector;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "conductor.archive.db.enabled", havingValue = "true")
    public ExecutionDAO getExecutionDAO(ArchiveDAO archiveDAO) {
        return new ArchivedExecutionDAO(
                primaryExecutionDAO, archiveDAO, dynoQueueDAO, metricsCollector);
    }

    @Bean
    @Qualifier("primaryExecutionDAO")
    @ConditionalOnProperty(name = "conductor.archive.db.enabled", havingValue = "true")
    public ExecutionDAO getPrimaryExecutionDAO() {
        return primaryExecutionDAO;
    }

    @Bean(initMethod = "migrate", name = "flyway")
    @PostConstruct
    public Flyway flywayForPrimaryDb() {
        return Flyway.configure()
                .locations(
                        "classpath:db/migration_postgres",
                        "classpath:db/migration_archive_postgres")
                .schemas("public")
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .mixed(true)
                .load();
    }

    @Bean(name = "flywayInitializer")
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
        // override the default location.
        return configuration ->
                configuration.locations(
                        "classpath:db/migration_postgres",
                        "classpath:db/migration_archive_postgres");
    }

    @Bean
    @Qualifier("searchDatasource")
    public DataSource searchDatasource(DataSource defaultDatasource) {
        String url = environment.getProperty("spring.search-datasource.url");
        String user = environment.getProperty("spring.search-datasource.username");
        String password = environment.getProperty("spring.search-datasource.password");
        String maxPoolSizeString =
                environment.getProperty("spring.search-datasource.hikari.maximum-pool-size");

        if (Strings.isEmpty(url)) {
            return defaultDatasource;
        }
        log.info("Configuring searchDatasource with {}", url);

        int maxPoolSize = 10;
        if (Strings.isNotEmpty(maxPoolSizeString)) {
            try {
                maxPoolSize = Integer.parseInt(maxPoolSizeString);
            } catch (Exception e) {
            }
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setAutoCommit(true);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(maxPoolSize);

        HikariDataSource hikariDataSource = new HikariDataSource(config);
        return hikariDataSource;
    }

    @Bean
    @DependsOn({"flyway", "flywayInitializer"})
    @ConditionalOnProperty(value = "conductor.archive.db.type", havingValue = "postgres")
    public PostgresArchiveDAO getPostgresArchiveDAO(
            @Qualifier("searchDatasource") DataSource searchDatasource) {
        return new PostgresArchiveDAO(objectMapper, dataSource, searchDatasource);
    }
}
