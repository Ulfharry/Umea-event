package com.umeaevents.venue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface VenueOpeningHoursRepository extends JpaRepository<VenueOpeningHours, UUID> {

    List<VenueOpeningHours> findByVenueId(UUID venueId);

    void deleteByVenueId(UUID venueId);

    /** All opening hours for active venues, venue eagerly loaded (for the open-now computation). */
    @Query("SELECT h FROM VenueOpeningHours h JOIN FETCH h.venue v WHERE v.active = true")
    List<VenueOpeningHours> findAllForActiveVenues();
}
