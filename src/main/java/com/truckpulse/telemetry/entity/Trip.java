package com.truckpulse.telemetry.entity;

import com.truckpulse.telemetry.model.TripStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "trips")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_id",nullable = false)
    private Truck truck;
    private Instant startedAt;
    private Instant endedAt;
    @Enumerated(EnumType.STRING)
    private TripStatus status;
    protected Trip() {}
    public Trip(Truck truck, Instant startedAt,  TripStatus status) {
        this.truck = truck;
        this.startedAt = startedAt;

        this.status = status;

    }

    public Long getId() {
        return id;
    }

    public Truck getTruck() {
        return truck;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public TripStatus getStatus() {
        return status;
    }
    // Domain davranışını entity içinde tutarak trip'in tamamlanma durumunu tutarlı yönetiyoruz.
    public void complete(Instant endedAt) {
        this.endedAt = endedAt;
        this.status = TripStatus.COMPLETED;
    }
}
