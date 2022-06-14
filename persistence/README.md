# Persistence
Persistence modules allows you run Conductor with different backends for storing metadata and workflow executions.
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

#use spring datasource properties to configure MySQL connection
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

#use spring datasource properties to configure MySQL connection
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=
spring.datasource.hikari.maximum-pool-size=
spring.datasource.hikari.auto-commit=
```

### Oracle 

#### Configuration

``` properties
conductor.db.type=oracle

# ENSURE TO INCLUDE THIS PROPERTY FOR ORACLE DATABASE 
	# - If NOT set, then Invalid SQL Format exceptions like "missing braces" or invalid SQL format" etc. would arise as the SQLs from MYSQL db//migration will be picked up and have a slightly different format to Oracle SQLs
## There is currently so easier way to set this automatically on Oracle Persistence Configuration.
### Reason being, JUnits / Tests depend on Flyway 4.0.3, where as the actual Conductor runtime uses Flyway 6+ and overrides Oracle Persistence dependency.
#### The root cause of using Flyway for Test of Oracle Persistence is as GitHub Actions doesn't support Oracle version => 12
#### This will be updated upon GitHub Actions update and will no longer have two separate versions for Test and Runtime.
spring.flyway.locations=classpath:db/migration_oracle

# While using base docker image - conductorboot/oracle:19.3.0-ee-slim
#spring.datasource.url=jdbc:oracle:thin:@//oracle:1521/CONDUCTOR
#spring.datasource.username=conductor
#spring.datasource.password=conductor

# While using pre-configured test image - conductorboot/oracle:19.3.0-ee-slim-test
spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/JUNIT
spring.datasource.username=junit_user
spring.datasource.password=junit_user

## Used for JUnits -- Document purpose only - of no practical runtime use.
#spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/XE
#spring.datasource.username=junit_user
#spring.datasource.password=junit_user

# Hikari pool sizes are -1 by default and prevent startup
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2

```
