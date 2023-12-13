# Announcement

> Effective **December 13, 2023**, Netflix will discontinue maintenance of Conductor OSS on GitHub. This strategic decision, while difficult, is essential for realigning our resources to better serve our business objectives with our internal Conductor fork.
> 
> We are *deeply grateful* for your support and contributions over the years. While Netflix will no longer be maintaining this repo, members of the Conductor community have been active in promoting alternative forks of this project, we’ll leave the code as is and trust that the health of the community will remain strong and continue to develop moving forward.

## Netflix Conductor community modules

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

## Repository Structure and Published Artifcats
Binaries are available from [Netflix OSS Maven](https://artifacts.netflix.net/netflixoss/com/netflix/conductor/) repository, or the [Maven Central Repository](https://search.maven.org/search?q=g:com.netflix.conductor).

Binaries are published under the group: **com.netflix.conductor**

For the list of artifacts published please see the table below:

| Parent Folder | Description |
| ----------- | ----- |
|[event-queue](event-queue/README.md)| Support for external eventing systems like AMQP and NATS |
| [external-payload-storage](external-payload-storage/README.md) | Storage for large workflow payloads |
| [index](index/README.md)| Indexing for searching workflows |
|[metrics](metrics/README.md)| Support for various metrics integrations including Datadog and Prometheus |
|[persistence](persistence/README.md)| Persistence for metadata, execution and queue implementation |
| [task](task/README.md)| Various system tasks - Kafka Publish
| [lock](lock/README.md)| Workflow execution lock implementation |
|  [workflow-event-listener](workflow-event-listener/README.md)| Workflow Status Listener and Binary compatibility with previously published conductor-contribs |


## FAQ
#### Why separate repository?
The number of contributions, especially newer implementations of the core contracts in Conductor has increased over the past few years. 
There is interest in the community to contribute more implementations. 
To streamline the support and release of the existing community-contributed implementations and future ones, we are creating a new repository dedicated to hosting just contributions. 
Conductor users who wish to use a contributed module will have a dedicated place to ask questions directly to fellow members of the community. 

Having a separate repository will allow us to scale the contributions and also ensure we are able to review and merge PRs in a timely fashion.

#### How often builds are published?
Similar to core Conductor the builds are published often with each major release.
Release numbers are kept in sync with main Conductor releases, which removes the need for a version compatibility matrix.

#### How do I get help?
Please use the Discussions on Conductor repo at https://github.com/Netflix/conductor/discussions

#### How do I add new modules here?
1. Start with a proposal by posting on the discussion
2. Send a PR

#### I have a question not listed here.
Please use the Discussions on Conductor repo at https://github.com/Netflix/conductor/discussions

#### Does it change how I build Conductor or use the Conductor binaries? (Do I need to pull additional dependency in my builds going forward?)
Conductor (https://github.com/Netflix/conductor) **no longer** pulls in all the dependencies from this repository as part of the [conductor-server](https://github.com/Netflix/conductor/tree/main/server) build.
If you plan to use the modules from this repository, please build [community-server](/community-server) which pulls in all the dependencies. 



