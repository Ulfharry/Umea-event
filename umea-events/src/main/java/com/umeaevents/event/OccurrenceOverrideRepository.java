package com.umeaevents.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OccurrenceOverrideRepository extends JpaRepository<OccurrenceOverride, UUID> {

    Optional<OccurrenceOverride> findByEventAndOriginalDate(Event event, LocalDate originalDate);

    /** Dates the author unticked in the calendar — used to pre-fill the editor. */
    List<OccurrenceOverride> findByEventAndStatus(Event event, OverrideStatus status);

    void deleteByEventAndStatus(Event event, OverrideStatus status);
}
