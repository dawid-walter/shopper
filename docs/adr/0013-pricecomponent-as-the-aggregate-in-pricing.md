---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# PriceComponent as the aggregate in Pricing

## Context and Problem Statement

Pricing implements the valuation archetype (ADR 0003). Reading a price is a calculation
over configuration and changes nothing. Adding a new version of a price component (for
example a promotion) is a write that must keep the component's versions unambiguous,
and two people may do it at the same time. We have to decide where the consistency
boundary lies.

## Decision Drivers

* An aggregate belongs where a write must uphold an invariant under concurrent changes,
  and nowhere else.
* Per the archetype, a component keeps a stable identity while its configuration is
  versioned, and composite components refer to their parts by id.

## Considered Options

* `PriceComponent` as the aggregate: the component with its immutable versions
* No aggregates: Pricing as calculations over plain data

## Decision Outcome

Chosen option: "`PriceComponent` as the aggregate", because adding a version is the one
write in Pricing with invariants to protect:

* at any point in time at most one version applies; when several match, the one that
  started last wins;
* a version identical to an existing one is rejected;
* a validity period is half-open and its start precedes its end.

A write appends a version; it never edits one. The append is applied atomically to the
component's current state, so concurrent appends cannot overwrite each other. The read
path, calculators, `Money`, `Validity` and the price breakdown are not aggregates. The
composite is a `Tariff`: the ordered ids of the components that make up a market's
price, unversioned.

```mermaid
classDiagram
  direction LR
  class PriceComponent {
    <<aggregate root>>
    id
    name
    role
  }
  class ComponentVersion {
    <<immutable>>
    calculator
    validity
    applicability
    definedAt
  }
  class Validity {
    <<value object>>
    from (inclusive)
    to (exclusive)
  }
  class Calculator {
    <<pure function>>
    Fixed · Percentage
  }
  class Tariff {
    market
    ordered component ids
  }
  PriceComponent "1" *-- "1..*" ComponentVersion : versions
  ComponentVersion --> Validity
  ComponentVersion --> Calculator
  Tariff ..> PriceComponent : refers by id
```

```mermaid
sequenceDiagram
  actor A as Pricing manager A
  actor B as Pricing manager B
  participant P as vat-pl
  A->>P: add version 24% from 2027-01-01
  B->>P: add version 0% for exports
  Note over P: each append is applied atomically to the current state
  P-->>A: appended
  P-->>B: appended (A's version is kept)
```

### Consequences

* Good, because the invariants live in one place and concurrent edits cannot leave a
  component ambiguous.
* Good, because components referring to each other by id means a change to one
  component applies in every price that uses it, without rewiring.
* Neutral, because a customer discount contract is a second aggregate candidate (one
  applicable contract per customer per period); it is in scope only if time allows.

## More Information

Optimistic locking is deliberately absent. It protects a read-modify-write: a client
reads a component, edits it and saves the whole of it, possibly over someone else's
change. Appending a version is not that, and the atomic append already prevents lost
updates. If clients start editing components based on what they read, add a version
number to the component, return it as an `ETag`, require `If-Match` on the write and
answer `409` or `412` on a mismatch (with JPA: `@Version`).
