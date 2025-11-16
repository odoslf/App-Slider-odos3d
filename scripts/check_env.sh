#!/usr/bin/env bash
set -euo pipefail

echo "== Java =="; java -version || true
echo "== Gradle =="; ./gradlew -v || true
echo "== Propiedades =="; grep -E '^(agpVersion|kotlinVersion|compileSdk|targetSdk|minSdk)=' gradle.properties || true
