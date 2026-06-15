package com.umeaevents.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    Page<Event> findByVenueIdAndStatus(UUID venueId, EventStatus status, Pageable pageable);

    Page<Event> findByVenueId(UUID venueId, Pageable pageable);

    Page<Event> findByCategoryIdAndStatus(UUID categoryId, EventStatus status, Pageable pageable);

    Page<Event> findByCategoryId(UUID categoryId, Pageable pageable);

    Page<Event> findByVenueIdAndCategoryIdAndStatus(UUID venueId, UUID categoryId, EventStatus status, Pageable pageable);

    Page<Event> findByVenueIdAndCategoryId(UUID venueId, UUID categoryId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.status = :status")
    long countByStatus(@Param("status") EventStatus status);
}
