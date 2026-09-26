package com.truckpulse.telemetry.entity;

import com.truckpulse.telemetry.model.DrivingEventType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "driving_events")
public class DrivingEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_id",nullable = false)
    private Truck truck;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;
    @Enumerated(EnumType.STRING)
    private DrivingEventType eventType;
    private double previousSpeed;
    private double currentSpeed;
    private double speedDifference;
    private long durationMs;
    private double deceleration;
    private Instant  occurredAt;

    protected DrivingEventEntity() {}

    public DrivingEventEntity(Truck truck, Trip trip, DrivingEventType eventType, double previousSpeed, double currentSpeed, double speedDifference, long durationMs, double deceleration, Instant  occurredAt) {
        this.truck = truck;
        this.trip = trip;
        this.eventType = eventType;
        this.previousSpeed = previousSpeed;
        this.currentSpeed = currentSpeed;
        this.speedDifference = speedDifference;
        this.durationMs = durationMs;
        this.deceleration = deceleration;
        this. occurredAt =  occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Truck getTruck() {
        return truck;
    }

    public Trip getTrip() {
        return trip;
    }

    public DrivingEventType getEventType() {
        return eventType;
    }

    public double getPreviousSpeed() {
        return previousSpeed;
    }

    public double getCurrentSpeed() {
        return currentSpeed;
    }

    public double getSpeedDifference() {
        return speedDifference;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public double getDeceleration() {
        return deceleration;
    }

    public Instant getOccurredAt() {
        return  occurredAt;
    }
}
