---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# Java and Spring Boot as the technology stack

## Context and Problem Statement

The requirements allow Java 17+, Kotlin or Go. One stack has to be chosen for the
aggregator and for every service that simulates an upstream.

## Decision Drivers

* The team has to be able to explain and maintain every part of the code.
* The platform: object-oriented Java, a JVM with a JIT compiler and a garbage collector.
* Spring offers extensive configuration options.

## Considered Options

* Java + Spring Boot
* Kotlin
* Go

## Decision Outcome

Chosen option: "Java + Spring Boot", because it is a mature object-oriented platform:
the JVM brings JIT and GC, and Spring brings extensive configuration options.

## More Information

The Java and Spring Boot versions, and whether Postgres is used, are decided separately.
