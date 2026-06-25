package com.umeaevents.venue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {
    Page<Venue> findAllByActiveTrue(Pageable pageable);

    Page<Venue> findByOwnerIdAndActiveTrue(UUID ownerId, Pageable pageable);
}
