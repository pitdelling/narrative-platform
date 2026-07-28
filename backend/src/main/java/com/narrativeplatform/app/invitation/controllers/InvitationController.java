package com.narrativeplatform.app.invitation.controllers;

import com.narrativeplatform.app.invitation.models.requests.AcceptInviteRequest;
import com.narrativeplatform.app.invitation.models.requests.CreateInviteRequest;
import com.narrativeplatform.app.invitation.models.responses.InvitePreviewResponse;
import com.narrativeplatform.app.invitation.models.responses.InviteResponse;
import com.narrativeplatform.app.invitation.services.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InvitationController {
    private final InvitationService invitationService;

    @PostMapping("/api/parties/{partyId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    InviteResponse create(
            @PathVariable("partyId") final UUID partyId,
            @Valid @RequestBody final CreateInviteRequest request
    ) {
        return invitationService.create(partyId, request);
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

    @DeleteMapping("/api/parties/{partyId}/invites/{inviteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("inviteId") final UUID inviteId
    ) {
        invitationService.revoke(partyId, inviteId);
    }
}
