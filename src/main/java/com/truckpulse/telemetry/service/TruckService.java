package com.truckpulse.telemetry.service;

import com.truckpulse.telemetry.dto.TruckRequest;
import com.truckpulse.telemetry.entity.Truck;
import com.truckpulse.telemetry.repository.TruckRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TruckService {
    private final TruckRepository truckRepository;

    public TruckService(TruckRepository truckRepository) {
        this.truckRepository = truckRepository;
    }

    public Truck registerTruck(TruckRequest truckRequest) {
return   truckRepository.findByTruckId(truckRequest.truckId())
                .orElseGet(() -> {
                    Truck truck = new Truck(truckRequest.truckId(), truckRequest.model());
                    return truckRepository.save(truck);
                });

    }

    public List<Truck> getAllTrucks() {
        return truckRepository.findAll();
    }

    public Optional<Truck> getTruckByTruckId(String truckId) {
        return truckRepository.findByTruckId(truckId);
    }
}
