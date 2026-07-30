package com.narrativeplatform.app.canon.services;

import com.narrativeplatform.app.canon.models.entities.PartyAiTagSettingEntity;
import com.narrativeplatform.app.canon.models.enums.CanonCategoryType;
import com.narrativeplatform.app.canon.models.enums.TagColorType;
import com.narrativeplatform.app.canon.models.requests.TagSettingItemRequest;
import com.narrativeplatform.app.canon.models.requests.UpdateAiTagSettingsRequest;
import com.narrativeplatform.app.canon.repositories.PartyAiTagSettingRepository;
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
import com.narrativeplatform.shared.exceptions.BadRequestException;
import com.narrativeplatform.shared.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link PartyAccessService} is constructed for real (wired to mocked repositories), matching
 * {@code InvitationServiceTest}'s established pattern, so authorization is genuinely exercised.
 */
@ExtendWith(MockitoExtension.class)
class PartyAiTagSettingsServiceTest {
    @Mock
    private PartyAiTagSettingRepository partyAiTagSettingRepository;
    @Mock
    private PartyRepository partyRepository;
    @Mock
    private PartyMemberRepository partyMemberRepository;
    @Mock
    private CurrentUserService currentUserService;

    private PartyAccessService partyAccessService;
    private PartyAiTagSettingsService service;

    private PartyEntity party;
    private UUID partyId;
    private UserEntity owner;

    @BeforeEach
    void setUp() {
        partyAccessService = new PartyAccessService(partyMemberRepository, currentUserService);
        service = new PartyAiTagSettingsService(partyAiTagSettingRepository, partyRepository, partyAccessService);

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
    void getLazilyCreatesTheDocumentedDefaultsWhenNoneExist() {
        currentUserIs(owner, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        when(partyAiTagSettingRepository.findAllByPartyIdOrderByDisplayOrderAsc(partyId)).thenReturn(List.of());
        when(partyRepository.findById(partyId)).thenReturn(Optional.of(party));
        when(partyAiTagSettingRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var response = service.get(partyId);

        assertEquals(5, response.settings().size());
        final var byCategory = response.settings().stream()
                .collect(java.util.stream.Collectors.toMap(item -> item.category(), item -> item));
        assertEquals(TagColorType.VIOLET, byCategory.get(CanonCategoryType.PERSON).color());
        assertEquals(1, byCategory.get(CanonCategoryType.PERSON).displayOrder());
        assertEquals(TagColorType.GOLD, byCategory.get(CanonCategoryType.PLACE).color());
        assertEquals(TagColorType.COPPER, byCategory.get(CanonCategoryType.ITEM).color());
        assertEquals(TagColorType.AZURE, byCategory.get(CanonCategoryType.SPELL).color());
        assertEquals(TagColorType.GREEN, byCategory.get(CanonCategoryType.CREATURE).color());
        assertTrue(byCategory.values().stream().allMatch(item -> item.enabled()));
    }

    @Test
    void getDoesNotRecreateWhenSettingsAlreadyExist() {
        currentUserIs(owner, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        final var existing = List.of(new PartyAiTagSettingEntity(party, CanonCategoryType.PERSON, true, TagColorType.VIOLET, 1));
        when(partyAiTagSettingRepository.findAllByPartyIdOrderByDisplayOrderAsc(partyId))
                .thenReturn(existing, existing);

        service.get(partyId);
        service.get(partyId);

        verify(partyAiTagSettingRepository, never()).saveAll(any());
        verifyNoInteractions(partyRepository);
    }

    @Test
    void ordinaryPlayerCannotUpdate() {
        currentUserIs(owner, PartyRoleType.PLAYER, MemberStatusType.ACTIVE);
        assertThrows(ForbiddenException.class, () -> service.update(partyId, new UpdateAiTagSettingsRequest(List.of())));
        verifyNoInteractions(partyAiTagSettingRepository);
    }

    @Test
    void narratorCanUpdateAndZeroEnabledCategoriesIsValid() {
        currentUserIs(owner, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        final var existing = allDefaultEntities();
        when(partyAiTagSettingRepository.findAllByPartyIdOrderByDisplayOrderAsc(partyId)).thenReturn(existing);

        final var allDisabled = List.of(CanonCategoryType.values()).stream()
                .map(category -> new TagSettingItemRequest(category, false, TagColorType.SLATE, 1))
                .toList();

        assertDoesNotThrow(() -> service.update(partyId, new UpdateAiTagSettingsRequest(allDisabled)));
        assertTrue(existing.stream().noneMatch(PartyAiTagSettingEntity::isEnabled));
    }

    @Test
    void updateRejectsDuplicateCategory() {
        currentUserIs(owner, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        final var duplicated = List.of(
                new TagSettingItemRequest(CanonCategoryType.PERSON, true, TagColorType.VIOLET, 1),
                new TagSettingItemRequest(CanonCategoryType.PERSON, true, TagColorType.GOLD, 2)
        );
        assertThrows(BadRequestException.class, () -> service.update(partyId, new UpdateAiTagSettingsRequest(duplicated)));
    }

    @Test
    void updateRejectsMissingCategory() {
        currentUserIs(owner, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        final var incomplete = List.of(new TagSettingItemRequest(CanonCategoryType.PERSON, true, TagColorType.VIOLET, 1));
        assertThrows(BadRequestException.class, () -> service.update(partyId, new UpdateAiTagSettingsRequest(incomplete)));
    }

    @Test
    void updateNeverDeletesAndRecreatesRows() {
        currentUserIs(owner, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        final var existing = allDefaultEntities();
        when(partyAiTagSettingRepository.findAllByPartyIdOrderByDisplayOrderAsc(partyId)).thenReturn(existing);

        final var updates = List.of(CanonCategoryType.values()).stream()
                .map(category -> new TagSettingItemRequest(category, true, TagColorType.ROSE, 9))
                .toList();
        service.update(partyId, new UpdateAiTagSettingsRequest(updates));

        verify(partyAiTagSettingRepository, never()).deleteAll();
        verify(partyAiTagSettingRepository, never()).save(any());
        assertTrue(existing.stream().allMatch(item -> item.getColor() == TagColorType.ROSE && item.getDisplayOrder() == 9));
    }

    private List<PartyAiTagSettingEntity> allDefaultEntities() {
        return List.of(CanonCategoryType.values()).stream()
                .map(category -> new PartyAiTagSettingEntity(party, category, true, TagColorType.VIOLET, 1))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
    }
}
