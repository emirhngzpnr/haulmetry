package com.truckpulse.telemetry.service;

import com.truckpulse.telemetry.dto.TripResponse;
import com.truckpulse.telemetry.entity.Trip;
import com.truckpulse.telemetry.entity.Truck;
import com.truckpulse.telemetry.exception.ActiveTripAlreadyExistsException;
import com.truckpulse.telemetry.exception.TripAlreadyCompletedException;
import com.truckpulse.telemetry.exception.TripNotFoundException;
import com.truckpulse.telemetry.exception.TruckNotFoundException;
import com.truckpulse.telemetry.model.TripStatus;
import com.truckpulse.telemetry.repository.TripRepository;
import com.truckpulse.telemetry.repository.TruckRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TripService {
    private final TripRepository tripRepository;
    private final TruckRepository truckRepository;

    public TripService(TripRepository tripRepository, TruckRepository truckRepository) {
        this.tripRepository = tripRepository;
        this.truckRepository = truckRepository;
    }
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
Trip savedTrip = tripRepository.save(trip);
return new TripResponse(
        savedTrip.getId(),
        savedTrip.getTruck().getTruckId(),
        savedTrip.getStartedAt(),
        savedTrip.getEndedAt(),
        savedTrip.getStatus()
);
    }

    public TripResponse completeTrip(Long tripId) {
        Trip trip=tripRepository
                .findById(tripId)
                .orElseThrow(()
                                -> new TripNotFoundException(tripId));
                if(trip.getStatus()==TripStatus.COMPLETED) {

                   throw new TripAlreadyCompletedException(tripId);

                }
                trip.complete(Instant.now());
                Trip savedTrip= tripRepository.save(trip);
                return new TripResponse(
                        savedTrip.getId(),
                        savedTrip.getTruck().getTruckId(),
                        savedTrip.getStartedAt(),
                        savedTrip.getEndedAt(),
                        savedTrip.getStatus()
                );
    }
}
