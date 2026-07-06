package cz.ok1xoe.arcorotor.desktop;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TcpRotorClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(2);
    private static final Pattern AZ_VALUE_RESPONSE = Pattern.compile("AZ\\s*=\\s*(\\d{1,4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMERIC_RESPONSE = Pattern.compile("[+-]?(\\d{3,4})");
    private static final CommunicationListener NOOP_COMMUNICATION_LISTENER = event -> {
    };

    private final String host;
    private final int port;
    private final CommunicationListener communicationListener;

    public TcpRotorClient(String host, int port) {
        this(host, port, null);
    }

    public TcpRotorClient(String host, int port, CommunicationListener communicationListener) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("IP address is required.");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("TCP port must be between 1 and 65535.");
        }
        this.host = host.trim();
        this.port = port;
        this.communicationListener = communicationListener == null
                ? NOOP_COMMUNICATION_LISTENER
                : communicationListener;
    }

    public int readAzimuth() {
        String response = sendCommand("C", true);
        return parseAzimuth(response);
    }

    public void moveToAzimuth(int degrees) {
        if (degrees < 0 || degrees > 360) {
            throw new IllegalArgumentException("Azimuth must be between 0 and 360 degrees.");
        }
        sendCommand("M" + String.format(Locale.ROOT, "%03d", degrees), false);
    }

    public void rotateCounterClockwise() {
        sendCommand("L", false);
    }

    public void rotateClockwise() {
        sendCommand("R", false);
    }

    public void stop() {
        sendCommand("S", false);
    }

    public void setRotationSpeed(int speed) {
        if (speed < 1 || speed > 4) {
            throw new IllegalArgumentException("Rotation speed must be between 1 and 4.");
        }
        sendCommand("X" + speed, false);
    }

    public static boolean isArcoReachable(String host, int port) {
        return isArcoReachable(host, port, null);
    }

    public static boolean isArcoReachable(String host, int port, CommunicationListener communicationListener) {
        try {
            new TcpRotorClient(host, port, communicationListener).readAzimuth();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String sendCommand(String command, boolean expectResponse) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), (int) CONNECT_TIMEOUT.toMillis());
            socket.setSoTimeout((int) READ_TIMEOUT.toMillis());

            OutputStream outputStream = socket.getOutputStream();
            outputStream.write((command + "\r").getBytes(StandardCharsets.US_ASCII));
            outputStream.flush();

            if (!expectResponse) {
                notifyCommunication(command, "", null, false);
                return "";
            }

            String response = readResponse(socket.getInputStream());
            notifyCommunication(command, response, null, true);
            return response;
        } catch (IOException exception) {
            IllegalStateException failure = new IllegalStateException("TCP communication with ARCO failed: " + exception.getMessage(), exception);
            notifyCommunication(command, null, failure.getMessage(), expectResponse);
            throw failure;
        } catch (RuntimeException exception) {
            notifyCommunication(command, null, exception.getMessage(), expectResponse);
            throw exception;
        }
    }

    private void notifyCommunication(String command, String response, String errorMessage, boolean responseExpected) {
        try {
            communicationListener.onCommunication(new CommunicationEvent(host, port, command, response, errorMessage, responseExpected));
        } catch (RuntimeException ignored) {
            // Communication diagnostics must never affect rotor control.
        }
    }

    private static String readResponse(InputStream inputStream) throws IOException {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        long deadline = System.nanoTime() + READ_TIMEOUT.toNanos();

        while (System.nanoTime() < deadline) {
            int value;
            try {
                value = inputStream.read();
            } catch (SocketTimeoutException exception) {
                break;
            }
            if (value == -1) {
                break;
            }
            response.write(value);
            if (value == '\r' || value == '\n') {
                break;
            }
        }

        String rawResponse = response.toString(StandardCharsets.US_ASCII);
        if (rawResponse.isBlank()) {
            throw new IllegalStateException("ARCO did not return heading before timeout.");
        }
        return rawResponse;
    }

    private static int parseAzimuth(String rawResponse) {
        Matcher azMatcher = AZ_VALUE_RESPONSE.matcher(rawResponse);
        if (azMatcher.find()) {
            return validateAzimuth(Integer.parseInt(azMatcher.group(1)));
        }

        Matcher numericMatcher = NUMERIC_RESPONSE.matcher(rawResponse);
        if (numericMatcher.find()) {
            return validateAzimuth(Integer.parseInt(numericMatcher.group(1)));
        }

        throw new IllegalStateException("Cannot parse azimuth from ARCO response: " + printable(rawResponse));
    }

    private static int validateAzimuth(int degrees) {
        if (degrees < 0 || degrees > 450) {
            throw new IllegalStateException("ARCO returned azimuth outside supported range: " + degrees);
        }
        return degrees;
    }

    private static String printable(String rawResponse) {
        return rawResponse
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    public record CommunicationEvent(
            String host,
            int port,
            String command,
            String response,
            String errorMessage,
            boolean responseExpected
    ) {

        public boolean successful() {
            return errorMessage == null;
        }
    }

    @FunctionalInterface
    public interface CommunicationListener {

        void onCommunication(CommunicationEvent event);
    }
}
