package com.umeaevents.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Admin: create an event that goes straight to PUBLISHED (no review queue).
 * Provide {@code startsAt} for a single event, or {@code recurrence} for a recurring series
 * (which is materialised immediately).
 */
public record CreatePublishedEventRequest(
        @NotBlank String title,
        String description,
        String imageUrl,
        @NotNull UUID venueId,
        @NotNull UUID categoryId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Recurrence recurrence
) {
    public record Recurrence(
            String rrule,
            LocalTime startTime,
            Integer durationMinutes,
            String timezone
    ) {}
}
