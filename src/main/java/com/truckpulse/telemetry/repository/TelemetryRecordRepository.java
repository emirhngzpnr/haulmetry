package com.truckpulse.telemetry.repository;

import com.truckpulse.telemetry.dto.TelemetryRecordResponse;
import com.truckpulse.telemetry.entity.TelemetryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TelemetryRecordRepository extends JpaRepository<TelemetryRecord, Long> {

    List<TelemetryRecord> findByTruck_TruckIdOrderByTimestampAsc(String truckId);
}
