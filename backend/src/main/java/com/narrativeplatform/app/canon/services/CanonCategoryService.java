package com.narrativeplatform.app.canon.services;

import com.narrativeplatform.app.canon.models.entities.CanonCategoryEntity;
import com.narrativeplatform.app.canon.models.requests.CreateCanonCategoryRequest;
import com.narrativeplatform.app.canon.models.requests.UpdateCanonCategoryRequest;
import com.narrativeplatform.app.canon.models.responses.CanonCategoryConfigResponse;
import com.narrativeplatform.app.canon.repositories.CanonCategoryRepository;
import com.narrativeplatform.app.party.repositories.PartyRepository;
import com.narrativeplatform.app.party.services.PartyAccessService;
import com.narrativeplatform.shared.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CanonCategoryService {
    private final CanonCategoryRepository canonCategoryRepository;
    private final PartyRepository partyRepository;
    private final PartyAccessService partyAccessService;

    public List<CanonCategoryConfigResponse> list(final UUID partyId) {
        partyAccessService.requireActiveMember(partyId);
        return canonCategoryRepository.findAllByPartyIdOrderByDisplayOrderAsc(partyId)
                .stream().map(CanonCategoryEntity::toResponse).toList();
    }

    @Transactional
    public CanonCategoryConfigResponse create(final UUID partyId, final CreateCanonCategoryRequest request) {
        partyAccessService.requireNarrator(partyId);
        final var party = partyRepository.findById(partyId).orElseThrow(() -> new NotFoundException("Party not found."));
        final var existing = canonCategoryRepository.findAllByPartyIdOrderByDisplayOrderAsc(partyId);
        final var nextOrder = existing.isEmpty() ? 0 : existing.getLast().getDisplayOrder() + 1;
        final var category = canonCategoryRepository.save(new CanonCategoryEntity(
                party, request.name().trim(), trimToNull(request.description()), request.color(), nextOrder
        ));
        return category.toResponse();
    }

    @Transactional
    public CanonCategoryConfigResponse update(final UUID partyId, final UUID categoryId, final UpdateCanonCategoryRequest request) {
        partyAccessService.requireNarrator(partyId);
        final var category = requireOwnedCategory(partyId, categoryId);
        category.setName(request.name().trim());
        category.setDescription(trimToNull(request.description()));
        category.setColor(request.color());
        return category.toResponse();
    }

    @Transactional
    public void delete(final UUID partyId, final UUID categoryId) {
        partyAccessService.requireNarrator(partyId);
        final var category = requireOwnedCategory(partyId, categoryId);
        canonCategoryRepository.delete(category);
    }

    @Transactional
    public void moveUp(final UUID partyId, final UUID categoryId) {
        swapWithAdjacent(partyId, categoryId, -1);
    }

    @Transactional
    public void moveDown(final UUID partyId, final UUID categoryId) {
        swapWithAdjacent(partyId, categoryId, 1);
    }

    private void swapWithAdjacent(final UUID partyId, final UUID categoryId, final int direction) {
        partyAccessService.requireNarrator(partyId);
        final var ordered = canonCategoryRepository.findAllByPartyIdOrderByDisplayOrderAsc(partyId);
        final var index = ordered.stream().map(CanonCategoryEntity::getId).toList().indexOf(categoryId);
        if (index < 0) {
            throw new NotFoundException("Canon category not found.");
        }
        final var adjacentIndex = index + direction;
        if (adjacentIndex < 0 || adjacentIndex >= ordered.size()) {
            return;
        }
        final var current = ordered.get(index);
        final var adjacent = ordered.get(adjacentIndex);
        final var currentOrder = current.getDisplayOrder();
        current.setDisplayOrder(adjacent.getDisplayOrder());
        adjacent.setDisplayOrder(currentOrder);
    }

    private CanonCategoryEntity requireOwnedCategory(final UUID partyId, final UUID categoryId) {
        return canonCategoryRepository.findByIdAndPartyId(categoryId, partyId)
                .orElseThrow(() -> new NotFoundException("Canon category not found."));
    }

    private String trimToNull(final String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
