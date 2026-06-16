package com.umeaevents.scraping;

import com.umeaevents.category.CategoryRepository;
import com.umeaevents.event.EventOccurrenceRepository;
import com.umeaevents.event.EventRepository;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the dedup behaviour of saveFromSitemap (and that saveFromScraper does not dedup).
 */
@ExtendWith(MockitoExtension.class)
class ScrapedEventServiceTest {

    @Mock private RawScrapedEventRepository scrapedRepo;
    @Mock private EventRepository eventRepository;
    @Mock private EventOccurrenceRepository occurrenceRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ScrapedEventService service;

    private static ScrapeCandidate candidate(String title, String url) {
        return new ScrapeCandidate(title, "desc", null, url, OffsetDateTime.now());
    }

    @Test
    void saveFromSitemap_skipsAlreadyStagedUrls() {
        var fresh = candidate("New Event", "https://x/events/new/");
        var existing = candidate("Old Event", "https://x/events/old/");
        when(scrapedRepo.findExistingExternalIds(eq(ScrapedEventSource.WEB_SCRAPER), anySet()))
                .thenReturn(Set.of("https://x/events/old/"));
        when(scrapedRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.saveFromSitemap(List.of(fresh, existing));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).rawTitle()).isEqualTo("New Event");

        var saved = ArgumentCaptor.forClass(RawScrapedEvent.class);
        verify(scrapedRepo).save(saved.capture());
        assertThat(saved.getValue().getExternalId()).isEqualTo("https://x/events/new/");
        assertThat(saved.getValue().getStatus()).isEqualTo(ScrapedEventStatus.PENDING_REVIEW);
    }

    @Test
    void saveFromSitemap_collapsesInBatchDuplicateUrls() {
        var first = candidate("Quiz", "https://x/events/quiz/");
        var dup = candidate("Quiz again", "https://x/events/quiz/");
        when(scrapedRepo.findExistingExternalIds(eq(ScrapedEventSource.WEB_SCRAPER), anySet()))
                .thenReturn(Set.of());
        when(scrapedRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.saveFromSitemap(List.of(first, dup));

        assertThat(result).hasSize(1);
        verify(scrapedRepo).save(any());
    }

    @Test
    void saveFromSitemap_allDuplicates_savesNothing() {
        var c = candidate("Old", "https://x/events/old/");
        when(scrapedRepo.findExistingExternalIds(eq(ScrapedEventSource.WEB_SCRAPER), anySet()))
                .thenReturn(Set.of("https://x/events/old/"));

        var result = service.saveFromSitemap(List.of(c));

        assertThat(result).isEmpty();
        verify(scrapedRepo, never()).save(any());
    }

    @Test
    void saveFromScraper_doesNotDedup_andSetsNoExternalId() {
        // Listing-page path: all candidates share one URL; dedup must NOT apply here.
        var a = candidate("A", "https://x/events/");
        var b = candidate("B", "https://x/events/");
        when(scrapedRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.saveFromScraper(List.of(a, b));

        assertThat(result).hasSize(2);
        verify(scrapedRepo, never()).findExistingExternalIds(any(), anySet());

        var saved = ArgumentCaptor.forClass(RawScrapedEvent.class);
        verify(scrapedRepo, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).allMatch(e -> e.getExternalId() == null);
    }
}
