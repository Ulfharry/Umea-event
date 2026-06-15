package com.umeaevents.admin;

import com.umeaevents.user.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull Role role) {}
