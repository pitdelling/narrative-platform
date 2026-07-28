package com.narrativeplatform.app.party.models.requests;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransferPartyRequest(@NotNull UUID newOwnerId) {
}
