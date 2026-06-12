package com.umeaevents.event.dto;

import com.umeaevents.event.EventStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        UUID venueId,
        String venueName,
        UUID categoryId,
        String categoryName,
        EventStatus status,
        UUID ownerId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
