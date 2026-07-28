package com.narrativeplatform.app.aijob.services;

import com.narrativeplatform.app.aijob.models.entities.AiJobEntity;
import com.narrativeplatform.app.aijob.models.enums.AiJobStatusType;
import com.narrativeplatform.app.aijob.repositories.AiJobRepository;
import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.shared.exceptions.ConflictException;
import com.narrativeplatform.shared.integrations.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiJobService {
    private static final String IDEMPOTENCY_KEY_SEPARATOR = ":";
    private static final Set<AiJobStatusType> ACTIVE_STATUSES = Set.of(
            AiJobStatusType.PENDING,
            AiJobStatusType.PROCESSING
    );

    private final AiJobRepository aiJobRepository;
    private final OpenAiClient openAiClient;

    @Transactional
    public void enqueue(final ChronicleEntity chronicle, final UserEntity requestedBy) {
        enqueueInternal(chronicle, requestedBy);
    }

    @Transactional
    public void enqueueRequested(final ChronicleEntity chronicle, final UserEntity requestedBy) {
        openAiClient.requireConfigured();
        enqueueInternal(chronicle, requestedBy);
    }

    private void enqueueInternal(final ChronicleEntity chronicle, final UserEntity requestedBy) {
        if (aiJobRepository.existsByChronicleIdAndStatusIn(chronicle.getId(), ACTIVE_STATUSES)) {
            throw new ConflictException("An AI generation is already pending for this chronicle.");
        }
        final var idempotencyKey = chronicle.getId() + IDEMPOTENCY_KEY_SEPARATOR + UUID.randomUUID();
        aiJobRepository.save(new AiJobEntity(chronicle, requestedBy, idempotencyKey));
        chronicle.setStatus(ChronicleStatusType.AI_PENDING);
    }
}
