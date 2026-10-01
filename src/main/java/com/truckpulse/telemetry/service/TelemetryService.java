package com.truckpulse.telemetry.service;

import com.truckpulse.telemetry.dto.DrivingEventResponse;
import com.truckpulse.telemetry.dto.TelemetryRecordResponse;
import com.truckpulse.telemetry.dto.TelemetryRequest;
import com.truckpulse.telemetry.entity.DrivingEventEntity;
import com.truckpulse.telemetry.entity.TelemetryRecord;
import com.truckpulse.telemetry.entity.Trip;
import com.truckpulse.telemetry.entity.Truck;
import com.truckpulse.telemetry.exception.OutOfOrderTelemetryException;
import com.truckpulse.telemetry.exception.TripNotFoundException;
import com.truckpulse.telemetry.exception.TruckNotFoundException;
import com.truckpulse.telemetry.model.DrivingEventType;
import com.truckpulse.telemetry.model.TelemetrySnapshot;
import com.truckpulse.telemetry.model.TripStatus;
import com.truckpulse.telemetry.repository.DrivingEventRepository;
import com.truckpulse.telemetry.repository.TelemetryRecordRepository;
import com.truckpulse.telemetry.repository.TripRepository;
import com.truckpulse.telemetry.repository.TruckRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TelemetryService {
    // ani fren değeri (ivme)
    private static final double HARSH_BRAKING_THRESHOLD = 4.0;
    // operasyonlarını güvenli sağlanması için ConccurentHashMap (thread-safe)
    private final Map<String, TelemetrySnapshot> latestTelemetry =
            new ConcurrentHashMap<>();

    //truck bazlı lockları tutmak için
    private final Map<String, Object> truckLocks =
            new ConcurrentHashMap<>();

    private final Map<String, Long> lastSequences =
            new ConcurrentHashMap<>();

    // kayıp sequencesNumber'ı izleyebilmek amacıyla kullanılıyor
    private static final Logger log =
            LoggerFactory.getLogger(TelemetryService.class);

 private final TruckRepository truckRepository;
 private final TripRepository tripRepository;
 private final TelemetryRecordRepository telemetryRecordRepository;
 private final DrivingEventRepository drivingEventRepository;
 private final Clock clock;

 public TelemetryService(TruckRepository truckRepository,
                         TripRepository tripRepository ,
                         TelemetryRecordRepository telemetryRecordRepository,
                         DrivingEventRepository drivingEventRepository,
                         Clock clock
 ) {
     this.truckRepository = truckRepository;
     this.tripRepository = tripRepository;
     this.telemetryRecordRepository = telemetryRecordRepository;
     this.drivingEventRepository = drivingEventRepository;
     this.clock = clock;
 }


    @Transactional
    public TelemetryRequest processTelemetryRequest(TelemetryRequest telemetryRequest
    ) {
        Truck truck = truckRepository
                .findByTruckId(telemetryRequest.truckId())
                .orElseThrow(() ->
                        new TruckNotFoundException(
                                telemetryRequest.truckId()
                        )
                );
         // eğer Validasyon kullanmayıp bu şekilde kontrol sağlarsak 500 ınternal server hatası alırız.
        // Validasyon kullandığımız zaman ise 400 bad request alırız ki bu daha sağlıklı olan yoldur.

        Object lock = truckLocks
                .computeIfAbsent(
                     telemetryRequest.truckId(),
                     key -> new Object()
        );
        synchronized (lock) {
            Long lastSequence =
                    lastSequences.get(telemetryRequest.truckId());

            if (lastSequence != null
                    && telemetryRequest.sequenceNumber() <= lastSequence) {

                throw new OutOfOrderTelemetryException(
                        telemetryRequest.truckId(),
                        telemetryRequest.sequenceNumber(),
                        lastSequence
                );
            }
            if (lastSequence != null
                    && telemetryRequest.sequenceNumber() > lastSequence + 1) {

                long missingCount =
                        telemetryRequest.sequenceNumber()
                                - lastSequence
                                - 1;

                log.warn(
                        "Telemetry sequence gap detected for truck {}. "
                                + "Last sequence: {}, incoming sequence: {}, missing count: {}",
                        telemetryRequest.truckId(),
                        lastSequence,
                        telemetryRequest.sequenceNumber(),
                        missingCount
                );
            }
        TelemetrySnapshot current = new TelemetrySnapshot(
                telemetryRequest.truckId(),
                telemetryRequest.speed(),
                telemetryRequest.rpm(),
                telemetryRequest.fuel(),
                telemetryRequest.gear(),
                Instant.now(clock)
        );
        Trip activeTrip = tripRepository
                .findByTruck_TruckIdAndStatus(
                        telemetryRequest.truckId(),
                        TripStatus.ACTIVE
                )
                .orElse(null);
                        TelemetryRecord telemetryRecord = new TelemetryRecord(
                                truck,
                                activeTrip,
                                current.speed(),
                                current.rpm(),
                                current.fuel(),
                                current.gear(),
                                current.timestamp()
                        );
        telemetryRecordRepository.save(telemetryRecord);

        TelemetrySnapshot previous =
                latestTelemetry.get(current.truckId());


if(previous != null) {
    double speedDifference = previous.speed() - current.speed();
    long milliseconds = Duration
            .between(previous.timestamp(), current.timestamp())
            .toMillis();
    double seconds = milliseconds / 1000.0;

    if (seconds > 0 && speedDifference > 0) {

        double speedDifferenceMs =
                speedDifference / 3.6;

        double deceleration =
                speedDifferenceMs / seconds;


        if (deceleration >= HARSH_BRAKING_THRESHOLD) {


            DrivingEventEntity drivingEventEntity =
                    new DrivingEventEntity(
                            truck,
                            activeTrip,
                            DrivingEventType.HARSH_BRAKING,
                            previous.speed(),
                            current.speed(),
                            speedDifference,
                            milliseconds,
                            deceleration,
                            current.timestamp()
                    );

drivingEventRepository.save(drivingEventEntity);
        }
    }
}                   latestTelemetry.put(
                    current.truckId(),
                    current
            );

            lastSequences.put(
                    current.truckId(),
                    telemetryRequest.sequenceNumber()
            );
        }

            return telemetryRequest;
    }

    public Optional<TelemetrySnapshot> getLatestTelemetry(String truckId) {
        return Optional.ofNullable(latestTelemetry.get(truckId)); // aranan keye ait value olmayabilir bu durumda null değer döndürebilir bunun önüne geçmek için Optional kullanırız.

        }
        public List<DrivingEventResponse> getDrivingEvents(String truckId) {
           return  drivingEventRepository.findByTruck_TruckIdOrderByOccurredAtAsc(truckId)
                   .stream()
                   .map(record -> new DrivingEventResponse(
                           record.getId(),
                           record.getTruck().getTruckId(),
                           record.getTrip()!=null
                           ? record.getTrip().getId():
                                   null,
                           record.getEventType(),
                           record.getPreviousSpeed(),
                           record.getCurrentSpeed(),
                           record.getSpeedDifference(),
                           record.getDurationMs(),
                           record.getDeceleration(),
                           record.getOccurredAt()

                   )).toList();
        }
    public List<TelemetryRecordResponse> getTelemetryHistory(String truckId) {

        return telemetryRecordRepository
                .findByTruck_TruckIdOrderByTimestampAsc(truckId)
                .stream()
                .map(record -> new TelemetryRecordResponse(
                        record.getId(),
                        record.getTruck().getTruckId(),
                        record.getTrip()!=null
                            ? record.getTrip().getId():null,
                        record.getSpeed(),
                        record.getRpm(),
                        record.getFuel(),
                        record.getGear(),
                        record.getTimestamp()
                ))
                .toList();
    }
    public List<TelemetryRecordResponse> getTelemetryByTripId(Long tripId) {
     tripRepository
             .findById(tripId)
             .orElseThrow(()->new TripNotFoundException(tripId));


        return telemetryRecordRepository
                .findByTrip_IdOrderByTimestampAsc(tripId)
                .stream()
                .map(record -> new TelemetryRecordResponse(
                        record.getId(),
                        record.getTruck().getTruckId(),
                        record.getTrip().getId(),
                        record.getSpeed(),
                        record.getRpm(),
                        record.getFuel(),
                        record.getGear(),
                        record.getTimestamp()
                ))
                .toList();

    }

    public List<DrivingEventResponse> getEventsByTripId(Long tripId) {
     tripRepository
             .findById(tripId)
             .orElseThrow(()->new TripNotFoundException(tripId));

     return drivingEventRepository
             .findByTrip_IdOrderByOccurredAtAsc(tripId)
             .stream()
             .map(record -> new DrivingEventResponse(
                     record.getId(),
                     record.getTruck().getTruckId(),
                     record.getTrip().getId(),
                     record.getEventType(),
                     record.getPreviousSpeed(),
                     record.getCurrentSpeed(),
                     record.getSpeedDifference(),
                     record.getDurationMs(),
                     record.getDeceleration(),
                     record.getOccurredAt()
             )).toList();
    }
}
