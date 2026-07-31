package com.narrativeplatform.app.invitation.controllers;

import com.narrativeplatform.app.invitation.models.requests.AcceptInviteRequest;
import com.narrativeplatform.app.invitation.models.responses.InvitePreviewResponse;
import com.narrativeplatform.app.invitation.models.responses.PartyInvitationLinkResponse;
import com.narrativeplatform.app.invitation.services.InvitationService;
import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InvitationController {
    private final InvitationService invitationService;

    @GetMapping("/api/parties/{partyId}/invitation")
    PartyInvitationLinkResponse currentLink(@PathVariable("partyId") final UUID partyId) {
        return invitationService.getCurrentLink(partyId, PartyRoleType.PLAYER);
    }

    @PostMapping("/api/parties/{partyId}/invitation/regenerate")
    PartyInvitationLinkResponse regenerate(@PathVariable("partyId") final UUID partyId) {
        return invitationService.regenerateLink(partyId, PartyRoleType.PLAYER);
    }

    @GetMapping("/api/parties/{partyId}/invitation/spectator")
    PartyInvitationLinkResponse spectatorLink(@PathVariable("partyId") final UUID partyId) {
        return invitationService.getCurrentLink(partyId, PartyRoleType.SPECTATOR);
    }

    @PostMapping("/api/parties/{partyId}/invitation/spectator/regenerate")
    PartyInvitationLinkResponse regenerateSpectator(@PathVariable("partyId") final UUID partyId) {
        return invitationService.regenerateLink(partyId, PartyRoleType.SPECTATOR);
    }

    @GetMapping("/api/invites/{token}")
    InvitePreviewResponse preview(@PathVariable("token") final String token) {
        return invitationService.preview(token);
    }

    @PostMapping("/api/invites/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void accept(@Valid @RequestBody final AcceptInviteRequest request) {
        invitationService.acceptForCurrentUser(request.token());
    }
}
