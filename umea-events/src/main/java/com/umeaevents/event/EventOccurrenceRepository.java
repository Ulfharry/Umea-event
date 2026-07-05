package com.umeaevents.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface EventOccurrenceRepository extends JpaRepository<EventOccurrence, UUID> {

    @Query(
        value = """
            SELECT
                o.id,
                e.id            AS event_id,
                e.title,
                e.description,
                e.image_url,
                v.id            AS venue_id,
                v.name          AS venue_name,
                c.id            AS category_id,
                c.name          AS category_name,
                e.status,
                o.starts_at,
                o.ends_at,
                o.created_at
            FROM event_occurrence o
            JOIN event    e ON e.id = o.event_id
            JOIN venue    v ON v.id = e.venue_id
            JOIN category c ON c.id = e.category_id
            WHERE e.status = 'PUBLISHED'
              AND (CAST(:q         AS TEXT)        IS NULL OR e.search_vector @@ plainto_tsquery('swedish', :q))
              AND (CAST(:categoryId AS UUID)        IS NULL OR e.category_id  = CAST(:categoryId AS UUID))
              AND (CAST(:venueId    AS UUID)        IS NULL OR e.venue_id     = CAST(:venueId    AS UUID))
              AND (CAST(:from       AS TIMESTAMPTZ) IS NULL OR o.starts_at   >= CAST(:from       AS TIMESTAMPTZ))
              AND (CAST(:to         AS TIMESTAMPTZ) IS NULL OR o.starts_at   <= CAST(:to         AS TIMESTAMPTZ))
            ORDER BY o.starts_at ASC
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM event_occurrence o
            JOIN event e ON e.id = o.event_id
            WHERE e.status = 'PUBLISHED'
              AND (CAST(:q         AS TEXT)        IS NULL OR e.search_vector @@ plainto_tsquery('swedish', :q))
              AND (CAST(:categoryId AS UUID)        IS NULL OR e.category_id  = CAST(:categoryId AS UUID))
              AND (CAST(:venueId    AS UUID)        IS NULL OR e.venue_id     = CAST(:venueId    AS UUID))
              AND (CAST(:from       AS TIMESTAMPTZ) IS NULL OR o.starts_at   >= CAST(:from       AS TIMESTAMPTZ))
              AND (CAST(:to         AS TIMESTAMPTZ) IS NULL OR o.starts_at   <= CAST(:to         AS TIMESTAMPTZ))
            """,
        nativeQuery = true
    )
    Page<EventOccurrenceRow> search(
            @Param("q")          String q,
            @Param("categoryId") String categoryId,
            @Param("venueId")    String venueId,
            @Param("from")       String from,
            @Param("to")         String to,
            Pageable pageable
    );

    boolean existsByEventAndRecurrenceDate(Event event, LocalDate recurrenceDate);

    java.util.Optional<EventOccurrence> findFirstByEventOrderByStartsAtAsc(Event event);

    void deleteByEvent(Event event);

    @Query("SELECT COUNT(o) FROM EventOccurrence o WHERE o.startsAt >= :from AND o.startsAt <= :to")
    long countByStartsAtBetween(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
