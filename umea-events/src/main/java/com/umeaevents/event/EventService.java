package com.umeaevents.event;

import com.umeaevents.category.Category;
import com.umeaevents.category.CategoryRepository;
import com.umeaevents.common.exception.ResourceNotFoundException;
import com.umeaevents.event.dto.CreateEventRequest;
import com.umeaevents.event.dto.CreatePublishedEventRequest;
import com.umeaevents.event.dto.CreateRecurringEventRequest;
import com.umeaevents.event.dto.EventOccurrenceResponse;
import com.umeaevents.event.dto.EventResponse;
import com.umeaevents.event.dto.UpdateEventRequest;
import com.umeaevents.user.User;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.Venue;
import com.umeaevents.venue.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventOccurrenceRepository occurrenceRepository;
    private final RecurrenceRuleRepository recurrenceRuleRepository;
    private final VenueRepository venueRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final OccurrenceMaterializerJob materializerJob;

    @Transactional(readOnly = true)
    public Page<EventOccurrenceResponse> search(
            String q, UUID categoryId, UUID venueId,
            OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        // Strip sort from pageable — the native query has its own ORDER BY starts_at ASC
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return occurrenceRepository.search(
                q,
                categoryId != null ? categoryId.toString() : null,
                venueId    != null ? venueId.toString()    : null,
                from       != null ? from.toString()       : null,
                to         != null ? to.toString()         : null,
                unsorted
        ).map(eventMapper::toOccurrenceResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> listMine(String ownerEmail, EventStatus status, Pageable pageable) {
        User owner = findUserOrThrow(ownerEmail);
        Page<Event> events = (status != null)
                ? eventRepository.findByOwnerIdAndStatus(owner.getId(), status, pageable)
                : eventRepository.findByOwnerId(owner.getId(), pageable);
        return events.map(eventMapper::toEventResponse);
    }

    @Transactional(readOnly = true)
    public EventOccurrenceResponse getOccurrenceById(UUID occurrenceId) {
        EventOccurrence occurrence = occurrenceRepository.findById(occurrenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Occurrence hittades inte: " + occurrenceId));
        if (occurrence.getEvent().getStatus() != EventStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Occurrence hittades inte: " + occurrenceId);
        }
        return eventMapper.toOccurrenceResponse(occurrence);
    }

    @Transactional
    public EventResponse create(CreateEventRequest request, String ownerEmail) {
        User owner = findUserOrThrow(ownerEmail);
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue hittades inte: " + request.venueId()));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori hittades inte: " + request.categoryId()));

        Event event = Event.builder()
                .title(request.title())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .venue(venue)
                .category(category)
                .owner(owner)
                .build();
        event = eventRepository.save(event);

        EventOccurrence occurrence = EventOccurrence.builder()
                .event(event)
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .build();
        occurrenceRepository.save(occurrence);

        return eventMapper.toEventResponse(event);
    }

    @Transactional
    public EventResponse createRecurring(CreateRecurringEventRequest request, String ownerEmail) {
        User owner = findUserOrThrow(ownerEmail);
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue hittades inte: " + request.venueId()));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori hittades inte: " + request.categoryId()));

        // Validate timezone and RRULE syntax early to fail fast
        ZoneId.of(request.timezone());

        Event event = Event.builder()
                .title(request.title())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .venue(venue)
                .category(category)
                .owner(owner)
                .build();
        event = eventRepository.save(event);

        recurrenceRuleRepository.save(
                RecurrenceRule.builder()
                        .event(event)
                        .rrule(request.rrule())
                        .startTime(request.startTime())
                        .durationMinutes(request.durationMinutes())
                        .timezone(request.timezone())
                        .build()
        );

        return eventMapper.toEventResponse(event);
    }

    @Transactional
    public EventResponse submit(UUID eventId, String callerEmail) {
        Event event = findEventOrThrow(eventId);
        checkOwner(event, callerEmail);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new IllegalArgumentException("Endast DRAFT-event kan skickas in för granskning");
        }
        event.setStatus(EventStatus.PENDING_REVIEW);
        return eventMapper.toEventResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse publish(UUID eventId) {
        Event event = findEventOrThrow(eventId);
        if (event.getStatus() != EventStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("Endast PENDING_REVIEW-event kan publiceras");
        }
        event.setStatus(EventStatus.PUBLISHED);
        return eventMapper.toEventResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse cancel(UUID eventId, String callerEmail) {
        Event event = findEventOrThrow(eventId);
        checkOwnerOrAdmin(event, callerEmail);
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.ARCHIVED) {
            throw new IllegalArgumentException("Event är redan avbokat eller arkiverat");
        }
        event.setStatus(EventStatus.CANCELLED);
        return eventMapper.toEventResponse(eventRepository.save(event));
    }

    /**
     * Edit an event. Shared fields (title/description/image/venue/category) update the Event, so
     * they apply to every occurrence. Schedule changes are optional and mode-specific: a single
     * event updates its one occurrence; a recurring event updates its rule and regenerates the
     * whole series.
     */
    @Transactional
    public EventResponse update(UUID eventId, UpdateEventRequest request, String callerEmail) {
        Event event = findEventOrThrow(eventId);
        checkOwnerOrAdmin(event, callerEmail);

        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue hittades inte: " + request.venueId()));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori hittades inte: " + request.categoryId()));

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setImageUrl(request.imageUrl());
        event.setVenue(venue);
        event.setCategory(category);
        eventRepository.save(event);

        recurrenceRuleRepository.findByEvent(event).ifPresentOrElse(
                rule -> {
                    if (request.recurrence() != null) {
                        applyRecurrenceUpdate(event, rule, request.recurrence());
                    }
                },
                () -> {
                    if (request.startsAt() != null) {
                        EventOccurrence occ = occurrenceRepository.findFirstByEventOrderByStartsAtAsc(event)
                                .orElseGet(() -> EventOccurrence.builder().event(event).build());
                        occ.setStartsAt(request.startsAt());
                        occ.setEndsAt(request.endsAt());
                        occurrenceRepository.save(occ);
                    }
                });

        return eventMapper.toEventResponse(event);
    }

    private void applyRecurrenceUpdate(Event event, RecurrenceRule rule, UpdateEventRequest.Recurrence rec) {
        validateRecurrence(rec.rrule(), rec.startTime(), rec.timezone());
        rule.setRrule(rec.rrule());
        rule.setStartTime(rec.startTime());
        rule.setDurationMinutes(rec.durationMinutes());
        rule.setTimezone(rec.timezone());
        rule.setHorizon(null); // regenerate from today
        recurrenceRuleRepository.save(rule);
        occurrenceRepository.deleteByEvent(event); // clear occurrences generated with the old schedule
        materializerJob.materializeRule(rule);
    }

    private void validateRecurrence(String rrule, java.time.LocalTime startTime, String timezone) {
        if (rrule == null || rrule.isBlank() || startTime == null || timezone == null || timezone.isBlank()) {
            throw new IllegalArgumentException("Återkommande kräver rrule, startTime och timezone");
        }
        try {
            ZoneId.of(timezone);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Ogiltig tidszon: " + timezone);
        }
    }

    /** Admin: create an event straight to PUBLISHED — single occurrence or a materialised series. */
    @Transactional
    public EventResponse createPublished(CreatePublishedEventRequest request, String adminEmail) {
        User owner = findUserOrThrow(adminEmail);
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue hittades inte: " + request.venueId()));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori hittades inte: " + request.categoryId()));

        Event event = Event.builder()
                .title(request.title())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .venue(venue)
                .category(category)
                .owner(owner)
                .status(EventStatus.PUBLISHED)
                .build();
        eventRepository.save(event);

        if (request.recurrence() != null) {
            var rec = request.recurrence();
            validateRecurrence(rec.rrule(), rec.startTime(), rec.timezone());
            RecurrenceRule rule = recurrenceRuleRepository.save(RecurrenceRule.builder()
                    .event(event).rrule(rec.rrule()).startTime(rec.startTime())
                    .durationMinutes(rec.durationMinutes()).timezone(rec.timezone()).build());
            materializerJob.materializeRule(rule);
        } else {
            if (request.startsAt() == null) {
                throw new IllegalArgumentException("startsAt krävs för ett engångsevent (eller ange recurrence)");
            }
            occurrenceRepository.save(EventOccurrence.builder()
                    .event(event).startsAt(request.startsAt()).endsAt(request.endsAt()).build());
        }
        return eventMapper.toEventResponse(event);
    }

    /** Permanently delete an event and its occurrences/rule/overrides (DB cascades). */
    @Transactional
    public void delete(UUID eventId, String callerEmail) {
        Event event = findEventOrThrow(eventId);
        checkOwnerOrAdmin(event, callerEmail);
        eventRepository.delete(event);
    }

    private Event findEventOrThrow(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event hittades inte: " + id));
    }

    private User findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Användare hittades inte: " + email));
    }

    private void checkOwner(Event event, String callerEmail) {
        if (!event.getOwner().getEmail().equals(callerEmail)) {
            throw new AccessDeniedException("Ingen behörighet att ändra detta event");
        }
    }

    private void checkOwnerOrAdmin(Event event, String callerEmail) {
        boolean isOwner = event.getOwner().getEmail().equals(callerEmail);
        boolean isAdmin = userRepository.findByEmail(callerEmail)
                .map(u -> u.getRole().name().equals("ADMIN"))
                .orElse(false);
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Ingen behörighet att ändra detta event");
        }
    }
}
