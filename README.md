# XOE ArcoRotorController

Cross-platform desktop application for controlling a microHAM ARCO rotator controller over LAN/TCP.

The app communicates directly with ARCO through a TCP socket using the Yaesu GS-232 control protocol. It does not require a REST API, USB serial bridge, or background server.

## Features

- Live azimuth readout, polled every 150 ms
- Click-to-rotate compass with degree labels and localized cardinal directions
- Absolute azimuth entry from `0` to `360`
- Relative azimuth entry such as `+10` or `-45`
- Manual `CCW`, `CW`, and `STOP` controls
- Rotation speed control from `1` to `4`
- Ten editable azimuth presets
- TCP communication log window
- Local network scan for ARCO on the configured TCP port
- Offline azimuth map generated from a Maidenhead locator
- Physical and political offline map styles
- Configurable map range in kilometers
- Optional live grayline overlay that updates with time
- English and Czech UI
- Native installer workflow for macOS, Windows, and Linux

## Requirements

- Java 26+
- ARCO and the computer must be on the same local network
- ARCO LAN `CONTROL PROTOCOL` enabled
- ARCO `Control Protocol` set to `Yaesu GS-232`
- Known ARCO endpoint (`IP:PORT`), for example:

```text
192.168.15.131:4001
```

## Quick Start

macOS / Linux:

```bash
./run-desktop.sh
```

Windows:

```bat
run-desktop.cmd
```

The scripts build the runnable JAR and then start the app.

## Build

```bash
./mvnw package
```

Output JAR:

```text
target/XOE-ArcoRotorController-1.1.0.jar
```

Run manually:

```bash
java -jar target/XOE-ArcoRotorController-1.1.0.jar
```

## Build Native Installers

`jpackage` can produce installers only for the OS you are currently running on.

- macOS -> `dmg` / `pkg`
- Windows -> `msi` / `exe`
- Linux -> `deb` / `rpm` (depends on installed tooling)

macOS default (`dmg`):

```bash
./build-installer.sh
```

macOS explicit format:

```bash
./build-installer.sh pkg
```

Windows default (`msi`):

```bat
build-installer.cmd
```

Windows explicit format:

```bat
build-installer.cmd exe
```

Installer output:

```text
target/installer
```

## First Connection

Connection details are configured in `Settings > Connection and map`.

1. Open `Settings > Connection and map`.
2. Enter the ARCO IP address.
3. Enter the ARCO TCP control port.
4. Optionally click `Scan network` to search the local IPv4 network for ARCO.
5. Close the settings dialog.
6. Click `Connect`.

The large azimuth display should start updating after a successful connection.

## Main Controls

### Current Azimuth

The large number shows the current azimuth reported by ARCO.

Double-click the large azimuth value to edit it. Enter an absolute azimuth and press `Enter` to send a move command.

Allowed absolute range:

```text
0-360
```

### Relative Input

The azimuth editor also accepts relative values:

```text
+10
-45
```

Relative values do not wrap around. If the result would be outside `0-360`, no command is sent and an error is shown.

### Compass

- Click inside the compass to send a target azimuth.
- Red line shows current azimuth.
- Blue line shows target azimuth.
- Numeric degree labels are on the outer label ring.
- Cardinal directions are on the inner label ring.
- Cardinal directions are localized (`N/E/S/W` in English, `S/V/J/Z` in Czech).

### Manual Rotation

- Press and hold `CCW` or `CW` for manual rotation.
- Releasing the button sends the `S` stop command.
- Moving the mouse out of a held button also sends stop.
- `STOP` sends an immediate stop command.
- `Speed` sends GS-232 `X1` to `X4` speed commands and is remembered between starts.

### Presets

- Ten presets are shown at the bottom in two rows of five buttons.
- Left-click a preset to send its stored azimuth.
- Right-click a preset to edit its label and azimuth.
- Hold a preset for 3 seconds to store the currently reported azimuth.
- Preset labels can have at most 10 characters.

## Map And Grayline

Map settings are in `Settings > Connection and map`.

- `Map`: choose `Physical` or `Political`.
- `Range km`: sets the map radius in kilometers from the Maidenhead locator.
- `Locator`: enter a Maidenhead locator such as `JO70`, `JO70FD`, or `JN88`.
- `Generate map`: creates the compass map for the selected locator and map settings.
- `Clear map`: removes the map overlay.
- `Show grayline`: toggles the live day/night overlay.

All map data is bundled in the application. Internet access is not needed at runtime.

Bundled map sources:

- Physical map: Natural Earth II 50m raster, rendered as `8192 x 4096` JPEG
- Political map: Natural Earth 50m Admin 0 countries, rendered as `8192 x 4096` PNG

Natural Earth data is public domain. See `src/main/resources/maps/README.md`.

## Project Structure

- `src/main/java/.../ArcoRotorDesktopApplication.java` - desktop UI, compass, map, grayline
- `src/main/java/.../TcpRotorClient.java` - TCP communication with ARCO
- `src/main/resources/maps/` - bundled offline map assets
- `src/main/resources/icons/` - platform icon assets
- `src/main/resources/cz/ok1xoe/arcorotor/desktop/messages_*.properties` - UI translations
- `src/test/java/...` - unit tests
- `run-desktop.sh`, `run-desktop.cmd` - quick run scripts
- `build-installer.sh`, `build-installer.cmd` - native installer scripts
- `HELP.md` - detailed user help

## License

This project is licensed under the GNU General Public License v3.0 or later (`GPL-3.0-or-later`).

You may use, study, share, and modify the application. If you distribute modified versions, they must remain open source under the same GPL terms. See `LICENSE` for the full license text.
