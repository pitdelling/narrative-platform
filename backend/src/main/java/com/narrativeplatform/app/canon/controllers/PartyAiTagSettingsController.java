package com.narrativeplatform.app.canon.controllers;

import com.narrativeplatform.app.canon.models.requests.UpdateAiTagSettingsRequest;
import com.narrativeplatform.app.canon.models.responses.PartyAiTagSettingsResponse;
import com.narrativeplatform.app.canon.services.PartyAiTagSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/parties/{partyId}/ai-tag-settings")
@RequiredArgsConstructor
public class PartyAiTagSettingsController {
    private final PartyAiTagSettingsService partyAiTagSettingsService;

    @GetMapping
    PartyAiTagSettingsResponse get(@PathVariable("partyId") final UUID partyId) {
        return partyAiTagSettingsService.get(partyId);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable("partyId") final UUID partyId, @Valid @RequestBody final UpdateAiTagSettingsRequest request) {
        partyAiTagSettingsService.update(partyId, request);
    }
}
