package com.truckpulse.telemetry.service;

import com.truckpulse.telemetry.concurrency.TruckLockManager;
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
import com.truckpulse.telemetry.model.TelemetrySessionKey;
import com.truckpulse.telemetry.model.TelemetrySnapshot;
import com.truckpulse.telemetry.model.TripStatus;
import com.truckpulse.telemetry.repository.DrivingEventRepository;
import com.truckpulse.telemetry.repository.TelemetryRecordRepository;
import com.truckpulse.telemetry.repository.TripRepository;
import com.truckpulse.telemetry.repository.TruckRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

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

    private final Map<TelemetrySessionKey, Long> lastSequences =
            new ConcurrentHashMap<>();

    // kayıp sequencesNumber'ı izleyebilmek amacıyla kullanılıyor
    private static final Logger log =
            LoggerFactory.getLogger(TelemetryService.class);

 private final TruckRepository truckRepository;
 private final TripRepository tripRepository;
 private final TelemetryRecordRepository telemetryRecordRepository;
 private final DrivingEventRepository drivingEventRepository;
 private final Clock clock;
 private final TruckLockManager truckLockManager;
 private final TransactionTemplate transactionTemplate;
 private final SimpMessagingTemplate messagingTemplate;

 public TelemetryService(TruckRepository truckRepository,
                         TripRepository tripRepository ,
                         TelemetryRecordRepository telemetryRecordRepository,
                         DrivingEventRepository drivingEventRepository,
                         Clock clock,
                         TruckLockManager truckLockManager,
                         TransactionTemplate transactionTemplate,
                         SimpMessagingTemplate messagingTemplate
 ) {
     this.truckRepository = truckRepository;
     this.tripRepository = tripRepository;
     this.telemetryRecordRepository = telemetryRecordRepository;
     this.drivingEventRepository = drivingEventRepository;
     this.clock = clock;
     this.truckLockManager = truckLockManager;
     this.transactionTemplate = transactionTemplate;
     this.messagingTemplate = messagingTemplate;
 }

    public TelemetryRequest processTelemetryRequest(
            TelemetryRequest telemetryRequest
    ) {

        Object lock = truckLockManager.getLock(
                telemetryRequest.truckId()
        );

        synchronized (lock) {

            TelemetrySessionKey sessionKey =
                    new TelemetrySessionKey(
                            telemetryRequest.truckId(),
                            telemetryRequest.sessionId()
                    );

            Long lastSequence =
                    lastSequences.get(sessionKey);

            // Duplicate veya out-of-order telemetry kontrolü
            if (lastSequence != null
                    && telemetryRequest.sequenceNumber() <= lastSequence) {

                throw new OutOfOrderTelemetryException(
                        telemetryRequest.truckId(),
                        telemetryRequest.sessionId(),
                        telemetryRequest.sequenceNumber(),
                        lastSequence
                );
            }

            // Sequence gap kontrolü
            if (lastSequence != null
                    && telemetryRequest.sequenceNumber() > lastSequence + 1) {

                long missingCount =
                        telemetryRequest.sequenceNumber()
                                - lastSequence
                                - 1;

                log.warn(
                        "Telemetry sequence gap detected for truck {}, session {}. "
                                + "Last sequence: {}, incoming sequence: {}, missing count: {}",
                        telemetryRequest.truckId(),
                        telemetryRequest.sessionId(),
                        lastSequence,
                        telemetryRequest.sequenceNumber(),
                        missingCount
                );
            }
            // RAM'deki önceki telemetry
            TelemetrySnapshot previousForTruck =
                    latestTelemetry.get(
                            telemetryRequest.truckId()
                    );

            TelemetrySnapshot previous =
                    previousForTruck != null
                            && previousForTruck.sessionId()
                            .equals(telemetryRequest.sessionId())
                            ? previousForTruck
                            : null;

            // Şu an gelen telemetry
            TelemetrySnapshot current =
                    new TelemetrySnapshot(
                            telemetryRequest.truckId(),
                            telemetryRequest.sessionId(),
                            telemetryRequest.speed(),
                            telemetryRequest.rpm(),
                            telemetryRequest.fuel(),
                            telemetryRequest.gear(),
                            telemetryRequest.sequenceNumber(),
                            Instant.now(clock)
                    );

            /*
             * Buradan itibaren transaction başlıyor.
             * DB ile ilgili işlemler bu blokta.
             */
            transactionTemplate.executeWithoutResult(status -> {

                Truck truck = truckRepository
                        .findByTruckId(
                                telemetryRequest.truckId()
                        )
                        .orElseThrow(() ->
                                new TruckNotFoundException(
                                        telemetryRequest.truckId()
                                )
                        );

                Trip activeTrip = tripRepository
                        .findByTruck_TruckIdAndStatus(
                                telemetryRequest.truckId(),
                                TripStatus.ACTIVE
                        )
                        .orElse(null);

                TelemetryRecord telemetryRecord =
                        new TelemetryRecord(
                                truck,
                                activeTrip,
                                current.speed(),
                                current.rpm(),
                                current.fuel(),
                                current.gear(),
                                current.timestamp()
                        );

                telemetryRecordRepository.save(
                        telemetryRecord
                );

                // Önceki telemetry varsa ani fren kontrolü yap
                if (previous != null) {

                    double speedDifference =
                            previous.speed()
                                    - current.speed();

                    long milliseconds = Duration
                            .between(
                                    previous.timestamp(),
                                    current.timestamp()
                            )
                            .toMillis();

                    double seconds =
                            milliseconds / 1000.0;

                    if (seconds > 0
                            && speedDifference > 0) {

                        double speedDifferenceMs =
                                speedDifference / 3.6;

                        double deceleration =
                                speedDifferenceMs / seconds;

                        if (deceleration
                                >= HARSH_BRAKING_THRESHOLD) {

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

                            drivingEventRepository.save(
                                    drivingEventEntity
                            );
                        }
                    }
                }
            });

            /*
             * Buraya geldiysek transaction başarıyla tamamlandı.
             * Şimdi RAM state'ini güncelliyoruz.
             */

            latestTelemetry.put(
                    current.truckId(),
                    current
            );

            lastSequences.put(
                    sessionKey,
                    telemetryRequest.sequenceNumber()
            );

            try {

                messagingTemplate.convertAndSend(
                        "/topic/telemetry",
                        current
                );

            } catch (RuntimeException exception) {

                log.warn(
                        "Could not publish live telemetry for truck {}.",
                        current.truckId(),
                        exception
                );
            }

            return telemetryRequest;
        }
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
