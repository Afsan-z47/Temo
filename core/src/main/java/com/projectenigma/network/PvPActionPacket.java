package com.projectenigma.network;

import com.projectenigma.model.BattleAction;

import java.io.Serializable;

/**
 * Sent by a client to the host when it is that client's turn. The host
 * (via {@link PvPMatch}) re-validates the turn and the action before
 * touching any hero state -- this packet is a request, not a command.
 */
public record PvPActionPacket(BattleAction action) implements Serializable {
    private static final long serialVersionUID = 1L;
}
