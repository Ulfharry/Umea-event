package com.umeaevents.scraping;

import com.umeaevents.event.Event;
import com.umeaevents.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "raw_scraped_event")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawScrapedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ScrapedEventSource source;

    @Column(length = 255)
    private String externalId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawTitle;

    @Column(columnDefinition = "TEXT")
    private String rawDescription;

    @Column(columnDefinition = "TEXT")
    private String rawVenueName;

    @Column(columnDefinition = "TEXT")
    private String rawStartsAt;

    @Column(columnDefinition = "TEXT")
    private String rawEndsAt;

    private OffsetDateTime parsedStartsAt;
    private OffsetDateTime parsedEndsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScrapedEventStatus status = ScrapedEventStatus.PENDING_REVIEW;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    private OffsetDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promoted_event_id")
    private Event promotedEvent;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}
