# Event Queue
## Published Artifacts

Group: `com.netflix.conductor`

| Published Artifact | Description |
| ----------- | ----------- | 
| conductor-amqp | Support for integration with AMQP  |
| conductor-nats | Support for integration with NATS  |

## Modules
### AMQP
https://www.amqp.org/

Provides ability to publish and consume messages from AMQP compatible message broker.

#### Configuration
(Default values shown below)
```properties
conductor.event-queues.amqp.enabled=true
conductor.event-queues.amqp.hosts=localhost
conductor.event-queues.amqp.port=5672
conductor.event-queues.amqp.username=guest
conductor.event-queues.amqp.password=guest
conductor.event-queues.amqp.virtualhost=/
#milliseconds
conductor.event-queues.amqp.connectionTimeout=60000
conductor.event-queues.amqp.useExchange=true
conductor.event-queues.amqp.listenerQueuePrefix=
```
For advanced configurations, see [AMQPEventQueueProperties](src/main/java/com/netflix/conductor/contribs/queue/amqp/config/AMQPEventQueueProperties.java)

### NATS

## Backward Compatibility
Contrib module retains the backward compatiblity with ealier verions published from core Conductor repository.
This is achived by created a shaded jar that also includes the output of the following modules:
1. event-queue (AMQP and NATS)
    1. SQS queue support is now part of core conductor
2. Metrics
3. Local only locks
4. Kafka and JQ tasks
