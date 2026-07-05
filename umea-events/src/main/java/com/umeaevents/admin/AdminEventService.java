package com.umeaevents.admin;

import com.umeaevents.event.Event;
import com.umeaevents.event.EventRepository;
import com.umeaevents.event.EventStatus;
import com.umeaevents.event.RecurrenceRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminEventService {

    private final EventRepository eventRepository;
    private final RecurrenceRuleRepository recurrenceRuleRepository;

    @Transactional(readOnly = true)
    public Page<AdminEventResponse> listEvents(EventStatus status, UUID venueId, UUID categoryId, Pageable pageable) {
        Page<?> page;

        if (venueId != null && categoryId != null && status != null) {
            page = eventRepository.findByVenueIdAndCategoryIdAndStatus(venueId, categoryId, status, pageable);
        } else if (venueId != null && categoryId != null) {
            page = eventRepository.findByVenueIdAndCategoryId(venueId, categoryId, pageable);
        } else if (venueId != null && status != null) {
            page = eventRepository.findByVenueIdAndStatus(venueId, status, pageable);
        } else if (categoryId != null && status != null) {
            page = eventRepository.findByCategoryIdAndStatus(categoryId, status, pageable);
        } else if (venueId != null) {
            page = eventRepository.findByVenueId(venueId, pageable);
        } else if (categoryId != null) {
            page = eventRepository.findByCategoryId(categoryId, pageable);
        } else if (status != null) {
            page = eventRepository.findByStatus(status, pageable);
        } else {
            page = eventRepository.findAll(pageable);
        }

        @SuppressWarnings("unchecked")
        Page<Event> events = (Page<Event>) page;
        Set<UUID> recurringIds = recurrenceRuleRepository.findRecurringEventIds(
                events.getContent().stream().map(Event::getId).toList());
        return events.map(e -> AdminEventResponse.from(e, recurringIds.contains(e.getId())));
    }
}
