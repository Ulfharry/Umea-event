package com.umeaevents.scraping;

import com.umeaevents.category.Category;
import com.umeaevents.category.CategoryRepository;
import com.umeaevents.event.EventOccurrenceRepository;
import com.umeaevents.event.RecurrenceRuleRepository;
import com.umeaevents.user.Role;
import com.umeaevents.user.User;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.Venue;
import com.umeaevents.venue.VenueType;
import com.umeaevents.venue.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Promoting a scraped event as a recurring series must create a RecurrenceRule and materialise
 * concrete occurrences. Runs against real Postgres (the controller test mocks the service).
 */
@SpringBootTest
@Transactional
class PromoteRecurringIntegrationTest {

    @Autowired private ScrapedEventService scrapedEventService;
    @Autowired private RawScrapedEventRepository scrapedRepo;
    @Autowired private UserRepository userRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private RecurrenceRuleRepository recurrenceRuleRepository;
    @Autowired private EventOccurrenceRepository occurrenceRepository;

    @Test
    void promote_recurring_createsRuleAndMaterialisesOccurrences() {
        var adminEmail = "admin-" + UUID.randomUUID() + "@test.com";
        userRepository.save(User.builder().email(adminEmail).passwordHash("x").role(Role.ADMIN).build());
        var owner = userRepository.save(User.builder()
                .email("owner-" + UUID.randomUUID() + "@test.com").passwordHash("x").role(Role.RESTAURANT).build());
        Category category = categoryRepository.findAll().get(0);
        var venue = venueRepository.save(Venue.builder().name("Lokal").type(VenueType.PUB).owner(owner).build());
        var raw = scrapedRepo.save(RawScrapedEvent.builder()
                .source(ScrapedEventSource.WEB_SCRAPER).rawTitle("Musikquiz").build());

        long occBefore = occurrenceRepository.count();
        long ruleBefore = recurrenceRuleRepository.count();

        var request = new PromoteScrapedEventRequest(
                venue.getId(), category.getId(), null, null, "promoterad",
                new PromoteScrapedEventRequest.RecurrenceInput(
                        "FREQ=WEEKLY;BYDAY=WE", LocalTime.of(20, 0), 120, "Europe/Stockholm"));

        var resp = scrapedEventService.promote(raw.getId(), request, adminEmail);

        assertThat(resp.status()).isEqualTo("PROMOTED");
        assertThat(resp.promotedEventId()).isNotNull();
        assertThat(recurrenceRuleRepository.count()).isEqualTo(ruleBefore + 1);
        assertThat(occurrenceRepository.count())
                .as("weekly rule should materialise occurrences within the horizon")
                .isGreaterThan(occBefore);
    }
}
