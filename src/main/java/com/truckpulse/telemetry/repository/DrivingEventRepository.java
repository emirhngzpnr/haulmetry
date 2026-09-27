package com.truckpulse.telemetry.repository;

import com.truckpulse.telemetry.entity.DrivingEventEntity;
import com.truckpulse.telemetry.model.DrivingEvent;
import com.truckpulse.telemetry.model.DrivingEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DrivingEventRepository extends JpaRepository<DrivingEventEntity, Long> {
    List<DrivingEventEntity> findByTruck_TruckIdOrderByOccurredAtAsc(String truckId);

    List<DrivingEventEntity> findByTrip_IdOrderByOccurredAtAsc(Long tripId);

    long countByTrip_IdAndEventType(Long tripId, DrivingEventType eventType);

}
