#!/usr/bin/env bash
set -euo pipefail

usage() { echo "usage: outage.sh stop|start <catalog|pricing|availability|customer>" >&2; exit 1; }

action=${1:-}
case "${2:-}" in
  catalog|pricing|availability|customer) service="$2-service" ;;
  *) usage ;;
esac

case "$action" in
  stop) sg docker -c "docker compose stop $service" > /dev/null 2>&1; echo "$service: stopped" ;;
  start) sg docker -c "docker compose start $service" > /dev/null 2>&1; echo "$service: started" ;;
  *) usage ;;
esac
