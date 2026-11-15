package com.explodenot.gli.util;

import java.time.Clock;
import java.time.Instant;

public interface TimeProvider {
    Instant now();
    long nanoTime();
    void sleep(long millis) throws InterruptedException;

    TimeProvider SYSTEM = new TimeProvider() {
        @Override
        public Instant now() {
            return Instant.now();
        }

        @Override
        public long nanoTime() {
            return System.nanoTime();
        }

        @Override
        public void sleep(long millis) throws InterruptedException {
            Thread.sleep(millis);
        }
    };
}
