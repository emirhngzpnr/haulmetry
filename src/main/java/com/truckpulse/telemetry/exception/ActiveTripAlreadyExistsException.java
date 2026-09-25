package com.truckpulse.telemetry.exception;

public class ActiveTripAlreadyExistsException extends RuntimeException {
    public ActiveTripAlreadyExistsException(String truckId) {

        super("Truck already has an active trip: "+ truckId);
    }
}
