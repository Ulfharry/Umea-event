package com.umeaevents.scraping;

/** Thrown when fetching or parsing a remote URL fails. Maps to HTTP 502. */
public class ScrapingException extends RuntimeException {
    public ScrapingException(String message, Throwable cause) {
        super(message, cause);
    }
}
