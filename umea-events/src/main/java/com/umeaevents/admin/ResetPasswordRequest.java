package com.umeaevents.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(min = 8, message = "Lösenord måste vara minst 8 tecken") String password
) {}
