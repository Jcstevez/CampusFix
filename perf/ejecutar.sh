#!/usr/bin/env bash
# Corre baseline, carga y estres. La app se reinicia entre escenarios (base H2 limpia).
set -u
cd "$(dirname "$0")/.."
mkdir -p perf/reports
for esc in baseline carga estres; do
  echo "=== $esc ==="
  mvn -q compile exec:java > "perf/reports/app-$esc.log" 2>&1 &
  APP=$!
  for i in $(seq 1 60); do curl -sf localhost:8080/salud >/dev/null && break; sleep 1; done
  k6 run --summary-export="perf/reports/$esc.json" "perf/scripts/$esc.js"
  pkill -P $APP 2>/dev/null; kill $APP 2>/dev/null
  pkill -f com.campusfix.bootstrap.CampusFixApp 2>/dev/null
  sleep 2
done
echo "Listo. Resumenes en perf/reports/*.md"
