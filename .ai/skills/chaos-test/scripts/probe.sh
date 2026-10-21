#!/usr/bin/env bash
set -euo pipefail

requests=${1:-10}
url="http://localhost:8080/products/P-1001?market=pl-PL&customerId=C-42"

printf '%-4s %-6s %-8s %-12s %-8s %-13s %s\n' '#' status time price stock customer traceId
codes=""; slowest=0
for i in $(seq 1 "$requests"); do
  out=$(curl -s -D - --max-time 5 -w '\n%{http_code} %{time_total}' "$url")
  read -r code time < <(printf '%s\n' "$out" | tail -1)
  body=$(printf '%s\n' "$out" | sed -n '/^{/p')
  trace=$(printf '%s\n' "$out" | grep -i '^x-trace-id:' | awk '{print $2}' | tr -d '\r')
  section() { printf '%s' "$body" | grep -o "\"$1\":{\"status\":\"[A-Z]*\"" | cut -d'"' -f6 || true; }
  price=$(section price); stock=$(section stock); customer=$(section customer)
  ms=$(awk "BEGIN { printf \"%d\", $time * 1000 }")
  [ "$ms" -gt "$slowest" ] && slowest=$ms
  printf '%-4s %-6s %-8s %-12s %-8s %-13s %s\n' "$i" "$code" "${ms}ms" "${price:--}" "${stock:--}" "${customer:--}" "$trace"
  codes="$codes $code"
done

echo
echo "statuses:$(printf '%s\n' $codes | sort | uniq -c | awk '{printf " %s×%s", $1, $2}')  slowest: ${slowest}ms (budget 500 ms)"
curl -s 'http://localhost:9090/api/v1/query?query=resilience4j_circuitbreaker_state%3D%3D1' 2>/dev/null \
  | grep -o '"name":"[a-z]*","state":"[a-z_]*"' | sed 's/"name":"\([a-z]*\)","state":"\([a-z_]*\)"/\1=\2/' | tr '\n' ' ' \
  | sed 's/^/circuits: /' || true
echo
