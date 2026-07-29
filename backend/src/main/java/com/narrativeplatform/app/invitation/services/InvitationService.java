package com.narrativeplatform.app.invitation.services;

import com.narrativeplatform.app.auth.repositories.UserRepository;
import com.narrativeplatform.app.chronicle.services.GameChronicleService;
import com.narrativeplatform.app.invitation.models.entities.PartyInviteEntity;
import com.narrativeplatform.app.invitation.models.enums.InviteChannelType;
import com.narrativeplatform.app.invitation.models.requests.CreateInviteRequest;
import com.narrativeplatform.app.invitation.models.responses.InvitePreviewResponse;
import com.narrativeplatform.app.invitation.models.responses.InviteResponse;
import com.narrativeplatform.app.invitation.repositories.PartyInviteRepository;
import com.narrativeplatform.app.party.models.entities.PartyMemberEntity;
import com.narrativeplatform.app.party.models.enums.MemberStatusType;
import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import com.narrativeplatform.app.party.repositories.PartyMemberRepository;
import com.narrativeplatform.app.party.services.PartyAccessService;
import com.narrativeplatform.configuration.AppProperties;
import com.narrativeplatform.security.CurrentUserService;
import com.narrativeplatform.shared.exceptions.BadRequestException;
import com.narrativeplatform.shared.exceptions.ConflictException;
import com.narrativeplatform.shared.exceptions.NotFoundException;
import com.narrativeplatform.shared.integrations.ResendClient;
import com.narrativeplatform.shared.utils.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {
    private static final String INVITE_PATH = "/invite/";

    private final PartyInviteRepository inviteRepository;
    private final PartyMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PartyAccessService partyAccessService;
    private final GameChronicleService gameChronicleService;
    private final CurrentUserService currentUserService;
    private final AppProperties properties;
    private final ResendClient resendClient;

    @Transactional
    public InviteResponse create(final UUID partyId, final CreateInviteRequest request) {
        final var narratorMembership = partyAccessService.requireNarrator(partyId);
        validateContact(request);
        final var rawToken = TokenUtils.randomToken();
        final var invite = inviteRepository.save(new PartyInviteEntity(
                narratorMembership.getParty(), narratorMembership.getUser(), TokenUtils.sha256(rawToken),
                request.channel(), trimToNull(request.recipientContact()),
                Instant.now().plus(properties.inviteExpirationHours(), ChronoUnit.HOURS)
        ));
        final var inviteUrl = properties.publicUrl() + INVITE_PATH + rawToken;
        final var message = "%s convidou você para entrar na party %s: %s"
                .formatted(narratorMembership.getUser().getDisplayName(), narratorMembership.getParty().getName(), inviteUrl);
        final var whatsappUrl = "https://wa.me/?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
        var emailSent = false;
        if (request.channel() == InviteChannelType.EMAIL) {
            emailSent = resendClient.sendInvite(
                    request.recipientContact(), narratorMembership.getParty().getName(),
                    narratorMembership.getUser().getDisplayName(), inviteUrl
            );
        }
        return invite.toResponse(inviteUrl, whatsappUrl, emailSent);
    }

    public InvitePreviewResponse preview(final String rawToken) {
        final var invite = requireAvailable(rawToken);
        return invite.toPreviewResponse();
    }

    public void validateForRegistration(final String rawToken) {
        requireAvailable(rawToken);
    }

    @Transactional
    public void acceptForCurrentUser(final String rawToken) {
        final var current = currentUserService.require();
        acceptForUser(rawToken, current.id());
    }

    @Transactional
    public void acceptForUser(final String rawToken, final UUID userId) {
        final var invite = requireAvailableForUpdate(rawToken);
        final var user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found."));
        final var existing = memberRepository.findByPartyIdAndUserId(invite.getParty().getId(), userId);
        if (existing.isPresent() && existing.get().getStatus() == MemberStatusType.ACTIVE) {
            throw new ConflictException("You already belong to this party.");
        }
        if (existing.isPresent()) {
            final var membership = existing.get();
            membership.setStatus(MemberStatusType.ACTIVE);
            membership.setRole(PartyRoleType.PLAYER);
        } else {
            memberRepository.save(new PartyMemberEntity(invite.getParty(), user, PartyRoleType.PLAYER, MemberStatusType.ACTIVE));
        }
        invite.setConsumedAt(Instant.now());
        invite.setConsumedBy(user);
        gameChronicleService.insertPartyMemberIntoActiveRuns(invite.getParty().getId(), userId);
    }

    @Transactional
    public void revoke(final UUID partyId, final UUID inviteId) {
        partyAccessService.requireNarrator(partyId);
        final var invite = inviteRepository.findById(inviteId).orElseThrow(() -> new NotFoundException("Invite not found."));
        if (!invite.getParty().getId().equals(partyId)) {
            throw new NotFoundException("Invite not found.");
        }
        invite.setRevokedAt(Instant.now());
    }

    private PartyInviteEntity requireAvailable(final String rawToken) {
        validateToken(rawToken);
        final var invite = inviteRepository.findByTokenHash(TokenUtils.sha256(rawToken))
                .orElseThrow(() -> new NotFoundException("Invitation not found."));
        validateAvailability(invite);
        return invite;
    }

    private PartyInviteEntity requireAvailableForUpdate(final String rawToken) {
        validateToken(rawToken);
        final var invite = inviteRepository.findForUpdateByTokenHash(TokenUtils.sha256(rawToken))
                .orElseThrow(() -> new NotFoundException("Invitation not found."));
        validateAvailability(invite);
        return invite;
    }

    private void validateToken(final String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Invite token is required.");
        }
    }

    private void validateAvailability(final PartyInviteEntity invite) {
        if (!invite.isAvailable(Instant.now())) {
            throw new BadRequestException("Invitation is expired, used or revoked.");
        }
    }

    private void validateContact(final CreateInviteRequest request) {
        if (request.channel() == InviteChannelType.EMAIL
                && (request.recipientContact() == null || !request.recipientContact().contains("@"))) {
            throw new BadRequestException("A valid email is required for email invitations.");
        }
    }

    private String trimToNull(final String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
