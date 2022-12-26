# Persistence
Persistence modules allow you to run Conductor with different backends for storing metadata and workflow executions.
Conductor core has support for Redis and Cassandra, while this module allows additional MySQL and Postgres as options.

**Note**: Both MySQL and Postgres persistence also implements queue implementations for Conductor.  So these modules 
support the following:

1. Metadata store
2. Execution Store
3. Queue DAO
4. Concurrency Limits

## Published Artifacts

Group: `com.netflix.conductor`

| Published Artifact | Description |
| ----------- | ----------- | 
| conductor-mysql-persistence | MySQL based Persistence, Concurrency Limiter and Queues  |
| conductor-postgres-persistence | Postgres based Persistence, Concurrency Limiter and Queues  |

## Modules
### MySQL

#### Configuration

```properties
conductor.db.type=mysql

#Cache expiry for teh task definitions in seconds
conductor.mysql.taskDefCacheRefreshInterval=60

#Use spring datasource properties to configure MySQL connection
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=
spring.datasource.hikari.maximum-pool-size=
spring.datasource.hikari.auto-commit=
```

### Postgres

#### Configuration
(Default values shown below)

```properties
conductor.db.type=postgres
#Cache expiry for teh task definitions in seconds
conductor.mysql.taskDefCacheRefreshInterval=60

#Use spring datasource properties to configure Postgres connection
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=
spring.datasource.hikari.maximum-pool-size=
spring.datasource.hikari.auto-commit=
```
