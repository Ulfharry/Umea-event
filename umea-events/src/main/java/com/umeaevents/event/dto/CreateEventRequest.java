package com.umeaevents.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateEventRequest(
        @NotBlank String title,
        String description,
        String imageUrl,
        @NotNull UUID venueId,
        @NotNull UUID categoryId,
        @NotNull OffsetDateTime startsAt,
        OffsetDateTime endsAt
) {}
