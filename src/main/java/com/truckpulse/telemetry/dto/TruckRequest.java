package com.truckpulse.telemetry.dto;

import jakarta.validation.constraints.NotBlank;

public record TruckRequest(
        @NotBlank
        String truckId,
        @NotBlank
        String model
) {
}
