package com.umeaevents.admin;

public record AdminStatsResponse(
        long totalUsers,
        long totalVenues,
        EventStats events,
        ScrapedStats scrapedEvents,
        long upcomingOccurrences7Days
) {
    public record EventStats(
            long draft,
            long pendingReview,
            long published,
            long cancelled,
            long archived
    ) {}

    public record ScrapedStats(
            long pendingReview,
            long promoted,
            long rejected
    ) {}
}
