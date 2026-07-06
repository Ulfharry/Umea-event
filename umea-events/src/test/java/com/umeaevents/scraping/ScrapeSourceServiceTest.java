package com.umeaevents.scraping;

import com.umeaevents.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScrapeSourceServiceTest {

    @Mock private ScrapeSourceRepository sourceRepo;
    @Mock private SitemapScraper sitemapScraper;
    @Mock private ScrapedEventService scrapedEventService;

    @InjectMocks private ScrapeSourceService service;

    private static ScrapeSource source() {
        return ScrapeSource.builder()
                .id(UUID.randomUUID())
                .name("O'Learys")
                .sitemapUrl("https://x/sitemap.xml")
                .urlPattern("/events/.+")
                .enabled(true)
                .build();
    }

    private static ScrapedEventResponse stub() {
        return new ScrapedEventResponse(UUID.randomUUID(), "WEB_SCRAPER", null, "t",
                null, null, null, null, null, null, "PENDING_REVIEW",
                null, null, null, null, OffsetDateTime.now());
    }

    // ── create ──────────────────────────────────────────────────────────────────

    @Test
    void create_defaultsEnabledToTrueWhenNull() {
        when(sourceRepo.existsBySitemapUrlAndUrlPattern(any(), any())).thenReturn(false);
        when(sourceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.create(new ScrapeSourceRequest("O'Learys", "https://x/s.xml", "/events/.+", null, null));

        assertThat(result.enabled()).isTrue();
    }

    @Test
    void create_invalidRegex_throwsBadRequest() {
        assertThatThrownBy(() ->
                service.create(new ScrapeSourceRequest("Bad", "https://x/s.xml", "[unclosed", true, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid urlPattern");
        verify(sourceRepo, never()).save(any());
    }

    @Test
    void create_duplicate_throwsConflict() {
        when(sourceRepo.existsBySitemapUrlAndUrlPattern(any(), any())).thenReturn(true);

        assertThatThrownBy(() ->
                service.create(new ScrapeSourceRequest("Dup", "https://x/s.xml", "/events/.+", true, null)))
                .isInstanceOf(IllegalStateException.class);
        verify(sourceRepo, never()).save(any());
    }

    // ── delete ──────────────────────────────────────────────────────────────────

    @Test
    void delete_missing_throwsNotFound() {
        var id = UUID.randomUUID();
        when(sourceRepo.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceNotFoundException.class);
        verify(sourceRepo, never()).deleteById(any());
    }

    // ── runSource ───────────────────────────────────────────────────────────────

    @Test
    void runSource_stagesEventsAndRecordsSuccess() throws Exception {
        var s = source();
        when(sitemapScraper.scrape(any(), any(), any()))
                .thenReturn(List.of(new ScrapeCandidate("E", "d", null, "https://x/events/e/", OffsetDateTime.now())));
        when(scrapedEventService.saveFromSitemap(any())).thenReturn(List.of(stub(), stub()));

        int count = service.runSource(s);

        assertThat(count).isEqualTo(2);
        assertThat(s.getLastRunNewCount()).isEqualTo(2);
        assertThat(s.getLastRunError()).isNull();
        assertThat(s.getLastRunAt()).isNotNull();
        verify(sourceRepo).save(s);
    }

    @Test
    void runSource_fetchFailure_recordsErrorAndRethrows() throws Exception {
        var s = source();
        when(sitemapScraper.scrape(any(), any(), any())).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> service.runSource(s)).isInstanceOf(IOException.class);

        assertThat(s.getLastRunError()).isEqualTo("boom");
        assertThat(s.getLastRunAt()).isNotNull();
        verify(sourceRepo).save(s);
        verify(scrapedEventService, never()).saveFromSitemap(any());
    }

    // ── runNow ──────────────────────────────────────────────────────────────────

    @Test
    void runNow_missing_throwsNotFound() {
        var id = UUID.randomUUID();
        when(sourceRepo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.runNow(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void runNow_fetchFailure_throwsScrapingException() throws Exception {
        var s = source();
        when(sourceRepo.findById(s.getId())).thenReturn(Optional.of(s));
        when(sitemapScraper.scrape(any(), any(), any())).thenThrow(new IOException("dns"));

        assertThatThrownBy(() -> service.runNow(s.getId()))
                .isInstanceOf(ScrapingException.class)
                .hasMessageContaining("Could not fetch sitemap");
    }
}
