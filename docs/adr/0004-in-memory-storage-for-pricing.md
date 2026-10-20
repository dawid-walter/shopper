---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# In-memory storage for Pricing

## Context and Problem Statement

Pricing implements the valuation archetype (ADR 0003) and holds state: price components,
their versions with validity periods, price lists and customer contracts. That state
needs storage.

## Decision Drivers

* Pricing stands in for an upstream service owned by another team; within this
  assignment it is still a simulated upstream.

## Considered Options

* Postgres
* In-memory maps, seeded at startup

## Decision Outcome

Chosen option: "In-memory maps, seeded at startup", because Pricing is a simulated
upstream, so durable storage adds setup without adding anything the assignment evaluates.

### Consequences

* Good, because Pricing needs no database container or migrations.
* Bad, because component versions added at runtime (e.g. a flash sale) are lost on
  restart.

## More Information

Storage sits behind a repository interface, so a database could replace the maps
without changes to the pricing model.
