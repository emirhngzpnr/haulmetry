package com.truckpulse.telemetry.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "telemetry_records")
public class TelemetryRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // birden çoğa ilişki kurarız. Bir aracın birden fazla ölçüm olabilir.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_id",nullable = false)
    private Truck truck;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;
    private double speed;
    private int rpm;
    private double fuel;
    private int gear;
    private Instant timestamp;

    protected TelemetryRecord() {}
    public TelemetryRecord(Truck truck,Trip trip ,double speed, int rpm, double fuel, int gear, Instant timestamp) {
    this.truck = truck;
    this.trip = trip;
    this.speed = speed;
    this.rpm = rpm;
    this.fuel = fuel;
    this.gear = gear;
    this.timestamp = timestamp;

    }

    public Truck getTruck() {
        return truck;
    }
    public Trip getTrip() {return trip;}

    public Long getId() {
        return id;
    }

    public double getSpeed() {
        return speed;
    }

    public int getRpm() {
        return rpm;
    }

    public double getFuel() {
        return fuel;
    }

    public int getGear() {
        return gear;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
