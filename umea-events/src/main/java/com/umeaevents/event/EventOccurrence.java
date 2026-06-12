package com.umeaevents.event;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_occurrence")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventOccurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime startsAt;

    @Column(columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime endsAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}
