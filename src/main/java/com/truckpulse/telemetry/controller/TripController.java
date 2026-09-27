package com.truckpulse.telemetry.controller;

import com.truckpulse.telemetry.dto.DrivingEventResponse;
import com.truckpulse.telemetry.dto.TelemetryRecordResponse;
import com.truckpulse.telemetry.dto.TripResponse;
import com.truckpulse.telemetry.entity.Trip;
import com.truckpulse.telemetry.service.TelemetryService;
import com.truckpulse.telemetry.service.TripService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {
    private final TripService tripService;
    private final TelemetryService telemetryService;
    public TripController(TripService tripService, TelemetryService telemetryService) {
        this.tripService = tripService;
        this.telemetryService = telemetryService;
    }
    @PostMapping("/start/{truckId}")
    public TripResponse startTrip(@PathVariable String truckId) {
        return tripService.startTrip(truckId);

    }
@PutMapping("/{tripId}/complete")
    public TripResponse completeTrip(@PathVariable  Long tripId){

        return tripService.completeTrip(tripId);
    }

    @GetMapping("/{tripId}/telemetry")
    public List<TelemetryRecordResponse> getTelemetryByTripId(@PathVariable Long tripId) {
        return telemetryService.getTelemetryByTripId(tripId);
    }
    @GetMapping("/{tripId}/events")
    public List<DrivingEventResponse> getDrivingEventsByTripId(@PathVariable Long tripId) {
        return telemetryService.getEventsByTripId(tripId);
    }
}
