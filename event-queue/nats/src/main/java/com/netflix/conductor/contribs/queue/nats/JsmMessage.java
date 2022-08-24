package com.netflix.conductor.contribs.queue.nats;

import com.netflix.conductor.core.events.queue.Message;

/**
 * @author andrey.stelmashenko@gmail.com
 */
public class JsmMessage extends Message {
    private io.nats.client.Message jsmMsg;

    public io.nats.client.Message getJsmMsg() {
        return jsmMsg;
    }

    public void setJsmMsg(io.nats.client.Message jsmMsg) {
        this.jsmMsg = jsmMsg;
    }
}
