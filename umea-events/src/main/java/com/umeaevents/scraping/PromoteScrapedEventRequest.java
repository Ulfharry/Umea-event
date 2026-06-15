package com.umeaevents.scraping;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PromoteScrapedEventRequest(
        @NotNull UUID venueId,
        @NotNull UUID categoryId,
        @NotNull OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String adminNotes
) {}
