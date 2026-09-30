package com.truckpulse.telemetry.service;

import com.truckpulse.telemetry.dto.TelemetryRequest;
import com.truckpulse.telemetry.entity.DrivingEventEntity;
import com.truckpulse.telemetry.entity.Truck;
import com.truckpulse.telemetry.repository.DrivingEventRepository;
import com.truckpulse.telemetry.repository.TelemetryRecordRepository;
import com.truckpulse.telemetry.repository.TripRepository;
import com.truckpulse.telemetry.repository.TruckRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock
    private TruckRepository truckRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TelemetryRecordRepository telemetryRecordRepository;

    @Mock
    private DrivingEventRepository drivingEventRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private TelemetryService telemetryService;

    @Test
    void shouldCreateHarshBrakingEventWithDeterministicTime() {

        Truck truck = new Truck(
                "TEST-TRUCK-001",
                "Test Truck"
        );

        when(truckRepository.findByTruckId("TEST-TRUCK-001"))
                .thenReturn(Optional.of(truck));

        when(tripRepository.findByTruck_TruckIdAndStatus(any(), any()))
                .thenReturn(Optional.empty());

        when(clock.instant())
                .thenReturn(
                        Instant.parse("2026-09-30T12:00:00Z"),
                        Instant.parse("2026-09-30T12:00:02Z")
                );

        TelemetryRequest firstTelemetry =
                new TelemetryRequest(
                        "TEST-TRUCK-001",
                        100,
                        1500,
                        300,
                        8
                );

        TelemetryRequest secondTelemetry =
                new TelemetryRequest(
                        "TEST-TRUCK-001",
                        64,
                        1200,
                        299,
                        6
                );

        telemetryService.processTelemetryRequest(firstTelemetry);
        telemetryService.processTelemetryRequest(secondTelemetry);

        ArgumentCaptor<DrivingEventEntity> eventCaptor =
                ArgumentCaptor.forClass(DrivingEventEntity.class);

        verify(drivingEventRepository)
                .save(eventCaptor.capture());

        DrivingEventEntity event =
                eventCaptor.getValue();

        assertEquals(100.0, event.getPreviousSpeed());
        assertEquals(64.0, event.getCurrentSpeed());
        assertEquals(36.0, event.getSpeedDifference());
        assertEquals(2000, event.getDurationMs());
        assertEquals(5.0, event.getDeceleration(), 0.001);
    }
}