/*
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
package com.netflix.conductor.contribs.queue.nats.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.nats.client.Options;

/**
 * @author andrey.stelmashenko@gmail.com
 */
@ConfigurationProperties("conductor.event-queues.jsm")
public class JetStreamProperties {
    private String listenerQueuePrefix = "";

    /** The durable subscriber name for the subscription */
    private String durableName = "defaultQueue";

    private String streamStorageType = "file";

    /** The NATS connection url */
    private String url = Options.DEFAULT_URL;

    private Duration pollTimeDuration = Duration.ofMillis(100);

    /** WAIT tasks default queue group, to make subscription round-robin delivery to single sub */
    private String defaultQueueGroup = "wait-group";

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

    public String getStreamStorageType() {
        return streamStorageType;
    }

    public void setStreamStorageType(String streamStorageType) {
        this.streamStorageType = streamStorageType;
    }

    public String getDefaultQueueGroup() {
        return defaultQueueGroup;
    }

    public void setDefaultQueueGroup(String defaultQueueGroup) {
        this.defaultQueueGroup = defaultQueueGroup;
    }
}
