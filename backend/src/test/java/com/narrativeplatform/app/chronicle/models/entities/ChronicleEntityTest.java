package com.narrativeplatform.app.chronicle.models.entities;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleType;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChronicleEntityTest {
    private ChronicleEntity chronicle;

    @BeforeEach
    void setUp() {
        final var party = new PartyEntity("Test Party", "test-party", null, null, null);
        final var creator = new UserEntity("author", "Author", "hash");
        chronicle = new ChronicleEntity(party, creator, ChronicleType.GAME, ChronicleStatusType.PUBLISHED, "Title");
    }

    @Test
    void cardPrefersTheSynopsisWhenPublishedAndAvailable() {
        chronicle.setSynopsis("A short teaser.");
        chronicle.setGeneratedPreview("A much longer raw excerpt of the adaptation.");

        final var card = chronicle.toCardResponse(null, null, null);

        assertEquals("A short teaser.", card.preview());
    }

    @Test
    void cardFallsBackToGeneratedPreviewWhenSynopsisIsMissing() {
        chronicle.setSynopsis(null);
        chronicle.setGeneratedPreview("A much longer raw excerpt of the adaptation.");

        final var card = chronicle.toCardResponse(null, null, null);

        assertEquals("A much longer raw excerpt of the adaptation.", card.preview());
    }

    @Test
    void cardFallsBackToGeneratedPreviewWhenNotPublishedEvenIfSynopsisExists() {
        chronicle.setStatus(ChronicleStatusType.AI_PENDING);
        chronicle.setSynopsis("A short teaser.");
        chronicle.setGeneratedPreview("Old preview from a previous version.");

        final var card = chronicle.toCardResponse(null, null, null);

        assertEquals("Old preview from a previous version.", card.preview());
    }
}
