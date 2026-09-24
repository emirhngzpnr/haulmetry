package com.truckpulse.telemetry.dto;

public record ApiError(  int status,
                         String message) {
}
