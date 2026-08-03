package com.projectenigma.network;

/** Connection-lifecycle status of a PvP match, independent of the fight outcome. */
public enum MatchStatus {
    /** Both players connected; waiting on / resolving actions normally. */
    IN_PROGRESS,
    /** One player disconnected. State is frozen; host is polling for reconnection. */
    WAITING_FOR_RECONNECT,
    /** The match ended: a hero was defeated, someone ran (surrendered), or a player abandoned. */
    FINISHED
}
