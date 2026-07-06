package com.umeaevents.scraping;

import com.umeaevents.category.CategoryRepository;
import com.umeaevents.common.exception.ResourceNotFoundException;
import com.umeaevents.event.Event;
import com.umeaevents.event.EventOccurrence;
import com.umeaevents.event.EventOccurrenceRepository;
import com.umeaevents.event.EventRepository;
import com.umeaevents.event.EventStatus;
import com.umeaevents.event.OccurrenceMaterializerJob;
import com.umeaevents.event.RecurrenceRule;
import com.umeaevents.event.RecurrenceRuleRepository;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScrapedEventService {

    private final RawScrapedEventRepository scrapedRepo;
    private final EventRepository eventRepository;
    private final EventOccurrenceRepository occurrenceRepository;
    private final VenueRepository venueRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final RecurrenceRuleRepository recurrenceRuleRepository;
    private final OccurrenceMaterializerJob materializerJob;

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

        // Admin may have edited the scraped title/description in the promote form; fall back to raw.
        var title = hasText(request.title()) ? request.title().trim() : raw.getRawTitle();
        var description = hasText(request.description()) ? request.description().trim() : raw.getRawDescription();

        var event = Event.builder()
                .title(title)
                .description(description)
                .venue(venue)
                .category(category)
                .owner(admin)
                .status(EventStatus.PUBLISHED)
                .build();
        eventRepository.save(event);

        if (request.recurrence() != null) {
            promoteAsRecurring(event, request.recurrence());
        } else {
            if (request.startsAt() == null) {
                throw new IllegalArgumentException("startsAt krävs för ett engångsevent (eller ange recurrence)");
            }
            occurrenceRepository.save(EventOccurrence.builder()
                    .event(event)
                    .startsAt(request.startsAt())
                    .endsAt(request.endsAt())
                    .build());
        }

        raw.setStatus(ScrapedEventStatus.PROMOTED);
        raw.setAdminNotes(request.adminNotes());
        raw.setReviewedAt(OffsetDateTime.now());
        raw.setReviewedBy(admin);
        raw.setPromotedEvent(event);
        return ScrapedEventResponse.from(scrapedRepo.save(raw));
    }

    /**
     * Persist candidates produced by the single-page scraper. Status is always PENDING_REVIEW.
     * No externalId is set: all candidates from one listing page share the same source URL,
     * so it would be a useless (colliding) dedup key.
     * NEVER call this with auto-publish intent — that violates the no-auto-publish rule.
     */
    @Transactional
    public List<ScrapedEventResponse> saveFromScraper(List<ScrapeCandidate> candidates) {
        return persist(candidates, false);
    }

    /**
     * Persist candidates produced by the sitemap scraper. Each candidate has its own detail-page
     * URL, stored as externalId. Candidates whose URL was already staged (in any status) are
     * skipped, so a weekly re-scrape only surfaces genuinely new events. Newly staged rows are
     * always PENDING_REVIEW.
     */
    @Transactional
    public List<ScrapedEventResponse> saveFromSitemap(List<ScrapeCandidate> candidates) {
        return persist(dropAlreadyStaged(candidates), true);
    }

    /**
     * Drop candidates whose detail URL has already been scraped before (matched on externalId),
     * and collapse any in-batch duplicate URLs. Candidates without a URL are always kept.
     */
    private List<ScrapeCandidate> dropAlreadyStaged(List<ScrapeCandidate> candidates) {
        var urls = candidates.stream()
                .map(ScrapeCandidate::sourceUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (urls.isEmpty()) {
            return candidates;
        }
        var seen = new HashSet<>(scrapedRepo.findExistingExternalIds(ScrapedEventSource.WEB_SCRAPER, urls));
        return candidates.stream()
                .filter(c -> c.sourceUrl() == null || seen.add(c.sourceUrl()))
                .toList();
    }

    private List<ScrapedEventResponse> persist(List<ScrapeCandidate> candidates, boolean urlAsExternalId) {
        return candidates.stream()
                .map(c -> RawScrapedEvent.builder()
                        .source(ScrapedEventSource.WEB_SCRAPER)
                        .externalId(urlAsExternalId ? c.sourceUrl() : null)
                        .rawTitle(c.title())
                        .rawDescription(c.description())
                        .rawStartsAt(c.rawDateText())
                        .status(ScrapedEventStatus.PENDING_REVIEW)
                        .build())
                .map(scrapedRepo::save)
                .map(ScrapedEventResponse::from)
                .toList();
    }

    private void promoteAsRecurring(Event event, PromoteScrapedEventRequest.RecurrenceInput rec) {
        if (rec.rrule() == null || rec.rrule().isBlank()
                || rec.startTime() == null
                || rec.timezone() == null || rec.timezone().isBlank()) {
            throw new IllegalArgumentException("Återkommande event kräver rrule, startTime och timezone");
        }
        try {
            ZoneId.of(rec.timezone());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Ogiltig tidszon: " + rec.timezone());
        }
        var rule = recurrenceRuleRepository.save(RecurrenceRule.builder()
                .event(event)
                .rrule(rec.rrule())
                .startTime(rec.startTime())
                .durationMinutes(rec.durationMinutes())
                .timezone(rec.timezone())
                .build());
        // Materialise immediately so the event shows up right away; the weekly job tops it up.
        materializerJob.materializeRule(rule);
    }

    private RawScrapedEvent findOrThrow(UUID id) {
        return scrapedRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scraped event not found"));
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private OffsetDateTime parseTimestamp(String s) {
        if (s == null || s.isBlank()) return null;
        return OffsetDateTime.parse(s);
    }
}
