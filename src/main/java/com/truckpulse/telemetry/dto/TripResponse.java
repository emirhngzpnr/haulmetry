package com.truckpulse.telemetry.dto;

import com.truckpulse.telemetry.model.TripStatus;

import java.time.Instant;

public record TripResponse(Long id,
                           String truckId,
                           Instant startedAt,
                           Instant endedAt,
                           TripStatus status) {
}
