package com.projectenigma.network;

import java.io.Serializable;
import java.util.List;

/**
 * The single message the host broadcasts after every resolved turn (and on
 * reconnect). This -- not the {@code Hero} objects themselves -- is what
 * goes over the wire.
 *
 * <p>Player 0 is always the host, player 1 is always the guest. Each
 * client maps "player 0 / player 1" to "me / opponent" using the player
 * index it was assigned when the match started.
 *
 * @param player0     host's hero snapshot
 * @param player1     guest's hero snapshot
 * @param currentTurn 0 or 1 -- whose action is awaited next; meaningless
 *                    when {@code outcome != ONGOING}
 * @param outcome     match-level result
 * @param status      connection-lifecycle status
 * @param log         short list of the most recent battle-log lines (the
 *                    messages produced by the turn that produced this
 *                    state); capped by the sender, see {@link PvPMatch}
 */
public record PvPBattleState(HeroSnapshot player0, HeroSnapshot player1, int currentTurn,
                              PvPOutcome outcome, MatchStatus status, List<String> log) implements Serializable {
    private static final long serialVersionUID = 1L;
}
