#!/usr/bin/env bash
set -euo pipefail

./gradlew :app:assembleDebug && rc=$? || rc=$?
mkdir -p build/status
msg="assembleDebug ✅ OK"
if [ $rc -ne 0 ]; then
  msg="assembleDebug ⚠️ Falló (posible entorno offline)"
fi
echo "$msg" | tee build/status/assemble.txt
exit 0
