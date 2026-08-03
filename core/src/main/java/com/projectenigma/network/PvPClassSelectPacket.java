package com.projectenigma.network;

import com.projectenigma.model.HeroClass;

import java.io.Serializable;

/**
 * Sent once by the guest after they confirm a class on
 * {@code PvPClassSelectScreen}. The host applies its own selection locally
 * (it never sends this packet to itself); the match starts once the host
 * has both selections.
 */
public record PvPClassSelectPacket(HeroClass heroClass) implements Serializable {
    private static final long serialVersionUID = 1L;
}
