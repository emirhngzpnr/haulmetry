package com.truckpulse.telemetry.repository;

import com.truckpulse.telemetry.entity.TelemetryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetryRecordRepository extends JpaRepository<TelemetryRecord, Long> {
}
