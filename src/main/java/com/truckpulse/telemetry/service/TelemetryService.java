package com.truckpulse.telemetry.service;

import com.truckpulse.telemetry.concurrency.TruckLockManager;
import com.truckpulse.telemetry.dto.DrivingEventResponse;
import com.truckpulse.telemetry.dto.TelemetryRecordResponse;
import com.truckpulse.telemetry.dto.TelemetryRequest;
import com.truckpulse.telemetry.entity.DrivingEventEntity;
import com.truckpulse.telemetry.entity.TelemetryRecord;
import com.truckpulse.telemetry.entity.Trip;
import com.truckpulse.telemetry.entity.Truck;
import com.truckpulse.telemetry.event.DrivingEventCreatedEvent;
import com.truckpulse.telemetry.event.TelemetryReceivedEvent;
import com.truckpulse.telemetry.exception.OutOfOrderTelemetryException;
import com.truckpulse.telemetry.exception.TripNotFoundException;
import com.truckpulse.telemetry.exception.TruckNotFoundException;
import com.truckpulse.telemetry.kafka.DrivingEventProducer;
import com.truckpulse.telemetry.kafka.TelemetryEventProducer;
import com.truckpulse.telemetry.model.DrivingEventType;
import com.truckpulse.telemetry.model.TelemetrySessionKey;
import com.truckpulse.telemetry.model.TelemetrySnapshot;
import com.truckpulse.telemetry.model.TripStatus;
import com.truckpulse.telemetry.repository.DrivingEventRepository;
import com.truckpulse.telemetry.repository.TelemetryRecordRepository;
import com.truckpulse.telemetry.repository.TripRepository;
import com.truckpulse.telemetry.repository.TruckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TelemetryService {

    private static final double HARSH_BRAKING_THRESHOLD = 4.0;

    private static final Logger log =
            LoggerFactory.getLogger(TelemetryService.class);

    /*
     * Truck'ın en son telemetry durumunu RAM'de tutuyoruz.
     */
    private final Map<String, TelemetrySnapshot> latestTelemetry =
            new ConcurrentHashMap<>();

    /*
     * Her truck + session kombinasyonu için
     * son sequence number'ı tutuyoruz.
     */
    private final Map<TelemetrySessionKey, Long> lastSequences =
            new ConcurrentHashMap<>();

    private final TruckRepository truckRepository;
    private final TripRepository tripRepository;
    private final TelemetryRecordRepository telemetryRecordRepository;
    private final DrivingEventRepository drivingEventRepository;
    private final Clock clock;
    private final TruckLockManager truckLockManager;
    private final TransactionTemplate transactionTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final TelemetryEventProducer telemetryEventProducer;
    private final DrivingEventProducer drivingEventProducer;

    public TelemetryService(
            TruckRepository truckRepository,
            TripRepository tripRepository,
            TelemetryRecordRepository telemetryRecordRepository,
            DrivingEventRepository drivingEventRepository,
            Clock clock,
            TruckLockManager truckLockManager,
            TransactionTemplate transactionTemplate,
            SimpMessagingTemplate messagingTemplate,
            TelemetryEventProducer telemetryEventProducer,
            DrivingEventProducer drivingEventProducer
    ) {
        this.truckRepository = truckRepository;
        this.tripRepository = tripRepository;
        this.telemetryRecordRepository = telemetryRecordRepository;
        this.drivingEventRepository = drivingEventRepository;
        this.clock = clock;
        this.truckLockManager = truckLockManager;
        this.transactionTemplate = transactionTemplate;
        this.messagingTemplate = messagingTemplate;
        this.telemetryEventProducer = telemetryEventProducer;
        this.drivingEventProducer = drivingEventProducer;
    }

    public TelemetryRequest processTelemetryRequest(
            TelemetryRequest telemetryRequest
    ) {

        Object lock = truckLockManager.getLock(
                telemetryRequest.truckId()
        );

        synchronized (lock) {

            /*
             * Aynı truck yeni bir ETS2 session başlattığında
             * sequence number tekrar 1'den başlayabilir.
             */
            TelemetrySessionKey sessionKey =
                    new TelemetrySessionKey(
                            telemetryRequest.truckId(),
                            telemetryRequest.sessionId()
                    );

            Long lastSequence =
                    lastSequences.get(sessionKey);

            /*
             * Duplicate veya out-of-order telemetry kontrolü.
             */
            if (lastSequence != null
                    && telemetryRequest.sequenceNumber() <= lastSequence) {

                throw new OutOfOrderTelemetryException(
                        telemetryRequest.truckId(),
                        telemetryRequest.sessionId(),
                        telemetryRequest.sequenceNumber(),
                        lastSequence
                );
            }

            /*
             * Sequence gap kontrolü.
             *
             * Örneğin:
             * son sequence = 10
             * gelen sequence = 13
             *
             * 11 ve 12 kayıp demektir.
             */
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

            /*
             * Truck'ın RAM'deki önceki telemetry'si.
             */
            TelemetrySnapshot previousForTruck =
                    latestTelemetry.get(
                            telemetryRequest.truckId()
                    );

            /*
             * Harsh braking hesabında sadece aynı session içindeki
             * telemetry değerlerini karşılaştırıyoruz.
             *
             * Yeni session başladıysa previous = null olur.
             */
            TelemetrySnapshot previous =
                    previousForTruck != null
                            && previousForTruck.sessionId()
                            .equals(telemetryRequest.sessionId())
                            ? previousForTruck
                            : null;

            /*
             * Yeni telemetry snapshot.
             */
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
             * DB transaction.
             *
             * Buradan DrivingEventCreatedEvent döndürüyoruz.
             *
             * Eğer harsh braking oluşmadıysa null dönecek.
             */
            DrivingEventCreatedEvent drivingEventCreatedEvent =
                    transactionTemplate.execute(status -> {

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

                        /*
                         * Gelen telemetry'yi DB'ye kaydet.
                         */
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

                        /*
                         * Önceki telemetry yoksa henüz
                         * hız değişimini hesaplayamayız.
                         */
                        if (previous == null) {
                            return null;
                        }

                        double speedDifference =
                                previous.speed()
                                        - current.speed();

                        long milliseconds =
                                Duration.between(
                                        previous.timestamp(),
                                        current.timestamp()
                                ).toMillis();

                        double seconds =
                                milliseconds / 1000.0;

                        /*
                         * Süre pozitif olmalı ve araç yavaşlamış olmalı.
                         */
                        if (seconds <= 0
                                || speedDifference <= 0) {

                            return null;
                        }

                        /*
                         * km/h farkını m/s'ye çeviriyoruz.
                         */
                        double speedDifferenceMs =
                                speedDifference / 3.6;

                        /*
                         * Ortalama negatif ivmenin büyüklüğü.
                         */
                        double deceleration =
                                speedDifferenceMs / seconds;

                        /*
                         * Ani fren eşiği aşılmadıysa
                         * driving event oluşturmuyoruz.
                         */
                        if (deceleration
                                < HARSH_BRAKING_THRESHOLD) {

                            return null;
                        }

                        /*
                         * HARSH_BRAKING oluştu.
                         *
                         * Önce business event'i DB'ye kaydediyoruz.
                         */
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

                        /*
                         * Kafka'ya göndereceğimiz event contract'ını
                         * oluşturuyoruz.
                         *
                         * Burada henüz Kafka'ya publish etmiyoruz.
                         */
                        return new DrivingEventCreatedEvent(
                                UUID.randomUUID(),
                                current.truckId(),
                                DrivingEventType.HARSH_BRAKING,
                                previous.speed(),
                                current.speed(),
                                speedDifference,
                                current.timestamp()
                        );
                    });

            /*
             * transactionTemplate.execute() buraya döndüyse
             * DB transaction başarıyla tamamlandı.
             *
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

            /*
             * Her başarılı telemetry işlemi için
             * TelemetryReceivedEvent oluşturuyoruz.
             */
            TelemetryReceivedEvent telemetryReceivedEvent =
                    new TelemetryReceivedEvent(
                            UUID.randomUUID(),
                            current.truckId(),
                            current.sessionId(),
                            current.speed(),
                            current.rpm(),
                            current.fuel(),
                            current.gear(),
                            current.sequenceNumber(),
                            current.timestamp()
                    );

            /*
             * Telemetry event Kafka publish.
             */
            try {

                telemetryEventProducer.publishTelemetryReceived(
                        telemetryReceivedEvent
                );

            } catch (RuntimeException exception) {

                log.warn(
                        "Could not publish telemetry event for truck {}.",
                        current.truckId(),
                        exception
                );
            }

            /*
             * Eğer transaction sırasında HARSH_BRAKING oluştuysa
             * şimdi Kafka'ya driving event'i yayınlıyoruz.
             *
             * Buraya geldiğimizde DB commit tamamlanmış durumda.
             */
            if (drivingEventCreatedEvent != null) {

                try {

                    drivingEventProducer.publishDrivingEventCreated(
                            drivingEventCreatedEvent
                    );

                } catch (RuntimeException exception) {

                    log.warn(
                            "Could not publish driving event for truck {}.",
                            current.truckId(),
                            exception
                    );
                }
            }

            /*
             * Live dashboard için WebSocket yayını.
             */
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

    public Optional<TelemetrySnapshot> getLatestTelemetry(
            String truckId
    ) {

        return Optional.ofNullable(
                latestTelemetry.get(truckId)
        );
    }

    public List<DrivingEventResponse> getDrivingEvents(
            String truckId
    ) {

        return drivingEventRepository
                .findByTruck_TruckIdOrderByOccurredAtAsc(
                        truckId
                )
                .stream()
                .map(record ->
                        new DrivingEventResponse(
                                record.getId(),
                                record.getTruck().getTruckId(),
                                record.getTrip() != null
                                        ? record.getTrip().getId()
                                        : null,
                                record.getEventType(),
                                record.getPreviousSpeed(),
                                record.getCurrentSpeed(),
                                record.getSpeedDifference(),
                                record.getDurationMs(),
                                record.getDeceleration(),
                                record.getOccurredAt()
                        )
                )
                .toList();
    }

    public List<TelemetryRecordResponse> getTelemetryHistory(
            String truckId
    ) {

        return telemetryRecordRepository
                .findByTruck_TruckIdOrderByTimestampAsc(
                        truckId
                )
                .stream()
                .map(record ->
                        new TelemetryRecordResponse(
                                record.getId(),
                                record.getTruck().getTruckId(),
                                record.getTrip() != null
                                        ? record.getTrip().getId()
                                        : null,
                                record.getSpeed(),
                                record.getRpm(),
                                record.getFuel(),
                                record.getGear(),
                                record.getTimestamp()
                        )
                )
                .toList();
    }

    public List<TelemetryRecordResponse> getTelemetryByTripId(
            Long tripId
    ) {

        tripRepository
                .findById(tripId)
                .orElseThrow(() ->
                        new TripNotFoundException(
                                tripId
                        )
                );

        return telemetryRecordRepository
                .findByTrip_IdOrderByTimestampAsc(
                        tripId
                )
                .stream()
                .map(record ->
                        new TelemetryRecordResponse(
                                record.getId(),
                                record.getTruck().getTruckId(),
                                record.getTrip().getId(),
                                record.getSpeed(),
                                record.getRpm(),
                                record.getFuel(),
                                record.getGear(),
                                record.getTimestamp()
                        )
                )
                .toList();
    }

    public List<DrivingEventResponse> getEventsByTripId(
            Long tripId
    ) {

        tripRepository
                .findById(tripId)
                .orElseThrow(() ->
                        new TripNotFoundException(
                                tripId
                        )
                );

        return drivingEventRepository
                .findByTrip_IdOrderByOccurredAtAsc(
                        tripId
                )
                .stream()
                .map(record ->
                        new DrivingEventResponse(
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
                        )
                )
                .toList();
    }
}