package com.truckpulse.telemetry.repository;

import com.truckpulse.telemetry.entity.DrivingEventEntity;
import com.truckpulse.telemetry.model.DrivingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrivingEventRepository extends JpaRepository<DrivingEventEntity, Long> {

}
