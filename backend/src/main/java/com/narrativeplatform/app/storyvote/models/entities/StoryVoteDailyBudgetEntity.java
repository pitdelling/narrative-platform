package com.narrativeplatform.app.storyvote.models.entities;

import com.narrativeplatform.app.party.models.entities.PartyEntity;
import com.narrativeplatform.app.party.models.entities.PartyMemberEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "story_vote_daily_budgets", uniqueConstraints = @UniqueConstraint(columnNames = {"party_id", "membership_id", "vote_date"}))
public class StoryVoteDailyBudgetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    @ToString.Exclude
    private PartyEntity party;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id")
    @ToString.Exclude
    private PartyMemberEntity membership;

    @Column(name = "vote_date", nullable = false)
    private LocalDate voteDate;

    @Column(name = "used_units", nullable = false)
    private short usedUnits;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public StoryVoteDailyBudgetEntity(final PartyEntity party, final PartyMemberEntity membership, final LocalDate voteDate) {
        this.party = party;
        this.membership = membership;
        this.voteDate = voteDate;
        this.usedUnits = 0;
    }
}
