package com.truckpulse.telemetry.controller;

import com.truckpulse.telemetry.dto.TelemetryRequest;
import com.truckpulse.telemetry.model.DrivingEvent;
import com.truckpulse.telemetry.service.TelemetryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class TelemetryController {
        private final TelemetryService telemetryService;
    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;

    }

    @PostMapping("/telemetry")
    public TelemetryRequest postTelemetryRequest(@Valid @RequestBody TelemetryRequest telemetryRequest) {
        return telemetryService.processTelemetryRequest(telemetryRequest);
    }
    @GetMapping("/telemetry/{truckId}")
    public ResponseEntity<TelemetryRequest> getTelemetryRequest(
            @PathVariable String truckId) {

        Optional<TelemetryRequest> telemetry =
                telemetryService.getLatestTelemetry(truckId);

        return telemetry
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
//        if (telemetry.isPresent()) {
//            return ResponseEntity.ok(telemetry.get());
//        }
//
//        return ResponseEntity.notFound().build();
    }
    @GetMapping("/events/{truckId}")
    public List<DrivingEvent> getDrivingEvents(
            @PathVariable String truckId
    ) {
        return telemetryService.getDrivingEvents(truckId);

    }

}
