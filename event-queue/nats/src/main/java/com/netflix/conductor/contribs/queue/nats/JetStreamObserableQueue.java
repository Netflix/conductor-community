package com.netflix.conductor.contribs.queue.nats;

import com.netflix.conductor.contribs.queue.nats.config.JetStreamProperties;
import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.core.events.queue.ObservableQueue;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.Nats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author astelmashenko@viax.io.
 */
public class JetStreamObserableQueue implements ObservableQueue {
    private static final Logger LOG = LoggerFactory.getLogger(JetStreamObserableQueue.class);
    private final String queueType;
    private final String subject;
    private final JetStreamProperties properties;
    private final Scheduler scheduler;
    private volatile boolean running;

    public JetStreamObserableQueue(JetStreamProperties properties, String queueType, String subject, Scheduler scheduler) {
        LOG.debug("JSM obs queue create, qtype={}, quri={}", queueType, subject);
        this.queueType = queueType;
        this.subject = subject;
        this.properties = properties;
        this.scheduler = scheduler;
    }

    @Override
    public Observable<Message> observe() {
        LOG.debug("Observing");
        return Observable.create(subscriber -> {
//            subscriber.
        });
    }

    @Override
    public String getType() {
        return queueType;
    }

    @Override
    public String getName() {
        return subject;
    }

    @Override
    public String getURI() {
        return subject;
    }

    @Override
    public List<String> ack(List<Message> messages) {
        // TODO message to acked manually
        return Collections.emptyList();
    }

    @Override
    public void publish(List<Message> messages) {
        try (Connection nc = Nats.connect(properties.getUrl())) {
            JetStream js = nc.jetStream();
            for (Message msg : messages) {
                js.publish(subject, msg.getPayload().getBytes());
            }
        } catch (IOException | JetStreamApiException e) {
            throw new RuntimeException("Failed to publish to jsm", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to publish to jsm", e);
        }
    }

    @Override
    public void setUnackTimeout(Message message, long unackTimeout) {

    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
