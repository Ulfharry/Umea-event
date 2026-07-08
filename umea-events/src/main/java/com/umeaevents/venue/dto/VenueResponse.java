package com.umeaevents.venue.dto;

import com.umeaevents.venue.VenueType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VenueResponse(
        UUID id,
        String name,
        String description,
        VenueType type,
        String address,
        String imageUrl,
        UUID ownerId,
        boolean active,
        OffsetDateTime createdAt
) {}
