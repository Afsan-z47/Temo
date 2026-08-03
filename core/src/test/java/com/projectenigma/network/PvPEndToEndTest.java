package com.projectenigma.network;

import com.projectenigma.model.BattleAction;
import com.projectenigma.model.Hero;
import com.projectenigma.model.HeroClass;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the real lobby + combat flow end to end: a real {@link PvPServer}
 * bound to a port, a real {@link PvPClient} connecting to it, a class
 * selection packet, a {@link PvPMatch}, action packets, and a full fight to
 * completion -- the same sequence {@code ProjectEnigmaGame}/{@code
 * PvPCombatScreen} drive in the actual game. This is the test that would
 * have caught the original KryoNet/record serialization failure before it
 * ever reached a manual run.
 */
class PvPEndToEndTest {

    @Test
    void fullMatchPlaysToCompletionOverRealSockets() throws Exception {
        try (PvPServer server = new PvPServer(0)) {
            int port = server.port();
            BlockingQueue<PvPBattleState> guestStates = new ArrayBlockingQueue<>(200);
            BlockingQueue<Boolean> guestConnectedSignal = new ArrayBlockingQueue<>(1);

            PvPClient client = new PvPClient();
            try {
                client.setListener(new PvPClient.EventListener() {
                    @Override public void onConnected(boolean isReconnect) {
                        guestConnectedSignal.add(isReconnect);
                    }
                    @Override public void onDisconnected() { }
                    @Override public void onStateReceived(PvPBattleState state) {
                        guestStates.add(state);
                    }
                });

                Hero hostHero = new Hero(HeroClass.WARRIOR);
                PvPMatch[] matchHolder = new PvPMatch[1];

                server.setListener(new PvPServer.EventListener() {
                    @Override public void onGuestConnected(boolean isReconnect) { }
                    @Override public void onGuestDisconnected() { }
                    @Override public void onClassSelected(HeroClass guestClass) {
                        PvPMatch match = new PvPMatch(hostHero, new Hero(guestClass), 777L);
                        matchHolder[0] = match;
                        server.broadcast(match.currentState());
                    }
                    @Override public void onActionReceived(BattleAction action) {
                        server.broadcast(matchHolder[0].applyAction(1, action));
                    }
                    @Override public void onAbandon() { }
                });

                client.connect("127.0.0.1", port);
                assertNotNull(guestConnectedSignal.poll(5, TimeUnit.SECONDS), "guest should connect within 5s");

                client.sendClassSelection(HeroClass.MAGE);
                PvPBattleState state = pollState(guestStates);
                assertEquals(MatchStatus.IN_PROGRESS, state.status());
                assertEquals(0, state.currentTurn(), "host should act first");
                assertEquals(HeroClass.WARRIOR, state.player0().heroClass());
                assertEquals(HeroClass.MAGE, state.player1().heroClass());

                int rounds = 0;
                int hostTurns = 0;
                int guestTurns = 0;
                while (state.status() != MatchStatus.FINISHED && rounds < 500) {
                    if (state.currentTurn() == 0) {
                        // Mirrors PvPCombatScreen on the host machine: apply locally, then broadcast.
                        state = matchHolder[0].applyAction(0, BattleAction.ATTACK);
                        server.broadcast(state);
                        hostTurns++;
                    } else {
                        // Mirrors PvPCombatScreen on the guest machine: send over the wire and wait
                        // for the host's resulting broadcast -- this proves the round trip, not just
                        // the host's local math.
                        client.sendAction(BattleAction.ATTACK);
                        state = pollState(guestStates);
                        guestTurns++;
                    }
                    rounds++;
                }

                assertEquals(MatchStatus.FINISHED, state.status(), "match should finish within 500 ATTACK-only rounds");
                assertTrue(state.outcome() == PvPOutcome.HOST_WINS || state.outcome() == PvPOutcome.GUEST_WINS,
                        "an ATTACK-only match should have a definite winner, got " + state.outcome());
                assertTrue(hostTurns > 0 && guestTurns > 0, "both sides should have actually taken at least one turn");
                assertEquals(MatchStatus.FINISHED, matchHolder[0].status(), "host's own PvPMatch should agree the match is finished");
            } finally {
                client.close();
            }
        }
    }

    private static PvPBattleState pollState(BlockingQueue<PvPBattleState> queue) throws InterruptedException {
        PvPBattleState state = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(state, "timed out waiting for a PvPBattleState broadcast");
        return state;
    }
}
