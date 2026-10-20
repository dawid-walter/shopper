---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# REST only, no gRPC

## Context and Problem Statement

The assignment requires REST endpoints and treats a gRPC implementation as a bonus.
We have to decide whether the aggregator also exposes a gRPC API.

## Decision Drivers

* The author has not worked with gRPC before.
* The budget is 3–4 hours, and every part of the solution has to be defended in the interview.

## Considered Options

* REST only
* REST + gRPC

## Decision Outcome

Chosen option: "REST only", because REST meets the requirement, while gRPC would mean
learning a new technology under time pressure, with no certainty of being able to
defend it in the interview.

### Consequences

* Bad, because we give up the bonus points for gRPC.
