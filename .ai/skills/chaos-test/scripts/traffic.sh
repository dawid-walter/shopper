#!/usr/bin/env bash
set -euo pipefail

PID_FILE=${TMPDIR:-/tmp}/shopper-chaos-traffic.pid

case "${1:-}" in
  start)
    rps=${2:-10}
    if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
      echo "Traffic already running (pid $(cat "$PID_FILE"))"
      exit 0
    fi
    (
      while true; do
        curl -s -o /dev/null --max-time 2 "http://localhost:8080/products/P-1001?market=pl-PL&customerId=C-42" &
        curl -s -o /dev/null --max-time 2 "http://localhost:8080/products/P-1001?market=de-DE" &
        sleep "$(awk "BEGIN { print 2 / $rps }")"
      done
    ) > /dev/null 2>&1 &
    echo $! > "$PID_FILE"
    echo "Traffic started: ~$rps requests per second (pid $!)"
    ;;
  stop)
    if [ -f "$PID_FILE" ]; then
      pkill -P "$(cat "$PID_FILE")" 2>/dev/null || true
      kill "$(cat "$PID_FILE")" 2>/dev/null || true
      rm -f "$PID_FILE"
      echo "Traffic stopped"
    else
      echo "No traffic running"
    fi
    ;;
  *)
    echo "usage: traffic.sh start [rps]|stop" >&2
    exit 1
    ;;
esac
