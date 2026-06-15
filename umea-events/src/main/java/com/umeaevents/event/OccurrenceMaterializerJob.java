package com.umeaevents.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OccurrenceMaterializerJob {

    static final int HORIZON_DAYS = 60;

    private final RecurrenceRuleRepository ruleRepository;
    private final EventOccurrenceRepository occurrenceRepository;
    private final OccurrenceOverrideRepository overrideRepository;
    private final RecurrenceExpander expander;

    @Scheduled(cron = "${app.materializer.cron:0 0 * * * *}")
    @Transactional
    public void materialize() {
        List<RecurrenceRule> rules = ruleRepository.findByEventStatus(EventStatus.PUBLISHED);
        if (rules.isEmpty()) return;

        log.info("Materializing occurrences for {} recurring event(s)", rules.size());
        rules.forEach(this::materializeRule);
    }

    void materializeRule(RecurrenceRule rule) {
        ZoneId zone = ZoneId.of(rule.getTimezone());
        LocalDate today = LocalDate.now(zone);
        LocalDate horizonDate = today.plusDays(HORIZON_DAYS);

        // Start expansion from the day after the last run, or from today if first run
        LocalDate from = rule.getHorizon() != null
                ? rule.getHorizon().atZoneSameInstant(zone).toLocalDate().plusDays(1)
                : today;

        if (!from.isBefore(horizonDate)) return; // already up to date

        List<LocalDate> dates = expander.expand(rule.getRrule(), from, horizonDate);
        int created = 0;

        for (LocalDate date : dates) {
            if (occurrenceRepository.existsByEventAndRecurrenceDate(rule.getEvent(), date)) {
                continue; // idempotent — already materialized
            }

            var override = overrideRepository.findByEventAndOriginalDate(rule.getEvent(), date);
            if (override.isPresent() && override.get().getStatus() == OverrideStatus.CANCELLED) {
                continue; // cancelled by owner or admin
            }

            OffsetDateTime startsAt;
            OffsetDateTime endsAt = null;

            if (override.isPresent() && override.get().getStatus() == OverrideStatus.MODIFIED) {
                startsAt = override.get().getNewStartsAt();
                endsAt   = override.get().getNewEndsAt();
            } else {
                startsAt = expander.toUtc(date, rule.getStartTime(), zone);
                if (rule.getDurationMinutes() != null) {
                    endsAt = startsAt.plusMinutes(rule.getDurationMinutes());
                }
            }

            occurrenceRepository.save(
                    EventOccurrence.builder()
                            .event(rule.getEvent())
                            .startsAt(startsAt)
                            .endsAt(endsAt)
                            .recurrenceDate(date)
                            .build()
            );
            created++;
        }

        rule.setHorizon(horizonDate.atStartOfDay(zone).toOffsetDateTime());
        ruleRepository.save(rule);

        if (created > 0) {
            log.info("Event {}: created {} occurrence(s) through {}", rule.getEvent().getId(), created, horizonDate);
        }
    }
}
