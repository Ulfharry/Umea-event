package com.umeaevents.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Admin: create an event that goes straight to PUBLISHED (no review queue).
 *
 * <p>Exactly one schedule mode: {@code startsAt} for a single event, {@code recurrence} for a
 * rule-driven series (materialised immediately), or {@code pickedDates} for a hand-picked set of
 * days from the calendar.
 */
public record CreatePublishedEventRequest(
        @NotBlank String title,
        String description,
        String imageUrl,
        @NotNull UUID venueId,
        @NotNull UUID categoryId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Recurrence recurrence,
        PickedDates pickedDates
) {
    /**
     * @param excludedDates days the author unticked in the preview calendar; stored as cancelled
     *                      occurrences, which the materialiser already skips.
     */
    public record Recurrence(
            String rrule,
            LocalTime startTime,
            Integer durationMinutes,
            String timezone,
            java.time.LocalDate startsOn,
            java.util.List<java.time.LocalDate> excludedDates
    ) {}
}
