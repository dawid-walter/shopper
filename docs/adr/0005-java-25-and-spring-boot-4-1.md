---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# Java 25 and Spring Boot 4.1

## Context and Problem Statement

ADR 0000 chose Java and Spring Boot but left the versions open. Every service in the
project uses the same versions.

## Decision Drivers

* We do not need the newest language features, but newer JVMs bring JIT and garbage
  collector optimisations.
* Java 25 is the JDK installed in the development environment.

## Considered Options

* Java 25 (latest LTS) + Spring Boot 4.1
* Java 27 (latest release, non-LTS) + Spring Boot 4.1

## Decision Outcome

Chosen option: "Java 25 (latest LTS) + Spring Boot 4.1", because it is the newest
long-term-support release, carries the JIT and GC improvements, and is already installed.
Spring Boot 4.1 is the latest GA line (4.2 was still a milestone on 2026-10-20).

### Consequences

* Good, because Resilience4j 2.4.0 ships a Spring Boot 4 module, so the resilience
  library needs no workaround.
* Neutral, because improvements from Java 26–27 are left out.
