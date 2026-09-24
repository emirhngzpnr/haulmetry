package com.truckpulse.telemetry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TruckRequest(
        @NotBlank
        String truckId,
        @NotBlank
        String model
) {
}
