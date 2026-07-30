package com.narrativeplatform.app.canon.services;

import com.narrativeplatform.app.canon.models.entities.PartyAiTagSettingEntity;
import com.narrativeplatform.app.canon.models.enums.CanonCategoryType;
import com.narrativeplatform.app.canon.models.enums.TagColorType;
import com.narrativeplatform.app.canon.models.requests.TagSettingItemRequest;
import com.narrativeplatform.app.canon.models.requests.UpdateAiTagSettingsRequest;
import com.narrativeplatform.app.canon.models.responses.PartyAiTagSettingsResponse;
import com.narrativeplatform.app.canon.repositories.PartyAiTagSettingRepository;
import com.narrativeplatform.app.party.repositories.PartyRepository;
import com.narrativeplatform.app.party.services.PartyAccessService;
import com.narrativeplatform.shared.exceptions.BadRequestException;
import com.narrativeplatform.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartyAiTagSettingsService {
    private static final Map<CanonCategoryType, TagColorType> DEFAULT_COLORS = new EnumMap<>(CanonCategoryType.class);
    private static final Map<CanonCategoryType, Integer> DEFAULT_ORDER = new EnumMap<>(CanonCategoryType.class);

    static {
        DEFAULT_COLORS.put(CanonCategoryType.PERSON, TagColorType.VIOLET);
        DEFAULT_COLORS.put(CanonCategoryType.PLACE, TagColorType.GOLD);
        DEFAULT_COLORS.put(CanonCategoryType.ITEM, TagColorType.COPPER);
        DEFAULT_COLORS.put(CanonCategoryType.SPELL, TagColorType.AZURE);
        DEFAULT_COLORS.put(CanonCategoryType.CREATURE, TagColorType.GREEN);

        DEFAULT_ORDER.put(CanonCategoryType.PERSON, 1);
        DEFAULT_ORDER.put(CanonCategoryType.PLACE, 2);
        DEFAULT_ORDER.put(CanonCategoryType.ITEM, 3);
        DEFAULT_ORDER.put(CanonCategoryType.SPELL, 4);
        DEFAULT_ORDER.put(CanonCategoryType.CREATURE, 5);
    }

    private final PartyAiTagSettingRepository partyAiTagSettingRepository;
    private final PartyRepository partyRepository;
    private final PartyAccessService partyAccessService;

    public PartyAiTagSettingsResponse get(final UUID partyId) {
        partyAccessService.requireActiveMember(partyId);
        final var settings = getOrCreateDefaults(partyId);
        return new PartyAiTagSettingsResponse(settings.stream().map(PartyAiTagSettingEntity::toResponse).toList());
    }

    @Transactional
    public void update(final UUID partyId, final UpdateAiTagSettingsRequest request) {
        partyAccessService.requireNarrator(partyId);
        final var byCategory = new EnumMap<CanonCategoryType, TagSettingItemRequest>(CanonCategoryType.class);
        for (final var item : request.settings()) {
            if (byCategory.put(item.category(), item) != null) {
                throw new BadRequestException("Duplicate category in tag settings: " + item.category() + ".");
            }
        }
        if (byCategory.size() != CanonCategoryType.values().length) {
            throw new BadRequestException("Exactly one setting per category is required.");
        }

        final var existing = getOrCreateDefaults(partyId);
        for (final var setting : existing) {
            final var update = byCategory.get(setting.getCategory());
            setting.setEnabled(update.enabled());
            setting.setColor(update.color());
            setting.setDisplayOrder(update.displayOrder());
        }
    }

    /**
     * Loads the party's tag settings, lazily creating the five documented defaults on first
     * access. Never deletes/recreates existing rows once they exist.
     */
    @Transactional
    public List<PartyAiTagSettingEntity> getOrCreateDefaults(final UUID partyId) {
        final var existing = partyAiTagSettingRepository.findAllByPartyIdOrderByDisplayOrderAsc(partyId);
        if (!existing.isEmpty()) {
            return existing;
        }
        final var party = partyRepository.findById(partyId).orElseThrow(() -> new NotFoundException("Party not found."));
        final var created = Arrays.stream(CanonCategoryType.values())
                .map(category -> new PartyAiTagSettingEntity(party, category, true, DEFAULT_COLORS.get(category), DEFAULT_ORDER.get(category)))
                .toList();
        try {
            return partyAiTagSettingRepository.saveAll(created);
        } catch (final DataIntegrityViolationException raceLostToAnotherRequest) {
            // Another concurrent first-access already created the defaults (uq_party_ai_tag_settings_category).
            return partyAiTagSettingRepository.findAllByPartyIdOrderByDisplayOrderAsc(partyId);
        }
    }
}
