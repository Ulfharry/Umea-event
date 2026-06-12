package com.umeaevents.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * Enhetligt felformat för hela API:t. Alla fel ut ur appen ska se likadana ut
 * så att frontend kan hantera dem generiskt.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    /** Ett enskilt valideringsfel på ett fält. */
    public record FieldError(String field, String message) {
    }
}
