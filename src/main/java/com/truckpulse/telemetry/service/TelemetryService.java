package com.truckpulse.telemetry.service;

import com.truckpulse.telemetry.dto.TelemetryRequest;
import com.truckpulse.telemetry.model.DrivingEvent;
import com.truckpulse.telemetry.model.DrivingEventType;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TelemetryService {

    // truck - speed eşleştirmesi için HashMap tanımlarız. Key - Value değeri olarak tutmamız O(1) zaman karmaşıklığını elde etmemizi sağlar
    Map<String, TelemetryRequest> latestTelemetry = new HashMap<>();

    // truck'a ait event geçmişini saklamak istiyoruz, hashmap ile truckId üzerinden ArrayList'e erişebilmek mantıklı
    Map<String, List<DrivingEvent>> drivingEvents = new HashMap<>();

    public TelemetryRequest processTelemetryRequest(TelemetryRequest telemetryRequest
    ) {
         // eğer Validasyon kullanmayıp bu şekilde kontrol sağlarsak 500 ınternal server hatası alırız.
        // Validasyon kullandığımız zaman ise 400 bad request alırız ki bu daha sağlıklı olan yoldur.
//        if(telemetryRequest.speed()<0 || telemetryRequest.fuel()<0
//                || telemetryRequest.gear()<0 || telemetryRequest.rpm()<0 ){
//            throw  new IllegalArgumentException("Invalid telemetry data");
//        }

           TelemetryRequest previous=  latestTelemetry.put(
                    telemetryRequest.truckId(), // key
                    telemetryRequest           // value
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
    double speedDifference= previous.speed()-telemetryRequest.speed();
    if(speedDifference>=30){

        // event oluşturma
        DrivingEvent drivingEvent = new DrivingEvent(
                telemetryRequest.truckId(),
                DrivingEventType.HARSH_BRAKING,
                previous.speed(),
                telemetryRequest.speed(),
                speedDifference

        );
        System.out.println(drivingEvent);
       // truckId için liste yoksa liste oluşturma
//        if(!drivingEvents.containsKey(telemetryRequest.truckId())) {
//            drivingEvents.put(telemetryRequest.truckId(),
//                            new ArrayList<>());
//        }
//
//        // listeyi get ile al ve add ile eventi ekle
//        drivingEvents.get(telemetryRequest.truckId())
//                        .add(drivingEvent);

        // daha modern yöntem clean code -> computeIfAbsent() kullanmak
        drivingEvents
                .computeIfAbsent(
                        telemetryRequest.truckId(),
                        key -> new ArrayList<>()
                )
                .add(drivingEvent);

    }
}
            return telemetryRequest;
    }

    public Optional<TelemetryRequest> getLatestTelemetry(String truckId) {
        return Optional.ofNullable(latestTelemetry.get(truckId)); // aranan keye ait value olmayabilir bu durumda null değer döndürebilirl bunun önüne geçmek için Optional kullanırız.

        }
        public List<DrivingEvent> getDrivingEvents(String truckId) {
           return  drivingEvents.getOrDefault(truckId,List.of());
        }

}
