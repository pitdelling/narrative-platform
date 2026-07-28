package com.narrativeplatform.app.party.controllers;

import com.narrativeplatform.app.party.models.requests.CreatePartyRequest;
import com.narrativeplatform.app.party.models.requests.TransferPartyRequest;
import com.narrativeplatform.app.party.models.requests.UpdateMemberRoleRequest;
import com.narrativeplatform.app.party.models.responses.PartyDetailResponse;
import com.narrativeplatform.app.party.models.responses.PartySummaryResponse;
import com.narrativeplatform.app.party.services.PartyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parties")
@RequiredArgsConstructor
public class PartyController {
    private final PartyService partyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PartySummaryResponse create(@Valid @RequestBody final CreatePartyRequest request) {
        return partyService.create(request);
    }

    @GetMapping
    List<PartySummaryResponse> listMine() {
        return partyService.listMine();
    }

    @GetMapping("/{partyId}")
    PartyDetailResponse detail(@PathVariable("partyId") final UUID partyId) {
        return partyService.detail(partyId);
    }

    @PostMapping("/{partyId}/members/{userId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void disable(@PathVariable("partyId") final UUID partyId, @PathVariable("userId") final UUID userId) {
        partyService.disableMember(partyId, userId);
    }

    @PostMapping("/{partyId}/members/{userId}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reactivate(@PathVariable("partyId") final UUID partyId, @PathVariable("userId") final UUID userId) {
        partyService.reactivateMember(partyId, userId);
    }

    @PutMapping("/{partyId}/members/{userId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateRole(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("userId") final UUID userId,
            @Valid @RequestBody final UpdateMemberRoleRequest request
    ) {
        partyService.updateMemberRole(partyId, userId, request);
    }

    @DeleteMapping("/{partyId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable("partyId") final UUID partyId, @PathVariable("userId") final UUID userId) {
        partyService.removeMember(partyId, userId);
    }

    @PostMapping("/{partyId}/transfer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void transfer(@PathVariable("partyId") final UUID partyId, @Valid @RequestBody final TransferPartyRequest request) {
        partyService.transfer(partyId, request);
    }
}
