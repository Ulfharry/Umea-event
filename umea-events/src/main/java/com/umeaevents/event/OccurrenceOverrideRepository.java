package com.umeaevents.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface OccurrenceOverrideRepository extends JpaRepository<OccurrenceOverride, UUID> {

    Optional<OccurrenceOverride> findByEventAndOriginalDate(Event event, LocalDate originalDate);
}
