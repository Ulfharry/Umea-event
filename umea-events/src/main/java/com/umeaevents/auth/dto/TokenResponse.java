package com.umeaevents.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresInMs
) {}
