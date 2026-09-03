package com.umeaevents.event;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "recurrence_rule")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurrenceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rrule;

    @Column(nullable = false)
    private LocalTime startTime;

    /**
     * First day of the series. Nothing is generated before it, and it anchors INTERVAL parity
     * (every other week) so the rhythm survives the materialiser's sliding expansion window.
     * Null = start from whenever expansion happens to begin, as before.
     */
    @Column(name = "starts_on")
    private LocalDate startsOn;

    private Integer durationMinutes;

    @Column(nullable = false, length = 50)
    private String timezone;

    private OffsetDateTime horizon;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}
