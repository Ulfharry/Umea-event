package com.umeaevents.event.dto;

import com.umeaevents.event.EventStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventOccurrenceResponse(
        UUID id,
        UUID eventId,
        String title,
        String description,
        String imageUrl,
        UUID venueId,
        String venueName,
        UUID categoryId,
        String categoryName,
        EventStatus status,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        OffsetDateTime createdAt
) {}
