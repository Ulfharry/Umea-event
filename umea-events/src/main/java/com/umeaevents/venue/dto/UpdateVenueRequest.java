package com.umeaevents.venue.dto;

import com.umeaevents.venue.VenueType;
import jakarta.validation.constraints.Size;

public record UpdateVenueRequest(
        @Size(max = 200) String name,
        String description,
        VenueType type,
        @Size(max = 300) String address,
        String imageUrl
) {}
