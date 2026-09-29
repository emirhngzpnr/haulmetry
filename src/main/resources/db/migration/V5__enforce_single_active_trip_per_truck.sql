CREATE UNIQUE INDEX ux_trips_one_active_per_truck
    ON trips (truck_id)
    WHERE status = 'ACTIVE';