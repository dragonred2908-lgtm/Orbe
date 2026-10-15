#!/usr/bin/env bash
set -euo pipefail

# Script para generar el reporte de dependencias de la aplicación Android
# Salida: deps.txt

echo "Generando reporte de dependencias para :app (releaseRuntimeClasspath)..."
./gradlew :app:dependencies --configuration releaseRuntimeClasspath > deps.txt

echo "Reporte generado con éxito en deps.txt"
