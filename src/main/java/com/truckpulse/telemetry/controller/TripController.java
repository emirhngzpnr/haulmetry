package com.truckpulse.telemetry.controller;

import com.truckpulse.telemetry.dto.TripResponse;
import com.truckpulse.telemetry.entity.Trip;
import com.truckpulse.telemetry.service.TripService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
public class TripController {
    private final TripService tripService;
    public TripController(TripService tripService) {
        this.tripService = tripService;
    }
    @PostMapping("/start/{truckId}")
    public TripResponse startTrip(@PathVariable String truckId) {
        return tripService.startTrip(truckId);

    }
@PutMapping("/{tripId}/complete")
    public TripResponse completeTrip(@PathVariable  Long tripId){
        return tripService.completeTrip(tripId);
    }
}
