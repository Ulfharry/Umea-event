package com.umeaevents.scraping;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Promote a scraped event to a real PUBLISHED event.
 *
 * <p>Either a single occurrence (provide {@code startsAt}) or a recurring series (provide
 * {@code recurrence}). When {@code recurrence} is set, {@code startsAt}/{@code endsAt} are ignored
 * and the materialisation job generates concrete occurrences from the rule.
 */
public record PromoteScrapedEventRequest(
        @NotNull UUID venueId,
        @NotNull UUID categoryId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String adminNotes,
        RecurrenceInput recurrence
) {
    /**
     * @param rrule           RFC 5545 rule, e.g. {@code FREQ=WEEKLY;BYDAY=WE} or {@code FREQ=DAILY}
     * @param startTime       wall-clock start time in {@code timezone}
     * @param durationMinutes optional event length
     * @param timezone        IANA zone, e.g. {@code Europe/Stockholm}
     */
    public record RecurrenceInput(
            String rrule,
            LocalTime startTime,
            Integer durationMinutes,
            String timezone
    ) {}
}
