package com.truckpulse.telemetry.service;

import com.truckpulse.telemetry.concurrency.TruckLockManager;
import com.truckpulse.telemetry.dto.TripResponse;
import com.truckpulse.telemetry.dto.TripSummaryResponse;
import com.truckpulse.telemetry.entity.TelemetryRecord;
import com.truckpulse.telemetry.entity.Trip;
import com.truckpulse.telemetry.entity.Truck;
import com.truckpulse.telemetry.exception.ActiveTripAlreadyExistsException;
import com.truckpulse.telemetry.exception.TripAlreadyCompletedException;
import com.truckpulse.telemetry.exception.TripNotFoundException;
import com.truckpulse.telemetry.exception.TruckNotFoundException;
import com.truckpulse.telemetry.model.DrivingEventType;
import com.truckpulse.telemetry.model.TripStatus;
import com.truckpulse.telemetry.repository.DrivingEventRepository;
import com.truckpulse.telemetry.repository.TelemetryRecordRepository;
import com.truckpulse.telemetry.repository.TripRepository;
import com.truckpulse.telemetry.repository.TruckRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class TripService {
    private final TripRepository tripRepository;
    private final TruckRepository truckRepository;
    private final TelemetryRecordRepository telemetryRecordRepository;
    private final DrivingEventRepository drivingEventRepository;
    private final TruckLockManager truckLockManager;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public TripService(TripRepository tripRepository
            , TruckRepository truckRepository
            , TelemetryRecordRepository telemetryRecordRepository
            , DrivingEventRepository drivingEventRepository
            , TruckLockManager truckLockManager
            , TransactionTemplate transactionTemplate
            , Clock clock
    )
    {
        this.tripRepository = tripRepository;
        this.truckRepository = truckRepository;
        this.telemetryRecordRepository = telemetryRecordRepository;
        this.drivingEventRepository = drivingEventRepository;
        this.truckLockManager = truckLockManager;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }
    @Transactional
    public TripResponse startTrip(String truckId) {
        Truck truck= truckRepository
                .findByTruckId(truckId)
                .orElseThrow(() -> new TruckNotFoundException(truckId));
        boolean hasActiveTrip=tripRepository
                .findByTruck_TruckIdAndStatus(truckId,TripStatus.ACTIVE)
                .isPresent();
        if (hasActiveTrip) {
            throw new ActiveTripAlreadyExistsException(truckId);
        }

        Trip trip = new Trip(
                truck,
                Instant.now(),
                TripStatus.ACTIVE

        );

        Trip savedTrip;
        try {
            savedTrip = tripRepository.saveAndFlush(trip);
        } catch (DataIntegrityViolationException e) {
            throw new ActiveTripAlreadyExistsException(truckId);
        }
return new TripResponse(
        savedTrip.getId(),
        savedTrip.getTruck().getTruckId(),
        savedTrip.getStartedAt(),
        savedTrip.getEndedAt(),
        savedTrip.getStatus()
);
    }
    public TripResponse completeTrip(Long tripId) {

        String truckId = tripRepository
                .findTruckIdByTripId(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        Object lock = truckLockManager.getLock(truckId);

        synchronized (lock) {

            TripResponse response =
                    transactionTemplate.execute(status -> {

                        Trip trip = tripRepository
                                .findById(tripId)
                                .orElseThrow(() ->
                                        new TripNotFoundException(tripId)
                                );

                        if (trip.getStatus() == TripStatus.COMPLETED) {
                            throw new TripAlreadyCompletedException(tripId);
                        }

                        trip.complete(Instant.now(clock));

                        return new TripResponse(
                                trip.getId(),
                                trip.getTruck().getTruckId(),
                                trip.getStartedAt(),
                                trip.getEndedAt(),
                                trip.getStatus()
                        );
                    });

            return response;
        }
    }
    public List<TripResponse> getTripsByTruckId(String truckId) {
        truckRepository
                .findByTruckId(truckId)
                .orElseThrow(()->new TruckNotFoundException(truckId));

      return  tripRepository
                .findByTruck_TruckIdOrderByStartedAtDesc(truckId)
                .stream()
              .map(
                      record -> new TripResponse(
                              record.getId(),
                              record.getTruck().getTruckId(),
                              record.getStartedAt(),
                              record.getEndedAt(),
                              record.getStatus()
                      )
              ).toList();

    }

public TripSummaryResponse getTripSummary(Long tripId) {
        Trip trip=
                tripRepository.findById(tripId)
                        .orElseThrow(()->new TripNotFoundException(tripId));

        List<TelemetryRecord> telemetryRecords =
                telemetryRecordRepository
                        .findByTrip_IdOrderByTimestampAsc(tripId);

        int telemetryCount = telemetryRecords.size();

        double averageSpeed = telemetryRecords.stream()
                .mapToDouble(TelemetryRecord::getSpeed)
                .average()
                .orElse(0.0);

        double maxSpeed= telemetryRecords.stream()
                .mapToDouble(TelemetryRecord::getSpeed)
                .max()
                .orElse(0.0);

        long harshBrakingCount=
                drivingEventRepository.countByTrip_IdAndEventType(tripId, DrivingEventType.HARSH_BRAKING);

        // trip tamamlandıysa endedAt kullan, tamamlanmadıysa şu anki zamanı kullan
    Instant endTime = trip.getEndedAt() != null
            ? trip.getEndedAt()
            : Instant.now();

         long durationSeconds = Duration
            .between(trip.getStartedAt(), endTime)
            .getSeconds();

    return new TripSummaryResponse(
            trip.getId(),
            trip.getTruck().getTruckId(),
            trip.getStartedAt(),
            trip.getEndedAt(),
            durationSeconds,
            averageSpeed,
            maxSpeed,
            telemetryCount,
            harshBrakingCount
    );
}

    }


