package com.narrativeplatform.app.chronicle.services;

import com.narrativeplatform.app.auth.repositories.UserRepository;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.chronicle.models.entities.WrittenStoryDocumentEntity;
import com.narrativeplatform.app.chronicle.models.entities.WrittenStoryPermissionEntity;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleType;
import com.narrativeplatform.app.chronicle.models.requests.CreateWrittenChronicleRequest;
import com.narrativeplatform.app.chronicle.models.requests.SaveWrittenStoryRequest;
import com.narrativeplatform.app.chronicle.models.requests.UpdateEditorsRequest;
import com.narrativeplatform.app.chronicle.models.responses.WrittenChronicleDetailResponse;
import com.narrativeplatform.app.chronicle.models.responses.WrittenStoryLockResponse;
import com.narrativeplatform.app.chronicle.repositories.ChronicleRepository;
import com.narrativeplatform.app.chronicle.repositories.WrittenStoryDocumentRepository;
import com.narrativeplatform.app.chronicle.repositories.WrittenStoryPermissionRepository;
import com.narrativeplatform.app.party.models.enums.MemberStatusType;
import com.narrativeplatform.app.party.repositories.PartyMemberRepository;
import com.narrativeplatform.app.party.services.PartyAccessService;
import com.narrativeplatform.configuration.AppProperties;
import com.narrativeplatform.security.CurrentUserService;
import com.narrativeplatform.shared.exceptions.BadRequestException;
import com.narrativeplatform.shared.exceptions.ConflictException;
import com.narrativeplatform.shared.exceptions.ForbiddenException;
import com.narrativeplatform.shared.exceptions.NotFoundException;
import com.narrativeplatform.shared.utils.RichTextSanitizer;
import com.narrativeplatform.shared.utils.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WrittenChronicleService {
    private final ChronicleRepository chronicleRepository;
    private final WrittenStoryDocumentRepository documentRepository;
    private final WrittenStoryPermissionRepository permissionRepository;
    private final PartyMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PartyAccessService partyAccessService;
    private final ChronicleAccessService chronicleAccessService;
    private final CurrentUserService currentUserService;
    private final AppProperties properties;

    @Transactional
    public UUID create(final UUID partyId, final CreateWrittenChronicleRequest request) {
        final var narrator = partyAccessService.requireNarrator(partyId);
        final var chronicle = chronicleRepository.save(new ChronicleEntity(
                narrator.getParty(), narrator.getUser(), ChronicleType.WRITTEN, ChronicleStatusType.DRAFT,
                request.title().trim()
        ));
        documentRepository.save(new WrittenStoryDocumentEntity(chronicle));
        updateEditorsInternal(chronicle, narrator.getUser().getId(), request.editorIds() == null ? Set.of() : request.editorIds());
        return chronicle.getId();
    }

    public WrittenChronicleDetailResponse detail(final UUID partyId, final UUID chronicleId) {
        final var context = chronicleAccessService.requireMember(partyId, chronicleId);
        if (context.chronicle().getType() != ChronicleType.WRITTEN) {
            throw new BadRequestException("This chronicle is not a written chronicle.");
        }
        final var document = documentRepository.findByChronicleId(chronicleId)
                .orElseThrow(() -> new NotFoundException("Written document not found."));
        final var current = currentUserService.require();
        final var canEdit = context.narrator() || permissionRepository.existsByChronicleIdAndUserIdAndRevokedAtIsNull(chronicleId, current.id());
        final var editorIds = activeEditorIds(chronicleId);
        final var activeLock = document.hasActiveLock(Instant.now());
        return new WrittenChronicleDetailResponse(
                chronicleId, context.chronicle().getTitle(), context.chronicle().getStatus(), document.getContent(),
                document.getContentVersion(), canEdit,
                activeLock ? document.getLockedBy().getDisplayName() : null,
                activeLock ? document.getLockExpiresAt() : null,
                editorIds
        );
    }

    @Transactional
    public WrittenStoryLockResponse acquireLock(final UUID partyId, final UUID chronicleId) {
        final var context = chronicleAccessService.requireMember(partyId, chronicleId);
        requireCanEdit(context.narrator(), chronicleId);
        final var current = currentUserService.require();
        final var document = documentRepository.findForUpdate(chronicleId)
                .orElseThrow(() -> new NotFoundException("Written document not found."));
        final var now = Instant.now();
        if (document.hasActiveLock(now) && !document.getLockedBy().getId().equals(current.id())) {
            return new WrittenStoryLockResponse(false, null, document.getLockedBy().getDisplayName(), document.getLockExpiresAt());
        }
        final var rawToken = TokenUtils.randomToken();
        final var user = userRepository.findById(current.id()).orElseThrow(() -> new NotFoundException("User not found."));
        document.setLockedBy(user);
        document.setLockTokenHash(TokenUtils.sha256(rawToken));
        document.setLockedAt(now);
        document.setLockExpiresAt(now.plus(properties.writtenLockExpirationHours(), ChronoUnit.HOURS));
        return new WrittenStoryLockResponse(true, rawToken, user.getDisplayName(), document.getLockExpiresAt());
    }

    @Transactional
    public long save(
            final UUID partyId,
            final UUID chronicleId,
            final String rawLockToken,
            final SaveWrittenStoryRequest request
    ) {
        final var context = chronicleAccessService.requireMember(partyId, chronicleId);
        requireCanEdit(context.narrator(), chronicleId);
        final var current = currentUserService.require();
        final var document = documentRepository.findForUpdate(chronicleId)
                .orElseThrow(() -> new NotFoundException("Written document not found."));
        validateLock(document, current.id(), rawLockToken);
        if (document.getContentVersion() != request.expectedVersion()) {
            throw new ConflictException("The document changed. Reload before saving.");
        }
        document.setContent(RichTextSanitizer.sanitize(request.content()));
        document.setContentVersion(document.getContentVersion() + 1);
        document.setLockExpiresAt(Instant.now().plus(properties.writtenLockExpirationHours(), ChronoUnit.HOURS));
        context.chronicle().setStatus(ChronicleStatusType.IN_PROGRESS);
        return document.getContentVersion();
    }

    @Transactional
    public void releaseLock(final UUID partyId, final UUID chronicleId, final String rawLockToken) {
        chronicleAccessService.requireMember(partyId, chronicleId);
        final var current = currentUserService.require();
        final var document = documentRepository.findForUpdate(chronicleId)
                .orElseThrow(() -> new NotFoundException("Written document not found."));
        validateLock(document, current.id(), rawLockToken);
        clearLock(document);
    }

    @Transactional
    public void publish(final UUID partyId, final UUID chronicleId) {
        final var context = chronicleAccessService.requireNarrator(partyId, chronicleId);
        final var document = documentRepository.findByChronicleId(chronicleId)
                .orElseThrow(() -> new NotFoundException("Written document not found."));
        if (document.getContent().isBlank()) {
            throw new BadRequestException("A blank written chronicle cannot be published.");
        }
        context.chronicle().setStatus(ChronicleStatusType.PUBLISHED);
        context.chronicle().setPublishedAt(Instant.now());
        context.chronicle().setGeneratedPreview(preview(RichTextSanitizer.toPlainText(document.getContent())));
        clearLock(document);
    }

    @Transactional
    public void updateEditors(final UUID partyId, final UUID chronicleId, final UpdateEditorsRequest request) {
        final var context = chronicleAccessService.requireNarrator(partyId, chronicleId);
        updateEditorsInternal(context.chronicle(), context.membership().getUser().getId(), request.editorIds());
        final var document = documentRepository.findForUpdate(chronicleId)
                .orElseThrow(() -> new NotFoundException("Written document not found."));
        if (document.getLockedBy() == null || request.editorIds().contains(document.getLockedBy().getId())) {
            return;
        }
        final var lockOwnerMembership = memberRepository.findByPartyIdAndUserId(partyId, document.getLockedBy().getId()).orElse(null);
        final var lockOwnerCanNarrate = lockOwnerMembership != null
                && lockOwnerMembership.getStatus() == MemberStatusType.ACTIVE
                && lockOwnerMembership.getRole().canNarrate();
        if (!lockOwnerCanNarrate) {
            clearLock(document);
        }
    }

    private void updateEditorsInternal(final ChronicleEntity chronicle, final UUID narratorId, final Set<UUID> editorIds) {
        final var activePartyUserIds = memberRepository.findAllByPartyIdAndStatusOrderByJoinedAtAsc(
                chronicle.getParty().getId(), MemberStatusType.ACTIVE
        ).stream().map(membership -> membership.getUser().getId()).collect(java.util.stream.Collectors.toSet());
        for (final var editorId : editorIds) {
            if (!activePartyUserIds.contains(editorId)) {
                throw new BadRequestException("Every editor must be an active party member.");
            }
            final var permission = permissionRepository.findByChronicleIdAndUserId(chronicle.getId(), editorId);
            if (permission.isPresent()) {
                permission.get().setRevokedAt(null);
                continue;
            }
            final var editor = userRepository.findById(editorId).orElseThrow(() -> new NotFoundException("Editor not found."));
            final var narrator = userRepository.findById(narratorId).orElseThrow(() -> new NotFoundException("Narrator not found."));
            permissionRepository.save(new WrittenStoryPermissionEntity(chronicle, editor, narrator));
        }
        final var existing = activeEditorIds(chronicle.getId());
        for (final var existingId : existing) {
            if (!editorIds.contains(existingId)) {
                permissionRepository.findByChronicleIdAndUserId(chronicle.getId(), existingId)
                        .ifPresent(permission -> permission.setRevokedAt(Instant.now()));
            }
        }
    }

    private Set<UUID> activeEditorIds(final UUID chronicleId) {
        return permissionRepository.findAllByChronicleIdAndRevokedAtIsNull(chronicleId)
                .stream()
                .map(permission -> permission.getUser().getId())
                .collect(java.util.stream.Collectors.toSet());
    }

    private void requireCanEdit(final boolean narrator, final UUID chronicleId) {
        final var current = currentUserService.require();
        if (!narrator && !permissionRepository.existsByChronicleIdAndUserIdAndRevokedAtIsNull(chronicleId, current.id())) {
            throw new ForbiddenException("You cannot edit this written chronicle.");
        }
    }

    private void validateLock(final WrittenStoryDocumentEntity document, final UUID userId, final String rawLockToken) {
        if (!document.hasActiveLock(Instant.now())) {
            throw new ConflictException("The editing lock expired.");
        }
        if (!document.getLockedBy().getId().equals(userId)) {
            throw new ForbiddenException("Another member owns the editing lock.");
        }
        if (rawLockToken == null || !TokenUtils.sha256(rawLockToken).equals(document.getLockTokenHash())) {
            throw new ForbiddenException("The editing lock token is invalid.");
        }
    }

    private void clearLock(final WrittenStoryDocumentEntity document) {
        document.setLockedBy(null);
        document.setLockTokenHash(null);
        document.setLockedAt(null);
        document.setLockExpiresAt(null);
    }

    private String preview(final String content) {
        return content.length() <= 600 ? content : content.substring(0, 597) + "...";
    }
}
