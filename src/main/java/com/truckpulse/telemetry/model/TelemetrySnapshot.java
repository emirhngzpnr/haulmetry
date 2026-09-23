package com.truckpulse.telemetry.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

public record TelemetrySnapshot(@NotBlank
                                String truckId,
                                @PositiveOrZero
                                double speed,
                                @PositiveOrZero
                                int rpm,
                                @PositiveOrZero
                                double fuel,
                                int gear,
                                Instant timestamp

) {
}
