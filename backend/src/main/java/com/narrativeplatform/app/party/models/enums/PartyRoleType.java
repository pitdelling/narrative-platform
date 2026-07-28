package com.narrativeplatform.app.party.models.enums;

public enum PartyRoleType {
    OWNER,
    NARRATOR,
    PLAYER;

    public boolean canNarrate() {
        return this == OWNER || this == NARRATOR;
    }
}
