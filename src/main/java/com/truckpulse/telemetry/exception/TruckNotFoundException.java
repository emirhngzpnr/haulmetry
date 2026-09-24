package com.truckpulse.telemetry.exception;

public class TruckNotFoundException extends RuntimeException {
    public TruckNotFoundException(String truckId) {
        super("Truck Not Found: "+truckId);
    }
}
