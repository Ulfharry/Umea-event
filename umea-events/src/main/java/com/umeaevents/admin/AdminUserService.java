package com.umeaevents.admin;

import com.umeaevents.auth.RefreshTokenRepository;
import com.umeaevents.common.exception.ResourceNotFoundException;
import com.umeaevents.user.Role;
import com.umeaevents.user.User;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.Venue;
import com.umeaevents.venue.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(AdminUserResponse::from);
    }

    /**
     * Create a user directly and, if venueIds are given, transfer those venues' ownership to it.
     * ADMIN accounts cannot be created here — admin rights are granted only via {@link #changeRole}.
     */
    @Transactional
    public AdminUserResponse createUser(AdminCreateUserRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin-konton kan inte skapas här; höj rollen via rolländring");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("E-postadressen är redan registrerad");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();
        userRepository.save(user);

        if (request.venueIds() != null) {
            for (UUID venueId : request.venueIds()) {
                Venue venue = venueRepository.findById(venueId)
                        .orElseThrow(() -> new ResourceNotFoundException("Venue hittades inte: " + venueId));
                venue.setOwner(user);
                venueRepository.save(venue);
            }
        }

        return AdminUserResponse.from(user);
    }

    /** Activate or deactivate an account. Deactivating blocks login and revokes refresh tokens. */
    @Transactional
    public AdminUserResponse setActive(UUID targetId, boolean active, String adminEmail) {
        var admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));
        if (admin.getId().equals(targetId)) {
            throw new IllegalArgumentException("Du kan inte inaktivera ditt eget konto");
        }
        var target = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        target.setActive(active);
        if (!active) {
            refreshTokenRepository.revokeAllByUserId(target.getId());
        }
        return AdminUserResponse.from(userRepository.save(target));
    }

    /** Admin sets a new password for a user and revokes their existing sessions. */
    @Transactional
    public AdminUserResponse resetPassword(UUID targetId, String newPassword) {
        var target = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        target.setPasswordHash(passwordEncoder.encode(newPassword));
        refreshTokenRepository.revokeAllByUserId(target.getId());
        return AdminUserResponse.from(userRepository.save(target));
    }

    @Transactional
    public AdminUserResponse changeRole(UUID targetId, ChangeRoleRequest request, String adminEmail) {
        var admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));
        if (admin.getId().equals(targetId)) {
            throw new IllegalArgumentException("Cannot change your own role");
        }
        var target = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        target.setRole(request.role());
        return AdminUserResponse.from(userRepository.save(target));
    }
}
