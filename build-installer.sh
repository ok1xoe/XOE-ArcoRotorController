#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")"

APP_NAME="XOE-MacRotorController"
APP_VERSION="1.0.0"
MAIN_JAR="${APP_NAME}-${APP_VERSION}.jar"
MAIN_CLASS="cz.ok1xoe.macrotor.desktop.MacRotorDesktopApplication"
DEST_DIR="target/installer"
TYPE="${1:-dmg}"

./mvnw -q -DskipTests package

mkdir -p "$DEST_DIR"

jpackage \
  --name "$APP_NAME" \
  --type "$TYPE" \
  --dest "$DEST_DIR" \
  --input target \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --app-version "$APP_VERSION" \
  --icon src/main/resources/icons/macos/xoe-mrc-compass.icns

echo "Installer created in: $DEST_DIR"