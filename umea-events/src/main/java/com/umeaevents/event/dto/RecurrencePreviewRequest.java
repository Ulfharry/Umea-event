package com.umeaevents.event.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Ask the server which dates a rule would generate, so the authoring calendar can show them
 * without a second RRULE implementation in the browser drifting away from the expander.
 *
 * @param from/to optional window; defaults to the series start (or today) plus a few months.
 */
public record RecurrencePreviewRequest(
        @NotBlank String rrule,
        LocalDate startsOn,
        LocalDate from,
        LocalDate to
) {}
