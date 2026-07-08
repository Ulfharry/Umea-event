package com.umeaevents.venue.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * One opening interval for a weekday. {@code closesAt} before {@code opensAt} means it runs past
 * midnight (e.g. 17:00–02:00).
 */
public record OpeningHoursDto(
        @Min(1) @Max(7) int dayOfWeek,
        @NotNull LocalTime opensAt,
        @NotNull LocalTime closesAt
) {}
