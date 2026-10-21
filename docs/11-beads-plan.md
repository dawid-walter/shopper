# 11 · Beads plan

> Status: **created in beads** (2026-10-21). Epic `shop-d5k`; items below. The backlog is local to this machine (`.beads/` is excluded from git).

## Parent (epic)

**Implement the shopper services from the accepted code plan**

Holds everything the items share, so each item only links to it:
- **Source of truth:** the code of every class and test in `hub/code-plan/files/<module>/…`, the stages in `hub/pages/08-code-plan.md`.
- **Path mapping from the hub to the repo:**
  - `aggregator/application/…` → `product-aggregator/src/main/java/com/shopper/aggregator/application/…` (same for the other modules);
  - `test/…` → `src/test/java/…`;
  - `resources/…` → `src/main/resources/…`;
  - files marked "(stage 0)" are intermediate versions, replaced later.
- **Commit rules:** two commits per stage, 🔴 tests only (do not compile yet, on purpose), then 🟢 implementation; messages in English exactly as in the plan; no `Co-Authored-By` trailer; no comments in code.
- **Environment:** Java 25 from SDKMAN (`source ~/.sdkman/bin/sdkman-init.sh`); Docker through `sg docker -c …`.

## Items

```mermaid
flowchart LR
  S0["1 · Stage 0<br/>Gradle skeleton"]
  S1["2 · Stage 1<br/>mocks"]
  S2["3 · Stage 2<br/>pricing core"]
  S3["4 · Stage 3<br/>pricing API"]
  S4["5 · Stage 4<br/>aggregator core"]
  S5["6 · Stage 5<br/>aggregator adapters,<br/>REST API, observability"]
  S6["7 · Stage 6<br/>docker compose,<br/>Swagger UI"]
  S0 --> S1 & S2 & S4
  S2 --> S3
  S4 --> S5
  S1 & S3 & S5 --> S6

  classDef agg fill:#e4002b,stroke:#9f0020,color:#fff
  classDef other fill:#eef2ff,stroke:#4f46e5,color:#1e1b4b
  classDef base fill:#1f2937,stroke:#1f2937,color:#fff
  class S4,S5 agg
  class S1,S2,S3 other
  class S0,S6 base
```

Arrows point from a prerequisite to the item it unblocks. Items 2, 3 and 5 can run in parallel, then 4 and 6, then 7.

| # | Bead | Title | Depends on | Done when |
|---|---|---|---|---|
| 1 | `shop-9dd` | `Stage 0: Gradle skeleton for five services` | — | Gradle 9.8.2 wrapper; `./gradlew test`: 5 green |
| 2 | `shop-ie3` | `Stage 1: catalog, availability and customer mocks` | 1 | 🔴 → 🟢; mock tests: 7 green |
| 3 | `shop-y11` | `Stage 2: pricing core (valuation archetype)` | 1 | 🔴 → 🟢; `PricingServiceTest`: 3 green |
| 4 | `shop-h1m` | `Stage 3: pricing REST API` | 3 | 🔴 → 🟢; `PricingApiTest`: 2 green |
| 5 | `shop-gh2` | `Stage 4: aggregator core` | 1 | 🔴 → 🟢; `ProductViewServiceTest`: 9 green |
| 6 | `shop-ix3` | `Stage 5: aggregator adapters, REST API and observability` | 5 | 🔴 → 🟢; `ProductEndpointComponentTest`: 14 green |
| 7 | `shop-pn5` | `Stage 6: docker compose and Swagger UI` | 2, 4, 6 | `docker compose up --build` and the checks on page 08 (200, 404, 400, Pricing and Catalog stopped, Swagger on 8080 and 8082); `./gradlew test`: 40 green |

**Would it still work if we stopped after any item?** Yes: every item ends with a 🟢 commit and a green build. The only broken state, the 🔴 commit, always sits inside one item.

**Parallel items:** each worker gets its own git worktree and branch; the coordinator merges into `main` after verifying. Items 2, 3 and 5 touch different modules. Item 7 edits the stage-0 `build.gradle.kts` (adds springdoc) but runs last.
