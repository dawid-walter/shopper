# 05 · Behaviour spec (tests first)

> Status: **proposal**. Each row is one observable behaviour and becomes one test before any implementation. Kept to what makes the application work.

## Shared test data

| Id | What |
|---|---|
| `P-1001` | Oil filter, translated to `pl`, `de`, `nl`, `en`; priced in PL (PLN) and DE (EUR) |
| `P-9999` | does not exist |
| `C-42` | dealer with a 10% discount contract in Pricing |
| `C-7` | customer without a contract |

Markets: `pl-PL` (PLN, VAT 23%), `de-DE` (EUR, VAT 19%), `nl-NL` (EUR, VAT 21%).

## Aggregator: `GET /products/{id}?market=…&customerId=…`

**Unit** = application service with fake ports, no Spring, no network.
**Component** = the whole aggregator app over HTTP, upstreams stubbed with WireMock (delays and errors set per test).

| # | Given | When | Then | Level |
|---|---|---|---|---|
| A1 | all upstreams answer | `GET /products/P-1001?market=pl-PL` | `200`; product, price and stock present | unit + component |
| A2 | — | request for `pl-PL` | catalog is asked for `pl-PL`, pricing and availability for market `pl-PL`; response is in Polish and PLN | component |
| A3 | no `customerId` | request | customer service is **not called**; customer section `STANDARD` | unit |
| A4 | customer service answers for `C-42` | request with `customerId=C-42` | customer section `PERSONALIZED` with the segment | unit |
| A5 | catalog answers `404` | request for `P-9999` | `404` | unit + component |
| A6 | catalog fails (5xx or timeout) | request | `503` | unit + component |
| A7 | pricing fails | request | `200`; price `UNAVAILABLE`; product and stock present | unit + component |
| A8 | availability fails | request | `200`; stock `UNKNOWN` | unit |
| A9 | customer service fails | request with `customerId` | `200`; customer section `STANDARD` | unit |
| A10 | pricing hangs longer than its timeout | request | `200` with price `UNAVAILABLE`, returned **within the request budget** (not after the hang) | component |
| A11 | every upstream takes ~100 ms | request | response in well under the sum (< 250 ms): calls run in parallel | component |
| A12 | — | `market=xx-XX` | `400` | component |
| A13 | pricing keeps failing | several requests | after the circuit opens pricing is **no longer called**; responses stay `200` with price `UNAVAILABLE` | component (if time) |

## Pricing: `GET /prices/{productId}?market=…&customerId=…`

Single example tests on the domain (pure, no Spring), plus one over HTTP.

| # | Given | When | Then |
|---|---|---|---|
| P1 | `P-1001` list price 100.00 PLN | price for `pl-PL` | net 100.00, VAT 23.00, gross 123.00 PLN; breakdown lists list price and VAT |
| P2 | `P-1001` list price 25.00 EUR in DE | price for `de-DE` | VAT 19%, EUR |
| P3 | contract `C-42` = −10% | price for `pl-PL`, `C-42` | discount −10.00 before VAT; gross 110.70 |
| P4 | flash-sale version −20% valid 10:00–12:00 | price at 11:00 / at 12:00 | applied at 11:00; not applied at 12:00 (time from an injected clock) |
| P5 | `P-9999`, or a market without a price list | price | "not priced" → `404` |

## Not tested in automated tests

Toxiproxy scenarios (real network faults) are run by hand for the demo; WireMock gives the component tests the same failures deterministically.
