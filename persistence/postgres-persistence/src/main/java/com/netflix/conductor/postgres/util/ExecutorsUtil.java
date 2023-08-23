package com.netflix.conductor.postgres.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutorsUtil {

    private ExecutorsUtil() {}

    public static ThreadFactory newNamedThreadFactory(final String threadNamePrefix) {
        return new ThreadFactory() {

            private final AtomicInteger counter = new AtomicInteger();

            @SuppressWarnings("NullableProblems")
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = Executors.defaultThreadFactory().newThread(r);
                thread.setName(threadNamePrefix + counter.getAndIncrement());
                return thread;
            }
        };
    }

}
