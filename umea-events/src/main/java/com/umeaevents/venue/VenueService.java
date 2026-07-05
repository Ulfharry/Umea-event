package com.umeaevents.venue;

import com.umeaevents.common.exception.ResourceNotFoundException;
import com.umeaevents.user.User;
import com.umeaevents.user.UserRepository;
import com.umeaevents.venue.dto.AdminVenueResponse;
import com.umeaevents.venue.dto.CreateVenueRequest;
import com.umeaevents.venue.dto.UpdateVenueRequest;
import com.umeaevents.venue.dto.VenueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final VenueMapper venueMapper;

    public Page<VenueResponse> listActive(Pageable pageable) {
        return venueRepository.findAllByActiveTrue(pageable).map(venueMapper::toResponse);
    }

    /** Admin listing: includes inactive venues and the owner's email. */
    @Transactional(readOnly = true)
    public Page<AdminVenueResponse> listForAdmin(Pageable pageable) {
        return venueRepository.findAll(pageable).map(AdminVenueResponse::from);
    }

    public Page<VenueResponse> listMine(String ownerEmail, Pageable pageable) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Användare hittades inte: " + ownerEmail));
        return venueRepository.findByOwnerIdAndActiveTrue(owner.getId(), pageable).map(venueMapper::toResponse);
    }

    public VenueResponse getById(UUID id) {
        return venueMapper.toResponse(findActiveOrThrow(id));
    }

    @Transactional
    public VenueResponse create(CreateVenueRequest request, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Användare hittades inte: " + ownerEmail));

        Venue venue = Venue.builder()
                .name(request.name())
                .description(request.description())
                .type(request.type())
                .address(request.address())
                .owner(owner)
                .build();

        return venueMapper.toResponse(venueRepository.save(venue));
    }

    @Transactional
    public VenueResponse update(UUID id, UpdateVenueRequest request, String callerEmail) {
        Venue venue = findActiveOrThrow(id);
        checkOwnerOrAdmin(venue, callerEmail);

        if (request.name() != null) venue.setName(request.name());
        if (request.description() != null) venue.setDescription(request.description());
        if (request.type() != null) venue.setType(request.type());
        if (request.address() != null) venue.setAddress(request.address());

        return venueMapper.toResponse(venueRepository.save(venue));
    }

    /**
     * Admin-only: activate or deactivate a venue. Unlike {@link #delete}, this can also reactivate
     * an inactive venue, so it looks the venue up regardless of its current active flag.
     */
    @Transactional
    public VenueResponse setActive(UUID venueId, boolean active) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue hittades inte: " + venueId));
        venue.setActive(active);
        return venueMapper.toResponse(venueRepository.save(venue));
    }

    /**
     * Admin-only: transfer a venue to another owner so that user can manage it and create events
     * for it. Authorization is enforced at the controller ({@code hasRole('ADMIN')}).
     */
    @Transactional
    public VenueResponse assignOwner(UUID venueId, UUID newOwnerId) {
        Venue venue = findActiveOrThrow(venueId);
        User owner = userRepository.findById(newOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Användare hittades inte: " + newOwnerId));
        venue.setOwner(owner);
        return venueMapper.toResponse(venueRepository.save(venue));
    }

    @Transactional
    public void delete(UUID id, String callerEmail) {
        Venue venue = findActiveOrThrow(id);
        checkOwnerOrAdmin(venue, callerEmail);
        venue.setActive(false);
        venueRepository.save(venue);
    }

    private Venue findActiveOrThrow(UUID id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue hittades inte: " + id));
        if (!venue.isActive()) {
            throw new ResourceNotFoundException("Venue hittades inte: " + id);
        }
        return venue;
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
