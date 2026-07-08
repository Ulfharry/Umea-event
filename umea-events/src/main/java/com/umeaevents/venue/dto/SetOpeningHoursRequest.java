package com.umeaevents.venue.dto;

import jakarta.validation.Valid;

import java.util.List;

/** Replace a venue's full weekly schedule. An empty list clears all hours (venue has none). */
public record SetOpeningHoursRequest(@Valid List<OpeningHoursDto> hours) {}
