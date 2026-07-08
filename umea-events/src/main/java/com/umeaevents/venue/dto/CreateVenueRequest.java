package com.umeaevents.venue.dto;

import com.umeaevents.venue.VenueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVenueRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        @NotNull VenueType type,
        @Size(max = 300) String address,
        String imageUrl
) {}
