package com.umeaevents.venue;

import com.umeaevents.venue.dto.VenueResponse;
import org.springframework.stereotype.Component;

@Component
public class VenueMapper {

    public VenueResponse toResponse(Venue venue) {
        return new VenueResponse(
                venue.getId(),
                venue.getName(),
                venue.getDescription(),
                venue.getType(),
                venue.getAddress(),
                venue.getOwner().getId(),
                venue.isActive(),
                venue.getCreatedAt()
        );
    }
}
