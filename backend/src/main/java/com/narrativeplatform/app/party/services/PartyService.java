package com.narrativeplatform.app.party.services;

import com.narrativeplatform.app.auth.repositories.UserRepository;
import com.narrativeplatform.app.chronicle.services.GameChronicleService;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
import com.narrativeplatform.app.party.models.entities.PartyMemberEntity;
import com.narrativeplatform.app.party.models.enums.MemberStatusType;
import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import com.narrativeplatform.app.party.models.requests.CreatePartyRequest;
import com.narrativeplatform.app.party.models.requests.TransferPartyRequest;
import com.narrativeplatform.app.party.models.requests.UpdateMemberRoleRequest;
import com.narrativeplatform.app.party.models.responses.PartyDetailResponse;
import com.narrativeplatform.app.party.models.responses.PartyMemberResponse;
import com.narrativeplatform.app.party.models.responses.PartySummaryResponse;
import com.narrativeplatform.app.party.repositories.PartyMemberRepository;
import com.narrativeplatform.app.party.repositories.PartyRepository;
import com.narrativeplatform.security.CurrentUserService;
import com.narrativeplatform.shared.exceptions.BadRequestException;
import com.narrativeplatform.shared.exceptions.NotFoundException;
import com.narrativeplatform.shared.utils.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartyService {
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final PartyAccessService partyAccessService;
    private final GameChronicleService gameChronicleService;

    @Transactional
    public PartySummaryResponse create(final CreatePartyRequest request) {
        final var current = currentUserService.require();
        final var owner = userRepository.findById(current.id()).orElseThrow(() -> new NotFoundException("User not found."));
        final var slug = uniqueSlug(request.name());
        final var party = partyRepository.save(new PartyEntity(
                request.name().trim(), slug, trimToNull(request.description()), trimToNull(request.imageUrl()), owner
        ));
        final var membership = partyMemberRepository.save(new PartyMemberEntity(
                party, owner, PartyRoleType.OWNER, MemberStatusType.ACTIVE
        ));
        return membership.toSummaryResponse();
    }

    public List<PartySummaryResponse> listMine() {
        final var current = currentUserService.require();
        return partyMemberRepository.findAllByUserIdAndStatusOrderByJoinedAtAsc(current.id(), MemberStatusType.ACTIVE)
                .stream().map(PartyMemberEntity::toSummaryResponse).toList();
    }

    public PartyDetailResponse detail(final UUID partyId) {
        final var membership = partyAccessService.requireActiveMember(partyId);
        final var members = partyMemberRepository.findAllByPartyIdAndStatusNotOrderByJoinedAtAsc(partyId, MemberStatusType.REMOVED)
                .stream().map(PartyMemberEntity::toMemberResponse).toList();
        return membership.getParty().toDetailResponse(membership.getRole(), members);
    }

    @Transactional
    public void disableMember(final UUID partyId, final UUID userId) {
        partyAccessService.requireNarrator(partyId);
        final var membership = requireMembership(partyId, userId);
        if (membership.getRole() == PartyRoleType.OWNER) {
            throw new BadRequestException("The owner cannot be disabled.");
        }
        membership.setStatus(MemberStatusType.DISABLED);
    }

    @Transactional
    public void removeMember(final UUID partyId, final UUID userId) {
        partyAccessService.requireNarrator(partyId);
        final var membership = requireMembership(partyId, userId);
        if (membership.getRole() == PartyRoleType.OWNER) {
            throw new BadRequestException("The owner cannot be removed.");
        }
        membership.setStatus(MemberStatusType.REMOVED);
        membership.setRole(PartyRoleType.PLAYER);
        gameChronicleService.removePartyMemberFromActiveRuns(partyId, userId);
    }

    @Transactional
    public void reactivateMember(final UUID partyId, final UUID userId) {
        partyAccessService.requireNarrator(partyId);
        final var membership = requireMembership(partyId, userId);
        if (membership.getStatus() == MemberStatusType.REMOVED) {
            throw new BadRequestException("A removed member must join again through a new invite.");
        }
        membership.setStatus(MemberStatusType.ACTIVE);
        gameChronicleService.insertPartyMemberIntoActiveRuns(partyId, userId);
    }

    @Transactional
    public void updateMemberRole(
            final UUID partyId,
            final UUID userId,
            final UpdateMemberRoleRequest request
    ) {
        partyAccessService.requireOwner(partyId);
        final var membership = requireMembership(partyId, userId);
        if (membership.getRole() == PartyRoleType.OWNER) {
            throw new BadRequestException("The owner role can only be changed by transferring the party.");
        }
        if (membership.getStatus() != MemberStatusType.ACTIVE) {
            throw new BadRequestException("Only active members can have their role changed.");
        }
        if (request.role() == PartyRoleType.OWNER) {
            throw new BadRequestException("Use the transfer operation to assign a new owner.");
        }
        membership.setRole(request.role());
    }

    @Transactional
    public void transfer(final UUID partyId, final TransferPartyRequest request) {
        final var ownerMembership = partyAccessService.requireOwner(partyId);
        final var target = requireMembership(partyId, request.newOwnerId());
        if (target.getStatus() != MemberStatusType.ACTIVE) {
            throw new BadRequestException("The new owner must be active.");
        }
        ownerMembership.setRole(PartyRoleType.PLAYER);
        target.setRole(PartyRoleType.OWNER);
        ownerMembership.getParty().setOwner(target.getUser());
    }

    private PartyMemberEntity requireMembership(final UUID partyId, final UUID userId) {
        return partyMemberRepository.findByPartyIdAndUserId(partyId, userId)
                .orElseThrow(() -> new NotFoundException("Party member not found."));
    }

    private String uniqueSlug(final String name) {
        final var base = SlugUtils.slugify(name);
        var candidate = base;
        var suffix = 2;
        while (partyRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String trimToNull(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
