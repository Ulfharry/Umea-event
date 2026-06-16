package com.umeaevents.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record CreateRecurringEventRequest(
        @NotBlank String title,
        String description,
        String imageUrl,
        @NotNull UUID venueId,
        @NotNull UUID categoryId,
        @NotBlank String rrule,
        @NotNull LocalTime startTime,
        Integer durationMinutes,
        @NotBlank String timezone
) {}
