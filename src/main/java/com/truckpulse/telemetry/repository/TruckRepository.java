package com.truckpulse.telemetry.repository;

import com.truckpulse.telemetry.entity.Truck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TruckRepository extends JpaRepository<Truck, Long> {
    Optional<Truck> findByTruckId(String truckId);
    boolean existsByTruckId(String truckId);
}
