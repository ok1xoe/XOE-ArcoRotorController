#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")"

APP_NAME="Arco Rotor Controller"
APP_VERSION="1.0.2"
MAIN_JAR="XOE-ArcoRotorController-${APP_VERSION}.jar"
MAIN_CLASS="cz.ok1xoe.arcorotor.desktop.ArcoRotorDesktopApplication"
DEST_DIR="target/installer"
JPACKAGE_EXTRA_ARGS=""
if [ "$#" -gt 0 ]; then
  TYPE="$1"
else
  case "$(uname -s)" in
    Darwin) TYPE="dmg" ;;
    Linux) TYPE="app-image" ;;
    *) TYPE="app-image" ;;
  esac
fi

case "$TYPE" in
  dmg|pkg)
    ICON="src/main/resources/icons/macos/xoe-arc-compass.icns"
    ;;
  exe|msi)
    ICON="src/main/resources/icons/windows/xoe-arc-compass.ico"
    ;;
  *)
    case "$(uname -s)" in
      Darwin) ICON="src/main/resources/icons/macos/xoe-arc-compass.icns" ;;
      *) ICON="src/main/resources/icons/png/xoe-arc-compass-512x512.png" ;;
    esac
    ;;
esac

case "$TYPE" in
  deb|rpm)
    JPACKAGE_EXTRA_ARGS="--linux-package-name arco-rotor-controller"
    ;;
esac

./mvnw -q -DskipTests package

rm -rf "$DEST_DIR"
mkdir -p "$DEST_DIR"

jpackage \
  --name "$APP_NAME" \
  --type "$TYPE" \
  --dest "$DEST_DIR" \
  --input target \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --app-version "$APP_VERSION" \
  --icon "$ICON" \
  $JPACKAGE_EXTRA_ARGS

echo "Installer created in: $DEST_DIR"
