package com.umeaevents.venue;

import com.umeaevents.user.Role;
import com.umeaevents.user.User;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.dto.OpeningHoursDto;
import com.umeaevents.venue.dto.SetOpeningHoursRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OpeningHoursIntegrationTest {

    @Autowired private OpeningHoursService service;
    @Autowired private UserRepository userRepository;
    @Autowired private VenueRepository venueRepository;

    private Venue venueOwnedBy(String ownerEmail) {
        var owner = userRepository.save(User.builder()
                .email(ownerEmail).passwordHash("x").role(Role.RESTAURANT).build());
        return venueRepository.save(Venue.builder()
                .name("L-" + UUID.randomUUID()).type(VenueType.PUB).owner(owner).build());
    }

    @Test
    void setAndGet_replacesWholeSchedule() {
        var v = venueOwnedBy("o-" + UUID.randomUUID() + "@t.com");
        var owner = v.getOwner().getEmail();

        service.setForVenue(v.getId(), new SetOpeningHoursRequest(List.of(
                new OpeningHoursDto(5, LocalTime.of(17, 0), LocalTime.of(1, 0)),
                new OpeningHoursDto(6, LocalTime.of(12, 0), LocalTime.of(2, 0)))), owner);
        assertThat(service.getForVenue(v.getId())).hasSize(2);

        // replacing overwrites, not appends
        service.setForVenue(v.getId(), new SetOpeningHoursRequest(List.of(
                new OpeningHoursDto(5, LocalTime.of(16, 0), LocalTime.of(23, 0)))), owner);
        var got = service.getForVenue(v.getId());
        assertThat(got).hasSize(1);
        assertThat(got.get(0).opensAt()).isEqualTo(LocalTime.of(16, 0));
    }

    @Test
    void setForVenue_deniedForNonOwner() {
        var v = venueOwnedBy("owner-" + UUID.randomUUID() + "@t.com");
        var stranger = userRepository.save(User.builder()
                .email("x-" + UUID.randomUUID() + "@t.com").passwordHash("x").role(Role.RESTAURANT).build());

        assertThatThrownBy(() -> service.setForVenue(v.getId(), new SetOpeningHoursRequest(List.of(
                new OpeningHoursDto(1, LocalTime.of(10, 0), LocalTime.of(20, 0)))), stranger.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void openVenuesAt_handlesOpenNowPastMidnightSpilloverAndOpensLater() {
        var v = venueOwnedBy("o2-" + UUID.randomUUID() + "@t.com");
        var base = ZonedDateTime.of(2026, 8, 7, 12, 0, 0, 0, OpeningHoursService.ZONE);
        int dow = base.getDayOfWeek().getValue();
        // that weekday: 17:00 – 01:00 (past midnight)
        service.setForVenue(v.getId(), new SetOpeningHoursRequest(List.of(
                new OpeningHoursDto(dow, LocalTime.of(17, 0), LocalTime.of(1, 0)))), v.getOwner().getEmail());

        // 22:00 same day → open now
        var atEve = find(service.openVenuesAt(base.withHour(22)), v.getId());
        assertThat(atEve).isNotNull();
        assertThat(atEve.openNow()).isTrue();

        // 00:30 the next day → still open (spillover)
        var atSpill = find(service.openVenuesAt(base.plusDays(1).withHour(0).withMinute(30)), v.getId());
        assertThat(atSpill).isNotNull();
        assertThat(atSpill.openNow()).isTrue();

        // 15:00 same day → opens later today (not yet open)
        var atAfternoon = find(service.openVenuesAt(base.withHour(15)), v.getId());
        assertThat(atAfternoon).isNotNull();
        assertThat(atAfternoon.openNow()).isFalse();
        assertThat(atAfternoon.opensAt()).isEqualTo(LocalTime.of(17, 0));

        // two days later 22:00 → closed (no hours that weekday)
        assertThat(find(service.openVenuesAt(base.plusDays(2).withHour(22)), v.getId())).isNull();
    }

    private static com.umeaevents.venue.dto.OpenVenueResponse find(
            List<com.umeaevents.venue.dto.OpenVenueResponse> list, UUID venueId) {
        return list.stream().filter(o -> o.venueId().equals(venueId)).findFirst().orElse(null);
    }
}
