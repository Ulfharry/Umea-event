package com.umeaevents.admin;

import com.umeaevents.event.EventOccurrenceRepository;
import com.umeaevents.event.EventRepository;
import com.umeaevents.event.EventStatus;
import com.umeaevents.scraping.RawScrapedEventRepository;
import com.umeaevents.scraping.ScrapedEventStatus;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final EventRepository eventRepository;
    private final EventOccurrenceRepository occurrenceRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final RawScrapedEventRepository scrapedRepo;

    @Transactional(readOnly = true)
    public AdminStatsResponse stats() {
        var now = OffsetDateTime.now();
        var in7Days = now.plusDays(7);

        var eventStats = new AdminStatsResponse.EventStats(
                eventRepository.countByStatus(EventStatus.DRAFT),
                eventRepository.countByStatus(EventStatus.PENDING_REVIEW),
                eventRepository.countByStatus(EventStatus.PUBLISHED),
                eventRepository.countByStatus(EventStatus.CANCELLED),
                eventRepository.countByStatus(EventStatus.ARCHIVED)
        );

        var scrapedStats = new AdminStatsResponse.ScrapedStats(
                scrapedRepo.countByStatus(ScrapedEventStatus.PENDING_REVIEW),
                scrapedRepo.countByStatus(ScrapedEventStatus.PROMOTED),
                scrapedRepo.countByStatus(ScrapedEventStatus.REJECTED)
        );

        return new AdminStatsResponse(
                userRepository.count(),
                venueRepository.count(),
                eventStats,
                scrapedStats,
                occurrenceRepository.countByStartsAtBetween(now, in7Days)
        );
    }
}
