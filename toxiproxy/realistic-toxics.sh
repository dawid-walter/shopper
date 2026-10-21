#!/bin/sh
set -eu

API=http://toxiproxy:8474

until curl -sf "$API/version" > /dev/null; do sleep 1; done

toxic() {
  curl -sf -X POST "$API/proxies/$1/toxics" -H 'Content-Type: application/json' -d "$2" > /dev/null
}

realistic() {
  toxic "$1" "{\"name\":\"latency\",\"type\":\"latency\",\"stream\":\"downstream\",\"attributes\":{\"latency\":$2,\"jitter\":$3}}"
  toxic "$1" "{\"name\":\"failures\",\"type\":\"reset_peer\",\"stream\":\"downstream\",\"toxicity\":$4,\"attributes\":{\"timeout\":0}}"
}

realistic catalog 50 25 0.001
realistic pricing 80 40 0.005
realistic availability 100 50 0.02
realistic customer 60 30 0.01

echo "Realistic latency and failures applied"
