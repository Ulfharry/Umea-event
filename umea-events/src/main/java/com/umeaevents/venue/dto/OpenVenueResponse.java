package com.umeaevents.venue.dto;

import com.umeaevents.venue.VenueType;

import java.time.LocalTime;
import java.util.UUID;

/**
 * A venue that is open now or opens later today, for the "nothing on — but these are open" view.
 * {@code openNow} distinguishes currently-open from opens-later; {@code opensAt}/{@code closesAt}
 * describe the relevant interval today.
 */
public record OpenVenueResponse(
        UUID venueId,
        String name,
        VenueType type,
        String address,
        String imageUrl,
        LocalTime opensAt,
        LocalTime closesAt,
        boolean openNow
) {}
