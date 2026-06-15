package com.umeaevents.event;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecurrenceExpanderTest {

    private final RecurrenceExpander expander = new RecurrenceExpander();
    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    // --- WEEKLY ---

    @Test
    void weekly_thursday_returns_thursdays() {
        List<LocalDate> dates = expander.expand(
                "FREQ=WEEKLY;BYDAY=TH",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30));

        assertThat(dates).containsExactly(
                LocalDate.of(2026, 6, 4),
                LocalDate.of(2026, 6, 11),
                LocalDate.of(2026, 6, 18),
                LocalDate.of(2026, 6, 25));
        dates.forEach(d -> assertThat(d.getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY));
    }

    @Test
    void weekly_count_limits_results() {
        List<LocalDate> dates = expander.expand(
                "FREQ=WEEKLY;BYDAY=TH;COUNT=3",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 12, 31));
        assertThat(dates).hasSize(3);
    }

    @Test
    void weekly_until_clips_results() {
        List<LocalDate> dates = expander.expand(
                "FREQ=WEEKLY;BYDAY=TH;UNTIL=20260618",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 12, 31));
        assertThat(dates).containsExactly(
                LocalDate.of(2026, 6, 4),
                LocalDate.of(2026, 6, 11),
                LocalDate.of(2026, 6, 18));
    }

    @Test
    void weekly_multiple_days() {
        List<LocalDate> dates = expander.expand(
                "FREQ=WEEKLY;BYDAY=MO,FR",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 7));
        assertThat(dates).containsExactly(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5));
    }

    // --- MONTHLY ---

    @Test
    void monthly_first_thursday() {
        List<LocalDate> dates = expander.expand(
                "FREQ=MONTHLY;BYDAY=1TH",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 9, 30));
        assertThat(dates).containsExactly(
                LocalDate.of(2026, 6, 4),
                LocalDate.of(2026, 7, 2),
                LocalDate.of(2026, 8, 6),
                LocalDate.of(2026, 9, 3));
    }

    @Test
    void monthly_last_friday() {
        List<LocalDate> dates = expander.expand(
                "FREQ=MONTHLY;BYDAY=-1FR",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 8, 31));
        assertThat(dates).containsExactly(
                LocalDate.of(2026, 6, 26),
                LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 8, 28));
        dates.forEach(d -> assertThat(d.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY));
    }

    // --- DAILY ---

    @Test
    void daily_returns_every_day() {
        List<LocalDate> dates = expander.expand(
                "FREQ=DAILY;COUNT=5",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 12, 31));
        assertThat(dates).hasSize(5);
        assertThat(dates.get(0)).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(dates.get(4)).isEqualTo(LocalDate.of(2026, 6, 5));
    }

    // --- DST correctness ---

    @Test
    void toUtc_summer_CEST_offset_is_plus2() {
        // Thursday 2026-07-02 is in CEST (UTC+2)
        OffsetDateTime result = expander.toUtc(
                LocalDate.of(2026, 7, 2),
                LocalTime.of(19, 0),
                STOCKHOLM);

        assertThat(result.getHour()).isEqualTo(19);
        assertThat(result.getOffset().getTotalSeconds()).isEqualTo(2 * 3600); // +02:00
    }

    @Test
    void toUtc_winter_CET_offset_is_plus1() {
        // Thursday 2026-11-05 is in CET (UTC+1) — after DST fall-back on Oct 25
        OffsetDateTime result = expander.toUtc(
                LocalDate.of(2026, 11, 5),
                LocalTime.of(19, 0),
                STOCKHOLM);

        assertThat(result.getHour()).isEqualTo(19);
        assertThat(result.getOffset().getTotalSeconds()).isEqualTo(1 * 3600); // +01:00
    }

    @Test
    void toUtc_dst_fallback_week_preserves_wallclock_time() {
        // DST fall-back in 2026: last Sunday of October = Oct 25
        // Thursday before (Oct 22) → CEST +2, 19:00 local = 17:00 UTC
        // Thursday after  (Oct 29) → CET  +1, 19:00 local = 18:00 UTC
        LocalTime eventTime = LocalTime.of(19, 0);

        OffsetDateTime beforeFallback = expander.toUtc(LocalDate.of(2026, 10, 22), eventTime, STOCKHOLM);
        OffsetDateTime afterFallback  = expander.toUtc(LocalDate.of(2026, 10, 29), eventTime, STOCKHOLM);

        // Wall-clock hour is preserved in both cases
        assertThat(beforeFallback.getHour()).isEqualTo(19);
        assertThat(afterFallback.getHour()).isEqualTo(19);

        // Oct 29 CET (+1) is 1 h later in UTC than Oct 22 CEST (+2) at the same wall-clock time
        long diffSeconds = afterFallback.toInstant().getEpochSecond()
                         - beforeFallback.toInstant().getEpochSecond();
        assertThat(diffSeconds).isEqualTo(7L * 24 * 3600 + 3600); // 7 calendar days + 1 h DST shift
    }

    @Test
    void toUtc_dst_springforward_gap_advances_time() {
        // DST spring-forward 2027: last Sunday of March = March 28
        // At 02:00 CET clocks jump to 03:00 CEST — the 02:00-02:59 hour doesn't exist.
        // An event at 02:30 should be pushed forward to 03:30 CEST (ZonedDateTime.of behaviour).
        OffsetDateTime result = expander.toUtc(
                LocalDate.of(2027, 3, 28),
                LocalTime.of(2, 30),
                STOCKHOLM);

        // Spring-forward: 02:30 doesn't exist → ZonedDateTime.of advances to 03:30 CEST
        assertThat(result.getHour()).isEqualTo(3);
        assertThat(result.getOffset().getTotalSeconds()).isEqualTo(2 * 3600); // CEST +02:00
    }

    @Test
    void unsupported_freq_throws() {
        assertThatThrownBy(() -> expander.expand(
                "FREQ=YEARLY;BYDAY=1TH",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("YEARLY");
    }
}
