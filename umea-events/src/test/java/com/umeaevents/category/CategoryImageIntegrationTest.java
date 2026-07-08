package com.umeaevents.category;

import com.umeaevents.event.Event;
import com.umeaevents.event.EventOccurrence;
import com.umeaevents.event.EventOccurrenceRepository;
import com.umeaevents.event.EventRepository;
import com.umeaevents.event.EventService;
import com.umeaevents.event.EventStatus;
import com.umeaevents.user.Role;
import com.umeaevents.user.User;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.Venue;
import com.umeaevents.venue.VenueType;
import com.umeaevents.venue.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CategoryImageIntegrationTest {

    @Autowired private CategoryService categoryService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private EventService eventService;
    @Autowired private UserRepository userRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventOccurrenceRepository occurrenceRepository;

    @Test
    void updateImage_setsCategoryStockImage() {
        var category = categoryRepository.findAll().get(0);
        var resp = categoryService.updateImage(category.getId(), "https://img.test/cat.jpg");
        assertThat(resp.imageUrl()).isEqualTo("https://img.test/cat.jpg");
    }

    @Test
    void search_usesCategoryImageWhenEventHasNone() {
        var category = categoryRepository.findAll().get(0);
        category.setImageUrl("https://img.test/fallback.jpg");
        categoryRepository.saveAndFlush(category);

        var owner = userRepository.save(User.builder()
                .email("o-" + UUID.randomUUID() + "@test.com").passwordHash("x").role(Role.RESTAURANT).build());
        var venue = venueRepository.save(Venue.builder().name("L").type(VenueType.PUB).owner(owner).build());
        var title = "Bildlöst-" + UUID.randomUUID();
        var event = eventRepository.save(Event.builder()
                .title(title).venue(venue).category(category).owner(owner)
                .status(EventStatus.PUBLISHED).build()); // imageUrl null
        occurrenceRepository.saveAndFlush(EventOccurrence.builder()
                .event(event).startsAt(OffsetDateTime.now().plusDays(1)).build());

        var page = eventService.search(null, null, null, null, null, PageRequest.of(0, 50));
        var found = page.getContent().stream().filter(o -> title.equals(o.title())).findFirst().orElseThrow();

        assertThat(found.imageUrl()).isEqualTo("https://img.test/fallback.jpg");
    }

    @Test
    void search_prefersVenueImageOverCategoryWhenEventHasNone() {
        var category = categoryRepository.findAll().get(0);
        category.setImageUrl("https://img.test/cat-fallback.jpg");
        categoryRepository.saveAndFlush(category);

        var owner = userRepository.save(User.builder()
                .email("o-" + UUID.randomUUID() + "@test.com").passwordHash("x").role(Role.RESTAURANT).build());
        var venue = venueRepository.save(Venue.builder()
                .name("L").type(VenueType.PUB).owner(owner)
                .imageUrl("https://img.test/venue-logo.jpg").build());
        var title = "Venuebild-" + UUID.randomUUID();
        var event = eventRepository.save(Event.builder()
                .title(title).venue(venue).category(category).owner(owner)
                .status(EventStatus.PUBLISHED).build()); // imageUrl null
        occurrenceRepository.saveAndFlush(EventOccurrence.builder()
                .event(event).startsAt(OffsetDateTime.now().plusDays(1)).build());

        var page = eventService.search(null, null, null, null, null, PageRequest.of(0, 50));
        var found = page.getContent().stream().filter(o -> title.equals(o.title())).findFirst().orElseThrow();

        assertThat(found.imageUrl()).isEqualTo("https://img.test/venue-logo.jpg");
    }
}
