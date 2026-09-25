package com.truckpulse.telemetry.exception;

public class TripAlreadyCompletedException extends RuntimeException {
    public TripAlreadyCompletedException(Long tripId ) {
        super("Trip is already completed: "+tripId);
    }
}
