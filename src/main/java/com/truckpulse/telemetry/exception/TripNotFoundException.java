package com.truckpulse.telemetry.exception;

public class TripNotFoundException extends RuntimeException {
    public TripNotFoundException(Long tripId){
        super("Trip Not Found: "+ tripId);
    }
}
