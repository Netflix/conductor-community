package com.netflix.conductor.contribs.queue.nats;

public class NatsException extends RuntimeException {
    public NatsException() {
        super();
    }

    public NatsException(String message) {
        super(message);
    }

    public NatsException(String message, Throwable cause) {
        super(message, cause);
    }

    public NatsException(Throwable cause) {
        super(cause);
    }

    protected NatsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
