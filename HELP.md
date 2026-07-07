# XOE ArcoRotorController Help

XOE ArcoRotorController is a cross-platform desktop controller for microHAM ARCO over LAN/TCP.

The application talks directly to ARCO using a TCP socket and Yaesu GS-232 commands. No web server, REST API, USB serial adapter, or internet connection is required for normal operation.

## Requirements

- Java 26 or newer
- ARCO and the computer on the same local network
- ARCO LAN `CONTROL PROTOCOL` enabled
- ARCO `Control Protocol` set to `Yaesu GS-232`
- ARCO IP address and TCP control port

Example endpoint:

```text
192.168.15.131:4001
```

## Start The Application

macOS/Linux:

```bash
./run-desktop.sh
```

Windows:

```bat
run-desktop.cmd
```

The startup scripts build the runnable JAR and then run it with `java -jar`.

## Build The Application

```bash
./mvnw package
```

Generated JAR:

```text
target/XOE-ArcoRotorController-1.1.0.jar
```

Manual run:

```bash
java -jar target/XOE-ArcoRotorController-1.1.0.jar
```

## Build Installer Package

`jpackage` creates native installers only for the OS where it is run.

- macOS: `dmg` or `pkg`
- Windows: `msi` or `exe`
- Linux: `deb`, `rpm`, or app image depending on available tooling

macOS default:

```bash
./build-installer.sh
```

macOS explicit format:

```bash
./build-installer.sh pkg
```

Windows default:

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

## Settings

Open `Settings > Connection and map`.

This dialog contains connection, language, map, and grayline settings.

### Connection

- `IP address`: ARCO IPv4 address.
- `TCP port`: ARCO TCP control port, usually `4001`.
- `Scan network`: scans local IPv4 networks for reachable ARCO devices on the selected port.

The scan checks local subnet candidates only. It does not use the internet.

### Language

- `EN`
- `CZ`

The selected language is stored locally using Java Preferences.

### Map

- `Map`: `Physical` or `Political`.
- `Range km`: map radius from the selected Maidenhead locator.
- `Locator`: Maidenhead locator used as the map center, for example `JO70`, `JO70FD`, or `JN88`.
- `Generate map`: creates a new azimuth map for the selected locator.
- `Clear map`: removes the map overlay.
- `Show grayline`: enables or disables the live day/night overlay.

Map settings are stored locally using Java Preferences.

## First Connection

1. Open `Settings > Connection and map`.
2. Enter ARCO IP address and TCP port.
3. Optionally use `Scan network`.
4. Close the settings dialog.
5. Click `Connect`.

After connection, the large azimuth readout should update every 150 ms.

The main window intentionally keeps only the primary `Connect` / `Disconnect` action visible. Detailed connection and map settings are in the settings dialog.

## Main Controls

### Current Azimuth

The large number shows the current azimuth reported by ARCO.

Double-click the large number to enter a target azimuth manually. Press `Enter` to send it.

Allowed absolute range:

```text
0-360
```

Values outside the range are rejected and no command is sent.

### Relative Azimuth Entry

The manual azimuth field also accepts relative input:

```text
+10
-45
```

Examples:

```text
Current azimuth: 100
Input: +10
Sent target: 110
```

```text
Current azimuth: 100
Input: -45
Sent target: 55
```

Relative input does not wrap around. If the result would go below `0` or above `360`, the app shows an error and sends no command.

Example:

```text
Current azimuth: 350
Input: +20
Result: error, command not sent
```

### Compass

Click inside the compass to send a target azimuth.

- Red line: current azimuth.
- Blue line: target azimuth.
- Numeric degree labels: outer label ring.
- Cardinal directions: inner label ring.
- English cardinal directions: `N`, `E`, `S`, `W`.
- Czech cardinal directions: `S`, `V`, `J`, `Z`.

The `Target Azimuth` panel shows the last target azimuth sent from the manual field, compass, or preset.

### Azimuth Map

When a map is generated from a locator, it is drawn inside the compass.

The map is azimuthal around the selected Maidenhead locator:

- the center is the selected locator
- directions match compass azimuths
- the outer map edge represents the selected `Range km`

Example:

```text
Locator: JN88
Range km: 2000
```

The map edge is approximately 2000 km from the center locator.

### Grayline

The grayline overlay shows approximate day and night regions.

- Daylight areas remain unchanged.
- Night areas are slightly darkened.
- The transition is smooth around the terminator.
- The overlay updates automatically once per minute.
- It can be enabled or disabled with `Show grayline`.

The grayline is calculated locally from the current system time. It does not need internet access.

### Manual Rotation

Use `CCW` and `CW` for manual rotation.

- Press and hold `CCW` to rotate counter-clockwise.
- Press and hold `CW` to rotate clockwise.
- Releasing the button sends the `S` stop command.
- Moving the mouse out of the button while holding it also sends stop.
- Click `STOP` to send an immediate stop command.
- Set `Speed` from `1` to `4` to change ARCO rotation speed.
- The selected speed is stored locally and sent to ARCO after connecting.

### Presets

There are 10 editable preset buttons at the bottom of the main window.

- Left-click a preset to send its stored azimuth.
- Right-click a preset to edit its label and azimuth.
- Hold a preset for 3 seconds to store the currently reported azimuth.

Preset labels can have at most 10 characters.

Preset values are stored locally using Java Preferences.

## TCP Communication Log

Open `Windows > TCP communication`.

The log shows recent TCP commands and responses, including:

- endpoint (`host:port`)
- command sent
- response or error
- timestamp

This is useful for troubleshooting ARCO communication.

## Protocol Commands

The application uses GS-232-compatible commands:

```text
C       read current azimuth
M150    rotate to azimuth 150
L       manual counter-clockwise rotation
R       manual clockwise rotation
S       stop
X1-X4   set rotation speed
```

Commands are sent over TCP and terminated with carriage return (`\r`).

## Offline Map Assets

All map data is bundled in the application.

Current bundled maps:

- `natural-earth-8192.jpg` - saturated Natural Earth II 50m physical raster, `8192 x 4096`
- `natural-earth-countries-8192.png` - Natural Earth 50m Admin 0 countries political map, `8192 x 4096`

Natural Earth data is public domain. See:

```text
src/main/resources/maps/README.md
```

## Error Handling

Errors are displayed under the large azimuth number.

Example relative overflow error:

```text
Relative input would exceed the 0-360 range. Command was not sent.
```

After a relative input overflow, the next user action refreshes the current azimuth from ARCO and replaces the edit field value with the real current azimuth.

## Troubleshooting

If the azimuth does not update:

- Check that ARCO is powered on.
- Check that ARCO and the computer are on the same local network.
- Check ARCO IP address and TCP port in `Settings > Connection and map`.
- Try `Scan network`.
- Verify that ARCO LAN `CONTROL PROTOCOL` is enabled.
- Verify that ARCO `Control Protocol` is set to `Yaesu GS-232`.
- Check that no firewall blocks the TCP connection.

Manual TCP test:

```bash
printf 'C\r' | nc -w 2 192.168.15.131 4001
```

Expected example response:

```text
+0180
```
