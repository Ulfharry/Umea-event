package com.umeaevents.common.exception;

/**
 * Kastas när en efterfrågad resurs inte finns. Fångas av
 * GlobalExceptionHandler och översätts till HTTP 404. Används från och med
 * Milestone 3-4; definieras redan nu så felhanteringen är komplett.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
