package com.narrativeplatform.app.chronicle.services;

import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.app.chronicle.models.responses.ChronicleCardResponse;
import com.narrativeplatform.app.chronicle.repositories.ChronicleRepository;
import com.narrativeplatform.app.party.services.PartyAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChronicleService {
    private final ChronicleRepository chronicleRepository;
    private final PartyAccessService partyAccessService;
    private final ChronicleAccessService chronicleAccessService;

    public List<ChronicleCardResponse> list(final UUID partyId) {
        partyAccessService.requireActiveMember(partyId);
        return chronicleRepository.findAllByPartyIdAndStatusNotOrderByUpdatedAtDesc(partyId, ChronicleStatusType.ARCHIVED)
                .stream().map(com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity::toCardResponse).toList();
    }

    @Transactional
    public void archive(final UUID partyId, final UUID chronicleId) {
        final var context = chronicleAccessService.requireNarrator(partyId, chronicleId);
        context.chronicle().setStatus(ChronicleStatusType.ARCHIVED);
        context.chronicle().setArchivedAt(Instant.now());
    }
}
