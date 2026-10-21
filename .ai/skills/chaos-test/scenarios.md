# Scenarios

Run in this order with `traffic.sh` running. Before each step: `toxic.sh clear-all` and every service started, unless the step says otherwise. Probe with `probe.sh 20` after waiting ~15 s for the circuit breakers to react.

| # | Inject | Expected from `probe.sh` | Expected on the dashboard |
|---|---|---|---|
| 0 | nothing (realistic latency only) | all `200`; all sections available; slowest < 500 ms | p99 well under the budget line; all circuits closed |
| 1 | `toxic.sh pricing slow` | all `200`; price `AVAILABLE`; responses ~200 ms slower, still < 500 ms | p99 rises towards the budget line but stays under it; pricing p99 ~300 ms |
| 2 | `toxic.sh pricing hang` | all `200`; price `UNAVAILABLE`; once the circuit opens, responses are faster than in step 0 | pricing circuit `open`; degraded `price`; "pricing rejected (circuit open)"; warnings `pricing call failed: CIRCUIT_OPEN` |
| 3 | `toxic.sh availability reset` | all `200`; stock `UNKNOWN` | failed `availability` calls (each retried once), then its circuit `open` |
| 4 | `outage.sh stop customer` | all `200`; customer `STANDARD` | customer circuit `open`; degraded `customer` |
| 5 | `outage.sh stop catalog` | all `503` | `503` in responses by status; catalog circuit `open` |
| 6 | recovery: `toxic.sh clear-all`, `outage.sh start customer`, `outage.sh start catalog` | after ~30 s all `200` and every section available again | circuits half-open, then closed; degraded sections back to 0 |
