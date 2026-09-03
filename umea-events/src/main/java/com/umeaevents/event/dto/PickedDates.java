package com.umeaevents.event.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * A schedule given as an explicit list of days rather than a rule — the calendar mode.
 *
 * <p>These events deliberately have no {@code RecurrenceRule}: they are just occurrences, so the
 * materialiser leaves them alone. All days share one time, which keeps the picker simple; a venue
 * running the same thing at two different times makes two events.
 */
public record PickedDates(
        @NotEmpty List<LocalDate> dates,
        @NotNull LocalTime startTime,
        Integer durationMinutes,
        String timezone
) {}
