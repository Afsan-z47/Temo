package com.projectenigma.network;

import java.io.Serializable;

/**
 * Marker packet: "I am leaving the match." Sent opportunistically when a
 * live connection exists (it may not -- see the reconnect notes in
 * DESIGN.md).
 */
public record PvPAbandonPacket(String reason) implements Serializable {
    private static final long serialVersionUID = 1L;
}
