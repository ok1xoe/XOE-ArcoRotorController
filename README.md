# XOE MacRotorController

Cross-platform desktop application for controlling a microHAM ARCO rotator controller over LAN/TCP.

The app communicates directly with ARCO via TCP socket using the Yaesu GS-232 control protocol.
No REST API, USB serial bridge, or background server is required.

## Features

- Live azimuth readout (polling every 150 ms)
- Click-to-rotate compass control
- Manual absolute azimuth input (`0-360`)
- Relative input support (`+10`, `-45`)
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
target/XOE-MacRotorController-1.0.0.jar
```

Run manually:

```bash
java -jar target/XOE-MacRotorController-1.0.0.jar
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
- Double-click value to enter target azimuth, press `Enter` to send.

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

## Project Structure (important files)

- `src/main/java/.../MacRotorDesktopApplication.java` – application entry point
- `src/main/java/.../TcpRotorClient.java` – TCP communication with ARCO
- `run-desktop.sh`, `run-desktop.cmd` – quick run scripts
- `build-installer.sh`, `build-installer.cmd` – native installer scripts
- `src/main/resources/icons/` – platform icon assets
- `HELP.md` – extended user help

## License

No license file is currently defined in this repository.