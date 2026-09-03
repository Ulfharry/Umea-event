package com.umeaevents.event;

import com.umeaevents.category.Category;
import com.umeaevents.category.CategoryRepository;
import com.umeaevents.event.dto.CreatePublishedEventRequest;
import com.umeaevents.event.dto.PickedDates;
import com.umeaevents.event.dto.RecurrencePreviewRequest;
import com.umeaevents.event.dto.UpdateEventRequest;
import com.umeaevents.user.Role;
import com.umeaevents.user.User;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.Venue;
import com.umeaevents.venue.VenueRepository;
import com.umeaevents.venue.VenueType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The calendar schedule modes: a hand-picked set of days, and unticking single dates out of a
 * rule-driven series.
 */
@SpringBootTest
@Transactional
class CalendarScheduleIntegrationTest {

    @Autowired private EventService eventService;
    @Autowired private UserRepository userRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventOccurrenceRepository occurrenceRepository;
    @Autowired private RecurrenceRuleRepository recurrenceRuleRepository;
    @Autowired private OccurrenceOverrideRepository overrideRepository;

    private String adminEmail;
    private Venue venue;
    private Category category;

    private void seed() {
        adminEmail = "admin-" + UUID.randomUUID() + "@test.com";
        userRepository.save(User.builder().email(adminEmail).passwordHash("x").role(Role.ADMIN).build());
        var owner = userRepository.save(User.builder()
                .email("o-" + UUID.randomUUID() + "@test.com").passwordHash("x").role(Role.RESTAURANT).build());
        venue = venueRepository.save(Venue.builder()
                .name("L-" + UUID.randomUUID()).type(VenueType.PUB).owner(owner).build());
        category = categoryRepository.findAll().get(0);
    }

    @Test
    void pickedDates_createOneOccurrencePerDay_andNoRule() {
        seed();
        var dates = List.of(
                LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 9),
                LocalDate.of(2026, 10, 23));

        var resp = eventService.createPublished(new CreatePublishedEventRequest(
                "Handplockat", null, null, venue.getId(), category.getId(), null, null, null,
                new PickedDates(dates, LocalTime.of(19, 30), 90, "Europe/Stockholm")), adminEmail);

        var event = eventRepository.findById(resp.id()).orElseThrow();
        var occurrences = occurrenceRepository.findAll().stream()
                .filter(o -> o.getEvent().getId().equals(event.getId()))
                .sorted(java.util.Comparator.comparing(EventOccurrence::getStartsAt))
                .toList();

        assertThat(occurrences).hasSize(3);
        // No rule, so the materialiser will never touch this event.
        assertThat(recurrenceRuleRepository.findByEvent(event)).isEmpty();

        var zone = ZoneId.of("Europe/Stockholm");
        assertThat(occurrences.get(0).getStartsAt().atZoneSameInstant(zone).toLocalDate())
                .isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(occurrences.get(0).getStartsAt().atZoneSameInstant(zone).toLocalTime())
                .isEqualTo(LocalTime.of(19, 30));
        assertThat(occurrences.get(0).getEndsAt()).isNotNull();
    }

    @Test
    void pickedDates_update_replacesTheWholeSet() {
        seed();
        var resp = eventService.createPublished(new CreatePublishedEventRequest(
                "Att ändra", null, null, venue.getId(), category.getId(), null, null, null,
                new PickedDates(List.of(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 9)),
                        LocalTime.of(19, 0), null, "Europe/Stockholm")), adminEmail);
        var event = eventRepository.findById(resp.id()).orElseThrow();

        eventService.update(event.getId(), new UpdateEventRequest(
                "Att ändra", null, null, venue.getId(), category.getId(), null, null, null,
                new PickedDates(List.of(LocalDate.of(2026, 11, 6)), LocalTime.of(20, 0), null,
                        "Europe/Stockholm")), adminEmail);

        var occurrences = occurrenceRepository.findAll().stream()
                .filter(o -> o.getEvent().getId().equals(event.getId())).toList();
        assertThat(occurrences).hasSize(1);
        assertThat(occurrences.get(0).getStartsAt().atZoneSameInstant(ZoneId.of("Europe/Stockholm")).toLocalDate())
                .isEqualTo(LocalDate.of(2026, 11, 6));
    }

    @Test
    void excludedDates_areNeverMaterialised() {
        seed();
        // Weekly Fridays from 2026-10-02; untick the second one.
        var excluded = LocalDate.of(2026, 10, 9);
        var resp = eventService.createPublished(new CreatePublishedEventRequest(
                "Serie med hål", null, null, venue.getId(), category.getId(), null, null,
                new CreatePublishedEventRequest.Recurrence(
                        "FREQ=WEEKLY;BYDAY=FR", LocalTime.of(20, 0), null, "Europe/Stockholm",
                        LocalDate.of(2026, 10, 2), List.of(excluded)),
                null), adminEmail);

        var event = eventRepository.findById(resp.id()).orElseThrow();
        assertThat(overrideRepository.findByEventAndStatus(event, OverrideStatus.CANCELLED))
                .extracting(OccurrenceOverride::getOriginalDate)
                .containsExactly(excluded);

        var dates = occurrenceRepository.findAll().stream()
                .filter(o -> o.getEvent().getId().equals(event.getId()))
                .map(o -> o.getStartsAt().atZoneSameInstant(ZoneId.of("Europe/Stockholm")).toLocalDate())
                .toList();
        assertThat(dates).doesNotContain(excluded);
    }

    @Test
    void previewRecurrence_returnsTheDatesTheRuleWouldGenerate() {
        seed();
        var dates = eventService.previewRecurrence(new RecurrencePreviewRequest(
                "FREQ=WEEKLY;BYDAY=FR;INTERVAL=2",
                LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 30)));

        assertThat(dates).containsExactly(
                LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 30),
                LocalDate.of(2026, 11, 13),
                LocalDate.of(2026, 11, 27));
    }
}
