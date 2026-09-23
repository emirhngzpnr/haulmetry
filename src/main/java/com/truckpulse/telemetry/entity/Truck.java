package com.truckpulse.telemetry.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "trucks")
public class Truck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String truckId;

    private String model;

    protected Truck() {
    }

    public Truck(String truckId, String model) {
        this.truckId = truckId;
        this.model = model;
    }

    public Long getId() {
        return id;
    }

    public String getTruckId() {
        return truckId;
    }

    public String getModel() {
        return model;
    }
}
