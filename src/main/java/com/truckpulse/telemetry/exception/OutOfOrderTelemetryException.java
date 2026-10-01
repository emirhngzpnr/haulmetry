package com.truckpulse.telemetry.exception;

public class OutOfOrderTelemetryException extends RuntimeException {
    public OutOfOrderTelemetryException(String truckId,
                                        long incomingSequence,
                                        long lastSequence) {
        super( "Out-of-order telemetry for truck "
                + truckId
                + ". Incoming sequence: "
                + incomingSequence
                + ", last sequence: "
                + lastSequence
        );;
    }
}
