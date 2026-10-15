#!/usr/bin/env bash
set -euo pipefail

# Script para generar el reporte de dependencias de la aplicación Android
# Salida: deps.txt

echo "Generando reporte de dependencias para :app (releaseRuntimeClasspath)..."
if [ -x "./gradlew" ]; then
    ./gradlew :app:dependencies --configuration releaseRuntimeClasspath > deps.txt
elif command -v gradle &>/dev/null; then
    gradle :app:dependencies --configuration releaseRuntimeClasspath > deps.txt
else
    echo "Error: No se encontró ./gradlew ni gradle instalado." >&2
    exit 1
fi

echo "Reporte generado con éxito en deps.txt ($(wc -l < deps.txt) líneas)"
