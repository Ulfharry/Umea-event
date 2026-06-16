package com.umeaevents.event;

import java.time.OffsetDateTime;
import java.util.UUID;

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
    OffsetDateTime getStarts_at();
    OffsetDateTime getEnds_at();
    OffsetDateTime getCreated_at();
}
