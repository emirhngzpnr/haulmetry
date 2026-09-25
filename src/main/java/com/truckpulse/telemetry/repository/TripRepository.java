package com.truckpulse.telemetry.repository;

import com.truckpulse.telemetry.entity.Trip;
import com.truckpulse.telemetry.model.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    Optional<Trip> findByTruck_TruckIdAndStatus(String truckId, TripStatus status);

    Long id(Long id);
}
