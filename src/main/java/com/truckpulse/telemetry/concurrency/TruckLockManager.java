package com.truckpulse.telemetry.concurrency;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TruckLockManager {
    private final Map<String, Object> locks =
            new ConcurrentHashMap<>();

    public Object getLock(String truckId) {
        return locks.computeIfAbsent(
                truckId,
                key -> new Object()
        );
    }
}
