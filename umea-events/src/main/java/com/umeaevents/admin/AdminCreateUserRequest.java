package com.umeaevents.admin;

import com.umeaevents.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Admin creates a user directly (e.g. a RESTAURANT account) and optionally hands over a set of
 * venues to it. Each listed venue's owner is reassigned to the new user, so it can manage them.
 */
public record AdminCreateUserRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, message = "Lösenord måste vara minst 8 tecken") String password,
        @NotNull Role role,
        List<UUID> venueIds
) {}
