---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# REST only, no gRPC

## Context and Problem Statement

The requirements call for REST endpoints and list gRPC as optional. We have to decide
whether the aggregator also exposes a gRPC API.

## Decision Drivers

* The team has no experience with gRPC.
* The first version has limited time, and everything in it has to be maintainable by
  the team.

## Considered Options

* REST only
* REST + gRPC

## Decision Outcome

Chosen option: "REST only", because REST meets the requirement, while gRPC would mean
learning a new technology under time pressure, without the experience to operate and
maintain it.

### Consequences

* Bad, because clients that would benefit from gRPC use REST until it is added.
  ADR 0008 and ADR 0015 define where a gRPC adapter would plug in.
