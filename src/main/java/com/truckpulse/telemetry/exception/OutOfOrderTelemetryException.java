package com.truckpulse.telemetry.exception;

public class OutOfOrderTelemetryException extends RuntimeException {
    public OutOfOrderTelemetryException(String truckId,
                                        String sessionId,
                                        long incomingSequence,
                                        long lastSequence) {
        super(
                "Out-of-order telemetry for truck "
                        + truckId
                        + ", session "
                        + sessionId
                        + ". Incoming sequence: "
                        + incomingSequence
                        + ", last sequence: "
                        + lastSequence
        );
    }
}
