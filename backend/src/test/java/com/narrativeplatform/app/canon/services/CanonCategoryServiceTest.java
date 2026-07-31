package com.narrativeplatform.app.canon.services;

import com.narrativeplatform.app.canon.models.entities.CanonCategoryEntity;
import com.narrativeplatform.app.canon.models.requests.CreateCanonCategoryRequest;
import com.narrativeplatform.app.canon.models.requests.UpdateCanonCategoryRequest;
import com.narrativeplatform.app.canon.repositories.CanonCategoryRepository;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
import com.narrativeplatform.app.party.models.entities.PartyMemberEntity;
import com.narrativeplatform.app.party.models.enums.MemberStatusType;
import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import com.narrativeplatform.app.party.repositories.PartyMemberRepository;
import com.narrativeplatform.app.party.repositories.PartyRepository;
import com.narrativeplatform.app.party.services.PartyAccessService;
import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.security.AuthenticatedUser;
import com.narrativeplatform.security.CurrentUserService;
import com.narrativeplatform.shared.exceptions.ForbiddenException;
import com.narrativeplatform.shared.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link PartyAccessService} is constructed for real (wired to mocked repositories), matching
 * {@code InvitationServiceTest}'s established pattern, so authorization is genuinely exercised.
 */
@ExtendWith(MockitoExtension.class)
class CanonCategoryServiceTest {
    @Mock
    private CanonCategoryRepository canonCategoryRepository;
    @Mock
    private PartyRepository partyRepository;
    @Mock
    private PartyMemberRepository partyMemberRepository;
    @Mock
    private CurrentUserService currentUserService;

    private PartyAccessService partyAccessService;
    private CanonCategoryService service;

    private PartyEntity party;
    private UUID partyId;
    private UserEntity owner;

    @BeforeEach
    void setUp() {
        partyAccessService = new PartyAccessService(partyMemberRepository, currentUserService);
        service = new CanonCategoryService(canonCategoryRepository, partyRepository, partyAccessService);

        partyId = UUID.randomUUID();
        owner = new UserEntity("owner", "Owner", "hash");
        owner.setId(UUID.randomUUID());
        party = new PartyEntity("Test Party", "test-party", null, null, owner);
        party.setId(partyId);
    }

    private void currentUserIs(final UserEntity user, final PartyRoleType role, final MemberStatusType status) {
        when(currentUserService.require()).thenReturn(new AuthenticatedUser(user.getId(), user.getUsername()));
        final var membership = new PartyMemberEntity(party, user, role, status);
        when(partyMemberRepository.findByPartyIdAndUserId(partyId, user.getId())).thenReturn(Optional.of(membership));
    }

    @Test
    void listIsAllowedForAnyActiveMemberNotJustNarrator() {
        currentUserIs(owner, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        when(canonCategoryRepository.findAllByPartyIdOrderByDisplayOrderAsc(partyId)).thenReturn(List.of());

        assertDoesNotThrow(() -> service.list(partyId));
    }

    @Test
    void ordinaryPlayerCannotCreate() {
        currentUserIs(owner, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        assertThrows(ForbiddenException.class,
                () -> service.create(partyId, new CreateCanonCategoryRequest("Pessoas", null, "#7665a7")));
        verifyNoInteractions(canonCategoryRepository);
    }

    @Test
    void createAppendsAtTheEndOfTheExistingOrder() {
        currentUserIs(owner, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        final var existing = new ArrayList<>(List.of(
                new CanonCategoryEntity(party, "Pessoas", null, "#7665a7", 0),
                new CanonCategoryEntity(party, "Lugares", null, "#c29042", 1)
        ));
        when(canonCategoryRepository.findAllByPartyIdOrderByDisplayOrderAsc(partyId)).thenReturn(existing);
        when(partyRepository.findById(partyId)).thenReturn(Optional.of(party));
        when(canonCategoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var response = service.create(partyId, new CreateCanonCategoryRequest("Itens", "Objetos", "#a76d3d"));

        assertEquals(2, response.displayOrder());
    }

    @Test
    void createOnAnEmptyListStartsAtZero() {
        currentUserIs(owner, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        when(canonCategoryRepository.findAllByPartyIdOrderByDisplayOrderAsc(partyId)).thenReturn(List.of());
        when(partyRepository.findById(partyId)).thenReturn(Optional.of(party));
        when(canonCategoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var response = service.create(partyId, new CreateCanonCategoryRequest("Pessoas", null, "#7665a7"));

        assertEquals(0, response.displayOrder());
    }

    @Test
    void updateOnlyMutatesNameDescriptionAndColorNeverDisplayOrder() {
        currentUserIs(owner, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        final var category = new CanonCategoryEntity(party, "Pessoas", "Antiga descrição", "#7665a7", 3);
        when(canonCategoryRepository.findByIdAndPartyId(any(), eq(partyId))).thenReturn(Optional.of(category));

        final var response = service.update(partyId, UUID.randomUUID(), new UpdateCanonCategoryRequest("Gente", "Nova descrição", "#c29042"));

        assertEquals("Gente", response.name());
        assertEquals("Nova descrição", response.description());
        assertEquals("#c29042", response.color());
        assertEquals(3, response.displayOrder());
    }

    @Test
    void deleteRemovesTheRow() {
        currentUserIs(owner, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        final var categoryId = UUID.randomUUID();
        final var category = new CanonCategoryEntity(party, "Pessoas", null, "#7665a7", 0);
        when(canonCategoryRepository.findByIdAndPartyId(categoryId, partyId)).thenReturn(Optional.of(category));

        service.delete(partyId, categoryId);

        verify(canonCategoryRepository).delete(category);
    }

    @Test
    void mutatingACategoryFromAnotherPartyIsRejectedAsNotFound() {
        currentUserIs(owner, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        final var categoryId = UUID.randomUUID();
        when(canonCategoryRepository.findByIdAndPartyId(categoryId, partyId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete(partyId, categoryId));
    }

    @Test
    void moveUpSwapsDisplayOrderWithThePreviousRow() {
        currentUserIs(owner, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        final var first = new CanonCategoryEntity(party, "Pessoas", null, "#7665a7", 0);
        final var second = new CanonCategoryEntity(party, "Lugares", null, "#c29042", 1);
        final var secondId = UUID.randomUUID();
        when(canonCategoryRepository.findAllByPartyIdOrderByDisplayOrderAsc(partyId))
                .thenAnswer(invocation -> List.of(first, second));
        setId(second, secondId);

        service.moveUp(partyId, secondId);

        assertEquals(1, first.getDisplayOrder());
        assertEquals(0, second.getDisplayOrder());
    }

    @Test
    void moveUpOnTheFirstRowIsANoOp() {
        currentUserIs(owner, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        final var first = new CanonCategoryEntity(party, "Pessoas", null, "#7665a7", 0);
        final var firstId = UUID.randomUUID();
        setId(first, firstId);
        when(canonCategoryRepository.findAllByPartyIdOrderByDisplayOrderAsc(partyId)).thenReturn(List.of(first));

        assertDoesNotThrow(() -> service.moveUp(partyId, firstId));
        assertEquals(0, first.getDisplayOrder());
    }

    @Test
    void moveDownOnTheLastRowIsANoOp() {
        currentUserIs(owner, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        final var only = new CanonCategoryEntity(party, "Pessoas", null, "#7665a7", 0);
        final var onlyId = UUID.randomUUID();
        setId(only, onlyId);
        when(canonCategoryRepository.findAllByPartyIdOrderByDisplayOrderAsc(partyId)).thenReturn(List.of(only));

        assertDoesNotThrow(() -> service.moveDown(partyId, onlyId));
        assertEquals(0, only.getDisplayOrder());
    }

    private void setId(final CanonCategoryEntity entity, final UUID id) {
        entity.setId(id);
    }
}
