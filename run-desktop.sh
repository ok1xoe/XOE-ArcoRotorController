#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")"
./mvnw -q -DskipTests package
java -jar target/XOE-MacRotorController-0.0.1-SNAPSHOT.jar
