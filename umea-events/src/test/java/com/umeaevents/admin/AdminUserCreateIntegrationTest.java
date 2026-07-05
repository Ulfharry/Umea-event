package com.umeaevents.admin;

import com.umeaevents.user.Role;
import com.umeaevents.user.User;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.Venue;
import com.umeaevents.venue.VenueRepository;
import com.umeaevents.venue.VenueService;
import com.umeaevents.venue.VenueType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AdminUserCreateIntegrationTest {

    @Autowired private AdminUserService adminUserService;
    @Autowired private VenueService venueService;
    @Autowired private UserRepository userRepository;
    @Autowired private VenueRepository venueRepository;

    private User admin() {
        return userRepository.save(User.builder()
                .email("admin-" + UUID.randomUUID() + "@test.com").passwordHash("x").role(Role.ADMIN).build());
    }

    private Venue venueOwnedBy(User owner) {
        return venueRepository.save(Venue.builder()
                .name("L-" + UUID.randomUUID()).type(VenueType.PUB).owner(owner).build());
    }

    @Test
    void createUser_hashesPassword_andAssignsVenues() {
        var seedOwner = admin();
        var v1 = venueOwnedBy(seedOwner);
        var v2 = venueOwnedBy(seedOwner);
        var email = "venue-" + UUID.randomUUID() + "@test.com";

        var resp = adminUserService.createUser(new AdminCreateUserRequest(
                email, "hemligt123", Role.RESTAURANT, List.of(v1.getId(), v2.getId())));

        assertThat(resp.email()).isEqualTo(email);
        assertThat(resp.role()).isEqualTo("RESTAURANT");

        var created = userRepository.findByEmail(email).orElseThrow();
        assertThat(created.getPasswordHash()).isNotEqualTo("hemligt123"); // stored hashed

        assertThat(venueRepository.findById(v1.getId()).orElseThrow().getOwner().getId()).isEqualTo(created.getId());
        assertThat(venueRepository.findById(v2.getId()).orElseThrow().getOwner().getId()).isEqualTo(created.getId());
    }

    @Test
    void createUser_rejectsAdminRole() {
        assertThatThrownBy(() -> adminUserService.createUser(new AdminCreateUserRequest(
                "x-" + UUID.randomUUID() + "@test.com", "hemligt123", Role.ADMIN, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createUser_rejectsDuplicateEmail() {
        var email = "dup-" + UUID.randomUUID() + "@test.com";
        adminUserService.createUser(new AdminCreateUserRequest(email, "hemligt123", Role.RESTAURANT, null));
        assertThatThrownBy(() -> adminUserService.createUser(new AdminCreateUserRequest(
                email, "hemligt123", Role.RESTAURANT, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assignOwner_transfersVenueToAnotherUser() {
        var owner1 = admin();
        var venue = venueOwnedBy(owner1);
        var owner2 = userRepository.save(User.builder()
                .email("r-" + UUID.randomUUID() + "@test.com").passwordHash("x").role(Role.RESTAURANT).build());

        var resp = venueService.assignOwner(venue.getId(), owner2.getId());

        assertThat(resp.ownerId()).isEqualTo(owner2.getId());
        assertThat(venueRepository.findById(venue.getId()).orElseThrow().getOwner().getId()).isEqualTo(owner2.getId());
    }
}
