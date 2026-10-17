#!/usr/bin/env bash
# Builds (first time only) and starts the Smart Lost & Found Portal.
cd "$(dirname "$0")"
JAR=target/smart-lost-found-portal-1.0.0.jar
if [ ! -f "$JAR" ]; then
  echo "Building project (first run, downloads dependencies)..."
  mvn -q clean package -DskipTests || exit 1
fi
java -jar "$JAR"
