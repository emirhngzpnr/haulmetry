package com.truckpulse.telemetry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record TelemetryRequest(
        @NotBlank
        String truckId,
        @PositiveOrZero
        double speed,
        @PositiveOrZero
        int rpm,
        @PositiveOrZero
        double fuel,
        int gear


) {
}
