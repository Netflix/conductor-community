/*
 * Copyright 2020 Netflix, Inc.
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
package com.netflix.conductor.contribs.queue.stan;

import com.netflix.conductor.contribs.queue.stan.config.NATSStreamEventQueueProvider;
import com.netflix.conductor.contribs.queue.stan.config.NATSStreamProperties;
import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.core.events.queue.ObservableQueue;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Nats;
import io.nats.streaming.Options;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;
import io.nats.streaming.SubscriptionOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NATSStreamObservableQueue implements ObservableQueue {

    private static final Logger LOG = LoggerFactory.getLogger(NATSStreamObservableQueue.class);
    private final Lock mu = new ReentrantLock();
    private final String queueType;
    private final String subject;
    private final String queueUri;
    private final String clientId;
    private final Scheduler scheduler;
    private StreamingConnection conn;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Observable<Long> interval;
    private final String queueGroup;
    private final NATSStreamProperties properties;

    private final LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<>();

    public NATSStreamObservableQueue(
            NATSStreamProperties properties,
            String queueURI,
            Scheduler scheduler) {
        this.properties = properties;
        this.clientId = UUID.randomUUID().toString();
        this.scheduler = scheduler;

        this.queueType = NATSStreamEventQueueProvider.QUEUE_TYPE;
        LOG.debug("STAN obs queue create, qtype={}, quri={}", queueType, queueURI);

        this.queueUri = queueURI;
        // If queue specified (e.g. subject:queue) - split to subject & queue
        if (queueUri.contains(":")) {
            this.subject = queueUri.substring(0, queueUri.indexOf(':'));
            this.queueGroup = queueUri.substring(queueUri.indexOf(':') + 1);
        } else {
            this.subject = queueUri;
            this.queueGroup = null;
        }
    }

    public boolean isConnected() {
        return (conn != null
                && conn.getNatsConnection() != null
                && Connection.Status.CONNECTED.equals(conn.getNatsConnection().getStatus()));
    }

    @Override
    public void start() {
        mu.lock();
        try {
            stanConnect();
        } finally {
            mu.unlock();
        }
    }

    private void stanConnect() {
        if (running.get()) {
            return;
        }
        LOG.info("Starting NATS_STREAM observable, name={}", queueUri);
        try {
            Nats.connectAsynchronously(
                    new io.nats.client.Options.Builder()
                            .connectionListener(
                                    (natsConn, type) -> {
                                        LOG.info("Connection to STAN updated: {}", type);
                                        subscribeOnce(natsConn, type);
                                    })
                            .server(this.properties.getUrl())
                            .maxReconnects(properties.getMaxReconnects())
                            .build(),
                    true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to connect to Stan", e);
        }
    }

    private void subscribeOnce(Connection natsConn, ConnectionListener.Events type) {
        if (type == ConnectionListener.Events.CONNECTED
                || type == ConnectionListener.Events.RECONNECTED) {
            Options.Builder options = new Options.Builder();
            options.natsConn(natsConn)
                    .connectionLostHandler((stanConn, ex) -> {
                        LOG.error("Lost connection to stan", ex);
                    })
                    .clientId(clientId)
                    .clusterId(properties.getClusterId());
            StreamingConnectionFactory fact = new StreamingConnectionFactory(options.build());
            try {
                this.conn = fact.createConnection();
                subscribe(this.conn);
            } catch (IOException e) {
                LOG.warn("Failed to open connection to Stan", e);
                throw new RuntimeException("Failed to open connection to Stan", e);
            } catch (InterruptedException e) {
                LOG.error("Failed to open connection to Stan", e);
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to open connection to Stan", e);
            }
        }
    }

    public void subscribe(StreamingConnection conn) {
        try {
            SubscriptionOptions subscriptionOptions = new SubscriptionOptions.Builder()
                    .manualAcks()
                    .durableName(properties.getDurableName()).build();
            // Create subject/queue subscription if the queue has been provided
            LOG.debug("No subscription. Creating a queue subscription. subject={}, queue={}",
                    subject,
                    queueGroup);
            conn.subscribe(
                    subject,
                    queueGroup,
                    msg -> {
                        var message = new StanMessage();
                        message.setStanMsg(msg);
                        message.setId(msg.getSequence() + "-" + msg.getTimestamp());
                        message.setPayload(new String(msg.getData()));
                        messages.add(message);
                    },
                    subscriptionOptions);
            LOG.info("Subscribed successfully {}, queue={}", subject, queueGroup);
            this.running.set(true);
        } catch (Exception ex) {
            LOG.error("Subscription failed with " + ex.getMessage() + " for queueURI " + queueUri, ex);
        }
    }

    @Override
    public void stop() {
        LOG.info("Stopping observable queue: {}", queueUri);
        interval.unsubscribeOn(scheduler);
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Failed to close Stan connection", e);
        } catch (IOException | TimeoutException e) {
            LOG.error("Failed to close Stan connection", e);
        }
        running.set(false);
    }

    @Override
    public boolean isRunning() {
        return this.running.get();
    }

    @Override
    public Observable<Message> observe() {
        return Observable.create(getOnSubscribe());
    }

    private Observable.OnSubscribe<Message> getOnSubscribe() {
        return subscriber -> {
            interval =
                    Observable.interval(
                            properties.getPollTimeDuration().toMillis(),
                            TimeUnit.MILLISECONDS,
                            scheduler);
            interval.flatMap(
                            (Long x) -> {
                                if (!this.isRunning()) {
                                    LOG.trace(
                                            "Component stopped, skip listening for messages from STAN Queue '{}'",
                                            subject);
                                    return Observable.from(Collections.emptyList());
                                } else {
                                    List<Message> available = new ArrayList<>();
                                    messages.drainTo(available);
                                    if (!available.isEmpty()) {
                                        LOG.debug(
                                                "Processing STAN queue '{}' batch messages count={}",
                                                subject,
                                                available.size());
                                    }
                                    return Observable.from(available);
                                }
                            })
                    .subscribe(subscriber::onNext, subscriber::onError);
        };
    }

    @Override
    public String getType() {
        return queueType;
    }

    @Override
    public String getName() {
        return queueUri;
    }

    @Override
    public String getURI() {
        return getName();
    }

    @Override
    public List<String> ack(List<Message> messages) {
        List<String> unacked = new ArrayList<>();
        messages.forEach(m -> {
            try {
                LOG.debug("Ack msg: " + m.getId());
                ((StanMessage) m).getStanMsg().ack();
            } catch (IOException e) {
                LOG.warn("Failed to ack", e);
                unacked.add(m.getId());
            }
        });
        return unacked;
    }

    @Override
    public void publish(List<Message> messages) {
        if (!isConnected()) {
            throw new RuntimeException("Failed to publish to stan, connection is invalid status or null");
        }
        try {
            for (Message msg : messages) {
                conn.publish(subject, msg.getPayload().getBytes());
            }
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Failed to publish to stan", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to publish to stan", e);
        }
    }

    @Override
    public void setUnackTimeout(Message message, long unackTimeout) {
        // do nothing, not supported
    }

    @Override
    public long size() {
        return 0;
    }
}
