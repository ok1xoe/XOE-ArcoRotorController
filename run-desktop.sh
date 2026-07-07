#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")"
./mvnw -q -DskipTests package
java -jar target/XOE-ArcoRotorController-1.1.0.jar
