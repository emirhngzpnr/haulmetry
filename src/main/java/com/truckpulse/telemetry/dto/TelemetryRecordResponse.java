package com.truckpulse.telemetry.dto;

import java.time.Instant;

public record TelemetryRecordResponse(Long id,
                                      String truckId,
                                      double speed,
                                      int rpm,
                                      double fuel,
                                      int gear,
                                      Instant timestamp) {
}
