package com.umeaevents.admin;

import com.umeaevents.event.EventRepository;
import com.umeaevents.event.EventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminEventService {

    private final EventRepository eventRepository;

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

        return ((Page<com.umeaevents.event.Event>) page).map(AdminEventResponse::from);
    }
}
