---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# Ports and adapters in the aggregator

## Context and Problem Statement

The aggregator is the core of the assignment: it receives requests over REST and calls
four upstreams. We have to decide how its code is structured internally. Upstream
mocks and Pricing are outside the scope of this decision.

## Decision Drivers

* The assignment requires that new data sources can be added without major refactoring.
* gRPC is out of scope (ADR 0001), but it should be possible to show how it would be
  added.

## Considered Options

* Ports and adapters (hexagonal): a core with ports, an adapter per upstream and per
  inbound protocol
* Layered: controller → service → upstream clients

## Decision Outcome

Chosen option: "Ports and adapters", because inbound protocols and upstream sources
both become adapters around one application service. Adding gRPC means adding another
inbound adapter next to the REST controller, and adding a data source means adding a
port and its adapter, with no change to the core.

### Consequences

* Good, because the core is tested without a network by substituting the ports.
* Bad, because there are more types and mapping than in a layered structure.
