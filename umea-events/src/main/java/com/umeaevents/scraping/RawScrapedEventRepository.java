package com.umeaevents.scraping;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RawScrapedEventRepository extends JpaRepository<RawScrapedEvent, UUID> {

    Page<RawScrapedEvent> findByStatus(ScrapedEventStatus status, Pageable pageable);

    Page<RawScrapedEvent> findAll(Pageable pageable);

    Optional<RawScrapedEvent> findBySourceAndExternalId(ScrapedEventSource source, String externalId);
}
