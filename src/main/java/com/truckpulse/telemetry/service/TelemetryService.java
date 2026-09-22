package com.truckpulse.telemetry.service;

import com.truckpulse.telemetry.dto.TelemetryRequest;
import com.truckpulse.telemetry.model.DrivingEvent;
import com.truckpulse.telemetry.model.DrivingEventType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Optional;

@Service
public class TelemetryService {

    // truck - speed eşleştirmesi için HashMap tanımlarız. Key - Value değeri olarak tutmamız O(1) zaman karmaşıklığını elde etmemizi sağlar
    HashMap<String, TelemetryRequest> latestTelemetry = new HashMap<>();

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
// bu if bloğunda iki kere current speed hesaplanıyor bu da clean code a pek uymuyor o yüzden current speed tek bir değişkende tutulabilir.
        // fakat bu işlemi null kontrolünden önce gerçekleştirirsek previous = null -> previous.speed() -> NullPointerException hatası alırız
         // bu nedenle değişkene atamadan önce null kontrolü gerçekleştirmemiz gerekir.
//       if(previous != null && previous.speed() - telemetryRequest.speed()>=30) {
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
        DrivingEvent drivingEvent = new DrivingEvent(
                telemetryRequest.truckId(),
                DrivingEventType.HARSH_BRAKING,
                previous.speed(),
                telemetryRequest.speed(),
                speedDifference

        );
        System.out.println(drivingEvent);
    }
}
            return telemetryRequest;
    }

    public Optional<TelemetryRequest> getLatestTelemetry(String truckId) {
        return Optional.ofNullable(latestTelemetry.get(truckId)); // aranan keye ait value olmayabilir bu durumda null değer döndürebilirl bunun önüne geçmek için Optional kullanırız.

    }

}
