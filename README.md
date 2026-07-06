# XOE ArcoRotorController

Cross-platform desktop application for controlling a microHAM ARCO rotator controller over LAN/TCP.

The app communicates directly with ARCO via TCP socket using the Yaesu GS-232 control protocol.
No REST API, USB serial bridge, or background server is required.

## Features

- Live azimuth readout (polling every 150 ms)
- Click-to-rotate compass control with degree labels and localized cardinal directions
- Manual absolute azimuth input (`0-360`)
- Relative input support (`+10`, `-45`)
- Editable preset buttons for frequently used azimuths
- Input validation with clear error handling
- Cross-platform build and installer workflow (macOS, Windows, Linux)

## Requirements

- Java 26+
- ARCO and your computer must be on the same network
- ARCO LAN `CONTROL PROTOCOL` enabled
- ARCO `Control Protocol` set to `Yaesu GS-232`
- Known ARCO endpoint (`IP:PORT`), e.g.:

```text
192.168.15.131:4001
```

## Quick Start

### macOS / Linux

```bash
./run-desktop.sh
```

### Windows

```bat
run-desktop.cmd
```

These scripts build the runnable JAR and then start the app.

## Build

```bash
./mvnw package
```

Output JAR:

```text
target/XOE-ArcoRotorController-1.0.2.jar
```

Run manually:

```bash
java -jar target/XOE-ArcoRotorController-1.0.2.jar
```

## Build Native Installers

`jpackage` can produce installers only for the OS you are currently running on.

- macOS -> `dmg` / `pkg`
- Windows -> `msi` / `exe`
- Linux -> `deb` / `rpm` (depends on installed tooling)

### macOS

Default (`dmg`):

```bash
./build-installer.sh
```

Explicit format (example `pkg`):

```bash
./build-installer.sh pkg
```

### Windows

Default (`msi`):

```bat
build-installer.cmd
```

Explicit format (example `exe`):

```bat
build-installer.cmd exe
```

Installer output:

```text
target/installer
```

## First Connection

1. Enter ARCO IP into `IP address`.
2. Enter ARCO TCP port into `TCP port`.
3. Click `Connect`.
4. Verify the large azimuth display starts updating.

## Controls

### Current Azimuth

- Displays current heading reported by ARCO.
- Enter a target azimuth directly in the large azimuth field and press `Enter` to send.

### Target Azimuth

- Shows the last sent target azimuth.

### Manual Rotation

- Press and hold `CCW` / `CW` for manual rotation.
- Releasing the button sends `S` stop.
- `STOP` sends an immediate stop command.
- `Speed` sends GS-232 `X1` to `X4` speed commands and is remembered between starts.

### Absolute Input

- Allowed range: `0-360`
- Values above `360` are rejected.

### Relative Input

Supports:

```text
+10
-45
```

Example:

```text
Current azimuth: 100
Input: +10
Sent target: 110
```

Relative values do **not** wrap around.
If result is outside `0-360`, command is not sent and an error is shown.

### Compass

- Click inside the compass to send target azimuth.
- The compass shows degree labels every `30°` and localized cardinal directions.

### Presets

- Presets are shown at the bottom in two rows of five buttons.
- Click a preset to send its stored azimuth.
- Right-click a preset to edit its label and azimuth.
- Hold a preset for 3 seconds to store the currently reported azimuth.

## Project Structure (important files)

- `src/main/java/.../ArcoRotorDesktopApplication.java` – application entry point
- `src/main/java/.../TcpRotorClient.java` – TCP communication with ARCO
- `run-desktop.sh`, `run-desktop.cmd` – quick run scripts
- `build-installer.sh`, `build-installer.cmd` – native installer scripts
- `src/main/resources/icons/` – platform icon assets
- `HELP.md` – extended user help

## License

This project is licensed under the GNU General Public License v3.0 or later (`GPL-3.0-or-later`).

You may use, study, share, and modify the application. If you distribute modified versions, they must remain open source under the same GPL terms. See `LICENSE` for the full license text.
