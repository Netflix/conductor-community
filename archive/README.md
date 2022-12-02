# Workflow archival module
This modules uses Postgres as an archival and indexing system

See [schema folder](src/main/resources/db/migration_archive_postgres/) for the details on the schema

## Behavior
When enabled, all the completed workflows (COMPLETED, TERMINATED, FAILED) are moved to the postgres archival and removed from primary datasource

*Please do not use elasticsearch for indexing when using this - it will be redundant*

## Configuration properties
### Enable archival
```properties
conductor.archive.db.enabled=true
conductor.archive.db.type=postgres
conductor.archive.db.indexer.threadCount=1
conductor.archive.db.indexer.pollingInterval=1
```

### Database configuration
Below is the example for connecting to the local postgres
```properties
spring.datasource.url=jdbc:postgresql://localhost/<db_name>
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.hikari.maximum-pool-size=8
spring.datasource.hikari.auto-commit=false
```

### Disable elasticsearch indexing
```properties
conductor.app.asyncIndexingEnabled=false
conductor.indexing.enabled=false
```
