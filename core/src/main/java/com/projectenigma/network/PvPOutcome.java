package com.projectenigma.network;

/**
 * Match-level result, distinct from {@code com.projectenigma.model.BattleOutcome}
 * (which is always relative to whichever combatant just acted). PvPMatch
 * translates each engine turn into one of these before broadcasting.
 */
public enum PvPOutcome {
    ONGOING,
    HOST_WINS,
    GUEST_WINS,
    ABANDONED
}
