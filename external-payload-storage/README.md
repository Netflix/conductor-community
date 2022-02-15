# External Payload Storage
Store and retrieve workflows/tasks input/output payload that
goes over the thresholds defined in properties named `conductor.[workflow|task].[input|output].payload.threshold.kb`.
Cf. Documentation [External Payload Storage](https://netflix.github.io/conductor/externalpayloadstorage/)

## Published Artifacts

Group: `com.netflix.conductor`

| Published Artifact | Description |
| ----------- | ----------- | 
| conductor-azureblob-storage | Support for Azure blob |
| conductor-postgres-external-storage | Using Postgres to store large blobs |

## Modules
### Azureblob
https://azure.microsoft.com/en-us/services/storage/blobs/

Allows storing large workflow payloads to Azure

#### Configuration
(Default values shown below)
```properties
conductor.external-payload-storage.type=azureblob
conductor.external-payload-storage.azureblob.connectionString
conductor.external-payload-storage.azureblob.containerName=conductor-payloads
conductor.external-payload-storage.azureblob.endpoint
conductor.external-payload-storage.azureblob.sasToken
conductor.external-payload-storage.azureblob.signedUrlExpirationDuration=5

#paths where the inputs and outputs of workflow and tasks are stored on the server
conductor.external-payload-storage.azureblob.workflowInputPath=workflow/input/
conductor.external-payload-storage.azureblob.workflowOutputPath=workflow/output/
conductor.external-payload-storage.azureblob.taskInputPath=task/input/
conductor.external-payload-storage.azureblob.taskOutputPath=task/output/
```

### Postgres External Storage
Leverages Postgres dB for storing large blobs

(Default values shown below)
```properties
conductor.external-payload-storage.type=postgres
conductor.external-payload-storage.postgres.username=
conductor.external-payload-storage.postgres.password=
conductor.external-payload-storage.postgres.url=

# Maximum count of days of data age in PostgreSQL database. After overcoming limit, the oldest data will be deleted.
conductor.external-payload-storage.postgres.maxDataDays=0

# Maximum count of months of data age in PostgreSQL database. After overcoming limit, the oldest data will be deleted.
conductor.external-payload-storage.postgres.maxDataMonths=0

# Maximum count of years of data age in PostgreSQL database. After overcoming limit, the oldest data will be deleted.
conductor.external-payload-storage.postgres.maxDataYears=1

#URL, that can be used to pull the json configurations, that will be downloaded from PostgreSQL to the conductor server. For example: for local development it is "http://localhost:8080"
conductor.external-payload-storage.postgres.conductorUrl
```
