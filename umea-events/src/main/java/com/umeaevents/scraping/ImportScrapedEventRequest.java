package com.umeaevents.scraping;

import jakarta.validation.constraints.NotBlank;

public record ImportScrapedEventRequest(
        @NotBlank String rawTitle,
        String rawDescription,
        String rawVenueName,
        String rawStartsAt,
        String rawEndsAt,
        String parsedStartsAt,
        String parsedEndsAt
) {}
