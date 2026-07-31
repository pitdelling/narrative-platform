package com.narrativeplatform.app.invitation.services;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.auth.repositories.UserRepository;
import com.narrativeplatform.app.chronicle.services.GameChronicleService;
import com.narrativeplatform.app.invitation.models.entities.PartyInvitationLinkEntity;
import com.narrativeplatform.app.invitation.models.responses.InvitePreviewResponse;
import com.narrativeplatform.app.invitation.models.responses.PartyInvitationLinkResponse;
import com.narrativeplatform.app.invitation.repositories.PartyInvitationLinkRepository;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
import com.narrativeplatform.app.party.models.entities.PartyMemberEntity;
import com.narrativeplatform.app.party.models.enums.MemberStatusType;
import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import com.narrativeplatform.app.party.repositories.PartyMemberRepository;
import com.narrativeplatform.app.party.services.PartyAccessService;
import com.narrativeplatform.configuration.AppProperties;
import com.narrativeplatform.security.CurrentUserService;
import com.narrativeplatform.shared.exceptions.BadRequestException;
import com.narrativeplatform.shared.exceptions.NotFoundException;
import com.narrativeplatform.shared.utils.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {
    private static final String INVITE_PATH = "/invite/";

    private final PartyInvitationLinkRepository linkRepository;
    private final PartyMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PartyAccessService partyAccessService;
    private final GameChronicleService gameChronicleService;
    private final CurrentUserService currentUserService;
    private final AppProperties properties;

    @Transactional
    public PartyInvitationLinkResponse getCurrentLink(final UUID partyId, final PartyRoleType targetRole) {
        final var membership = partyAccessService.requireNarrator(partyId);
        final var link = linkRepository.findByPartyIdAndTargetRole(partyId, targetRole)
                .orElseGet(() -> getOrCreateForUpdate(membership.getParty(), membership.getUser(), targetRole));
        return toResponse(link);
    }

    @Transactional
    public PartyInvitationLinkResponse regenerateLink(final UUID partyId, final PartyRoleType targetRole) {
        final var membership = partyAccessService.requireNarrator(partyId);
        final var link = getOrCreateForUpdate(membership.getParty(), membership.getUser(), targetRole);
        final var rawToken = TokenUtils.randomToken();
        link.rotate(rawToken, TokenUtils.sha256(rawToken), membership.getUser());
        return toResponse(link);
    }

    @Transactional
    public void createInitialLink(final PartyEntity party, final UserEntity owner) {
        for (final var targetRole : new PartyRoleType[] {PartyRoleType.PLAYER, PartyRoleType.SPECTATOR}) {
            final var rawToken = TokenUtils.randomToken();
            linkRepository.save(new PartyInvitationLinkEntity(party, targetRole, rawToken, TokenUtils.sha256(rawToken), owner));
        }
    }

    public InvitePreviewResponse preview(final String rawToken) {
        return requireCurrentLink(rawToken).toPreviewResponse();
    }

    public void validateForRegistration(final String rawToken) {
        requireCurrentLink(rawToken);
    }

    @Transactional
    public void acceptForCurrentUser(final String rawToken) {
        acceptForUser(rawToken, currentUserService.require().id());
    }

    @Transactional
    public void acceptForUser(final String rawToken, final UUID userId) {
        final var link = requireCurrentLink(rawToken);
        final var user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found."));
        final var existing = memberRepository.findByPartyIdAndUserId(link.getPartyId(), userId);
        if (existing.isPresent() && existing.get().getStatus() == MemberStatusType.ACTIVE) {
            return;
        }
        if (existing.isPresent()) {
            final var membership = existing.get();
            membership.setStatus(MemberStatusType.ACTIVE);
            membership.setRole(link.getTargetRole());
        } else {
            memberRepository.save(new PartyMemberEntity(link.getParty(), user, link.getTargetRole(), MemberStatusType.ACTIVE));
        }
        gameChronicleService.insertPartyMemberIntoActiveRuns(link.getPartyId(), userId);
    }

    private PartyInvitationLinkEntity requireCurrentLink(final String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Invite token is required.");
        }
        return linkRepository.findByTokenHash(TokenUtils.sha256(rawToken))
                .orElseThrow(() -> new NotFoundException("Invitation not found."));
    }

    /**
     * Protects simultaneous regeneration/backfill requests for the same (party, target role):
     * {@code UNIQUE(party_id, target_role)} makes a second row for the same pair structurally
     * impossible, and the pessimistic write lock taken by {@code findForUpdateByPartyIdAndTargetRole}
     * serializes concurrent callers so the second one always observes the first one's committed
     * row instead of racing to insert a duplicate.
     */
    private PartyInvitationLinkEntity getOrCreateForUpdate(final PartyEntity party, final UserEntity actor, final PartyRoleType targetRole) {
        return linkRepository.findForUpdateByPartyIdAndTargetRole(party.getId(), targetRole).orElseGet(() -> {
            try {
                final var rawToken = TokenUtils.randomToken();
                return linkRepository.saveAndFlush(new PartyInvitationLinkEntity(party, targetRole, rawToken, TokenUtils.sha256(rawToken), actor));
            } catch (final DataIntegrityViolationException raceLost) {
                return linkRepository.findForUpdateByPartyIdAndTargetRole(party.getId(), targetRole).orElseThrow(() -> raceLost);
            }
        });
    }

    private PartyInvitationLinkResponse toResponse(final PartyInvitationLinkEntity link) {
        return link.toResponse(properties.publicUrl() + INVITE_PATH + link.getToken());
    }
}
