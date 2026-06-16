package com.umeaevents.scraping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Weekly scrape of every enabled {@link ScrapeSource}. Each source is independent: a failure on
 * one is logged and recorded, never aborting the rest. Results stage as PENDING_REVIEW and are
 * deduplicated against earlier runs, so this is safe to run repeatedly.
 *
 * <p>Default cron: Mondays at 04:00 (server time). Override with {@code app.scrape.cron}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledScrapeJob {

    private final ScrapeSourceRepository sourceRepo;
    private final ScrapeSourceService scrapeSourceService;

    @Scheduled(cron = "${app.scrape.cron:0 0 4 * * MON}")
    public void scrapeAllSources() {
        var sources = sourceRepo.findByEnabledTrue();
        if (sources.isEmpty()) {
            return;
        }
        log.info("Weekly scrape: running {} enabled source(s)", sources.size());
        for (var source : sources) {
            try {
                int newCount = scrapeSourceService.runSource(source);
                log.info("Scrape source '{}': {} new event(s) staged", source.getName(), newCount);
            } catch (Exception e) {
                log.error("Scrape source '{}' failed: {}", source.getName(), e.getMessage());
            }
        }
    }
}
