# Haulmetry

Haulmetry is a backend engineering project focused on real-time vehicle telemetry processing, driver behavior analysis, and event-driven system design.

The project uses **Euro Truck Simulator 2 (ETS2)** as a realistic telemetry source during development, but the backend is intentionally designed to remain independent from the simulator itself. The long-term goal is to build a small-scale fleet telemetry platform capable of receiving, processing, analyzing, storing, and streaming vehicle data in real time.

Rather than treating ETS2 as the product, Haulmetry treats it as a **data source** for a broader backend system.

---

## Project Purpose

Modern fleet and vehicle platforms continuously receive telemetry such as:

- vehicle speed
- engine RPM
- fuel level
- gear state
- location
- engine state
- braking behavior
- acceleration behavior
- trip information

Haulmetry is being built to process this type of data and gradually evolve into a backend system that can answer questions such as:

- What is the latest known state of a vehicle?
- Did the driver perform harsh braking?
- Is the vehicle overspeeding?
- How has the vehicle state changed over time?
- What events occurred during a trip?
- Can telemetry be delivered to a dashboard in real time?
- How should high-frequency telemetry be persisted and streamed?
- How should failures, duplicate events, and concurrent requests be handled?

The project is therefore not just a CRUD application. Its purpose is to explore the engineering problems that appear in telemetry, fleet-management, and real-time backend systems.

---

## Development Approach

Haulmetry is developed **incrementally and problem-first**.

The project does not begin by immediately adding technologies such as Kafka, Redis, PostgreSQL, WebSocket, or Docker just to make the stack look complex.

Instead, each component is introduced only when the system reaches a point where that component solves a real problem.

The development philosophy is roughly:

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
Choose the technology that solves that requirement
            |
            v
Refactor and evolve the architecture
```

For example:

- telemetry is first accepted over HTTP before connecting the ETS2 Telemetry SDK
- the latest vehicle state is first kept in memory before introducing persistence
- previous and current telemetry are compared before building more advanced analytics
- domain events are modeled before introducing Kafka
- validation is handled at the API boundary before adding more complex business rules
- real-time communication will be introduced only after the telemetry processing model is stable
- persistence will be added when the system needs historical telemetry and durable events

This makes the architecture a result of actual requirements rather than a collection of unrelated technologies.

---

## Current Development Stage

The project is currently focused on the **telemetry processing core**.

At this stage, telemetry is sent manually to the backend through HTTP requests. This allows the backend contract and domain logic to be developed before integrating ETS2.

The current flow is:

```text
Client / Postman
        |
        v
Spring Boot REST API
        |
        v
Request Validation
        |
        v
Telemetry Service
        |
        +--------------------------+
        |                          |
        v                          v
Latest Vehicle State        Event Detection
        |                          |
        v                          v
 In-Memory Storage          DrivingEvent
```

The current implementation can:

- receive telemetry through a REST endpoint
- validate incoming telemetry data
- keep the latest telemetry for each truck in memory
- retrieve the latest telemetry for a specific truck
- handle missing telemetry using proper HTTP status codes
- compare previous and current telemetry
- detect a basic harsh-braking condition
- represent driving events as domain objects

The current telemetry source is simulated manually. ETS2 integration will be added later.

---

## Example Telemetry

A telemetry request currently contains fields such as:

```json
{
  "truckId": "SCANIA-S730",
  "speed": 100,
  "rpm": 1800,
  "fuel": 300,
  "gear": 10
}
```

If another telemetry request is received for the same truck:

```json
{
  "truckId": "SCANIA-S730",
  "speed": 60,
  "rpm": 1200,
  "fuel": 299.9,
  "gear": 8
}
```

the backend can compare the new state with the previously stored state.

The current simplified harsh-braking rule is based on the difference between the previous and current speed.

```text
Previous speed: 100 km/h
Current speed:   60 km/h
Difference:      40 km/h
```

If the configured threshold is reached, the backend creates a `DrivingEvent` with the event type:

```text
HARSH_BRAKING
```

This rule is intentionally simple at the current stage. A more realistic implementation will later include timestamps and rate-of-change calculations.

---

## Current API

### Submit Telemetry

```http
POST /api/telemetry
```

The request body contains the current telemetry state of a truck.

Example:

```json
{
  "truckId": "SCANIA-S730",
  "speed": 82.5,
  "rpm": 1450,
  "fuel": 312.4,
  "gear": 10
}
```

### Get Latest Telemetry

```http
GET /api/telemetry/{truckId}
```

Example:

```http
GET /api/telemetry/SCANIA-S730
```

Possible responses currently include:

```text
200 OK
400 Bad Request
404 Not Found
```

---

## Domain Model

### TelemetryRequest

Represents the telemetry state received by the backend.

Current fields include:

```text
truckId
speed
rpm
fuel
gear
```

The request is modeled as a Java `record` because it is primarily a data carrier.

### DrivingEvent

Represents a driving-related event detected from telemetry data.

Current fields include:

```text
truckId
eventType
previousSpeed
currentSpeed
speedDifference
```

### DrivingEventType

Driving-event types are modeled with an `enum` rather than free-form strings.

The first event currently implemented is:

```text
HARSH_BRAKING
```

Additional event types will be introduced as the analytics layer grows.

---

## Architecture Direction

The current architecture is intentionally simple.

The target direction is expected to evolve toward something similar to:

```text
Euro Truck Simulator 2
          |
          v
SCS Telemetry SDK
          |
          v
Native Telemetry Bridge
          |
          v
Spring Boot Backend
          |
          +-----------------------------+
          |                             |
          v                             v
Telemetry Processing              Driver Events
          |                             |
          v                             v
     PostgreSQL                      Kafka
          |
          v
        Redis
          |
          v
 WebSocket / REST API
          |
          v
      Dashboard
```

The final architecture may change as the project evolves.

That is intentional.

The purpose of the project is not to force the system into a predefined architecture, but to let the architecture evolve according to actual technical requirements.

---

## Why ETS2?

ETS2 provides a useful environment for generating realistic vehicle telemetry without requiring access to real fleet hardware.

It makes it possible to work with data that resembles a real telemetry stream, including information related to vehicle state, engine behavior, speed, fuel, and driving activity.

The project will eventually use the **SCS Telemetry SDK** to obtain real telemetry from the game.

A native bridge will then forward that telemetry to the Java backend.

Conceptually:

```text
ETS2
 |
 | SCS Telemetry SDK
 v
Native Bridge
 |
 | HTTP / messaging
 v
Haulmetry Backend
```

The backend itself is designed so that ETS2 can later be replaced by another telemetry producer without redesigning the entire system.

---

## Planned Engineering Problems

As the project grows, Haulmetry will be used to explore several backend engineering topics.

### Persistence

The current in-memory state disappears when the application restarts.

PostgreSQL will be introduced to persist:

- trucks
- trips
- telemetry records
- driving events

Database migrations are planned to be managed with Flyway.

### Real-Time State

The backend will eventually maintain the latest state of multiple vehicles and expose live telemetry to clients.

WebSocket will be considered for real-time dashboard updates.

### Event Streaming

Driving events and telemetry processing will eventually be separated from direct request processing.

Apache Kafka is planned for event-driven communication between parts of the system.

Possible events may include:

```text
TELEMETRY_RECEIVED
HARSH_BRAKING
OVERSPEED
LOW_FUEL
TRIP_STARTED
TRIP_COMPLETED
```

### Caching

Redis may be introduced for fast access to frequently requested vehicle state and other short-lived data.

### Concurrency

The current prototype uses simple in-memory collections.

As multiple telemetry requests begin arriving concurrently, the project will need to address:

- thread safety
- concurrent state updates
- race conditions
- consistency
- ordering

### Reliability

Later stages will explore:

- idempotency
- retries
- duplicate event handling
- failure recovery
- message processing guarantees

### Observability

The system is expected to eventually expose metrics and logs for monitoring.

Potential areas include:

- request throughput
- telemetry processing latency
- event counts
- failed requests
- Kafka consumer lag
- application health
- database performance

### Testing

The project will progressively include:

- unit tests
- service tests
- controller tests
- integration tests
- load tests

---

## Planned Technology Stack

### Current

- Java 21
- Spring Boot
- Spring Web
- Jakarta Bean Validation
- Maven

### Planned

- PostgreSQL
- Spring Data JPA
- Flyway
- Apache Kafka
- Redis
- WebSocket
- Docker
- Testcontainers
- JUnit
- Mockito

### Telemetry Integration

- Euro Truck Simulator 2
- SCS Telemetry SDK
- Native C/C++ telemetry bridge

The technology list is expected to evolve with the project.

---

## Project Roadmap

The project is being developed in stages.

### Phase 1 — Telemetry Core

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

### Phase 2 — Event History and Persistence

- [x] Store multiple events per truck
- [x] Add timestamps
- [x] Introduce PostgreSQL
- [x] Model Truck entity
- [ ] Model Trip entity
- [x] Model TelemetryRecord entity
- [ ] Model persistent DrivingEvent entity
- [ ] Add Flyway migrations
- [x] Add repository layer

### Phase 3 — Real-Time Telemetry

- [ ] Add live vehicle state
- [ ] Add WebSocket communication
- [ ] Build a simple live telemetry dashboard
- [ ] Support multiple active trucks

### Phase 4 — ETS2 Integration

- [ ] Integrate SCS Telemetry SDK
- [ ] Build native telemetry bridge
- [ ] Forward ETS2 telemetry to the backend
- [ ] Replace manual telemetry input with live game telemetry

### Phase 5 — Event-Driven Architecture

- [ ] Introduce Apache Kafka
- [ ] Publish telemetry events
- [ ] Publish driving events
- [ ] Add consumers
- [ ] Handle retries
- [ ] Handle duplicate messages
- [ ] Introduce idempotent processing

### Phase 6 — Performance and Infrastructure

- [ ] Introduce Redis
- [ ] Add Docker
- [ ] Add automated tests
- [ ] Add load testing
- [ ] Add application metrics
- [ ] Add monitoring and observability

### Phase 7 — Analytics

- [ ] Harsh braking analysis
- [ ] Overspeed detection
- [ ] Sudden acceleration detection
- [ ] Fuel-efficiency analysis
- [ ] Driver scoring
- [ ] Trip summaries
- [ ] Fleet-level analytics

---

## Engineering Principles

Several principles guide the development of Haulmetry.

### Problem Before Technology

Technologies are introduced only when there is a clear reason to use them.

### Clear Layer Responsibilities

The project aims to keep responsibilities separated between:

```text
Controller
Service
Domain Model
Persistence
Messaging
Infrastructure
```

### Explicit Domain Modeling

Concepts such as telemetry and driving events are modeled as domain objects rather than being represented only as primitive values or console messages.

### Incremental Complexity

The project begins with a small working system and gains complexity only when required.

### Replaceable Data Sources

ETS2 is treated as one telemetry producer, not as a hard dependency of the entire backend.

### Learning Through Implementation

The project is intentionally developed step by step so that architectural decisions can be understood through implementation rather than only through theory.

---

## Long-Term Vision

The long-term goal is to evolve Haulmetry into a small but realistic fleet telemetry platform.

A future version should be capable of processing continuous telemetry from multiple vehicles, storing historical data, detecting driver events, publishing events asynchronously, serving live vehicle state, and producing useful analytics.

A possible end-state flow looks like:

```text
                    ┌─────────────────────┐
                    │   Vehicle / ETS2    │
                    └──────────┬──────────┘
                               |
                               v
                    ┌─────────────────────┐
                    │  Telemetry Producer │
                    └──────────┬──────────┘
                               |
                               v
                    ┌─────────────────────┐
                    │  Haulmetry Backend  │
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

The project starts small, but the architecture is designed to grow as new engineering requirements appear.

---

## Status

Haulmetry is currently under active development.

The current focus is the Java/Spring Boot telemetry processing core. Persistence, ETS2 integration, event streaming, caching, and real-time visualization will be introduced progressively as the project matures.
