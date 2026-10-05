# Haulmetry

Haulmetry is a backend engineering project focused on **real-time vehicle telemetry processing, trip management, driver-behavior analysis, concurrency, and reliability**.

The project uses **Euro Truck Simulator 2 (ETS2)** as a realistic telemetry producer during development, while keeping the Java backend independent from the simulator itself. ETS2 is treated as a replaceable data source rather than as the product.

The long-term goal is to evolve Haulmetry into a small but realistic fleet telemetry platform capable of receiving, validating, ordering, persisting, analyzing, and eventually streaming vehicle data in real time.

---

## Project Goal

Vehicle and fleet systems continuously receive data such as:

- speed
- engine RPM
- fuel level
- gear state
- location
- engine state
- braking behavior
- acceleration behavior
- trip state

Haulmetry is being built around the engineering problems created by that continuous data flow.

The project aims to answer questions such as:

- What is the latest known state of a vehicle?
- What telemetry belongs to a specific trip?
- Did a harsh-braking event occur?
- How should duplicate or out-of-order telemetry be handled?
- How can two requests for the same vehicle be processed safely?
- How can telemetry persistence and event creation remain atomic?
- How should live telemetry eventually be streamed to clients?
- How should the architecture evolve when throughput and reliability requirements increase?

Haulmetry is therefore intentionally more than a CRUD application.

---

## Development Approach

Haulmetry is developed **incrementally and problem-first**.

Technologies are not added simply to make the architecture look more complex. Each new component is introduced when the project reaches an engineering problem that justifies it.

The development approach is:

```text
Build the simplest working version
            |
            v
Understand its limitations
            |
            v
Introduce the next engineering requirement
            |
            v
Choose the technology or design that solves it
            |
            v
Refactor and evolve the architecture
```

Examples of this approach include:

- telemetry was first accepted through HTTP before integrating ETS2
- latest vehicle state was initially stored in memory
- PostgreSQL was introduced when durable history became necessary
- Flyway was introduced to manage schema evolution
- concurrency problems were reproduced before introducing per-truck locking
- transaction boundaries were refined after rollback and consistency problems were examined
- sequence numbers were introduced when telemetry ordering became a real concern
- Kafka, Redis, WebSocket, and other infrastructure are intentionally deferred until they solve concrete problems

---

## Current Development Stage

**Phase 3 — Concurrency, Data Integrity and Reliability is complete.**

The project is currently in:

> **Phase 4 — ETS2 Integration**

The Spring Boot backend already supports:

- persistent telemetry
- persistent trips
- persistent driving events
- telemetry sequence validation
- duplicate and out-of-order detection
- per-truck concurrency control
- transaction boundaries
- harsh-braking detection using deceleration
- deterministic time-dependent telemetry logic
- trip summaries
- reliability testing for critical race conditions

The native ETS2 integration has also reached the point where:

- the C++ telemetry bridge can send test telemetry to the backend
- backend HTTP responses are handled by the bridge
- sequence numbers are supported by the bridge
- the SCS Telemetry SDK plugin loads successfully inside ETS2
- live speed, RPM, fuel, and gear values can be read directly from the game

The next milestone is to map live SCS telemetry into the bridge telemetry model and forward the real game stream to the Spring Boot backend.

---

## Current Backend Flow

```text
Telemetry Producer
        |
        v
Spring Boot REST API
        |
        v
Request Validation
        |
        v
Per-Truck Lock
        |
        v
Sequence Validation
        |
        v
Database Transaction
        |
        +---------------------------+
        |                           |
        v                           v
TelemetryRecord              DrivingEvent
        |                           |
        +-------------+-------------+
                      |
                    COMMIT
                      |
                      v
             In-Memory Latest State
```

For telemetry belonging to the same truck, processing is serialized so that:

- sequence checks
- previous/current telemetry comparison
- trip association
- persistence
- event generation
- in-memory state updates

remain consistent.

Different trucks can still be processed independently.

---

## Current ETS2 Integration Flow

```text
Euro Truck Simulator 2
          |
          v
SCS Telemetry SDK
          |
          v
Native C++ Plugin
          |
          v
Live Telemetry
(speed / RPM / fuel / gear)
          |
          v
[ next: bridge mapping + HTTP forwarding ]
          |
          v
Haulmetry Backend
```

The SCS plugin is successfully loaded by ETS2 and currently receives real telemetry values from the game.

---

## Implemented Capabilities

### Telemetry

- Receive telemetry through REST
- Validate incoming telemetry
- Persist telemetry records in PostgreSQL
- Maintain the latest telemetry snapshot per truck in memory
- Retrieve latest telemetry by truck ID
- Retrieve telemetry history by truck
- Retrieve telemetry belonging to a trip
- Associate telemetry with an active trip when one exists
- Accept telemetry even when no trip is active

### Ordering and Reliability

- Require a source sequence number
- Reject duplicate telemetry
- Reject out-of-order telemetry
- Return `409 Conflict` for telemetry ordering conflicts
- Detect gaps in telemetry sequences
- Log missing-sequence gaps
- Update in-memory telemetry state only after successful database commit

### Trip Management

- Register trucks
- Start trips
- Complete trips
- Prevent multiple active trips for the same truck
- Retrieve trip history by truck
- Retrieve telemetry by trip
- Retrieve driving events by trip
- Generate trip summaries

### Driver Events

- Compare previous and current telemetry
- Calculate elapsed time between telemetry snapshots
- Calculate deceleration
- Detect harsh braking
- Persist harsh-braking events
- Associate driving events with an active trip when available

### Concurrency and Transactions

- Thread-safe latest-state storage
- Shared per-truck locking
- Independent processing for different trucks
- Coordinated telemetry processing and trip completion
- Atomic telemetry-record and driving-event persistence
- Database-level protection against duplicate active trips

---

## Telemetry Contract

A telemetry request currently contains:

```json
{
  "truckId": "SCANIA-S730",
  "speed": 82.5,
  "rpm": 1450,
  "fuel": 312.4,
  "gear": 10,
  "sequenceNumber": 42
}
```

### Fields

| Field | Type | Description |
|---|---|---|
| `truckId` | string | Unique logical truck identifier |
| `speed` | double | Vehicle speed in km/h |
| `rpm` | integer | Engine RPM |
| `fuel` | double | Current fuel level |
| `gear` | integer | Current gear |
| `sequenceNumber` | long | Monotonically increasing source sequence |

The backend expects sequence numbers to increase monotonically for each truck.

If an incoming sequence number is less than or equal to the last accepted sequence, the telemetry request is rejected with:

```text
409 Conflict
```

A gap in the sequence is accepted but logged.

Example:

```text
Last sequence:     42
Incoming sequence: 45

Missing:
43
44
```

This makes packet loss or missing telemetry observable without stopping the telemetry stream.

---

## Harsh-Braking Detection

Harsh braking is calculated using both speed difference and elapsed time.

The backend compares two telemetry snapshots:

```text
Previous telemetry
        |
        v
Current telemetry
```

The calculation is conceptually:

```text
speed difference (km/h)
        |
        v
convert to m/s
        |
        v
divide by elapsed seconds
        |
        v
deceleration (m/s²)
```

The current harsh-braking threshold is:

```text
4.0 m/s²
```

When calculated deceleration reaches or exceeds that threshold, the backend creates a persistent:

```text
HARSH_BRAKING
```

event.

The event stores information such as:

- previous speed
- current speed
- speed difference
- elapsed duration
- calculated deceleration
- occurrence timestamp
- truck
- associated trip when one exists

Time-dependent telemetry logic uses an injected `Clock`, which allows deterministic testing.

---

## Current API

### Trucks

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/trucks` | Register a truck |
| GET | `/api/trucks` | List trucks |
| GET | `/api/trucks/{truckId}` | Get a truck |
| GET | `/api/trucks/{truckId}/trips` | Get trips for a truck |

### Telemetry

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/telemetry` | Submit telemetry |
| GET | `/api/telemetry/{truckId}` | Get latest telemetry |
| GET | `/api/telemetry/{truckId}/history` | Get telemetry history |
| GET | `/api/events/{truckId}` | Get driving events for a truck |

### Trips

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/trips/start/{truckId}` | Start a trip |
| PUT | `/api/trips/{tripId}/complete` | Complete a trip |
| GET | `/api/trips/{tripId}/telemetry` | Get trip telemetry |
| GET | `/api/trips/{tripId}/events` | Get trip driving events |
| GET | `/api/trips/{tripId}/summary` | Get trip summary |

Important HTTP responses include:

```text
200 OK
400 Bad Request
404 Not Found
409 Conflict
```

Examples of `409 Conflict` scenarios include:

- an active trip already exists for the truck
- a completed trip is completed again
- duplicate telemetry is received
- out-of-order telemetry is received

---

## Persistence Model

Haulmetry currently persists four main concepts.

### Truck

Represents a registered vehicle.

A truck has its own logical:

```text
truckId
```

which is used throughout telemetry and trip processing.

### Trip

Represents a vehicle trip.

A trip contains:

- associated truck
- start time
- end time
- status

Current trip states are:

```text
ACTIVE
COMPLETED
```

### TelemetryRecord

Represents a persisted telemetry snapshot.

A telemetry record contains values including:

- speed
- RPM
- fuel
- gear
- timestamp
- truck
- optional trip association

Telemetry can exist without an active trip.

When an active trip exists, the telemetry record is associated with that trip.

### DrivingEvent

Represents an event derived from telemetry processing.

The first implemented event type is:

```text
HARSH_BRAKING
```

A driving event can also be associated with the trip that was active when it occurred.

---

## Data Integrity

PostgreSQL is used as the durable data store.

Flyway manages database schema migrations.

The database also participates directly in enforcing important business invariants.

For example, Haulmetry prevents more than one active trip for the same truck with a PostgreSQL partial unique index:

```sql
CREATE UNIQUE INDEX ux_trips_one_active_per_truck
ON trips (truck_id)
WHERE status = 'ACTIVE';
```

The application also checks whether a truck already has an active trip.

However, an application-level check alone is not enough:

```text
Request A -> no ACTIVE trip found
Request B -> no ACTIVE trip found

Request A -> INSERT
Request B -> INSERT
```

Without database protection, both requests could succeed during a race condition.

The unique index protects the invariant at the database level.

The application catches the resulting constraint violation and converts it into:

```text
409 Conflict
```

---

## Concurrency Model

Telemetry processing depends on several values that belong together:

```text
last sequence
previous telemetry
current telemetry
active trip
database persistence
driving-event detection
latest in-memory state
```

Processing these independently could create race conditions.

Haulmetry therefore uses a shared:

```text
TruckLockManager
```

to coordinate operations per truck.

Conceptually:

```text
TRUCK-A telemetry ----+
                      |
TRUCK-A telemetry ----+--> same lock --> serialized


TRUCK-B telemetry --------> different lock --> independent
```

This avoids one global lock for the whole application.

Requests involving different trucks do not unnecessarily block each other.

---

## Telemetry and Trip Coordination

The same per-truck lock is shared between telemetry processing and trip completion.

This prevents a telemetry request and trip completion for the same truck from crossing each other at an unsafe point.

For example:

```text
Telemetry begins
        |
        v
Reads ACTIVE trip
        |
        |
Trip completes concurrently
        |
        v
Telemetry persists with inconsistent state
```

The shared lock prevents this interleaving.

The intended ordering is:

```text
LOCK
  |
  v
TRANSACTION
  |
  v
DATABASE WORK
  |
  v
COMMIT
  |
  v
UPDATE IN-MEMORY STATE
  |
  v
UNLOCK
```

This ordering ensures that the transaction is committed before another operation for the same truck obtains the lock.

---

## Transaction Model

Telemetry processing uses `TransactionTemplate` inside the per-truck critical section.

Inside one telemetry transaction, the backend:

1. resolves the truck
2. resolves the active trip if one exists
3. persists the telemetry record
4. compares previous and current telemetry
5. calculates deceleration
6. creates and persists a driving event when necessary

The telemetry record and generated driving event therefore participate in the same transaction.

If event persistence fails, telemetry persistence is rolled back as well.

Only after the database transaction completes successfully are these in-memory values updated:

```text
latestTelemetry
lastSequences
```

This prevents a situation where:

```text
database transaction -> rollback

but

RAM state -> already advanced
```

Trip completion follows the same lock-before-transaction principle.

---

## Sequence Handling

Each telemetry producer sends a monotonically increasing sequence number.

Example:

```text
1
2
3
4
5
```

The backend remembers the last accepted sequence for each truck.

### Duplicate

```text
Last sequence: 5
Incoming:      5
```

Result:

```text
409 Conflict
```

### Out of Order

```text
Last sequence: 5
Incoming:      4
```

Result:

```text
409 Conflict
```

### Gap

```text
Last sequence: 5
Incoming:      8
```

The request is accepted but the backend logs:

```text
Missing:
6
7
```

This allows Haulmetry to identify telemetry quality problems without stopping the stream.

---

## Deterministic Time Handling

Telemetry analytics depend on time.

Using:

```java
Instant.now()
```

directly inside telemetry processing would make precise tests harder because the test does not control time.

Haulmetry therefore injects:

```java
Clock
```

into time-dependent telemetry logic.

This allows tests to use a fixed or controlled clock.

For example:

```text
Telemetry A:
100 km/h
12:00:00

Telemetry B:
60 km/h
12:00:02
```

The elapsed time is deterministic:

```text
2 seconds
```

and the resulting deceleration can be compared against an exact expected value.

---

## Trip Summary

Haulmetry exposes aggregated information for a trip.

Endpoint:

```http
GET /api/trips/{tripId}/summary
```

The current trip summary includes:

- trip ID
- truck ID
- start time
- end time
- trip duration
- average speed
- maximum speed
- telemetry-record count
- harsh-braking count

This is the first analytics-oriented feature built on top of persisted trip telemetry.

---

## ETS2 Integration

Haulmetry uses Euro Truck Simulator 2 as a realistic telemetry source.

The goal is not to build an ETS2-specific backend.

Instead:

```text
ETS2 = telemetry producer
Haulmetry = telemetry platform
```

The backend is intentionally designed so that the simulator can later be replaced by another telemetry producer.

---

## Native Telemetry Bridge

A separate native C++ component is being developed for ETS2 integration.

The bridge currently supports:

- HTTP telemetry submission
- JSON telemetry serialization
- backend response handling
- telemetry sequence numbers
- communication with the Spring Boot backend

The bridge has already successfully sent simulated telemetry to:

```http
POST /api/telemetry
```

including sequence numbers.

Duplicate sequence tests also successfully produced backend:

```text
409 Conflict
```

responses.

---

## SCS Telemetry SDK

The ETS2 plugin uses the official SCS Telemetry SDK.

The native plugin is compiled as an x64 DLL and loaded by ETS2 from:

```text
Euro Truck Simulator 2
└── bin
    └── win_x64
        └── plugins
            └── haulmetry-scs-plugin.dll
```

The plugin exports:

```text
scs_telemetry_init
scs_telemetry_shutdown
```

ETS2 successfully loads the plugin and calls:

```text
scs_telemetry_init()
```

during startup.

---

## Live ETS2 Telemetry

Live telemetry has now been successfully captured from ETS2.

Current channels include:

```text
speed
engine RPM
fuel
gear
```

Example telemetry observed from the game:

```text
Speed: 54.2 km/h
RPM: 1122
Fuel: 448.9 L
Gear: 12
```

and:

```text
Speed: 100.6 km/h
RPM: 1269
Fuel: 448.3 L
Gear: 14
```

Reverse motion is also observable through values such as:

```text
Speed: -7.0 km/h
Gear: -1
```

This reveals an integration detail that must be handled when mapping SCS telemetry into the backend model, because the backend currently treats speed as a non-negative magnitude.

The next integration step is:

```text
SCS LiveTelemetry
       |
       v
Bridge TelemetryData
       |
       v
HTTP POST
       |
       v
Spring Boot
       |
       v
PostgreSQL
```

---

## Technology Stack

### Backend

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Jakarta Bean Validation
- Spring Data JPA
- PostgreSQL
- Flyway
- Maven
- Lombok

### Telemetry Integration

- Euro Truck Simulator 2
- SCS Telemetry SDK
- C++
- CMake
- libcurl
- native Windows DLL

### Planned

- WebSocket
- Apache Kafka
- Redis
- Docker
- Testcontainers
- load testing
- application metrics
- observability

The technology list is intentionally allowed to evolve according to actual project requirements.

---

## Configuration

Database configuration is supplied through environment variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Example:

```text
DB_URL=jdbc:postgresql://localhost:5432/haulmetry
DB_USERNAME=postgres
DB_PASSWORD=...
```

Hibernate schema generation is disabled:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

The application therefore validates the schema but does not create or modify it automatically.

Schema evolution is managed through Flyway migrations.

---

## Database Migrations

Current migration responsibilities include:

```text
V1 -> trucks
V2 -> trips
V3 -> telemetry records
V4 -> driving events
V5 -> single ACTIVE trip constraint
```

Flyway keeps schema changes versioned alongside application development.

---

## Architecture Direction

The system is evolving toward an architecture similar to:

```text
                    ┌─────────────────────┐
                    │   Vehicle / ETS2    │
                    └──────────┬──────────┘
                               |
                               v
                    ┌─────────────────────┐
                    │ Telemetry Producer  │
                    └──────────┬──────────┘
                               |
                               v
                    ┌─────────────────────┐
                    │ Haulmetry Backend   │
                    └──────────┬──────────┘
                               |
                ┌──────────────┼──────────────┐
                |              |              |
                v              v              v
          PostgreSQL         Kafka          Redis
                |              |              |
                └──────────────┼──────────────┘
                               |
                               v
                    ┌─────────────────────┐
                    │ Analytics / Events  │
                    └──────────┬──────────┘
                               |
                               v
                    ┌─────────────────────┐
                    │      Dashboard      │
                    └─────────────────────┘
```

The final architecture is not considered fixed.

It will continue evolving as new engineering requirements appear.

---

# Project Roadmap

## Phase 1 — Telemetry Core

- [x] Create Spring Boot project
- [x] Create telemetry request model
- [x] Add request validation
- [x] Create telemetry REST endpoint
- [x] Store latest telemetry in memory
- [x] Retrieve latest telemetry by truck ID
- [x] Handle missing telemetry
- [x] Compare previous and current telemetry
- [x] Detect basic harsh braking
- [x] Model driving events

---

## Phase 2 — Event History and Persistence

- [x] Store multiple events per truck
- [x] Add timestamps
- [x] Introduce PostgreSQL
- [x] Model Truck entity
- [x] Model Trip entity
- [x] Model TelemetryRecord entity
- [x] Model persistent DrivingEvent entity
- [x] Add Flyway migrations
- [x] Add repository layer

---

## Phase 3 — Concurrency, Data Integrity and Reliability

### Trip Concurrency

- [x] Reproduce the `startTrip` race condition with concurrent requests
- [x] Prevent multiple `ACTIVE` trips for the same truck
- [x] Add a PostgreSQL constraint for active trips
- [x] Add the constraint through a Flyway migration
- [x] Handle database constraint violations gracefully
- [x] Return `409 Conflict` when an active trip already exists
- [x] Verify the solution with concurrent request tests

### Transaction Management

- [x] Define transaction boundaries for telemetry processing
- [x] Persist telemetry records and generated driving events atomically
- [x] Verify rollback behavior
- [x] Review trip start and completion transaction boundaries

### Thread-Safe Telemetry Processing

- [x] Replace unsafe shared telemetry state
- [x] Make `latestTelemetry` access thread-safe
- [x] Prevent race conditions in previous/current telemetry comparison
- [x] Allow different trucks to be processed independently
- [x] Serialize telemetry processing per truck
- [x] Add source sequence numbers to telemetry requests
- [x] Enforce monotonic telemetry sequence per truck
- [x] Reject duplicate and out-of-order telemetry with `409 Conflict`
- [x] Detect gaps in telemetry sequences

### Deterministic Telemetry Testing

- [x] Test harsh-braking detection with controlled speed values
- [x] Test known telemetry time intervals
- [x] Verify deceleration calculations against expected results
- [x] Make time-dependent logic testable
- [x] Inject `Clock` instead of directly using `Instant.now()`

### Reliability Testing

- [x] Test telemetry without an active trip
- [x] Test telemetry while a trip is being completed
- [x] Test duplicate concurrent trip-start requests
- [x] Test nonexistent truck and trip scenarios
- [x] Verify expected `200`, `404`, and `409` responses
- [x] Verify database consistency after failed requests
- [x] Verify duplicate telemetry rejection
- [x] Verify out-of-order telemetry rejection

---

## Phase 4 — ETS2 Integration

- [x] Build initial native C++ telemetry bridge
- [x] Forward test telemetry from C++ to the backend
- [x] Handle backend HTTP responses in the bridge
- [x] Integrate SCS Telemetry SDK
- [x] Read live vehicle telemetry from ETS2
- [ ] Map ETS2 telemetry to the bridge telemetry model
- [ ] Forward live ETS2 telemetry to the backend
- [ ] Handle game pause and telemetry interruptions
- [ ] Handle bridge reconnection scenarios
- [ ] Replace manual telemetry input with live game telemetry

---

## Phase 5 — Real-Time Telemetry

- [ ] Add live vehicle state
- [ ] Add WebSocket communication
- [ ] Build a simple live telemetry dashboard
- [ ] Support multiple simultaneously active trucks
- [ ] Stream live telemetry updates to connected clients

---

## Phase 6 — Event-Driven Architecture

- [ ] Introduce Apache Kafka
- [ ] Publish telemetry events
- [ ] Publish driving events
- [ ] Add consumers
- [ ] Handle retries
- [ ] Handle duplicate messages
- [ ] Introduce idempotent processing

---

## Phase 7 — Performance and Infrastructure

- [ ] Introduce Redis
- [ ] Add Docker
- [ ] Add automated tests
- [ ] Add load testing
- [ ] Add application metrics
- [ ] Add monitoring and observability
- [ ] Persist telemetry data-quality anomalies

---

## Phase 8 — Analytics

- [ ] Harsh braking analysis
- [ ] Overspeed detection
- [ ] Sudden acceleration detection
- [ ] Fuel-efficiency analysis
- [ ] Driver scoring
- [x] Trip summaries
- [ ] Fleet-level analytics

---

# Engineering Principles

## Problem Before Technology

Technologies are introduced when a concrete engineering requirement justifies them.

Kafka, Redis, WebSocket, Docker, and other infrastructure are not added simply because they are commonly associated with backend systems.

---

## Database Constraints for Database Invariants

Important invariants are not protected only by application-level checks.

Database constraints are used when concurrency could otherwise violate consistency.

---

## Commit Before Shared-State Update

In-memory telemetry state advances only after durable database work succeeds.

This prevents the application state from becoming ahead of the database after a rollback.

---

## Per-Entity Concurrency

Requests for the same truck are coordinated without unnecessarily serializing requests involving unrelated trucks.

---

## Replaceable Telemetry Producers

ETS2 is one telemetry producer.

The backend contract is designed so that another producer could replace ETS2 without redesigning the core backend.

---

## Deterministic Time-Dependent Logic

Time is injected through `Clock` where deterministic behavior matters.

This makes time-based analytics testable.

---

## Incremental Complexity

The project begins with the smallest useful implementation and gains complexity only when a real engineering problem requires it.

---

# Long-Term Vision

The long-term goal is to evolve Haulmetry into a small but realistic fleet telemetry platform.

A mature version should be capable of:

- receiving continuous telemetry from multiple vehicles
- maintaining current vehicle state
- storing telemetry history
- managing trips
- detecting driver-behavior events
- processing telemetry safely under concurrency
- handling missing, duplicate, and out-of-order data
- publishing domain events asynchronously
- streaming live telemetry to connected clients
- producing trip and fleet analytics
- exposing useful operational metrics

A possible end-state flow is:

```text
Vehicle / ETS2
      |
      v
Telemetry Producer
      |
      v
Haulmetry Backend
      |
      +-------------------+
      |        |          |
      v        v          v
 PostgreSQL   Kafka      Redis
      |        |          |
      +--------+----------+
               |
               v
       Analytics / Events
               |
               v
           Dashboard
```

---

# Status

Haulmetry is under active development.

Current milestone:

> **Phase 4 — ETS2 Integration**

The backend concurrency, transaction, persistence, and reliability foundation is in place.

Live ETS2 telemetry can now be captured through the SCS Telemetry SDK.

The next step is to connect the live SCS telemetry stream to the existing bridge and backend pipeline:

```text
ETS2
 |
 v
SCS Telemetry SDK
 |
 v
Native C++ Plugin
 |
 v
Bridge Telemetry Model
 |
 v
HTTP
 |
 v
Spring Boot
 |
 v
PostgreSQL
```