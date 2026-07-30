package com.narrativeplatform.app.chronicle.services;

import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleType;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.chronicle.models.projections.ActiveTurnUserProjection;
import com.narrativeplatform.app.chronicle.models.projections.ChronicleTurnProgressProjection;
import com.narrativeplatform.app.chronicle.models.responses.ChronicleCardResponse;
import com.narrativeplatform.app.chronicle.repositories.ChronicleRepository;
import com.narrativeplatform.app.chronicle.repositories.GameTurnRepository;
import com.narrativeplatform.app.party.services.PartyAccessService;
import com.narrativeplatform.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChronicleService {
    private final ChronicleRepository chronicleRepository;
    private final GameTurnRepository gameTurnRepository;
    private final PartyAccessService partyAccessService;
    private final ChronicleAccessService chronicleAccessService;
    private final CurrentUserService currentUserService;

    public List<ChronicleCardResponse> list(final UUID partyId) {
        partyAccessService.requireActiveMember(partyId);
        final var chronicles = chronicleRepository.findAllByPartyIdAndStatusNotOrderByUpdatedAtDesc(partyId, ChronicleStatusType.ARCHIVED);
        final var currentUserId = currentUserService.require().id();
        final var inProgressGameIds = chronicles.stream()
                .filter(chronicle -> chronicle.getType() == ChronicleType.GAME && chronicle.getStatus() == ChronicleStatusType.IN_PROGRESS)
                .map(ChronicleEntity::getId)
                .toList();
        final Map<UUID, ChronicleTurnProgressProjection> progressByChronicleId = inProgressGameIds.isEmpty()
                ? Map.of()
                : gameTurnRepository.findProgressByChronicleIds(inProgressGameIds).stream()
                        .collect(Collectors.toMap(ChronicleTurnProgressProjection::getChronicleId, Function.identity()));
        final Map<UUID, UUID> activeTurnUserByChronicleId = inProgressGameIds.isEmpty()
                ? Map.of()
                : gameTurnRepository.findActiveTurnUsersByChronicleIds(inProgressGameIds).stream()
                        .collect(Collectors.toMap(ActiveTurnUserProjection::getChronicleId, ActiveTurnUserProjection::getUserId));
        return chronicles.stream()
                .map(chronicle -> {
                    final var progress = progressByChronicleId.get(chronicle.getId());
                    final var activeTurnUserId = activeTurnUserByChronicleId.get(chronicle.getId());
                    final var awaitingCurrentUser = activeTurnUserId == null ? null : activeTurnUserId.equals(currentUserId);
                    return chronicle.toCardResponse(
                            progress == null ? null : (int) progress.getCompletedTurns(),
                            progress == null ? null : (int) progress.getTotalTurns(),
                            awaitingCurrentUser
                    );
                })
                .toList();
    }

    @Transactional
    public void archive(final UUID partyId, final UUID chronicleId) {
        final var context = chronicleAccessService.requireNarrator(partyId, chronicleId);
        context.chronicle().setStatus(ChronicleStatusType.ARCHIVED);
        context.chronicle().setArchivedAt(Instant.now());
    }
}
