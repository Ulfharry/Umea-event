package com.umeaevents.scraping;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScrapeSourceResponse(
        UUID id,
        String name,
        String sitemapUrl,
        String urlPattern,
        boolean enabled,
        OffsetDateTime lastRunAt,
        Integer lastRunNewCount,
        String lastRunError,
        OffsetDateTime createdAt
) {
    static ScrapeSourceResponse from(ScrapeSource s) {
        return new ScrapeSourceResponse(
                s.getId(),
                s.getName(),
                s.getSitemapUrl(),
                s.getUrlPattern(),
                s.isEnabled(),
                s.getLastRunAt(),
                s.getLastRunNewCount(),
                s.getLastRunError(),
                s.getCreatedAt()
        );
    }
}
