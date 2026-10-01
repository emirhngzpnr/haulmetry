package com.truckpulse.telemetry.repository;

import com.truckpulse.telemetry.entity.Trip;
import com.truckpulse.telemetry.model.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    Optional<Trip> findByTruck_TruckIdAndStatus(String truckId, TripStatus status);
    List<Trip> findByTruck_TruckIdOrderByStartedAtDesc(String truckId);
    @Query("""
       select t.truck.truckId
       from Trip t
       where t.id = :tripId
       """)
    Optional<String> findTruckIdByTripId(@Param("tripId") Long tripId);

}
