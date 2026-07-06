#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")"
./mvnw -q -DskipTests package
java -jar target/XOE-ArcoRotorController-1.0.0.jar
