package com.projectenigma.network;

import com.projectenigma.model.BattleAction;
import com.projectenigma.model.HeroClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link PvPConnection} over real localhost sockets -- this is
 * the layer that replaced KryoNet, so it's worth pinning down with actual
 * I/O rather than only unit-testing the pure logic above it. In particular
 * this guards against the exact failure this project hit once already: a
 * serialization library silently failing on a {@code record}-shaped DTO.
 */
class PvPConnectionTest {

    @Test
    void recordsWithNestedRecordsEnumsAndListsRoundTripCorrectly() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            BlockingQueue<Object> serverReceived = new ArrayBlockingQueue<>(10);

            Thread acceptThread = new Thread(() -> acceptOnce(serverSocket, serverReceived));
            acceptThread.start();

            Socket clientSocket = new Socket("127.0.0.1", port);
            PvPConnection client = new PvPConnection(clientSocket, noOpListener());
            acceptThread.join(2000);

            HeroSnapshot host = new HeroSnapshot("Sentinel", HeroClass.WARRIOR, 3, 88, 120, 4, 8);
            HeroSnapshot guest = new HeroSnapshot("Hacker", HeroClass.MAGE, 2, 60, 88, 10, 16);
            List<String> log = new ArrayList<>();
            log.add("Your attack deals 14 damage.");
            PvPBattleState state = new PvPBattleState(host, guest, 1, PvPOutcome.ONGOING, MatchStatus.IN_PROGRESS, log);

            client.send(state);
            Object received = poll(serverReceived);
            assertTrue(received instanceof PvPBattleState);
            PvPBattleState roundTripped = (PvPBattleState) received;
            assertEquals(state, roundTripped, "record equals() is component-wise, so a full round trip must be exactly equal");
            assertEquals(HeroClass.WARRIOR, roundTripped.player0().heroClass(), "nested enum must survive serialization");
            assertEquals(1, roundTripped.log().size());
            assertEquals(log.get(0), roundTripped.log().get(0));

            client.close();
        }
    }

    @Test
    void repeatedSendsSurviveTheStreamResetAfterEachOne() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            BlockingQueue<Object> serverReceived = new ArrayBlockingQueue<>(10);
            Thread acceptThread = new Thread(() -> acceptOnce(serverSocket, serverReceived));
            acceptThread.start();

            Socket clientSocket = new Socket("127.0.0.1", port);
            PvPConnection client = new PvPConnection(clientSocket, noOpListener());
            acceptThread.join(2000);

            BattleAction[] actions = BattleAction.values();
            for (int i = 0; i < actions.length; i++) {
                client.send(new PvPActionPacket(actions[i]));
            }
            for (int i = 0; i < actions.length; i++) {
                Object received = poll(serverReceived);
                assertTrue(received instanceof PvPActionPacket);
                assertEquals(actions[i], ((PvPActionPacket) received).action(),
                        "action #" + i + " should arrive in order and unaffected by reset() calls between sends");
            }
            client.close();
        }
    }

    @Test
    void closingOneSideNotifiesTheOtherViaOnClosed() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            BlockingQueue<Object> closedSignal = new ArrayBlockingQueue<>(1);
            Thread acceptThread = new Thread(() -> {
                try {
                    Socket socket = serverSocket.accept();
                    new PvPConnection(socket, new PvPConnection.Listener() {
                        @Override public void onReceived(Object payload) { }
                        @Override public void onClosed() { closedSignal.add("closed"); }
                    });
                } catch (IOException ignored) {
                }
            });
            acceptThread.start();

            Socket clientSocket = new Socket("127.0.0.1", port);
            PvPConnection client = new PvPConnection(clientSocket, noOpListener());
            acceptThread.join(2000);

            client.close(); // simulate the guest dropping
            assertNotNull(closedSignal.poll(2, TimeUnit.SECONDS),
                    "the still-open side should observe onClosed() once its peer's socket closes");
        }
    }

    private static void acceptOnce(ServerSocket serverSocket, BlockingQueue<Object> receivedInto) {
        try {
            Socket socket = serverSocket.accept();
            new PvPConnection(socket, new PvPConnection.Listener() {
                @Override public void onReceived(Object payload) {
                    receivedInto.add(payload);
                }
                @Override public void onClosed() { }
            });
        } catch (IOException ignored) {
        }
    }

    private static PvPConnection.Listener noOpListener() {
        return new PvPConnection.Listener() {
            @Override public void onReceived(Object payload) { }
            @Override public void onClosed() { }
        };
    }

    private static Object poll(BlockingQueue<Object> queue) throws InterruptedException {
        Object value = queue.poll(2, TimeUnit.SECONDS);
        assertNotNull(value, "timed out waiting for an object to arrive over the socket");
        return value;
    }
}
