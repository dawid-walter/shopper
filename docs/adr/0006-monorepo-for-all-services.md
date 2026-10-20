---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# Monorepo for all services

## Context and Problem Statement

The system consists of five services (ADR 0002): the aggregator and four upstreams.
We have to decide how their code is organised into repositories.

## Decision Drivers

* This is a recruitment assignment, submitted as a single GitHub link and run locally
  with minimal setup.

## Considered Options

* One repository with a module per service
* A separate repository per service

## Decision Outcome

Chosen option: "One repository with a module per service", because the assignment is
reviewed and run as one unit.

### Consequences

* Good, because one clone and one command start the whole system.
* Neutral, because the choice is driven by the assignment format, not by what a
  production system of this kind would need.

## More Information

Outside a recruitment assignment the choice depends on complexity: a modular monolith
is the likely default, and for a very large system each service would get its own
repository.
