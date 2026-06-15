package com.umeaevents.scraping;

import jakarta.validation.constraints.NotBlank;

public record ScraperTestRequest(@NotBlank String url) {}
