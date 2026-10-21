#!/usr/bin/env bash
set -euo pipefail

AGGREGATOR=http://localhost:8080/products/P-1001?market=pl-PL

case "${1:-}" in
  up)
    sg docker -c "docker compose --profile observability up --build -d"
    for _ in $(seq 1 120); do
      [ "$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "$AGGREGATOR" || true)" = 200 ] && break
      sleep 1
    done
    for _ in $(seq 1 20); do curl -s -o /dev/null "$AGGREGATOR"; done
    echo "Stack ready and warmed up. Grafana: http://localhost:3000  Toxiproxy API: http://localhost:8474"
    ;;
  down)
    sg docker -c "docker compose --profile observability down"
    ;;
  status)
    sg docker -c "docker compose --profile observability ps --format '{{.Service}}\t{{.State}}\t{{.Status}}'"
    ;;
  *)
    echo "usage: stack.sh up|down|status" >&2
    exit 1
    ;;
esac
