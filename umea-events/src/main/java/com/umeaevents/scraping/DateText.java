package com.umeaevents.scraping;

import java.util.regex.Pattern;

/**
 * Extracts the first plausible date from free text. Prefers strong, unambiguous signals —
 * ISO ({@code 2026-08-15}), then named months ({@code 13 december}) — over bare numeric
 * {@code d.m}/{@code d/m}, and rejects time-shaped tokens such as {@code 18.00} or {@code 20.30}
 * by requiring the month component to be 1-12. This stops opening-hours times being mistaken
 * for event dates.
 */
final class DateText {

    private static final Pattern ISO = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b");

    private static final Pattern NAMED = Pattern.compile(
            "\\b\\d{1,2}\\s+(jan|feb|mar|apr|maj|jun|jul|aug|sep|okt|nov|dec)\\w*",
            Pattern.CASE_INSENSITIVE);

    // day [./] month, optional [./] year; month is validated below so 18.00 / 20.30 don't match.
    private static final Pattern NUMERIC = Pattern.compile("\\b(\\d{1,2})[./](\\d{1,2})(?:[./]\\d{2,4})?\\b");

    private DateText() {}

    /** @return the first date-looking substring in {@code text}, or null if none is found. */
    static String firstDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        var iso = ISO.matcher(text);
        if (iso.find()) {
            return iso.group();
        }
        var named = NAMED.matcher(text);
        if (named.find()) {
            return named.group().strip();
        }
        var numeric = NUMERIC.matcher(text);
        while (numeric.find()) {
            int month = Integer.parseInt(numeric.group(2));
            if (month >= 1 && month <= 12) {
                return numeric.group();
            }
        }
        return null;
    }
}
