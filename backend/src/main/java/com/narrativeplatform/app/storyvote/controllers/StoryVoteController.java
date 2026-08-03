package com.narrativeplatform.app.storyvote.controllers;

import com.narrativeplatform.app.storyvote.models.requests.SetStoryVoteRequest;
import com.narrativeplatform.app.storyvote.models.responses.DailyStoryVoteStateResponse;
import com.narrativeplatform.app.storyvote.models.responses.StoryVoteSummaryResponse;
import com.narrativeplatform.app.storyvote.services.StoryVoteCommandService;
import com.narrativeplatform.app.storyvote.services.StoryVoteQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parties/{partyId}")
@RequiredArgsConstructor
public class StoryVoteController {
    private final StoryVoteCommandService storyVoteCommandService;
    private final StoryVoteQueryService storyVoteQueryService;

    @PutMapping("/chronicles/{chronicleId}/votes/today")
    DailyStoryVoteStateResponse setVote(
            @PathVariable("partyId") final UUID partyId,
            @PathVariable("chronicleId") final UUID chronicleId,
            @RequestBody final SetStoryVoteRequest request
    ) {
        return storyVoteCommandService.setVote(partyId, chronicleId, request.units());
    }

    @GetMapping("/story-votes/today")
    DailyStoryVoteStateResponse today(@PathVariable("partyId") final UUID partyId) {
        return storyVoteQueryService.getDailyState(partyId);
    }

    @GetMapping("/story-votes/summary")
    List<StoryVoteSummaryResponse> summary(@PathVariable("partyId") final UUID partyId) {
        return storyVoteQueryService.getSummary(partyId);
    }
}
