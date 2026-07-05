package com.umeaevents.admin;

import jakarta.validation.constraints.NotNull;

public record SetActiveRequest(@NotNull Boolean active) {}
