# Netflix Conductor community modules

This repository hosts all the community contributed modules and extensions for 
[Netflix Conductor](https://github.com/Netflix/conductor)

![Netflix Conductor Logo](https://github.com/Netflix/conductor/blob/main/docs/docs/img/conductor-vector-x.png)

## What is Conductor?
Conductor is a workflow orchestration engine that runs in the cloud.
You can find more details about Conductor at the main repository of [Netflix Conductor](https://github.com/Netflix/conductor)

### What is _this_ repository?
Conductor is an extensible platform that allows users to bring in their own persistence, queues, integrations eventing systems such as SQS, NATS, AMQP etc.

The core conductor project contains implementations tested and supported by Netflix, while this repository will contain all
the modules contributed by community.

### Who is responsible for maintaining this repository?
Netflix in collaboration with the team at Orkes (https://orkes.io/) will continue to maintain this repository.

## Repository Structure and Published Artifcats
Binaries are available from [Netflix OSS Maven](https://artifacts.netflix.net/netflixoss/com/netflix/conductor/) repository, or the [Maven Central Repository](https://search.maven.org/search?q=g:com.netflix.conductor).
Binaries are published under the group: **com.netflix.conductor**

| Parent Folder | Published Artifact | Description |
| ----------- | ----------- | --------------- |
| | conductor-contribs | Optional contrib package that holds extended workflow tasks and support for SQS, AMQP, etc.  <br/>To maintain backward compatibility, the contrib jar also contains the output from task, event-queue, metrics, local-lock and noop index|
|event-queue| conductor-amqp | Support for AMQP queues |
|event-queue| conductor-nats | Support for NATS queues |
|event-queue| conductor-sqs | Support for SQS queues |
|external-payload-storage| conductor-azureblob-storage | External payload storage implementation using AzureBlob |
|external-payload-storage| conductor-postgres-external-storage | External payload storage implementation using Postgres |
|external-payload-storage| conductor-s3-storage | External payload storage implementation using AWS S3 |
|index| conductor-es7-persistence | Indexing using Elasticsearch 7.X |
|index| conductor-noop | Disable indexing |
|metrics| conductor-metrics | Support for various metrics integrations including Datadog and Prometheus |
|persistence| conductor-mysql-persistence | Persistence and queue using MySQL |
|persistence| conductor-postgres-persistence | Persistence and queue using Postgres |
|task| conductor-task | Various system tasks - Http, Kafka Publish and Json JQ Transformer |
|lock| conductor-zookeeper-lock | Workflow execution lock implementation using Zookeeper |
|lock| conductor-local-lock | Local JVM only locks - suitable for single instance conductor |

### A a note about conductor-contrib module
To maintain backward compatibility with the conductor-contribs module from Netflix Conductor repo, the contribs module adds all the necessary classes in the final jar that are otherwise distributed amongst various sub modules such as lock (local lock), index (noop), tasks etc.
If you do not need event queues, persistence, zookeeper or es7 support, just add contribs dependencies to your builds.

## FAQ
#### Who maintains these repository?
#### How often builds are published?
#### How do I get help?
#### How do I add new modules here?
#### I have a question not listed here.