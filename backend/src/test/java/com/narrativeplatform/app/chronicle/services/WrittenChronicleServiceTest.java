package com.narrativeplatform.app.chronicle.services;

import com.narrativeplatform.app.auth.models.entities.UserEntity;
import com.narrativeplatform.app.auth.repositories.UserRepository;
import com.narrativeplatform.app.chronicle.models.entities.ChronicleEntity;
import com.narrativeplatform.app.chronicle.models.entities.WrittenStoryDocumentEntity;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleStatusType;
import com.narrativeplatform.app.chronicle.models.enums.ChronicleType;
import com.narrativeplatform.app.chronicle.models.requests.SaveWrittenStoryRequest;
import com.narrativeplatform.app.chronicle.repositories.ChronicleRepository;
import com.narrativeplatform.app.chronicle.repositories.WrittenStoryDocumentRepository;
import com.narrativeplatform.app.chronicle.repositories.WrittenStoryPermissionRepository;
import com.narrativeplatform.app.party.models.entities.PartyEntity;
import com.narrativeplatform.app.party.models.entities.PartyMemberEntity;
import com.narrativeplatform.app.party.models.enums.MemberStatusType;
import com.narrativeplatform.app.party.models.enums.PartyRoleType;
import com.narrativeplatform.app.party.repositories.PartyMemberRepository;
import com.narrativeplatform.app.party.services.PartyAccessService;
import com.narrativeplatform.configuration.AppProperties;
import com.narrativeplatform.security.AuthenticatedUser;
import com.narrativeplatform.security.CurrentUserService;
import com.narrativeplatform.shared.utils.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WrittenChronicleServiceTest {
    @Mock
    private ChronicleRepository chronicleRepository;
    @Mock
    private WrittenStoryDocumentRepository documentRepository;
    @Mock
    private WrittenStoryPermissionRepository permissionRepository;
    @Mock
    private PartyMemberRepository memberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChronicleAccessService chronicleAccessService;
    @Mock
    private CurrentUserService currentUserService;

    private WrittenChronicleService service;

    private UUID partyId;
    private UUID chronicleId;
    private ChronicleEntity chronicle;
    private UserEntity narrator;

    @BeforeEach
    void setUp() {
        final var partyAccessService = new PartyAccessService(memberRepository, currentUserService);
        final var properties = new AppProperties(
                "http://localhost:3000", "http://localhost:3000", "test-secret",
                168, 24, 24, 10,
                new AppProperties.OpenAi("", "gpt-5-mini", "https://api.openai.com/v1")
        );
        service = new WrittenChronicleService(
                chronicleRepository, documentRepository, permissionRepository, memberRepository,
                userRepository, partyAccessService, chronicleAccessService, currentUserService, properties
        );

        partyId = UUID.randomUUID();
        chronicleId = UUID.randomUUID();
        final var party = new PartyEntity("Test Party", "test-party", null, null, null);
        party.setId(partyId);
        narrator = new UserEntity("narrator", "Narrator", "hash");
        narrator.setId(UUID.randomUUID());
        chronicle = new ChronicleEntity(party, narrator, ChronicleType.WRITTEN, ChronicleStatusType.DRAFT, "Title");
        chronicle.setId(chronicleId);

        final var membership = new PartyMemberEntity(party, narrator, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE);
        lenient().when(chronicleAccessService.requireMember(partyId, chronicleId))
                .thenReturn(new ChronicleAccessService.AccessContext(chronicle, membership));
        lenient().when(currentUserService.require()).thenReturn(new AuthenticatedUser(narrator.getId(), narrator.getUsername()));
    }

    @Test
    void saveSanitizesContentBeforePersisting() {
        final var document = lockedDocument();
        when(documentRepository.findForUpdate(chronicleId)).thenReturn(Optional.of(document));

        service.save(partyId, chronicleId, "raw-token", new SaveWrittenStoryRequest("<p>Safe</p><script>alert(1)</script>", 0));

        assertEquals("<p>Safe</p>", document.getContent());
    }

    @Test
    void publishGeneratesAPlainTextPreviewWithoutHtmlTags() {
        final var document = new WrittenStoryDocumentEntity(chronicle);
        document.setContent("<p>Chapter one</p><p>Chapter <b>two</b></p>");
        when(chronicleAccessService.requireNarrator(partyId, chronicleId))
                .thenReturn(new ChronicleAccessService.AccessContext(chronicle, new PartyMemberEntity(
                        chronicle.getParty(), narrator, PartyRoleType.NARRATOR, MemberStatusType.ACTIVE)));
        when(documentRepository.findByChronicleId(chronicleId)).thenReturn(Optional.of(document));

        service.publish(partyId, chronicleId);

        assertEquals("Chapter one\nChapter two", chronicle.getGeneratedPreview());
        assertFalse(chronicle.getGeneratedPreview().contains("<"));
    }

    private WrittenStoryDocumentEntity lockedDocument() {
        final var document = new WrittenStoryDocumentEntity(chronicle);
        document.setLockedBy(narrator);
        document.setLockTokenHash(TokenUtils.sha256("raw-token"));
        document.setLockedAt(Instant.now());
        document.setLockExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        document.setContentVersion(0);
        return document;
    }
}
