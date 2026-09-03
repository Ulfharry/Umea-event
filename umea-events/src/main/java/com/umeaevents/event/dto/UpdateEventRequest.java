package com.umeaevents.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Edit an existing event. Title/description/image/venue/category always apply — and since these
 * live on the shared Event, a change is reflected on every occurrence automatically.
 *
 * <p>Schedule is optional and mode-specific:
 * <ul>
 *   <li>single event: set {@code startsAt}/{@code endsAt} to change the time (omit to keep it);</li>
 *   <li>recurring event: set {@code recurrence} to change the schedule — the whole series is
 *       regenerated (omit to keep it).</li>
 * </ul>
 */
public record UpdateEventRequest(
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
     * @param excludedDates the unticked days from the preview calendar. The submitted list is
     *                      authoritative: cancelled occurrences are replaced with exactly these.
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
