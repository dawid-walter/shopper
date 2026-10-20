---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# Toxiproxy for simulating upstream latency and failures

## Context and Problem Statement

The simulated upstreams have to behave realistically: artificial delays and
occasional failures matching each service's typical latency and reliability. We have to
decide where that behaviour is simulated.

## Decision Drivers

* The simulation should reproduce how failures happen in a distributed system, so that
  the aggregator's resilience is exercised against realistic faults.
* Failure scenarios ("Pricing starts timing out") should be switchable live.

## Considered Options

* Toxiproxy: a proxy container between the aggregator and each upstream
* A simulation filter inside every upstream service
* `tc netem` network emulation inside the containers

## Decision Outcome

Chosen option: "Toxiproxy", because it injects faults where they really happen, in the
network between services, so the mocks stay plain and the aggregator meets real
timeouts, resets and slow connections. Its HTTP API switches scenarios on at run time.

```mermaid
flowchart LR
  AGG["product-aggregator"] --> TP{{"Toxiproxy<br/>latency · jitter · timeout · reset<br/>toxicity = 1 − reliability"}}
  TP --> CAT["catalog-service"]
  TP --> PRI["pricing-service"]
  TP --> AVA["availability-service"]
  TP --> CUS["customer-service"]
  OPS(["operator / test"]) -. "HTTP API:<br/>switch a scenario on" .-> TP

  classDef ours fill:#1f2937,stroke:#1f2937,color:#fff
  classDef proxy fill:#e4002b,stroke:#9f0020,color:#fff
  classDef svc fill:#eef2ff,stroke:#4f46e5,color:#1e1b4b
  classDef ext fill:#f9fafb,stroke:#6b7280,color:#111827
  class AGG ours
  class TP proxy
  class CAT,PRI,AVA,CUS svc
  class OPS ext
```

### Consequences

* Good, because the mocks contain no simulation code.
* Bad, because Toxiproxy produces network faults only, not HTTP 5xx responses; a mock
  can return one directly if a 5xx case is needed.
* Neutral, because toxicity applies per connection, and the HTTP client reuses
  connections, so failures arrive in bursts rather than independently. Real outages
  behave that way too, and bursts are what open a circuit breaker.

## More Information

`tc netem` was rejected because packet loss shows up as TCP retransmissions rather than
clear, observable failures.
