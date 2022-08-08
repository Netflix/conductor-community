package com.netflix.conductor.contribs.queue.nats;

import com.netflix.conductor.contribs.queue.nats.config.JetStreamProperties;
import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.core.events.queue.ObservableQueue;
import io.nats.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author astelmashenko@viax.io.
 */
public class JetStreamObserableQueue implements ObservableQueue {
    private static final Logger LOG = LoggerFactory.getLogger(JetStreamObserableQueue.class);
    private final LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<>();
    protected final Lock mu = new ReentrantLock();
    private final String queueType;
    private final String subject;
    private final JetStreamProperties properties;
    private final Scheduler scheduler;
    private volatile boolean running;
    private Connection nc;
    private JetStreamSubscription sub;

    public JetStreamObserableQueue(JetStreamProperties properties, String queueType, String subject, Scheduler scheduler) {
        LOG.debug("JSM obs queue create, qtype={}, quri={}", queueType, subject);
        this.queueType = queueType;
        this.subject = subject;
        this.properties = properties;
        this.scheduler = scheduler;
    }

    @Override
    public Observable<Message> observe() {
        subscribe(nc);
        return Observable.create(getOnSubscribe());
    }

    private Observable.OnSubscribe<Message> getOnSubscribe() {
        return subscriber -> {
            Observable<Long> interval = Observable.interval(100, TimeUnit.MILLISECONDS, scheduler);
            interval.flatMap((Long x) -> {
                        if (!isRunning()) {
                            LOG.debug("Component stopped, skip listening for messages from NATS Queue");
                            return Observable.from(Collections.emptyList());
                        } else {
                            List<Message> available = new ArrayList<>();
                            messages.drainTo(available);
                            LOG.debug("Processing batch messages count={}", available.size());
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
        return subject;
    }

    @Override
    public String getURI() {
        return subject;
    }

    @Override
    public List<String> ack(List<Message> messages) {
        messages.forEach(m -> ((JsmMessage) m).getJsmMsg().ack());
        return Collections.emptyList();
    }

    @Override
    public void publish(List<Message> messages) {
        try (Connection conn = Nats.connect(properties.getUrl())) {
            JetStream js = conn.jetStream();
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
        // do nothing, not supported
    }

    @Override
    public long size() {
        try {
            return sub.getConsumerInfo().getNumPending();
        } catch (IOException | JetStreamApiException e) {
            LOG.warn("Failed to get stream '{}' info", subject);
        }
        return 0;
    }

    @Override
    public void start() {
        natsConnect();
        running = true;
    }

    @Override
    public void stop() {
        sub.unsubscribe();
        try {
            if (nc != null) {
                nc.close();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to close Nats connection", e);
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void natsConnect() {
        try {
            Nats.connectAsynchronously(new Options.Builder()
                    .connectionListener((conn, type) -> {
                        LOG.info("Connection to JSM updated: {}", type);
                        this.nc = conn;
                        subscribeOnce(conn, type);
                    })
                    .maxReconnects(-1)
                    .build(), true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to connect to JSM", e);
        }
    }

    private void subscribeOnce(Connection nc, ConnectionListener.Events type) {
        if (sub == null &&
                (type.equals(ConnectionListener.Events.CONNECTED)
                        || type.equals(ConnectionListener.Events.RECONNECTED))) {
            mu.lock();
            try {
                subscribe(nc);
            } finally {
                mu.unlock();
            }
        }
    }

    private void subscribe(Connection nc) {
        try {
            JetStream js = nc.jetStream();

            PushSubscribeOptions pso = PushSubscribeOptions.builder().durable("durName1").build();
            sub = js.subscribe(subject, "durName1", nc.createDispatcher(),
                    msg -> {
                        var message = new JsmMessage();
                        message.setJsmMsg(msg);
                        message.setId(msg.getSID());
                        message.setPayload(new String(msg.getData()));
                        messages.add(message);
                    },
                    false,
                    pso);
        } catch (IOException | JetStreamApiException e) {
            throw new RuntimeException("Failed to subscribe", e);
        }
    }

}
