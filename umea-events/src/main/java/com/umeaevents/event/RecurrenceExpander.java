package com.umeaevents.event;

import org.springframework.stereotype.Component;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands RFC 5545 RRULE strings to local dates within a given window.
 *
 * Supported subset:
 *   FREQ=DAILY | WEEKLY | MONTHLY
 *   BYDAY=MO,TU,WE,TH,FR,SA,SU (for WEEKLY)
 *   BYDAY=1TH | -1FR | 2MO etc. (ordinal weekday, for MONTHLY)
 *   COUNT=n
 *   UNTIL=YYYYMMDD | YYYYMMDDTHHMMSSz
 *
 * DST-correctness: convert local date + wall-clock time via ZonedDateTime so that
 * 19:00 Europe/Stockholm is always 19:00 regardless of summer/winter time.
 */
@Component
public class RecurrenceExpander {

    private static final Map<String, DayOfWeek> DAY_MAP = Map.of(
            "MO", DayOfWeek.MONDAY,    "TU", DayOfWeek.TUESDAY,
            "WE", DayOfWeek.WEDNESDAY, "TH", DayOfWeek.THURSDAY,
            "FR", DayOfWeek.FRIDAY,    "SA", DayOfWeek.SATURDAY,
            "SU", DayOfWeek.SUNDAY
    );

    private static final Pattern ORDINAL_BYDAY = Pattern.compile("^(-?\\d+)([A-Z]{2})$");

    /**
     * Returns all dates in [from, to] (inclusive) that match the RRULE.
     */
    public List<LocalDate> expand(String rruleString, LocalDate from, LocalDate to) {
        Map<String, String> parts = parse(rruleString);
        String freq = parts.getOrDefault("FREQ", "");

        int count = parts.containsKey("COUNT")
                ? Integer.parseInt(parts.get("COUNT"))
                : Integer.MAX_VALUE;

        LocalDate until = parts.containsKey("UNTIL")
                ? parseUntil(parts.get("UNTIL"))
                : LocalDate.MAX;

        LocalDate effectiveTo = until.isBefore(to) ? until : to;

        return switch (freq) {
            case "DAILY"   -> daily(from, effectiveTo, count);
            case "WEEKLY"  -> weekly(parts.get("BYDAY"), from, effectiveTo, count);
            case "MONTHLY" -> monthly(parts.get("BYDAY"), from, effectiveTo, count);
            default -> throw new IllegalArgumentException("Unsupported RRULE FREQ: " + freq);
        };
    }

    /**
     * Converts a date + local wall-clock time to an OffsetDateTime in the given zone.
     * ZonedDateTime.of() handles DST gaps (spring-forward) by advancing the time,
     * and for overlaps (fall-back) it picks the earlier (pre-transition) offset.
     */
    public OffsetDateTime toUtc(LocalDate date, LocalTime time, ZoneId zone) {
        return ZonedDateTime.of(date, time, zone).toOffsetDateTime();
    }

    // --- private helpers ---

    private Map<String, String> parse(String rrule) {
        String rule = rrule.startsWith("RRULE:") ? rrule.substring(6) : rrule;
        Map<String, String> map = new LinkedHashMap<>();
        for (String part : rule.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) map.put(kv[0].toUpperCase(), kv[1].toUpperCase());
        }
        return map;
    }

    private LocalDate parseUntil(String until) {
        String d = until.contains("T") ? until.substring(0, 8) : until.replace("-", "");
        return LocalDate.of(
                Integer.parseInt(d.substring(0, 4)),
                Integer.parseInt(d.substring(4, 6)),
                Integer.parseInt(d.substring(6, 8)));
    }

    private List<LocalDate> daily(LocalDate from, LocalDate to, int count) {
        List<LocalDate> result = new ArrayList<>();
        LocalDate cur = from;
        while (!cur.isAfter(to) && result.size() < count) {
            result.add(cur);
            cur = cur.plusDays(1);
        }
        return result;
    }

    private List<LocalDate> weekly(String byday, LocalDate from, LocalDate to, int count) {
        Set<DayOfWeek> days = parseWeeklyByday(byday);
        List<LocalDate> result = new ArrayList<>();
        LocalDate cur = from;
        while (!cur.isAfter(to) && result.size() < count) {
            if (days.contains(cur.getDayOfWeek())) result.add(cur);
            cur = cur.plusDays(1);
        }
        return result;
    }

    private List<LocalDate> monthly(String byday, LocalDate from, LocalDate to, int count) {
        if (byday == null) {
            // Same day-of-month each month — start from `from`
            List<LocalDate> result = new ArrayList<>();
            LocalDate cur = from;
            while (!cur.isAfter(to) && result.size() < count) {
                result.add(cur);
                cur = cur.plusMonths(1);
            }
            return result;
        }

        Matcher m = ORDINAL_BYDAY.matcher(byday.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "MONTHLY BYDAY must include ordinal prefix, e.g. 1TH or -1FR. Got: " + byday);
        }
        int n = Integer.parseInt(m.group(1));
        DayOfWeek dow = DAY_MAP.get(m.group(2));
        if (dow == null) throw new IllegalArgumentException("Unknown weekday: " + m.group(2));

        List<LocalDate> result = new ArrayList<>();
        YearMonth ym = YearMonth.from(from);
        YearMonth toYm = YearMonth.from(to);

        while (!ym.isAfter(toYm) && result.size() < count) {
            LocalDate candidate = nthWeekdayOfMonth(ym, n, dow);
            if (candidate != null && !candidate.isBefore(from) && !candidate.isAfter(to)) {
                result.add(candidate);
            }
            ym = ym.plusMonths(1);
        }
        return result;
    }

    private LocalDate nthWeekdayOfMonth(YearMonth ym, int n, DayOfWeek dow) {
        if (n > 0) {
            LocalDate date = ym.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth(n, dow));
            return date.getMonth() == ym.getMonth() ? date : null;
        } else {
            // n < 0: -1 = last, -2 = second-to-last, etc.
            LocalDate last = ym.atEndOfMonth().with(TemporalAdjusters.lastInMonth(dow));
            LocalDate date = last.minusWeeks((-n) - 1L);
            return date.getMonth() == ym.getMonth() ? date : null;
        }
    }

    private Set<DayOfWeek> parseWeeklyByday(String byday) {
        if (byday == null || byday.isBlank()) return EnumSet.allOf(DayOfWeek.class);
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String part : byday.split(",")) {
            // Strip any numeric prefix (e.g. "1TH" → "TH") — not meaningful for WEEKLY
            String day = part.trim().replaceAll("^-?\\d+", "");
            DayOfWeek d = DAY_MAP.get(day);
            if (d != null) days.add(d);
        }
        return days;
    }
}
