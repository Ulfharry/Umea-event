package com.umeaevents.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Native-query projection. timestamptz columns come back from the JDBC driver as
 * {@link Instant} — declaring them as OffsetDateTime here makes Spring's projection fail with
 * "Cannot project java.time.Instant to java.time.OffsetDateTime" once rows are returned.
 * The mapper converts Instant → OffsetDateTime (UTC).
 */
public interface EventOccurrenceRow {
    UUID getId();
    UUID getEvent_id();
    String getTitle();
    String getDescription();
    String getImage_url();
    UUID getVenue_id();
    String getVenue_name();
    UUID getCategory_id();
    String getCategory_name();
    String getStatus();
    Instant getStarts_at();
    Instant getEnds_at();
    Instant getCreated_at();
}
