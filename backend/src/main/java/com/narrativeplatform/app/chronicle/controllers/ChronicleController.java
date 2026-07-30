package com.narrativeplatform.app.chronicle.controllers;

import com.narrativeplatform.app.chronicle.models.requests.*;
import com.narrativeplatform.app.chronicle.models.responses.*;
import com.narrativeplatform.app.chronicle.services.ChronicleService;
import com.narrativeplatform.app.chronicle.services.GameChronicleService;
import com.narrativeplatform.app.chronicle.services.WrittenChronicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/parties/{partyId}/chronicles")
@RequiredArgsConstructor
public class ChronicleController {
    private final ChronicleService chronicleService;
    private final GameChronicleService gameChronicleService;
    private final WrittenChronicleService writtenChronicleService;

    @GetMapping
    List<ChronicleCardResponse> list(@PathVariable("partyId") final UUID partyId) {
        return chronicleService.list(partyId);
    }

    @PostMapping("/game")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> createGame(
            @PathVariable("partyId") final UUID partyId,
            @Valid @RequestBody final CreateGameChronicleRequest request
    ) {
        return Map.of("id", gameChronicleService.create(partyId, request));
    }

    @PostMapping("/written")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> createWritten(
            @PathVariable("partyId") final UUID partyId,
            @Valid @RequestBody final CreateWrittenChronicleRequest request
    ) {
        return Map.of("id", writtenChronicleService.create(partyId, request));
    }

    @GetMapping("/{chronicleId}/game")
    GameChronicleDetailResponse gameDetail(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId,
            @RequestParam(defaultValue = "false") final boolean reveal
    ) {
        return gameChronicleService.detail(partyId, chronicleId, reveal);
    }

    @PostMapping("/{chronicleId}/game/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void saveGameDraft(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId,
            @Valid @RequestBody final SaveGameDraftRequest request
    ) {
        gameChronicleService.saveDraft(partyId, chronicleId, request);
    }

    @DeleteMapping("/{chronicleId}/game/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void clearGameDraft(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId
    ) {
        gameChronicleService.clearDraft(partyId, chronicleId);
    }

    @PostMapping("/{chronicleId}/game/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void publishGameSegment(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId,
            @Valid @RequestBody final PublishGameSegmentRequest request
    ) {
        gameChronicleService.publish(partyId, chronicleId, request);
    }

    @PostMapping("/{chronicleId}/game/skip")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void skipGameTurn(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId
    ) {
        gameChronicleService.skip(partyId, chronicleId);
    }

    @PostMapping("/{chronicleId}/segments/{segmentId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void disableSegment(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId,
            @PathVariable("segmentId") final UUID segmentId,
            @Valid @RequestBody final DisableSegmentRequest request
    ) {
        gameChronicleService.disableSegment(partyId, chronicleId, segmentId, request);
    }

    @PostMapping("/{chronicleId}/segments/{segmentId}/edit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void editSegment(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId,
            @PathVariable("segmentId") final UUID segmentId,
            @Valid @RequestBody final EditSegmentRequest request
    ) {
        gameChronicleService.editSegment(partyId, chronicleId, segmentId, request);
    }

    @PostMapping("/{chronicleId}/segments/{segmentId}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void restoreSegment(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId,
            @PathVariable("segmentId") final UUID segmentId
    ) {
        gameChronicleService.restoreSegment(partyId, chronicleId, segmentId);
    }

    @GetMapping("/{chronicleId}/generated-stories")
    List<GeneratedStoryResponse> generatedStoryHistory(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId
    ) {
        return gameChronicleService.listGeneratedStories(partyId, chronicleId);
    }

    @PostMapping("/{chronicleId}/regenerate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void regenerate(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId
    ) {
        gameChronicleService.regenerate(partyId, chronicleId);
    }

    @GetMapping("/{chronicleId}/written")
    WrittenChronicleDetailResponse writtenDetail(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId
    ) {
        return writtenChronicleService.detail(partyId, chronicleId);
    }

    @PostMapping("/{chronicleId}/written/lock")
    WrittenStoryLockResponse acquireWrittenLock(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId
    ) {
        return writtenChronicleService.acquireLock(partyId, chronicleId);
    }

    @PostMapping("/{chronicleId}/written/save")
    Map<String, Long> saveWritten(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId,
            @RequestHeader("X-Lock-Token") final String lockToken,
            @Valid @RequestBody final SaveWrittenStoryRequest request
    ) {
        return Map.of("contentVersion", writtenChronicleService.save(partyId, chronicleId, lockToken, request));
    }

    @PostMapping("/{chronicleId}/written/release-lock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void releaseWrittenLock(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId,
            @RequestHeader("X-Lock-Token") final String lockToken
    ) {
        writtenChronicleService.releaseLock(partyId, chronicleId, lockToken);
    }

    @PostMapping("/{chronicleId}/written/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void publishWritten(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId
    ) {
        writtenChronicleService.publish(partyId, chronicleId);
    }

    @PutMapping("/{chronicleId}/written/editors")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateEditors(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId,
            @Valid @RequestBody final UpdateEditorsRequest request
    ) {
        writtenChronicleService.updateEditors(partyId, chronicleId, request);
    }

    @DeleteMapping("/{chronicleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void archive(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId
    ) {
        chronicleService.archive(partyId, chronicleId);
    }
}
