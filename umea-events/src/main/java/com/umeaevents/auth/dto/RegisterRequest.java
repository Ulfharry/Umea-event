package com.umeaevents.auth.dto;

import com.umeaevents.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, message = "Lösenord måste vara minst 8 tecken") String password,
        @NotNull Role role
) {}
