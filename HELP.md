# XOE ArcoRotorController

XOE ArcoRotorController is a cross-platform desktop application for controlling a microHAM ARCO rotator controller over LAN/TCP.

The application communicates directly with ARCO through a TCP socket using the Yaesu GS-232 control protocol. It does not use a REST API, USB serial control, or a background server.

## Requirements

- Java 26 or newer
- ARCO and the computer must be connected to the same network
- ARCO LAN `CONTROL PROTOCOL` must be enabled
- ARCO `Control Protocol` must be set to `Yaesu GS-232`
- You must know the ARCO IP address and TCP control port

Example ARCO TCP control endpoint:

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

The startup scripts build the runnable JAR and then start it with `java -jar`. You do not need to start any server.

## Build The Application

Build the runnable JAR:

```bash
./mvnw package
```

The generated application file is:

```text
target/XOE-ArcoRotorController-1.0.0.jar
```

You can run it directly:

```bash
java -jar target/XOE-ArcoRotorController-1.0.0.jar
```

## Build Installer Package

`jpackage` creates native installers, but only for the OS where you run it.

- macOS: run on macOS (`dmg` or `pkg`)
- Windows: run on Windows (`msi` or `exe`)
- Linux: run on Linux (`deb` or `rpm`, depending on available tooling)

macOS (default `dmg`):

```bash
./build-installer.sh
```

macOS (explicit format, for example `pkg`):

```bash
./build-installer.sh pkg
```

Windows (default `msi`):

```bat
build-installer.cmd
```

Windows (explicit format, for example `exe`):

```bat
build-installer.cmd exe
```

Installer output directory:

```text
target/installer
```

## First Connection

1. Enter the ARCO IP address in the `IP address` field.
2. Enter the ARCO TCP control port in the `TCP port` field.
3. Click `Connect`.
4. The large azimuth display should start showing the current heading.

The current azimuth is polled every 150 ms.

## Main Controls

### Current Azimuth

The large number shows the current azimuth reported by ARCO.

Double-click the large number to enter a target azimuth manually. Press `Enter` to send it.

Allowed absolute target range:

```text
0-360
```

Values above `360` are rejected and no command is sent.

### Relative Azimuth Entry

Manual entry also supports relative values:

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

Relative input does not wrap around. If the result would go below `0` or above `360`, the application shows an error and sends no command.

Example:

```text
Current azimuth: 350
Input: +20
Result: error, command not sent
```

### Compass

Click inside the compass to send a target azimuth.

- Red line: current azimuth
- Blue line: target azimuth

### Manual Rotation

Use `CCW` and `CW` for manual rotation.

- Press and hold `CCW` to rotate counter-clockwise.
- Press and hold `CW` to rotate clockwise.
- Releasing the button sends the `S` stop command.
- Moving the mouse out of the button while holding it also sends stop.

### Presets

There are 10 editable preset buttons.

- Left-click a preset to send its stored azimuth.
- Right-click a preset to edit its label and azimuth.
- Hold a preset for 3 seconds to store the currently reported azimuth in that preset.

Preset label limit:

```text
10 characters
```

Preset values are stored locally using Java Preferences, so they persist after the application is restarted.

## Language

The application supports:

- EN
- CZ

The default language is EN. The selected language is stored locally using Java Preferences.

## Protocol Commands

XOE ArcoRotorController uses these GS-232-compatible commands:

```text
C       read current azimuth
M150    rotate to azimuth 150
L       manual counter-clockwise rotation
R       manual clockwise rotation
S       stop
```

Commands are sent to ARCO over TCP and terminated with carriage return (`\r`).

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
- Check that ARCO and the computer are connected to the same network.
- Check that ARCO is reachable on the network.
- Verify the IP address and TCP port.
- Verify that ARCO LAN `CONTROL PROTOCOL` is enabled.
- Verify that ARCO `Control Protocol` is set to `Yaesu GS-232`.
- Check that no firewall blocks the TCP connection.

You can test ARCO manually from a terminal:

```bash
printf 'C\r' | nc -w 2 192.168.15.131 4001
```

Expected example response:

```text
+0180
```
