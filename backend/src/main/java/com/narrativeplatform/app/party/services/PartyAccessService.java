package com.narrativeplatform.app.party.services;

import com.narrativeplatform.app.party.models.entities.PartyMemberEntity;
import com.narrativeplatform.app.party.models.enums.MemberStatusType;
import com.narrativeplatform.app.party.repositories.PartyMemberRepository;
import com.narrativeplatform.security.CurrentUserService;
import com.narrativeplatform.shared.exceptions.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartyAccessService {
    private final PartyMemberRepository partyMemberRepository;
    private final CurrentUserService currentUserService;

    public PartyMemberEntity requireActiveMember(final UUID partyId) {
        final var user = currentUserService.require();
        final var membership = partyMemberRepository.findByPartyIdAndUserId(partyId, user.id())
                .orElseThrow(() -> new ForbiddenException("You are not a member of this party."));
        if (membership.getStatus() != MemberStatusType.ACTIVE) {
            throw new ForbiddenException("Your party membership is not active.");
        }
        return membership;
    }

    public PartyMemberEntity requireNarrator(final UUID partyId) {
        final var membership = requireActiveMember(partyId);
        if (!membership.getRole().canNarrate()) {
            throw new ForbiddenException("Narrator permission is required.");
        }
        return membership;
    }

    public PartyMemberEntity requireOwner(final UUID partyId) {
        final var membership = requireNarrator(partyId);
        if (membership.getRole() != com.narrativeplatform.app.party.models.enums.PartyRoleType.OWNER) {
            throw new ForbiddenException("Party owner permission is required.");
        }
        return membership;
    }
}
