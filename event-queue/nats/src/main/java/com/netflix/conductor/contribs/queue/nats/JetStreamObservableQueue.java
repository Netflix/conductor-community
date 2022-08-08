package com.netflix.conductor.contribs.queue.nats;

import com.netflix.conductor.contribs.queue.nats.config.JetStreamProperties;
import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.core.events.queue.ObservableQueue;
import io.nats.client.*;
import io.nats.client.api.RetentionPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
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

/**
 * @author andrey.stelmashenko@gmail.com
 */
public class JetStreamObservableQueue implements ObservableQueue {
    private static final Logger LOG = LoggerFactory.getLogger(JetStreamObservableQueue.class);
    private final LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<>();
    private final String queueType;
    private final String subject;
    private final JetStreamProperties properties;
    private final Scheduler scheduler;
    private volatile boolean running;
    private Connection nc;
    private JetStreamSubscription sub;
    private Observable<Long> interval;

    public JetStreamObservableQueue(JetStreamProperties properties, String queueType, String subject, Scheduler scheduler) {
        LOG.debug("JSM obs queue create, qtype={}, quri={}", queueType, subject);
        this.queueType = queueType;
        this.subject = subject;
        this.properties = properties;
        this.scheduler = scheduler;
    }

    @Override
    public Observable<Message> observe() {
        return Observable.create(getOnSubscribe());
    }

    private Observable.OnSubscribe<Message> getOnSubscribe() {
        return subscriber -> {
            interval = Observable.interval(properties.getPollTimeDuration().toMillis(), TimeUnit.MILLISECONDS, scheduler);
            interval.flatMap((Long x) -> {
                        if (!isRunning()) {
                            LOG.debug("Component stopped, skip listening for messages from JSM Queue");
                            return Observable.from(Collections.emptyList());
                        } else {
                            List<Message> available = new ArrayList<>();
                            messages.drainTo(available);
                            if (!available.isEmpty()) {
                                LOG.debug("Processing batch messages count={}", available.size());
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
        return subject;
    }

    @Override
    public String getURI() {
        return getName();
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
            throw new NatsException("Failed to publish to jsm", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NatsException("Failed to publish to jsm", e);
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
    }

    @Override
    public void stop() {
        if (sub != null && sub.isActive()) {
            sub.unsubscribe();
        }
        interval.unsubscribeOn(scheduler);
        try {
            if (nc != null) {
                nc.close();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Failed to close Nats connection", e);
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    private void natsConnect() {
        if (running) {
            return;
        }
        LOG.info("Starting JSM observable, name={}", subject);
        try {
            Nats.connectAsynchronously(new Options.Builder()
                    .connectionListener((conn, type) -> {
                        LOG.info("Connection to JSM updated: {}", type);
                        this.nc = conn;
                        subscribeOnce(conn, type);
                    })
                    .server(properties.getUrl())
                    .maxReconnects(-1)
                    .build(), true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NatsException("Failed to connect to JSM", e);
        }
    }

    private void createStream(Connection nc) {
        JetStreamManagement jsm;
        try {
            jsm = nc.jetStreamManagement();
        } catch (IOException e) {
            throw new NatsException("Failed to get jsm management", e);
        }

        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .name(subject)
                .retentionPolicy(RetentionPolicy.WorkQueue)
                .storageType(StorageType.Memory)
                .build();

        try {
            if (getStreamInfoOrNullWhenNotExist(jsm, subject) != null) {
                LOG.info("Stream '{}' already exists", subject);
                return;
            }
            StreamInfo streamInfo = jsm.addStream(streamConfig);
            LOG.debug("Create stream, info: {}", streamInfo);
        } catch (IOException | JetStreamApiException e) {
            throw new NatsException("Failed to add stream: " + streamConfig, e);
        }
    }

    public static StreamInfo getStreamInfoOrNullWhenNotExist(JetStreamManagement jsm, String streamName) throws IOException, JetStreamApiException {
        try {
            return jsm.getStreamInfo(streamName);
        }
        catch (JetStreamApiException e) {
            if (e.getErrorCode() == 404) {
                return null;
            }
            throw e;
        }
    }

    private synchronized void subscribeOnce(Connection nc, ConnectionListener.Events type) {
        if (sub == null &&
                (type.equals(ConnectionListener.Events.CONNECTED)
                        || type.equals(ConnectionListener.Events.RECONNECTED))) {
            createStream(nc);
            subscribe(nc);
        }
    }

    private void subscribe(Connection nc) {
        try {
            JetStream js = nc.jetStream();

            PushSubscribeOptions pso = PushSubscribeOptions.builder().durable("durName1").build();
            LOG.debug("Subscribing jsm, subject={}, options={}", subject, pso);
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
            LOG.debug("Subscribed successfully {}", sub.getConsumerInfo());
            running = true;
        } catch (IOException | JetStreamApiException e) {
            throw new NatsException("Failed to subscribe", e);
        }
    }

}