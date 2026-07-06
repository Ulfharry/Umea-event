package com.umeaevents.scraping;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScrapeSourceResponse(
        UUID id,
        String name,
        String sitemapUrl,
        String urlPattern,
        boolean enabled,
        Integer maxAgeDays,
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
                s.getMaxAgeDays(),
                s.getLastRunAt(),
                s.getLastRunNewCount(),
                s.getLastRunError(),
                s.getCreatedAt()
        );
    }
}
