package com.truckpulse.telemetry.controller;

import com.truckpulse.telemetry.dto.TripResponse;
import com.truckpulse.telemetry.dto.TruckRequest;
import com.truckpulse.telemetry.entity.Truck;
import com.truckpulse.telemetry.service.TripService;
import com.truckpulse.telemetry.service.TruckService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/trucks")
public class TruckController {

    private final TruckService truckService;
    private final TripService tripService;
    public TruckController(
            TruckService truckService,
           TripService tripService)
    {
        this.truckService = truckService;
        this.tripService = tripService;
    }
    @PostMapping
    public Truck postTruck(
            @Valid @RequestBody TruckRequest truckRequest) {
        return truckService.registerTruck(truckRequest);

    }
    @GetMapping
    public List<Truck> getAllTrucks() {

        return truckService.getAllTrucks();
    }

    @GetMapping("/{truckId}")
 public ResponseEntity<Truck> getTruckByTruckId(@PathVariable  String truckId) {
    return truckService.getTruckByTruckId(truckId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
 }

    @GetMapping("/{truckId}/trips")
    public List<TripResponse> getTripsByTruckId(@PathVariable String truckId) {
        return tripService.getTripsByTruckId(truckId);

    }
}
