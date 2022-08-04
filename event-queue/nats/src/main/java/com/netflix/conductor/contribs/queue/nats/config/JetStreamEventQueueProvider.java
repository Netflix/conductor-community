package com.netflix.conductor.contribs.queue.nats.config;

import com.netflix.conductor.contribs.queue.nats.JetStreamObserableQueue;
import com.netflix.conductor.core.events.EventQueueProvider;
import com.netflix.conductor.core.events.queue.ObservableQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import rx.Scheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author astelmashenko@viax.io.
 */
public class JetStreamEventQueueProvider implements EventQueueProvider {
    public static final String QUEUE_TYPE = "jsm";
    private static final Logger LOG = LoggerFactory.getLogger(JetStreamEventQueueProvider.class);
    private final Map<String, ObservableQueue> queues = new ConcurrentHashMap<>();
    private final JetStreamProperties properties;
    private final Scheduler scheduler;

    public JetStreamEventQueueProvider(JetStreamProperties properties, Scheduler scheduler) {
        LOG.info("NATS Event Queue Provider initialized...");
        this.properties = properties;
        this.scheduler = scheduler;
    }

    @Override
    public String getQueueType() {
        return QUEUE_TYPE;
    }

    @Override
    @NonNull
    public ObservableQueue getQueue(String queueURI) throws IllegalArgumentException {
        LOG.debug("Getting obs queue, quri={}", queueURI);
        return queues.computeIfAbsent(
                queueURI, q -> new JetStreamObserableQueue(properties, getQueueType(), queueURI, scheduler));
    }
}
