package com.netflix.conductor.contribs.queue.nats.config;

import io.nats.client.Options;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @author andrey.stelmashenko@gmail.com
 */
@ConfigurationProperties("conductor.event-queues.jsm")
public class JetStreamProperties {
    private String listenerQueuePrefix = "";
    /**
     * The cluster id of the STAN session
     */
    private String clusterId = "test-cluster";
    /**
     * The durable subscriber name for the subscription
     */
    private String durableName = null;
    /**
     * The NATS connection url
     */
    private String url = Options.DEFAULT_URL;
    private Duration pollTimeDuration = Duration.ofMillis(100);

    public Duration getPollTimeDuration() {
        return pollTimeDuration;
    }

    public void setPollTimeDuration(Duration pollTimeDuration) {
        this.pollTimeDuration = pollTimeDuration;
    }

    public String getListenerQueuePrefix() {
        return listenerQueuePrefix;
    }

    public void setListenerQueuePrefix(String listenerQueuePrefix) {
        this.listenerQueuePrefix = listenerQueuePrefix;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getDurableName() {
        return durableName;
    }

    public void setDurableName(String durableName) {
        this.durableName = durableName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
