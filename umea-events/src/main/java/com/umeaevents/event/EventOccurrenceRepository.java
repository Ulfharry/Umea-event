package com.umeaevents.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface EventOccurrenceRepository extends JpaRepository<EventOccurrence, UUID> {

    @Query("SELECT o FROM EventOccurrence o JOIN FETCH o.event e WHERE e.status = :status")
    Page<EventOccurrence> findAllByEventStatus(EventStatus status, Pageable pageable);
}
