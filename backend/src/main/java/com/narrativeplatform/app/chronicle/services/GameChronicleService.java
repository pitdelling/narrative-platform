package com.narrativeplatform.app.chronicle.services;

import com.narrativeplatform.app.aijob.models.enums.AiJobType;
import com.narrativeplatform.app.aijob.services.AiJobService;
import com.narrativeplatform.app.canon.services.CanonMapGenerationService;
import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.auth.repositories.UserRepository;
import com.narrativeplatform.app.chronicle.models.entities.*;
import com.narrativeplatform.app.chronicle.models.enums.*;
import com.narrativeplatform.app.chronicle.models.requests.*;
import com.narrativeplatform.app.chronicle.models.responses.*;
import com.narrativeplatform.app.chronicle.repositories.*;
import com.narrativeplatform.app.party.models.enums.MemberStatusType;
import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import com.narrativeplatform.app.party.repositories.PartyMemberRepository;
import com.narrativeplatform.app.party.services.PartyAccessService;
import com.narrativeplatform.configuration.AppProperties;
import com.narrativeplatform.security.CurrentUserService;
import com.narrativeplatform.shared.exceptions.BadRequestException;
import com.narrativeplatform.shared.exceptions.ConflictException;
import com.narrativeplatform.shared.exceptions.ForbiddenException;
import com.narrativeplatform.shared.exceptions.NotFoundException;
import com.narrativeplatform.shared.exceptions.TurnExpiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameChronicleService {
    private static final int EXPIRATION_SWEEP_MILLISECONDS = 60_000;

    private final ChronicleRepository chronicleRepository;
    private final GameRunRepository gameRunRepository;
    private final GameTurnRepository gameTurnRepository;
    private final GameDraftRepository gameDraftRepository;
    private final GameSegmentRepository gameSegmentRepository;
    private final GeneratedStoryRepository generatedStoryRepository;
    private final SegmentRevisionRepository segmentRevisionRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final UserRepository userRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final PartyAccessService partyAccessService;
    private final ChronicleAccessService chronicleAccessService;
    private final CurrentUserService currentUserService;
    private final AppProperties properties;
    private final AiJobService aiJobService;
    private final CanonMapGenerationService canonMapGenerationService;
    private final ChronicleSynopsisService chronicleSynopsisService;

    @Transactional
    public UUID create(final UUID partyId, final CreateGameChronicleRequest request) {
        final var creatorMembership = partyAccessService.requireActiveMember(partyId);
        if (creatorMembership.getRole() == PartyRoleType.SPECTATOR) {
            throw new BadRequestException("A spectator cannot create a game chronicle.");
        }
        final var activeMemberships = partyMemberRepository.findAllByPartyIdAndStatusAndRoleNotOrderByJoinedAtAsc(
                partyId, MemberStatusType.ACTIVE, PartyRoleType.SPECTATOR
        );
        if (activeMemberships.size() < 2) {
            throw new BadRequestException("A game chronicle requires at least two active members.");
        }
        final var chronicle = chronicleRepository.save(new ChronicleEntity(
                creatorMembership.getParty(), creatorMembership.getUser(), ChronicleType.GAME,
                ChronicleStatusType.IN_PROGRESS, request.title().trim()
        ));
        final var orderedUsers = buildFixedOrder(creatorMembership.getUser(), activeMemberships.stream()
                .map(membership -> membership.getUser()).toList());
        final var run = gameRunRepository.save(new GameRunEntity(chronicle, request.cycleCount(), orderedUsers.size()));
        final var turns = new ArrayList<GameTurnEntity>();
        var sequence = 1;
        for (short cycle = 1; cycle <= request.cycleCount(); cycle++) {
            for (var position = 0; position < orderedUsers.size(); position++) {
                turns.add(new GameTurnEntity(run, orderedUsers.get(position), cycle, position + 1, sequence++));
            }
        }

        final var startedAt = Instant.now();
        final var openingTurn = turns.getFirst();
        openingTurn.setStatus(GameTurnStatusType.SUBMITTED);
        openingTurn.setStartedAt(startedAt);
        openingTurn.setSubmittedAt(startedAt);
        gameTurnRepository.saveAll(turns);
        gameSegmentRepository.save(new GameSegmentEntity(openingTurn, request.initialContent().trim()));

        final var nextTurn = turns.get(1);
        run.setCurrentSequence(nextTurn.getSequenceNumber());
        activate(nextTurn, startedAt);
        gameParticipantRepository.saveAll(orderedUsers.stream().map(user -> new GameParticipantEntity(run, user)).toList());
        log.debug(
                "Created game chronicle {} for party {} with {} cycle(s) and {} participant(s).",
                chronicle.getId(), partyId, request.cycleCount(), orderedUsers.size()
        );
        return chronicle.getId();
    }

    public GameChronicleDetailResponse detail(final UUID partyId, final UUID chronicleId, final boolean reveal) {
        final var context = chronicleAccessService.requireMember(partyId, chronicleId);
        if (context.chronicle().getType() != ChronicleType.GAME) {
            throw new BadRequestException("This chronicle is not a game chronicle.");
        }
        final var run = gameRunRepository.findByChronicleId(chronicleId)
                .orElseThrow(() -> new NotFoundException("Game run not found."));
        final var current = currentUserService.require();
        final var turns = gameTurnRepository.findAllByRunIdOrderBySequenceNumberAsc(run.getId());
        final var segments = gameSegmentRepository.findAllByRunIdOrderBySequenceNumberAsc(run.getId());
        final var gameCompleted = run.getStatus() == GameRunStatusType.COMPLETED;
        final var revealAll = gameCompleted || (context.narrator() && reveal);
        final var visibleSequences = revealAll ? segments.stream().map(GameSegmentEntity::getSequenceNumber).collect(Collectors.toSet())
                : visibleSequencesFor(current.id(), run, segments);
        final var segmentResponses = segments.stream()
                .map(segment -> {
                    if (!visibleSequences.contains(segment.getSequenceNumber())) {
                        return segment.toHiddenResponse();
                    }
                    if (segment.getStatus() == SegmentStatusType.DISABLED && !context.narrator()) {
                        return segment.toPublicDisabledResponse();
                    }
                    return segment.toVisibleResponse();
                })
                .toList();
        final var currentTurn = turns.stream()
                .filter(turn -> turn.getStatus() == GameTurnStatusType.ACTIVE)
                .findFirst().orElse(null);
        final var currentUserTurn = currentTurn != null && currentTurn.getUser().getId().equals(current.id());
        final var currentDraft = currentUserTurn
                ? gameDraftRepository.findById(currentTurn.getId()).map(GameDraftEntity::getContent).orElse("")
                : null;
        final var participants = gameParticipantRepository.findAllByRunIdOrderByCreatedAtAsc(run.getId())
                .stream().map(GameParticipantEntity::toResponse).toList();
        return new GameChronicleDetailResponse(
                context.chronicle().getId(), context.chronicle().getTitle(), context.chronicle().getStatus(),
                context.chronicle().getCreator().getDisplayName(), context.chronicle().getCreatedAt(), run.getCompletedAt(),
                run.getCycleCount(), run.getCurrentSequence(), turns.size(), current.id(), currentUserTurn,
                context.narrator(), properties.narratorRevealSeconds(), currentDraft,
                context.chronicle().getCurrentGeneratedStory() == null ? null : context.chronicle().getCurrentGeneratedStory().toResponse(),
                turns.stream().map(GameTurnEntity::toResponse).toList(), segmentResponses, participants
        );
    }

    public List<GeneratedStoryResponse> listGeneratedStories(final UUID partyId, final UUID chronicleId) {
        final var context = chronicleAccessService.requireMember(partyId, chronicleId);
        if (context.chronicle().getType() != ChronicleType.GAME) {
            throw new BadRequestException("This chronicle is not a game chronicle.");
        }
        return generatedStoryRepository.findAllByChronicleIdOrderByVersionNumberDesc(chronicleId)
                .stream().map(GeneratedStoryEntity::toResponse).toList();
    }

    @Transactional(noRollbackFor = TurnExpiredException.class)
    public void saveDraft(final UUID partyId, final UUID chronicleId, final SaveGameDraftRequest request) {
        final var run = requireRunForUpdate(partyId, chronicleId);
        final var turn = requireCurrentTurn(run);
        requireCurrentUser(turn);
        final var draft = gameDraftRepository.findById(turn.getId()).orElseGet(() -> new GameDraftEntity(turn, ""));
        draft.setContent(request.content());
        gameDraftRepository.save(draft);
    }

    @Transactional(noRollbackFor = TurnExpiredException.class)
    public void clearDraft(final UUID partyId, final UUID chronicleId) {
        final var run = requireRunForUpdate(partyId, chronicleId);
        final var turn = requireCurrentTurn(run);
        requireCurrentUser(turn);
        gameDraftRepository.deleteById(turn.getId());
    }

    @Transactional(noRollbackFor = TurnExpiredException.class)
    public void publish(final UUID partyId, final UUID chronicleId, final PublishGameSegmentRequest request) {
        final var run = requireRunForUpdate(partyId, chronicleId);
        final var turn = requireCurrentTurn(run);
        requireCurrentUser(turn);
        gameSegmentRepository.save(new GameSegmentEntity(turn, request.content().trim()));
        turn.setStatus(GameTurnStatusType.SUBMITTED);
        turn.setSubmittedAt(Instant.now());
        gameDraftRepository.deleteById(turn.getId());
        advance(run, turn.getSequenceNumber());
    }

    @Transactional(noRollbackFor = TurnExpiredException.class)
    public void skip(final UUID partyId, final UUID chronicleId) {
        final var context = chronicleAccessService.requireMember(partyId, chronicleId);
        final var run = gameRunRepository.findForUpdateByChronicleId(chronicleId)
                .orElseThrow(() -> new NotFoundException("Game run not found."));
        final var turn = requireCurrentTurn(run);
        final var current = currentUserService.require();
        if (!turn.getUser().getId().equals(current.id()) && !context.narrator()) {
            throw new ForbiddenException("Only the active participant or a narrator can skip this turn.");
        }
        final var skippedBy = userRepository.findById(current.id()).orElseThrow(() -> new NotFoundException("User not found."));
        turn.setStatus(GameTurnStatusType.SKIPPED);
        turn.setSkippedBy(skippedBy);
        gameDraftRepository.deleteById(turn.getId());
        advance(run, turn.getSequenceNumber());
    }

    @Transactional
    public void disableSegment(
            final UUID partyId,
            final UUID chronicleId,
            final UUID segmentId,
            final DisableSegmentRequest request
    ) {
        final var context = chronicleAccessService.requireNarrator(partyId, chronicleId);
        final var segment = requireSegment(context.chronicle(), segmentId);
        final var actor = context.membership().getUser();
        segmentRevisionRepository.save(new SegmentRevisionEntity(
                segment, actor, segment.getContent(), segment.getContent(), segment.getStatus(),
                SegmentStatusType.DISABLED, trimToNull(request.reason())
        ));
        segment.setStatus(SegmentStatusType.DISABLED);
        segment.setDisabledReason(trimToNull(request.reason()));
    }

    @Transactional
    public void editSegment(
            final UUID partyId,
            final UUID chronicleId,
            final UUID segmentId,
            final EditSegmentRequest request
    ) {
        final var context = chronicleAccessService.requireNarrator(partyId, chronicleId);
        final var segment = requireSegment(context.chronicle(), segmentId);
        segmentRevisionRepository.save(new SegmentRevisionEntity(
                segment, context.membership().getUser(), segment.getContent(), request.content().trim(), segment.getStatus(),
                SegmentStatusType.EDITED, trimToNull(request.reason())
        ));
        segment.setContent(request.content().trim());
        segment.setStatus(SegmentStatusType.EDITED);
        segment.setDisabledReason(null);
    }

    @Transactional
    public void restoreSegment(final UUID partyId, final UUID chronicleId, final UUID segmentId) {
        final var context = chronicleAccessService.requireNarrator(partyId, chronicleId);
        final var segment = requireSegment(context.chronicle(), segmentId);
        segmentRevisionRepository.save(new SegmentRevisionEntity(
                segment, context.membership().getUser(), segment.getContent(), segment.getContent(), segment.getStatus(),
                SegmentStatusType.ACTIVE, "Restored by narrator."
        ));
        segment.setStatus(SegmentStatusType.ACTIVE);
        segment.setDisabledReason(null);
    }

    @Transactional
    public void insertPartyMemberIntoActiveRuns(final UUID partyId, final UUID userId) {
        final var chronicles = chronicleRepository.findAllByPartyIdAndTypeAndStatus(
                partyId, ChronicleType.GAME, ChronicleStatusType.IN_PROGRESS
        );
        if (chronicles.isEmpty()) return;
        final var user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found."));
        for (final var chronicle : chronicles) {
            insertIntoRun(chronicle, user);
        }
    }

    @Transactional
    public void removePartyMemberFromActiveRuns(final UUID partyId, final UUID userId, final UserEntity actor) {
        final var chronicles = chronicleRepository.findAllByPartyIdAndTypeAndStatus(
                partyId, ChronicleType.GAME, ChronicleStatusType.IN_PROGRESS
        );
        for (final var chronicle : chronicles) {
            removeFromRun(chronicle, userId, RemovedByType.NARRATOR, actor);
        }
    }

    @Transactional
    public void leaveChronicle(final UUID partyId, final UUID chronicleId, final UUID targetUserId) {
        final var context = chronicleAccessService.requireMember(partyId, chronicleId);
        requireGameType(context.chronicle());
        final var current = currentUserService.require();
        final var self = targetUserId.equals(current.id());
        if (!self && !context.narrator()) {
            throw new ForbiddenException("Only the participant themself or a narrator can remove them from this chronicle.");
        }
        final var run = gameRunRepository.findForUpdateByChronicleId(chronicleId)
                .orElseThrow(() -> new NotFoundException("Game run not found."));
        if (run.getStatus() != GameRunStatusType.IN_PROGRESS) {
            throw new BadRequestException("This chronicle has already finished.");
        }
        final var actor = userRepository.findById(current.id()).orElseThrow(() -> new NotFoundException("User not found."));
        removeFromRun(context.chronicle(), targetUserId, self ? RemovedByType.SELF : RemovedByType.NARRATOR, actor);
    }

    @Transactional
    public void rejoinChronicle(final UUID partyId, final UUID chronicleId, final UUID targetUserId) {
        final var context = chronicleAccessService.requireMember(partyId, chronicleId);
        requireGameType(context.chronicle());
        final var run = gameRunRepository.findForUpdateByChronicleId(chronicleId)
                .orElseThrow(() -> new NotFoundException("Game run not found."));
        if (run.getStatus() != GameRunStatusType.IN_PROGRESS) {
            throw new BadRequestException("This chronicle has already finished.");
        }
        final var participant = gameParticipantRepository.findByRunIdAndUserId(run.getId(), targetUserId)
                .orElseThrow(() -> new NotFoundException("This user has never participated in this chronicle."));
        if (participant.getStatus() != GameParticipantStatusType.LEFT) {
            throw new ConflictException("This participant has not left the chronicle.");
        }
        final var current = currentUserService.require();
        final var self = targetUserId.equals(current.id());
        if (participant.getRemovedByType() == RemovedByType.NARRATOR) {
            if (!context.narrator()) {
                throw new ForbiddenException("Only a narrator can bring this participant back.");
            }
        } else if (!self && !context.narrator()) {
            throw new ForbiddenException("Only the participant themself or a narrator can rejoin them.");
        }
        final var targetUser = userRepository.findById(targetUserId).orElseThrow(() -> new NotFoundException("User not found."));
        insertIntoRun(context.chronicle(), targetUser);
    }

    private void requireGameType(final ChronicleEntity chronicle) {
        if (chronicle.getType() != ChronicleType.GAME) {
            throw new BadRequestException("This chronicle is not a game chronicle.");
        }
    }

    @Transactional
    public void regenerate(final UUID partyId, final UUID chronicleId) {
        final var context = chronicleAccessService.requireNarrator(partyId, chronicleId);
        if (context.chronicle().getStatus() != ChronicleStatusType.PUBLISHED
                && context.chronicle().getStatus() != ChronicleStatusType.FAILED) {
            throw new BadRequestException("The chronicle is not ready for regeneration.");
        }
        log.debug("Regeneration requested for chronicle {} by user {}.", chronicleId, context.membership().getUser().getId());
        aiJobService.enqueueRequested(context.chronicle(), context.membership().getUser());
    }

    @Transactional
    public void reRunAi(final UUID partyId, final UUID chronicleId) {
        final var context = chronicleAccessService.requireNarrator(partyId, chronicleId);
        final var chronicle = context.chronicle();
        if (chronicle.getStatus() != ChronicleStatusType.PUBLISHED && chronicle.getStatus() != ChronicleStatusType.FAILED) {
            throw new BadRequestException("The chronicle is not ready for regeneration.");
        }
        aiJobService.requireConfigured();
        final var requestedBy = context.membership().getUser();
        aiJobService.enqueueIfIdle(chronicle, requestedBy, AiJobType.STORY_ADAPTATION_GENERATION);
        canonMapGenerationService.enqueueGenerationIfIdle(chronicle);
        chronicleSynopsisService.enqueueGenerationIfIdle(chronicle);
        log.debug("Full AI re-run requested for chronicle {} by user {}.", chronicleId, requestedBy.getId());
    }

    @Scheduled(fixedDelay = EXPIRATION_SWEEP_MILLISECONDS)
    @Transactional
    public void expireTurns() {
        final var expiredTurns = gameTurnRepository.findAllByStatusAndExpiresAtBefore(GameTurnStatusType.ACTIVE, Instant.now());
        if (!expiredTurns.isEmpty()) log.debug("Expiration sweep found {} candidate turn(s).", expiredTurns.size());
        for (final var turn : expiredTurns) {
            final var run = gameRunRepository.findForUpdateByChronicleId(turn.getRun().getChronicle().getId()).orElse(null);
            if (run == null) continue;
            final var current = gameTurnRepository.findByRunIdAndSequenceNumber(run.getId(), run.getCurrentSequence()).orElse(null);
            if (current == null || current.getStatus() != GameTurnStatusType.ACTIVE || !current.getId().equals(turn.getId())) continue;
            current.setStatus(GameTurnStatusType.EXPIRED);
            gameDraftRepository.deleteById(current.getId());
            log.debug("Turn {} expired for chronicle {}.", current.getId(), run.getChronicle().getId());
            advance(run, current.getSequenceNumber());
        }
    }

    private GameRunEntity requireRunForUpdate(final UUID partyId, final UUID chronicleId) {
        final var context = chronicleAccessService.requireMember(partyId, chronicleId);
        if (context.chronicle().getType() != ChronicleType.GAME) {
            throw new BadRequestException("This chronicle is not a game chronicle.");
        }
        return gameRunRepository.findForUpdateByChronicleId(chronicleId)
                .orElseThrow(() -> new NotFoundException("Game run not found."));
    }

    private GameTurnEntity requireCurrentTurn(final GameRunEntity run) {
        if (run.getStatus() != GameRunStatusType.IN_PROGRESS) {
            throw new ConflictException("This game chronicle is already complete.");
        }
        final var turn = gameTurnRepository.findByRunIdAndSequenceNumber(run.getId(), run.getCurrentSequence())
                .orElseThrow(() -> new NotFoundException("Current turn not found."));
        if (turn.getStatus() != GameTurnStatusType.ACTIVE) {
            throw new ConflictException("The current turn is not active.");
        }
        if (turn.getExpiresAt() != null && !turn.getExpiresAt().isAfter(Instant.now())) {
            turn.setStatus(GameTurnStatusType.EXPIRED);
            advance(run, turn.getSequenceNumber());
            throw new TurnExpiredException("Your turn expired and the game advanced.");
        }
        return turn;
    }

    private void requireCurrentUser(final GameTurnEntity turn) {
        if (!turn.getUser().getId().equals(currentUserService.require().id())) {
            throw new ForbiddenException("It is not your turn.");
        }
    }

    private void advance(final GameRunEntity run, final int completedSequence) {
        final var nextSequence = completedSequence + 1;
        final var next = gameTurnRepository.findByRunIdAndSequenceNumber(run.getId(), nextSequence);
        if (next.isPresent()) {
            run.setCurrentSequence(nextSequence);
            activate(next.get(), Instant.now());
            log.debug("Chronicle {} advanced to turn {} (user {}).", run.getChronicle().getId(), nextSequence, next.get().getUser().getId());
            return;
        }
        completeRun(run);
    }

    private void completeRun(final GameRunEntity run) {
        run.setStatus(GameRunStatusType.COMPLETED);
        run.setCompletedAt(Instant.now());
        final var chronicle = run.getChronicle();
        chronicle.setStatus(ChronicleStatusType.AI_PENDING);
        log.info("Game chronicle {} completed; queued the AI artifact pipeline.", chronicle.getId());
        aiJobService.enqueue(chronicle, chronicle.getCreator());
        canonMapGenerationService.enqueueGeneration(chronicle);
        chronicleSynopsisService.enqueueGeneration(chronicle);
    }

    private void insertIntoRun(final ChronicleEntity chronicle, final UserEntity user) {
        final var membership = partyMemberRepository.findByPartyIdAndUserId(chronicle.getParty().getId(), user.getId()).orElse(null);
        if (membership == null || membership.getRole() == PartyRoleType.SPECTATOR) return;

        final var run = gameRunRepository.findForUpdateByChronicleId(chronicle.getId()).orElse(null);
        if (run == null || run.getStatus() != GameRunStatusType.IN_PROGRESS) return;

        final var participant = gameParticipantRepository.findByRunIdAndUserId(run.getId(), user.getId())
                .orElseGet(() -> new GameParticipantEntity(run, user));
        participant.markRejoined();
        gameParticipantRepository.save(participant);

        final var alreadyPresentCycles = gameTurnRepository.findAllByRunIdAndUserId(run.getId(), user.getId())
                .stream().map(GameTurnEntity::getCycleNumber).collect(Collectors.toSet());
        final var currentTurn = gameTurnRepository.findByRunIdAndSequenceNumber(run.getId(), run.getCurrentSequence())
                .orElseThrow(() -> new NotFoundException("Current turn not found."));
        final var currentCycle = currentTurn.getCycleNumber();

        final var byCycle = new TreeMap<Short, List<GameTurnEntity>>();
        for (final var turn : gameTurnRepository.findAllByRunIdOrderBySequenceNumberAsc(run.getId())) {
            byCycle.computeIfAbsent(turn.getCycleNumber(), cycle -> new ArrayList<>()).add(turn);
        }

        var inserted = false;
        for (final var entry : byCycle.entrySet()) {
            final var cycle = entry.getKey();
            final var cycleTurns = entry.getValue();
            if (cycle >= currentCycle && !alreadyPresentCycles.contains(cycle)) {
                final var nextPosition = cycleTurns.getLast().getPositionInCycle() + 1;
                cycleTurns.add(new GameTurnEntity(run, user, cycle, nextPosition, 0));
                inserted = true;
            }
        }
        if (!inserted) return;

        var sequence = 0;
        for (final var cycleTurns : byCycle.values()) {
            for (final var turn : cycleTurns) {
                turn.setSequenceNumber(++sequence);
            }
        }
        run.setParticipantCount(byCycle.lastEntry().getValue().size());
        gameTurnRepository.saveAll(byCycle.values().stream().flatMap(List::stream).toList());
    }

    private void removeFromRun(
            final ChronicleEntity chronicle,
            final UUID userId,
            final RemovedByType removedByType,
            final UserEntity actor
    ) {
        final var run = gameRunRepository.findForUpdateByChronicleId(chronicle.getId()).orElse(null);
        if (run == null || run.getStatus() != GameRunStatusType.IN_PROGRESS) return;

        gameParticipantRepository.findByRunIdAndUserId(run.getId(), userId).ifPresent(participant -> {
            participant.markLeft(removedByType, actor);
            gameParticipantRepository.save(participant);
        });

        final var byCycle = new TreeMap<Short, List<GameTurnEntity>>();
        for (final var turn : gameTurnRepository.findAllByRunIdOrderBySequenceNumberAsc(run.getId())) {
            byCycle.computeIfAbsent(turn.getCycleNumber(), cycle -> new ArrayList<>()).add(turn);
        }

        final var currentSequence = run.getCurrentSequence();
        final var toDelete = new ArrayList<GameTurnEntity>();
        var activeRemoved = false;
        for (final var cycleTurns : byCycle.values()) {
            final var iterator = cycleTurns.iterator();
            while (iterator.hasNext()) {
                final var turn = iterator.next();
                final var notYetPlayed = turn.getStatus() == GameTurnStatusType.WAITING || turn.getStatus() == GameTurnStatusType.ACTIVE;
                if (turn.getUser().getId().equals(userId) && notYetPlayed) {
                    if (turn.getSequenceNumber() == currentSequence) activeRemoved = true;
                    toDelete.add(turn);
                    iterator.remove();
                }
            }
        }
        if (toDelete.isEmpty()) return;

        for (final var cycleTurns : byCycle.values()) {
            var position = 0;
            for (final var turn : cycleTurns) turn.setPositionInCycle(++position);
        }
        var sequence = 0;
        for (final var cycleTurns : byCycle.values()) {
            for (final var turn : cycleTurns) turn.setSequenceNumber(++sequence);
        }
        gameTurnRepository.saveAll(byCycle.values().stream().flatMap(List::stream).toList());
        gameTurnRepository.deleteAll(toDelete);
        run.setParticipantCount(byCycle.isEmpty() ? 0 : byCycle.lastEntry().getValue().size());

        if (activeRemoved) {
            final var next = byCycle.values().stream().flatMap(List::stream)
                    .filter(turn -> turn.getStatus() == GameTurnStatusType.WAITING)
                    .findFirst();
            if (next.isPresent()) {
                run.setCurrentSequence(next.get().getSequenceNumber());
                activate(next.get(), Instant.now());
            } else {
                completeRun(run);
            }
        }
    }

    private void activate(final GameTurnEntity turn, final Instant startedAt) {
        turn.setStatus(GameTurnStatusType.ACTIVE);
        turn.setStartedAt(startedAt);
        turn.setExpiresAt(startedAt.plus(properties.turnExpirationHours(), ChronoUnit.HOURS));
    }

    private List<UserEntity> buildFixedOrder(final UserEntity creator, final List<UserEntity> allUsers) {
        final var remaining = allUsers.stream().filter(user -> !user.getId().equals(creator.getId())).collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(remaining);
        final var ordered = new ArrayList<UserEntity>();
        ordered.add(creator);
        ordered.addAll(remaining);
        return ordered;
    }

    private Set<Integer> visibleSequencesFor(
            final UUID viewerId,
            final GameRunEntity run,
            final List<GameSegmentEntity> segments
    ) {
        final var bySequence = segments.stream().collect(Collectors.toMap(GameSegmentEntity::getSequenceNumber, Function.identity()));
        final var visible = new HashSet<Integer>();
        for (final var segment : segments) {
            if (segment.getAuthor().getId().equals(viewerId)) {
                visible.add(segment.getSequenceNumber());
                previousActiveSequence(segment.getSequenceNumber(), bySequence).ifPresent(visible::add);
            }
        }
        final var currentTurn = gameTurnRepository.findByRunIdAndSequenceNumber(run.getId(), run.getCurrentSequence()).orElse(null);
        if (currentTurn != null && currentTurn.getUser().getId().equals(viewerId)) {
            previousActiveSequence(currentTurn.getSequenceNumber(), bySequence).ifPresent(visible::add);
        }
        return visible;
    }

    private Optional<Integer> previousActiveSequence(final int sequence, final Map<Integer, GameSegmentEntity> bySequence) {
        for (var candidate = sequence - 1; candidate >= 1; candidate--) {
            final var segment = bySequence.get(candidate);
            if (segment != null && segment.getStatus() != SegmentStatusType.DISABLED) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private GameSegmentEntity requireSegment(final ChronicleEntity chronicle, final UUID segmentId) {
        final var segment = gameSegmentRepository.findById(segmentId)
                .orElseThrow(() -> new NotFoundException("Segment not found."));
        if (!segment.getRun().getChronicle().getId().equals(chronicle.getId())) {
            throw new NotFoundException("Segment not found.");
        }
        return segment;
    }

    private String trimToNull(final String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
