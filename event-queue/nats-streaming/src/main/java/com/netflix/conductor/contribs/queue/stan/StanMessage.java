package com.netflix.conductor.contribs.queue.stan;

import com.netflix.conductor.core.events.queue.Message;

public class StanMessage extends Message {
    private io.nats.streaming.Message stanMsg;

    public io.nats.streaming.Message getStanMsg() {
        return stanMsg;
    }

    public void setStanMsg(io.nats.streaming.Message stanMsg) {
        this.stanMsg = stanMsg;
    }
}
