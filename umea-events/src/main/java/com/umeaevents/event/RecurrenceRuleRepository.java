package com.umeaevents.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurrenceRuleRepository extends JpaRepository<RecurrenceRule, UUID> {

    @Query("SELECT r FROM RecurrenceRule r JOIN FETCH r.event e WHERE e.status = :status")
    List<RecurrenceRule> findByEventStatus(@Param("status") EventStatus status);

    Optional<RecurrenceRule> findByEvent(Event event);
}
