package com.umeaevents.event;

import com.umeaevents.event.dto.EventOccurrenceResponse;
import com.umeaevents.event.dto.EventResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class EventMapper {

    private static OffsetDateTime toOffset(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    /**
     * Event image, falling back to the venue's default image (e.g. its logo), then to the
     * category's stock image — so an event is never imageless and venue events look on-brand.
     */
    private static String effectiveImage(Event event) {
        if (event.getImageUrl() != null) return event.getImageUrl();
        if (event.getVenue() != null && event.getVenue().getImageUrl() != null) {
            return event.getVenue().getImageUrl();
        }
        return event.getCategory() != null ? event.getCategory().getImageUrl() : null;
    }

    public EventResponse toEventResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                effectiveImage(event),
                event.getVenue().getId(),
                event.getVenue().getName(),
                event.getCategory().getId(),
                event.getCategory().getName(),
                event.getStatus(),
                event.getOwner().getId(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    public EventOccurrenceResponse toOccurrenceResponse(EventOccurrence occurrence) {
        Event event = occurrence.getEvent();
        return new EventOccurrenceResponse(
                occurrence.getId(),
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                effectiveImage(event),
                event.getVenue().getId(),
                event.getVenue().getName(),
                event.getCategory().getId(),
                event.getCategory().getName(),
                event.getStatus(),
                occurrence.getStartsAt(),
                occurrence.getEndsAt(),
                occurrence.getCreatedAt()
        );
    }

    public EventOccurrenceResponse toOccurrenceResponse(EventOccurrenceRow row) {
        return new EventOccurrenceResponse(
                row.getId(),
                row.getEvent_id(),
                row.getTitle(),
                row.getDescription(),
                row.getImage_url(),
                row.getVenue_id(),
                row.getVenue_name(),
                row.getCategory_id(),
                row.getCategory_name(),
                EventStatus.valueOf(row.getStatus()),
                toOffset(row.getStarts_at()),
                toOffset(row.getEnds_at()),
                toOffset(row.getCreated_at())
        );
    }
}
