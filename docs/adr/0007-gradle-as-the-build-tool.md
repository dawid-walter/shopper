---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# Gradle as the build tool

## Context and Problem Statement

The monorepo (ADR 0006) needs a build tool that handles several service modules.

## Decision Drivers

* The author prefers Gradle and is also fluent in Maven, so either can be defended.

## Considered Options

* Gradle
* Maven

## Decision Outcome

Chosen option: "Gradle", because it is the author's preferred tool, and fluency in
Maven means the choice is a preference rather than a gap.

### Consequences

* Good, because the Gradle wrapper lets the project build without a locally installed
  Gradle.
