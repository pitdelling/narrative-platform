package com.narrativeplatform.app.chronicle.services;

import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.chronicle.repositories.ChronicleRepository;
import com.narrativeplatform.app.party.models.entities.PartyMemberEntity;
import com.narrativeplatform.app.party.services.PartyAccessService;
import com.narrativeplatform.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChronicleAccessService {
    private final ChronicleRepository chronicleRepository;
    private final PartyAccessService partyAccessService;

    public AccessContext requireMember(final UUID partyId, final UUID chronicleId) {
        final var membership = partyAccessService.requireActiveMember(partyId);
        final var chronicle = requireChronicle(partyId, chronicleId);
        return new AccessContext(chronicle, membership);
    }

    public AccessContext requireNarrator(final UUID partyId, final UUID chronicleId) {
        final var membership = partyAccessService.requireNarrator(partyId);
        final var chronicle = requireChronicle(partyId, chronicleId);
        return new AccessContext(chronicle, membership);
    }

    private ChronicleEntity requireChronicle(final UUID partyId, final UUID chronicleId) {
        final var chronicle = chronicleRepository.findOneById(chronicleId)
                .orElseThrow(() -> new NotFoundException("Chronicle not found."));
        if (!chronicle.getParty().getId().equals(partyId)) {
            throw new NotFoundException("Chronicle not found.");
        }
        return chronicle;
    }

    public record AccessContext(ChronicleEntity chronicle, PartyMemberEntity membership) {
        public boolean narrator() {
            return membership.getRole().canNarrate();
        }
    }
}
