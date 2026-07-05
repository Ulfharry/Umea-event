package com.umeaevents.event;

import com.umeaevents.category.Category;
import com.umeaevents.category.CategoryRepository;
import com.umeaevents.event.dto.UpdateEventRequest;
import com.umeaevents.scraping.RawScrapedEvent;
import com.umeaevents.scraping.RawScrapedEventRepository;
import com.umeaevents.scraping.ScrapedEventSource;
import com.umeaevents.scraping.ScrapedEventStatus;
import com.umeaevents.user.Role;
import com.umeaevents.user.User;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.Venue;
import com.umeaevents.venue.VenueType;
import com.umeaevents.venue.VenueRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EventUpdateDeleteIntegrationTest {

    @Autowired private EventService eventService;
    @Autowired private UserRepository userRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventOccurrenceRepository occurrenceRepository;
    @Autowired private RecurrenceRuleRepository recurrenceRuleRepository;
    @Autowired private RawScrapedEventRepository scrapedRepo;
    @Autowired private EntityManager em;

    private User admin;
    private Venue venue;
    private Category category;

    private void baseData() {
        admin = userRepository.save(User.builder()
                .email("admin-" + UUID.randomUUID() + "@test.com").passwordHash("x").role(Role.ADMIN).build());
        var owner = userRepository.save(User.builder()
                .email("owner-" + UUID.randomUUID() + "@test.com").passwordHash("x").role(Role.RESTAURANT).build());
        category = categoryRepository.findAll().get(0);
        venue = venueRepository.save(Venue.builder().name("Lokal").type(VenueType.PUB).owner(owner).build());
    }

    private Event publishedEvent() {
        return eventRepository.save(Event.builder()
                .title("Original").venue(venue).category(category).owner(admin)
                .status(EventStatus.PUBLISHED).build());
    }

    @Test
    void update_single_changesFieldsAndOccurrenceTime() {
        baseData();
        var event = publishedEvent();
        var start = OffsetDateTime.parse("2026-09-01T20:00:00Z");
        occurrenceRepository.saveAndFlush(EventOccurrence.builder().event(event).startsAt(start).build());

        var newStart = OffsetDateTime.parse("2026-09-02T21:00:00Z");
        eventService.update(event.getId(), new UpdateEventRequest(
                "Nytt namn", "beskr", "https://img/x.jpg", venue.getId(), category.getId(),
                newStart, null, null), admin.getEmail());

        var reloaded = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Nytt namn");
        assertThat(reloaded.getImageUrl()).isEqualTo("https://img/x.jpg");
        var occ = occurrenceRepository.findFirstByEventOrderByStartsAtAsc(reloaded).orElseThrow();
        assertThat(occ.getStartsAt()).isEqualTo(newStart);
    }

    @Test
    void update_recurring_regeneratesOccurrences() {
        baseData();
        var event = publishedEvent();
        recurrenceRuleRepository.saveAndFlush(RecurrenceRule.builder()
                .event(event).rrule("FREQ=WEEKLY;BYDAY=WE").startTime(LocalTime.of(20, 0))
                .timezone("Europe/Stockholm").build());

        long occBefore = occurrenceRepository.count();
        eventService.update(event.getId(), new UpdateEventRequest(
                "Uppdaterad serie", null, null, venue.getId(), category.getId(), null, null,
                new UpdateEventRequest.Recurrence("FREQ=WEEKLY;BYDAY=MO", LocalTime.of(19, 0), 90, "Europe/Stockholm")),
                admin.getEmail());

        var rule = recurrenceRuleRepository.findByEvent(event).orElseThrow();
        assertThat(rule.getRrule()).isEqualTo("FREQ=WEEKLY;BYDAY=MO");
        assertThat(rule.getStartTime()).isEqualTo(LocalTime.of(19, 0));
        assertThat(occurrenceRepository.count()).isGreaterThan(occBefore); // materialised from new schedule
    }

    @Test
    void delete_removesEventAndOccurrences() {
        baseData();
        var event = publishedEvent();
        var occ = occurrenceRepository.saveAndFlush(EventOccurrence.builder()
                .event(event).startsAt(OffsetDateTime.now().plusDays(1)).build());
        em.clear(); // mirror a real request: children not in the persistence context

        eventService.delete(event.getId(), admin.getEmail());
        em.flush();
        em.clear();

        assertThat(eventRepository.findById(event.getId())).isEmpty();
        assertThat(occurrenceRepository.findById(occ.getId())).isEmpty(); // ON DELETE CASCADE
    }

    @Test
    void delete_promotedEvent_nullsScrapedBackLinkInsteadOfBlocking() {
        baseData();
        var event = publishedEvent();
        var raw = scrapedRepo.saveAndFlush(RawScrapedEvent.builder()
                .source(ScrapedEventSource.WEB_SCRAPER).rawTitle("Scrapad")
                .status(ScrapedEventStatus.PROMOTED).promotedEvent(event).build());
        em.clear(); // mirror a real request: the scraped row isn't in the persistence context

        eventService.delete(event.getId(), admin.getEmail());
        em.flush();
        em.clear();

        assertThat(eventRepository.findById(event.getId())).isEmpty();
        var reloadedRaw = scrapedRepo.findById(raw.getId()).orElseThrow();
        assertThat(reloadedRaw.getPromotedEvent()).isNull(); // FK ON DELETE SET NULL (V13)
    }
}
