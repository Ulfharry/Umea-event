package com.umeaevents.scraping;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RawScrapedEventRepository extends JpaRepository<RawScrapedEvent, UUID> {

    Page<RawScrapedEvent> findByStatus(ScrapedEventStatus status, Pageable pageable);

    Page<RawScrapedEvent> findAll(Pageable pageable);

    Optional<RawScrapedEvent> findBySourceAndExternalId(ScrapedEventSource source, String externalId);

    long countByStatus(ScrapedEventStatus status);

    /**
     * Of the given externalIds, return those already stored for this source — regardless of
     * status (pending, rejected or promoted). Used to skip re-staging on re-scrapes.
     */
    @Query("select r.externalId from RawScrapedEvent r "
            + "where r.source = :source and r.externalId in :externalIds")
    Set<String> findExistingExternalIds(@Param("source") ScrapedEventSource source,
                                        @Param("externalIds") Collection<String> externalIds);
}
