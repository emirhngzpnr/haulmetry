package com.truckpulse.telemetry.service;

import com.truckpulse.telemetry.dto.DrivingEventResponse;
import com.truckpulse.telemetry.dto.TelemetryRecordResponse;
import com.truckpulse.telemetry.dto.TelemetryRequest;
import com.truckpulse.telemetry.entity.DrivingEventEntity;
import com.truckpulse.telemetry.entity.TelemetryRecord;
import com.truckpulse.telemetry.entity.Trip;
import com.truckpulse.telemetry.entity.Truck;
import com.truckpulse.telemetry.exception.TruckNotFoundException;
import com.truckpulse.telemetry.model.DrivingEventType;
import com.truckpulse.telemetry.model.TelemetrySnapshot;
import com.truckpulse.telemetry.model.TripStatus;
import com.truckpulse.telemetry.repository.DrivingEventRepository;
import com.truckpulse.telemetry.repository.TelemetryRecordRepository;
import com.truckpulse.telemetry.repository.TripRepository;
import com.truckpulse.telemetry.repository.TruckRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class TelemetryService {
    // ani fren değeri (ivme)
    private static final double HARSH_BRAKING_THRESHOLD = 4.0;
    // truck - speed eşleştirmesi için Map tanımlarız. Key - Value değeri olarak tutmamız O(1) zaman karmaşıklığını elde etmemizi sağlar
//    Map<String, TelemetryRequest> latestTelemetry = new HashMap<>();
    private final  Map<String, TelemetrySnapshot> latestTelemetry = new HashMap<>();


 private final TruckRepository truckRepository;
 private final TripRepository tripRepository;
 private final TelemetryRecordRepository telemetryRecordRepository;
 private final DrivingEventRepository drivingEventRepository;

 public TelemetryService(TruckRepository truckRepository,
                         TripRepository tripRepository ,
                         TelemetryRecordRepository telemetryRecordRepository,
                         DrivingEventRepository drivingEventRepository
 ) {
     this.truckRepository = truckRepository;
     this.tripRepository = tripRepository;
     this.telemetryRecordRepository = telemetryRecordRepository;
     this.drivingEventRepository = drivingEventRepository;
 }

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
//        if(telemetryRequest.speed()<0 || telemetryRequest.fuel()<0
//                || telemetryRequest.gear()<0 || telemetryRequest.rpm()<0 ){
//            throw  new IllegalArgumentException("Invalid telemetry data");
//        }
        TelemetrySnapshot current = new TelemetrySnapshot(
                telemetryRequest.truckId(),
                telemetryRequest.speed(),
                telemetryRequest.rpm(),
                telemetryRequest.fuel(),
                telemetryRequest.gear(),
                Instant.now()
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
           TelemetrySnapshot previous = latestTelemetry.put(
                   current.truckId(),
                   current
           );
// Hız farkı ifadesini tekrar etmemek ve kodu daha okunabilir
// hale getirmek için sonucu bir değişkende tutuyoruz.
        // fakat bu işlemi null kontrolünden önce gerçekleştirirsek previous = null -> previous.speed() -> NullPointerException hatası alırız
         // bu nedenle değişkene atamadan önce null kontrolü gerçekleştirmemiz gerekir.
    //    if(previous != null && previous.speed() - telemetryRequest.speed()>=30) {
//
//           DrivingEvent drivingEvent = new DrivingEvent(telemetryRequest.truckId(),
//                   DrivingEventType.HARSH_BRAKING,
//                   previous.speed(),
//                   telemetryRequest.speed(),
//                   previous.speed() - telemetryRequest.speed());
//         System.out.println("HARSH BRAKING!!!");
//           System.out.println(drivingEvent);
//       }

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
}
