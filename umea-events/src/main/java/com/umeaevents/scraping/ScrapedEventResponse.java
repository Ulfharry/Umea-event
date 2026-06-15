package com.umeaevents.scraping;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScrapedEventResponse(
        UUID id,
        String source,
        String externalId,
        String rawTitle,
        String rawDescription,
        String rawVenueName,
        String rawStartsAt,
        String rawEndsAt,
        OffsetDateTime parsedStartsAt,
        OffsetDateTime parsedEndsAt,
        String status,
        String adminNotes,
        OffsetDateTime reviewedAt,
        UUID reviewedBy,
        UUID promotedEventId,
        OffsetDateTime createdAt
) {
    static ScrapedEventResponse from(RawScrapedEvent e) {
        return new ScrapedEventResponse(
                e.getId(),
                e.getSource().name(),
                e.getExternalId(),
                e.getRawTitle(),
                e.getRawDescription(),
                e.getRawVenueName(),
                e.getRawStartsAt(),
                e.getRawEndsAt(),
                e.getParsedStartsAt(),
                e.getParsedEndsAt(),
                e.getStatus().name(),
                e.getAdminNotes(),
                e.getReviewedAt(),
                e.getReviewedBy() != null ? e.getReviewedBy().getId() : null,
                e.getPromotedEvent() != null ? e.getPromotedEvent().getId() : null,
                e.getCreatedAt()
        );
    }
}
