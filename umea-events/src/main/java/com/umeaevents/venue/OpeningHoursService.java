package com.umeaevents.venue;

import com.umeaevents.common.exception.ResourceNotFoundException;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.dto.OpenVenueResponse;
import com.umeaevents.venue.dto.OpeningHoursDto;
import com.umeaevents.venue.dto.SetOpeningHoursRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OpeningHoursService {

    static final ZoneId ZONE = ZoneId.of("Europe/Stockholm");

    private final VenueRepository venueRepository;
    private final VenueOpeningHoursRepository hoursRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<OpeningHoursDto> getForVenue(UUID venueId) {
        if (!venueRepository.existsById(venueId)) {
            throw new ResourceNotFoundException("Venue hittades inte: " + venueId);
        }
        return hoursRepository.findByVenueId(venueId).stream()
                .sorted(Comparator.comparingInt(VenueOpeningHours::getDayOfWeek)
                        .thenComparing(VenueOpeningHours::getOpensAt))
                .map(h -> new OpeningHoursDto(h.getDayOfWeek(), h.getOpensAt(), h.getClosesAt()))
                .toList();
    }

    /** Replace a venue's whole schedule (owner or admin only). */
    @Transactional
    public List<OpeningHoursDto> setForVenue(UUID venueId, SetOpeningHoursRequest request, String callerEmail) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue hittades inte: " + venueId));
        checkOwnerOrAdmin(venue, callerEmail);

        hoursRepository.deleteByVenueId(venueId);
        var hours = request.hours() == null ? List.<OpeningHoursDto>of() : request.hours();
        for (var dto : hours) {
            if (dto.opensAt().equals(dto.closesAt())) {
                throw new IllegalArgumentException("Öppnar- och stängningstid kan inte vara samma");
            }
            hoursRepository.save(VenueOpeningHours.builder()
                    .venue(venue)
                    .dayOfWeek(dto.dayOfWeek())
                    .opensAt(dto.opensAt())
                    .closesAt(dto.closesAt())
                    .build());
        }
        return getForVenue(venueId);
    }

    public List<OpenVenueResponse> openVenues() {
        return openVenuesAt(ZonedDateTime.now(ZONE));
    }

    /** Venues open at {@code now}, or opening later that same day; open-now first. Testable seam. */
    @Transactional(readOnly = true)
    public List<OpenVenueResponse> openVenuesAt(ZonedDateTime now) {
        DayOfWeek today = now.getDayOfWeek();
        LocalTime t = now.toLocalTime();

        Map<UUID, List<VenueOpeningHours>> byVenue = new HashMap<>();
        for (var h : hoursRepository.findAllForActiveVenues()) {
            byVenue.computeIfAbsent(h.getVenue().getId(), k -> new ArrayList<>()).add(h);
        }

        List<OpenVenueResponse> result = new ArrayList<>();
        for (var rows : byVenue.values()) {
            Venue venue = rows.get(0).getVenue();

            VenueOpeningHours openNow = rows.stream().filter(h -> isOpenAt(h, today, t)).findFirst().orElse(null);
            if (openNow != null) {
                result.add(toResponse(venue, openNow, true));
                continue;
            }
            VenueOpeningHours later = rows.stream()
                    .filter(h -> h.getDayOfWeek() == today.getValue() && h.getOpensAt().isAfter(t))
                    .min(Comparator.comparing(VenueOpeningHours::getOpensAt))
                    .orElse(null);
            if (later != null) {
                result.add(toResponse(venue, later, false));
            }
        }

        result.sort(Comparator.comparing(OpenVenueResponse::openNow).reversed()
                .thenComparing(OpenVenueResponse::opensAt));
        return result;
    }

    /** Is this interval open at (day, time)? Handles intervals that run past midnight. */
    static boolean isOpenAt(VenueOpeningHours h, DayOfWeek day, LocalTime t) {
        LocalTime opens = h.getOpensAt();
        LocalTime closes = h.getClosesAt();
        boolean pastMidnight = !closes.isAfter(opens); // closes <= opens

        if (h.getDayOfWeek() == day.getValue()) {
            return pastMidnight ? !t.isBefore(opens)               // open from `opens` on into next day
                                : (!t.isBefore(opens) && t.isBefore(closes)); // [opens, closes)
        }
        // Spillover: yesterday's past-midnight interval still running in the early hours of today.
        if (pastMidnight && h.getDayOfWeek() == day.minus(1).getValue()) {
            return t.isBefore(closes);
        }
        return false;
    }

    private static OpenVenueResponse toResponse(Venue v, VenueOpeningHours h, boolean openNow) {
        return new OpenVenueResponse(v.getId(), v.getName(), v.getType(), v.getAddress(),
                v.getImageUrl(), h.getOpensAt(), h.getClosesAt(), openNow);
    }

    private void checkOwnerOrAdmin(Venue venue, String callerEmail) {
        boolean isOwner = venue.getOwner().getEmail().equals(callerEmail);
        boolean isAdmin = userRepository.findByEmail(callerEmail)
                .map(u -> u.getRole().name().equals("ADMIN"))
                .orElse(false);
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Ingen behörighet att ändra denna venue");
        }
    }
}
