package com.umeaevents.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RecurrenceRuleRepository extends JpaRepository<RecurrenceRule, UUID> {

    @Query("SELECT r FROM RecurrenceRule r JOIN FETCH r.event e WHERE e.status = :status")
    List<RecurrenceRule> findByEventStatus(@Param("status") EventStatus status);

    Optional<RecurrenceRule> findByEvent(Event event);

    /** Of the given event ids, which ones have a recurrence rule (i.e. are recurring). */
    @Query("SELECT r.event.id FROM RecurrenceRule r WHERE r.event.id IN :eventIds")
    Set<UUID> findRecurringEventIds(@Param("eventIds") Collection<UUID> eventIds);
}
