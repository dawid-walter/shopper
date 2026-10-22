---
status: "proposed"
date: 2026-10-22
decision-makers:
consulted:
informed:
---

# No projection in the aggregator; every read goes to the upstreams

## Context and Problem Statement

ADR 0010 considered keeping a catalog projection in the aggregator, fed by a change
feed, and was rejected. Its text describes the rejected design, so the decision that
holds for the aggregator's read path is recorded here: whether the aggregator keeps any
copy of upstream data at all (ADR 0009).

## Decision Drivers

* Price and stock change often; a client must see current values.
* Catalog is required: if it fails, the request fails.
* No upstream publishes events today; a projection would have to poll.
* The first version has limited time.

## Considered Options

* No projection: every read calls the upstreams (ADR 0009)
* CQRS light: a catalog projection fed by a polled change feed (ADR 0010)
* Full CQRS: every source projected through a message broker
* Last-known-good snapshot or an in-memory cache, served when an upstream fails

## Decision Outcome

Chosen option: "No projection", because it returns current data, keeps the aggregator
stateless, and keeps the Catalog rule as stated. A catalog projection without events
needs a poller, a database and a second read path; full CQRS serves price and stock
that lag behind; a snapshot or cache serves stale data for exactly the sources that must
be current.

### Consequences

* Good, because the aggregator has no database, migrations or projector, and any
  instance can serve any request.
* Good, because "if Catalog fails, the request fails" holds without reinterpretation.
* Bad, because a catalog outage fails every request; the circuit breaker only makes
  that failure fast.
* Bad, because every read pays the fan-out to four upstreams.

## More Information

Related: ADR 0009 (synchronous fan-out), ADR 0010 (catalog projection, rejected),
ADR 0015 (a broker as a future adapter). Revisit when a message broker carries catalog
changes: a projection of slow-changing data (catalog, customer profile) then needs no
poller, while price and stock stay live.
