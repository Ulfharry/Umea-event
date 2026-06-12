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
}
