package cz.ok1xoe.arcorotor.desktop;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TcpRotorClientTests {

    @Test
    void readsAzimuthOverTcp() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            CompletableFuture<String> command = CompletableFuture.supplyAsync(() -> {
                try (Socket socket = serverSocket.accept()) {
                    String receivedCommand = readCommand(socket);
                    socket.getOutputStream().write("+0180\r".getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().flush();
                    return receivedCommand;
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });

            TcpRotorClient client = new TcpRotorClient("127.0.0.1", serverSocket.getLocalPort());

            assertThat(client.readAzimuth()).isEqualTo(180);
            assertThat(command.get(2, TimeUnit.SECONDS)).isEqualTo("C");
        }
    }

    @Test
    void reportsSuccessfulCommunicationEvent() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            CompletableFuture<String> command = CompletableFuture.supplyAsync(() -> {
                try (Socket socket = serverSocket.accept()) {
                    String receivedCommand = readCommand(socket);
                    socket.getOutputStream().write("+0180\r".getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().flush();
                    return receivedCommand;
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            CompletableFuture<TcpRotorClient.CommunicationEvent> communicationEvent = new CompletableFuture<>();

            TcpRotorClient client = new TcpRotorClient("127.0.0.1", serverSocket.getLocalPort(), communicationEvent::complete);

            assertThat(client.readAzimuth()).isEqualTo(180);
            TcpRotorClient.CommunicationEvent event = communicationEvent.get(2, TimeUnit.SECONDS);
            assertThat(command.get(2, TimeUnit.SECONDS)).isEqualTo("C");
            assertThat(event.host()).isEqualTo("127.0.0.1");
            assertThat(event.port()).isEqualTo(serverSocket.getLocalPort());
            assertThat(event.command()).isEqualTo("C");
            assertThat(event.response()).isEqualTo("+0180\r");
            assertThat(event.responseExpected()).isTrue();
            assertThat(event.successful()).isTrue();
        }
    }

    @Test
    void reportsFailedCommunicationEvent() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            CompletableFuture<String> command = CompletableFuture.supplyAsync(() -> {
                try (Socket socket = serverSocket.accept()) {
                    return readCommand(socket);
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            CompletableFuture<TcpRotorClient.CommunicationEvent> communicationEvent = new CompletableFuture<>();

            TcpRotorClient client = new TcpRotorClient("127.0.0.1", serverSocket.getLocalPort(), communicationEvent::complete);

            assertThatThrownBy(client::readAzimuth)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("did not return heading");
            TcpRotorClient.CommunicationEvent event = communicationEvent.get(2, TimeUnit.SECONDS);
            assertThat(command.get(2, TimeUnit.SECONDS)).isEqualTo("C");
            assertThat(event.command()).isEqualTo("C");
            assertThat(event.response()).isNull();
            assertThat(event.responseExpected()).isTrue();
            assertThat(event.successful()).isFalse();
            assertThat(event.errorMessage()).contains("did not return heading");
        }
    }

    @Test
    void sendsMoveCommandOverTcp() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            CompletableFuture<String> command = CompletableFuture.supplyAsync(() -> {
                try (Socket socket = serverSocket.accept()) {
                    return readCommand(socket);
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });

            TcpRotorClient client = new TcpRotorClient("127.0.0.1", serverSocket.getLocalPort());
            client.moveToAzimuth(150);

            assertThat(command.get(2, TimeUnit.SECONDS)).isEqualTo("M150");
        }
    }

    @Test
    void acceptsTargetAzimuthUpTo360Degrees() throws Exception {
        assertCommandSent(client -> client.moveToAzimuth(360), "M360");
    }

    @Test
    void rejectsTargetAzimuthAbove360Degrees() {
        TcpRotorClient client = new TcpRotorClient("127.0.0.1", 4001);

        assertThatThrownBy(() -> client.moveToAzimuth(361))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendsManualRotationCommandsOverTcp() throws Exception {
        assertCommandSent(client -> client.rotateCounterClockwise(), "L");
        assertCommandSent(client -> client.rotateClockwise(), "R");
        assertCommandSent(client -> client.stop(), "S");
    }

    private static void assertCommandSent(ClientAction action, String expectedCommand) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            CompletableFuture<String> command = CompletableFuture.supplyAsync(() -> {
                try (Socket socket = serverSocket.accept()) {
                    return readCommand(socket);
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });

            TcpRotorClient client = new TcpRotorClient("127.0.0.1", serverSocket.getLocalPort());
            action.run(client);

            assertThat(command.get(2, TimeUnit.SECONDS)).isEqualTo(expectedCommand);
        }
    }

    private static String readCommand(Socket socket) throws IOException {
        StringBuilder command = new StringBuilder();
        int value;
        while ((value = socket.getInputStream().read()) != -1) {
            if (value == '\r') {
                break;
            }
            command.append((char) value);
        }
        return command.toString();
    }

    @FunctionalInterface
    private interface ClientAction {

        void run(TcpRotorClient client);
    }
}
