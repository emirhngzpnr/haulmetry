package com.truckpulse.telemetry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record TelemetryRequest(
        @NotBlank
        String truckId,
        @NotBlank
        String sessionId,
        @PositiveOrZero
        double speed,
        @PositiveOrZero
        int rpm,
        @PositiveOrZero
        double fuel,
        int gear,
        @Positive
        long sequenceNumber


) {
}
