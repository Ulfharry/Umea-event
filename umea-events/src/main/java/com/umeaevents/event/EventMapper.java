package com.umeaevents.event;

import com.umeaevents.event.dto.EventOccurrenceResponse;
import com.umeaevents.event.dto.EventResponse;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public EventResponse toEventResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
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
                row.getVenue_id(),
                row.getVenue_name(),
                row.getCategory_id(),
                row.getCategory_name(),
                EventStatus.valueOf(row.getStatus()),
                row.getStarts_at(),
                row.getEnds_at(),
                row.getCreated_at()
        );
    }
}
