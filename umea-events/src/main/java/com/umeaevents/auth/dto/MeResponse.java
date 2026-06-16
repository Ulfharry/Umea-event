package com.umeaevents.auth.dto;

import com.umeaevents.user.Role;

import java.util.UUID;

/** The currently authenticated user. Lets a client learn its identity and role from the token. */
public record MeResponse(
        UUID id,
        String email,
        Role role
) {}
