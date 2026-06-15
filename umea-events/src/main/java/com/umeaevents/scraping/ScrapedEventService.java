package com.umeaevents.scraping;

import com.umeaevents.category.CategoryRepository;
import com.umeaevents.common.exception.ResourceNotFoundException;
import com.umeaevents.event.Event;
import com.umeaevents.event.EventOccurrence;
import com.umeaevents.event.EventOccurrenceRepository;
import com.umeaevents.event.EventRepository;
import com.umeaevents.event.EventStatus;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScrapedEventService {

    private final RawScrapedEventRepository scrapedRepo;
    private final EventRepository eventRepository;
    private final EventOccurrenceRepository occurrenceRepository;
    private final VenueRepository venueRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public ScrapedEventResponse importManual(ImportScrapedEventRequest request) {
        var raw = RawScrapedEvent.builder()
                .source(ScrapedEventSource.MANUAL_IMPORT)
                .rawTitle(request.rawTitle())
                .rawDescription(request.rawDescription())
                .rawVenueName(request.rawVenueName())
                .rawStartsAt(request.rawStartsAt())
                .rawEndsAt(request.rawEndsAt())
                .parsedStartsAt(parseTimestamp(request.parsedStartsAt()))
                .parsedEndsAt(parseTimestamp(request.parsedEndsAt()))
                .status(ScrapedEventStatus.PENDING_REVIEW)
                .build();
        return ScrapedEventResponse.from(scrapedRepo.save(raw));
    }

    @Transactional(readOnly = true)
    public Page<ScrapedEventResponse> list(ScrapedEventStatus status, Pageable pageable) {
        Page<RawScrapedEvent> page = (status != null)
                ? scrapedRepo.findByStatus(status, pageable)
                : scrapedRepo.findAll(pageable);
        return page.map(ScrapedEventResponse::from);
    }

    @Transactional(readOnly = true)
    public ScrapedEventResponse getById(UUID id) {
        return ScrapedEventResponse.from(findOrThrow(id));
    }

    @Transactional
    public ScrapedEventResponse reject(UUID id, RejectScrapedEventRequest request, String adminEmail) {
        var raw = findOrThrow(id);
        if (raw.getStatus() != ScrapedEventStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only PENDING_REVIEW events can be rejected");
        }
        var admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        raw.setStatus(ScrapedEventStatus.REJECTED);
        raw.setAdminNotes(request.adminNotes());
        raw.setReviewedAt(OffsetDateTime.now());
        raw.setReviewedBy(admin);
        return ScrapedEventResponse.from(scrapedRepo.save(raw));
    }

    /**
     * Promote a raw scraped event to a real published Event + EventOccurrence.
     *
     * CRITICAL: this is an EXPLICIT admin action — promoted events go directly to PUBLISHED
     * because the admin is making a deliberate choice. Scraped events are NEVER auto-published.
     */
    @Transactional
    public ScrapedEventResponse promote(UUID id, PromoteScrapedEventRequest request, String adminEmail) {
        var raw = findOrThrow(id);
        if (raw.getStatus() != ScrapedEventStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only PENDING_REVIEW events can be promoted");
        }
        var admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        var category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        var event = Event.builder()
                .title(raw.getRawTitle())
                .description(raw.getRawDescription())
                .venue(venue)
                .category(category)
                .owner(admin)
                .status(EventStatus.PUBLISHED)
                .build();
        eventRepository.save(event);

        var occurrence = EventOccurrence.builder()
                .event(event)
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .build();
        occurrenceRepository.save(occurrence);

        raw.setStatus(ScrapedEventStatus.PROMOTED);
        raw.setAdminNotes(request.adminNotes());
        raw.setReviewedAt(OffsetDateTime.now());
        raw.setReviewedBy(admin);
        raw.setPromotedEvent(event);
        return ScrapedEventResponse.from(scrapedRepo.save(raw));
    }

    private RawScrapedEvent findOrThrow(UUID id) {
        return scrapedRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scraped event not found"));
    }

    private OffsetDateTime parseTimestamp(String s) {
        if (s == null || s.isBlank()) return null;
        return OffsetDateTime.parse(s);
    }
}
