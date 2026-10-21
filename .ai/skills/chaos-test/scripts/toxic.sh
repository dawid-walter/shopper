#!/usr/bin/env bash
set -euo pipefail

API=http://localhost:8474
UPSTREAMS="catalog pricing availability customer"

add() {
  curl -sf -X POST "$API/proxies/$1/toxics" -H 'Content-Type: application/json' -d "$2" > /dev/null
}

clear_one() {
  for name in $(curl -sf "$API/proxies/$1/toxics" | grep -o '"name":"scenario-[a-z]*"' | cut -d'"' -f4); do
    curl -sf -X DELETE "$API/proxies/$1/toxics/$name" > /dev/null
  done
  echo "$1: scenario toxics removed"
}

case "${2:-${1:-}}" in
  slow)
    add "$1" '{"name":"scenario-slow","type":"latency","stream":"downstream","toxicity":1,"attributes":{"latency":200,"jitter":0}}'
    echo "$1: +200 ms on every response (under its timeout)"
    ;;
  hang)
    add "$1" '{"name":"scenario-hang","type":"timeout","stream":"downstream","toxicity":1,"attributes":{"timeout":0}}'
    echo "$1: responses never arrive (every call times out)"
    ;;
  reset)
    add "$1" '{"name":"scenario-reset","type":"reset_peer","stream":"downstream","toxicity":1,"attributes":{"timeout":0}}'
    echo "$1: every connection is reset (fast errors)"
    ;;
  clear)
    clear_one "$1"
    ;;
  clear-all)
    for upstream in $UPSTREAMS; do clear_one "$upstream"; done
    ;;
  list)
    for upstream in $UPSTREAMS; do
      echo "$upstream: $(curl -sf "$API/proxies/$upstream/toxics" | grep -o '"name":"[a-z-]*"' | cut -d'"' -f4 | tr '\n' ' ')"
    done
    ;;
  *)
    echo "usage: toxic.sh <catalog|pricing|availability|customer> slow|hang|reset|clear" >&2
    echo "       toxic.sh clear-all|list" >&2
    exit 1
    ;;
esac
