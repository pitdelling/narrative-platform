package com.narrativeplatform.app.canon.models.entities;

import com.narrativeplatform.app.chronicle.models.entities.GameSegmentEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "canon_tag_sources", uniqueConstraints = @UniqueConstraint(columnNames = {"tag_id", "segment_id"}))
public class CanonTagSourceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id")
    @ToString.Exclude
    private CanonTagEntity tag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "segment_id")
    @ToString.Exclude
    private GameSegmentEntity segment;

    public CanonTagSourceEntity(final CanonTagEntity tag, final GameSegmentEntity segment) {
        this.tag = tag;
        this.segment = segment;
    }
}
