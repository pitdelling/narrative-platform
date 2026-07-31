package com.narrativeplatform.app.canon.controllers;

import com.narrativeplatform.app.canon.models.requests.CreateCanonCategoryRequest;
import com.narrativeplatform.app.canon.models.requests.UpdateCanonCategoryRequest;
import com.narrativeplatform.app.canon.models.responses.CanonCategoryConfigResponse;
import com.narrativeplatform.app.canon.services.CanonCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parties/{partyId}/ai-tag-settings")
@RequiredArgsConstructor
public class CanonCategoryController {
    private final CanonCategoryService canonCategoryService;

    @GetMapping
    List<CanonCategoryConfigResponse> list(@PathVariable("partyId") final UUID partyId) {
        return canonCategoryService.list(partyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CanonCategoryConfigResponse create(
            @PathVariable("partyId") final UUID partyId,
            @Valid @RequestBody final CreateCanonCategoryRequest request
    ) {
        return canonCategoryService.create(partyId, request);
    }

    @PutMapping("/{categoryId}")
    CanonCategoryConfigResponse update(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("categoryId") final UUID categoryId,
            @Valid @RequestBody final UpdateCanonCategoryRequest request
    ) {
        return canonCategoryService.update(partyId, categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable("partyId") final UUID partyId, @PathVariable("categoryId") final UUID categoryId) {
        canonCategoryService.delete(partyId, categoryId);
    }

    @PostMapping("/{categoryId}/move-up")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void moveUp(@PathVariable("partyId") final UUID partyId, @PathVariable("categoryId") final UUID categoryId) {
        canonCategoryService.moveUp(partyId, categoryId);
    }

    @PostMapping("/{categoryId}/move-down")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void moveDown(@PathVariable("partyId") final UUID partyId, @PathVariable("categoryId") final UUID categoryId) {
        canonCategoryService.moveDown(partyId, categoryId);
    }
}
