package com.truckpulse.telemetry.dto;

import java.time.Instant;

public record TripSummaryResponse(
        Long tripId,
        String truckId,
        Instant startedAt,
        Instant endedAt,
        long durationSeconds,
        double averageSpeed,
        double maxSpeed,
        int telemetryCount,
        long harshBrakingCount
)
{
}
